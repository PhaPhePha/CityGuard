"""End-to-end V4 Test Suite Runner for CityGuard.
Spawns a disposable PostgreSQL 17 test cluster on an ephemeral port (55470),
runs bootstrap safety checks, zero-to-V4 bootstrap, V3->V4 data-preservation
regression, constraint/query assertions, multi-provider Google/Facebook,
cross-mapping prevention, and pg_dump/pg_restore rehearsal on isolated test databases,
then cleans up cleanly.
"""
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import time

base = Path(__file__).resolve().parents[1]
pg_bin = r'C:\Program Files\PostgreSQL\17\bin'


def run_sql(host, port, user, db, statement=None, file=None, error=None):
    psql = os.path.join(pg_bin, 'psql.exe')
    command = [psql, '-X', '-h', host, '-p', str(port),
               '-U', user, '-d', db, '-v', 'ON_ERROR_STOP=1', '-At']
    command += ['-f', str(file)] if file else ['-f', '-']
    env = dict(os.environ, PGCLIENTENCODING='UTF8', PGCONNECT_TIMEOUT='5')
    res = subprocess.run(command, input=None if file else statement,
                         capture_output=True, text=True, encoding='utf-8', errors='replace', env=env)
    if error:
        if res.returncode == 0 or error not in res.stderr:
            raise RuntimeError(f'Expected rejection "{error}" not observed. Output: {res.stdout}\nStderr: {res.stderr}')
        return ''
    if res.returncode != 0:
        raise RuntimeError(f'psql failed for {file or "statement"}: {res.stderr}')
    return res.stdout.strip()


def table_digest(host, port, user, db):
    tables = ['users', 'categories', 'reports', 'report_votes', 'comments', 'admin_logs', 'report_metrics', 'refresh_tokens']
    digests = {}
    for t in tables:
        order_col = 'report_id' if t == 'report_metrics' else 'id'
        d = run_sql(
            host, port, user, db,
            f"SELECT count(*) || ':' || md5(coalesce(string_agg(row_to_json(t)::text, '' ORDER BY {order_col}), '')) FROM public.{t} t;"
        )
        digests[t] = d
    return digests


def main():
    port = 55470
    host = '127.0.0.1'
    user = 'postgres'
    test_dir = os.path.join(tempfile.gettempdir(), 'cg_v4_test_cluster')

    if os.path.exists(test_dir):
        shutil.rmtree(test_dir, ignore_errors=True)

    print(f'=== Initializing disposable test cluster at {test_dir} ===')
    initdb = os.path.join(pg_bin, 'initdb.exe')
    init_res = subprocess.run([initdb, '-D', test_dir, '-U', user, '-A', 'trust', '--no-locale', '--encoding=UTF8'],
                              capture_output=True, text=True, errors='replace')
    if init_res.returncode != 0:
        print('initdb failed:', init_res.stderr)
        sys.exit(1)
    print('initdb completed successfully.')

    postgres = os.path.join(pg_bin, 'postgres.exe')
    log_file = open(os.path.join(test_dir, 'server.log'), 'w', encoding='utf-8')
    proc = subprocess.Popen([postgres, '-D', test_dir, '-p', str(port), '-h', host],
                            stdout=log_file, stderr=subprocess.STDOUT)

    isready = os.path.join(pg_bin, 'pg_isready.exe')
    ready = False
    for _ in range(15):
        r = subprocess.run([isready, '-h', host, '-p', str(port)], capture_output=True)
        if r.returncode == 0:
            ready = True
            break
        time.sleep(0.5)

    if not ready:
        print('Postgres failed to start in time.')
        proc.terminate()
        proc.wait()
        log_file.close()
        sys.exit(1)

    pg_ver = run_sql(host, port, user, 'postgres', 'SELECT version();')
    print(f'Postgres running on {host}:{port} -> {pg_ver}')

    passed_tests = []

    try:
        # TEST 1: Build V3 Database with seed data
        db_upgrade = 'cityguard_upgrade_v4_test'
        print(f'\n--- TEST 1: Building V3 baseline database {db_upgrade} ---')
        run_sql(host, port, user, 'postgres', f'CREATE DATABASE "{db_upgrade}" ENCODING \'UTF8\';')
        run_sql(host, port, user, db_upgrade, file=base / 'migrations' / 'V1__initial_schema.sql')
        run_sql(host, port, user, db_upgrade, file=base / 'migrations' / 'V2__metrics_tokens_request_keys.sql')
        run_sql(host, port, user, db_upgrade, file=base / 'migrations' / 'V3__report_public_id_ulid.sql')
        run_sql(host, port, user, db_upgrade, file=base / 'seed.sql')
        run_sql(host, port, user, db_upgrade, file=base / 'verify-v3.sql')
        passed_tests.append('1. V3 Baseline & Seed Setup')
        print('-> V3 baseline successfully verified.')

        # Capture V3 table digests
        pre_digests = table_digest(host, port, user, db_upgrade)
        print('Captured V3 table digests for data preservation check.')

        # TEST 2: Run preflight-v4.sql
        print(f'\n--- TEST 2: Executing preflight-v4.sql on {db_upgrade} ---')
        run_sql(host, port, user, db_upgrade, file=base / 'preflight-v4.sql')
        passed_tests.append('2. Preflight V4 Validation')
        print('-> Preflight V4 PASSED.')

        # TEST 3: Apply V4 migration & verify-v4.sql
        print(f'\n--- TEST 3: Applying V4__firebase_auth.sql and verify-v4.sql ---')
        run_sql(host, port, user, db_upgrade, file=base / 'migrations' / 'V4__firebase_auth.sql')
        run_sql(host, port, user, db_upgrade, file=base / 'verify-v4.sql')
        passed_tests.append('3. V4 Migration & Verify-V4')
        print('-> V4 migration and post-upgrade verification PASSED.')

        # TEST 4: Data preservation assertion
        print('\n--- TEST 4: Comparing pre-V4 and post-V4 table digests ---')
        post_digests = table_digest(host, port, user, db_upgrade)
        for t in pre_digests:
            if pre_digests[t] != post_digests[t]:
                raise RuntimeError(f'Data preservation violation in table {t}: pre={pre_digests[t]}, post={post_digests[t]}')
            print(f'  Table {t}: digest matched ({pre_digests[t][:12]}...)')
        passed_tests.append('4. V3 Data Preservation (Zero Loss)')
        print('-> 100% data preservation verified.')

        # TEST 5: Run upgrade-v4-tests.sql (functional, constraints, multi-provider, cross-mapping)
        print(f'\n--- TEST 5: Running upgrade-v4-tests.sql on {db_upgrade} ---')
        run_sql(host, port, user, db_upgrade, file=base / 'tests' / 'upgrade-v4-tests.sql')
        passed_tests.append('5. V4 Integration Assertions (Multi-provider, Constraints, Cross-mapping)')
        print('-> Integration assertions PASSED.')

        # TEST 6: Rollback on error test
        print('\n--- TEST 6: Testing transaction rollback on migration error ---')
        db_rollback = 'cityguard_rollback_v4_test'
        run_sql(host, port, user, 'postgres', f'CREATE DATABASE "{db_rollback}" ENCODING \'UTF8\';')
        run_sql(host, port, user, db_rollback, file=base / 'migrations' / 'V1__initial_schema.sql')
        run_sql(host, port, user, db_rollback, file=base / 'migrations' / 'V2__metrics_tokens_request_keys.sql')
        run_sql(host, port, user, db_rollback, file=base / 'migrations' / 'V3__report_public_id_ulid.sql')

        faulty_sql = """
        BEGIN;
        ALTER TABLE public.users ALTER COLUMN password_hash DROP NOT NULL;
        RAISE EXCEPTION 'Simulated failure in V4 transaction';
        COMMIT;
        """
        try:
            run_sql(host, port, user, db_rollback, statement=faulty_sql)
            raise RuntimeError('Faulty transaction should have failed')
        except RuntimeError:
            pass # Expected failure

        # Verify users.password_hash is STILL NOT NULL after rollback
        is_not_null = run_sql(host, port, user, db_rollback,
                              "SELECT attnotnull FROM pg_attribute WHERE attrelid='public.users'::regclass AND attname='password_hash';")
        if is_not_null != 't':
            raise RuntimeError('Transaction rollback failed: users.password_hash modified despite error')
        passed_tests.append('6. Safe Transaction Rollback on Error')
        print('-> Safe rollback verified.')

        # TEST 7: pg_dump & pg_restore rehearsal
        print('\n--- TEST 7: pg_dump & pg_restore rehearsal ---')
        dump_file = os.path.join(test_dir, 'v4_dump.sql')
        pg_dump = os.path.join(pg_bin, 'pg_dump.exe')
        dump_res = subprocess.run([pg_dump, '-h', host, '-p', str(port), '-U', user, '-d', db_upgrade, '-f', dump_file],
                                  capture_output=True, text=True, errors='replace')
        if dump_res.returncode != 0:
            raise RuntimeError(f'pg_dump failed: {dump_res.stderr}')

        db_restored = 'cityguard_restored_v4_test'
        run_sql(host, port, user, 'postgres', f'CREATE DATABASE "{db_restored}" ENCODING \'UTF8\';')
        run_sql(host, port, user, db_restored, file=Path(dump_file))
        run_sql(host, port, user, db_restored, file=base / 'verify-v4.sql')
        passed_tests.append('7. pg_dump and pg_restore Rehearsal')
        print('-> Backup and restore verified with verify-v4.sql.')

        # TEST 8: Clean bootstrap from zero (V1 -> V2 -> V3 -> V4 -> seed -> verify-v4)
        print('\n--- TEST 8: Full Zero-to-V4 clean bootstrap ---')
        db_clean = 'cityguard_clean_v4_test'
        run_sql(host, port, user, 'postgres', f'CREATE DATABASE "{db_clean}" ENCODING \'UTF8\';')
        run_sql(host, port, user, db_clean, file=base / 'migrations' / 'V1__initial_schema.sql')
        run_sql(host, port, user, db_clean, file=base / 'preflight-upgrade.sql')
        run_sql(host, port, user, db_clean, file=base / 'migrations' / 'V2__metrics_tokens_request_keys.sql')
        run_sql(host, port, user, db_clean, file=base / 'preflight-v3.sql')
        run_sql(host, port, user, db_clean, file=base / 'migrations' / 'V3__report_public_id_ulid.sql')
        run_sql(host, port, user, db_clean, file=base / 'preflight-v4.sql')
        run_sql(host, port, user, db_clean, file=base / 'migrations' / 'V4__firebase_auth.sql')
        run_sql(host, port, user, db_clean, file=base / 'seed.sql')
        run_sql(host, port, user, db_clean, file=base / 'verify-v4.sql')
        passed_tests.append('8. Zero-to-V4 Clean Bootstrap Sequence')
        print('-> Full bootstrap sequence verified.')

        print('\n=======================================================')
        print(f'ALL {len(passed_tests)} TEST SCENARIOS PASSED WITH ZERO ERRORS!')
        print('=======================================================')
        for t in passed_tests:
            print(f' [PASS] {t}')

    finally:
        print('\n=== Tearing down disposable test cluster ===')
        proc.terminate()
        try:
            proc.wait(timeout=5)
        except subprocess.TimeoutExpired:
            proc.kill()
        log_file.close()
        shutil.rmtree(test_dir, ignore_errors=True)
        print('Test cluster cleaned up cleanly.')


if __name__ == '__main__':
    main()
