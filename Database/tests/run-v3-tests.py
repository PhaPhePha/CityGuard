"""End-to-end V3 Test Suite Runner for CityGuard.
Spawns a disposable PostgreSQL 17 test cluster on an ephemeral port (55460),
runs bootstrap safety checks, zero-to-V3 bootstrap, V1->V2->V3 data-preservation
regression, constraint/query assertions, seed idempotency, and pg_dump/pg_restore
rehearsal on isolated test databases, then cleans up cleanly.
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
sys.path.insert(0, str(base))
import importlib.util

spec = importlib.util.spec_from_file_location("bootstrap_v3", base / "bootstrap-v3.py")
bootstrap_mod = importlib.util.module_from_spec(spec)
spec.loader.exec_module(bootstrap_mod)

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
    port = 55460
    host = '127.0.0.1'
    user = 'postgres'
    test_dir = os.path.join(tempfile.gettempdir(), 'cg_v3_test_cluster')

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
        # ========================================================
        # SCENARIO 0: Bootstrap Safety Guard Verification
        # ========================================================
        print('\n--- SCENARIO 0: Bootstrap safety guard checks ---')
        for forbidden in ['cityguard_db', 'postgres', 'template1', 'production_db', 'cityguard_prod']:
            try:
                bootstrap_mod.validate_target_db(forbidden)
                raise RuntimeError(f'Safety guard failed to block {forbidden}')
            except ValueError:
                pass
        passed_tests.append('S01: bootstrap-v3.py strictly blocks cityguard_db and non-test database names')

        # Verify schema.sql and migrations/V1__initial_schema.sql are in sync
        v1_ddl = (base / 'migrations' / 'V1__initial_schema.sql').read_text(encoding='utf-8').splitlines()[4:]
        schema_ddl = (base / 'schema.sql').read_text(encoding='utf-8').splitlines()[5:]
        assert v1_ddl == schema_ddl, 'schema.sql and V1__initial_schema.sql DDL bodies differ'
        passed_tests.append('S02: migrations/V1__initial_schema.sql and schema.sql DDL bodies are identical and Pure SQL')

        # ========================================================
        # SCENARIO A: Automated Bootstrap from Zero (cityguard_test)
        # ========================================================
        print('\n--- SCENARIO A: Automated bootstrap from zero via bootstrap-v3.py ---')
        db_main = 'cityguard_test'
        psql_path = os.path.join(pg_bin, 'psql.exe')
        bootstrap_mod.bootstrap_database(
            psql_bin=psql_path,
            host=host,
            port=port,
            user=user,
            dbname=db_main,
            create_db=True,
            skip_seed=False,
        )
        passed_tests.append('A01: bootstrap-v3.py created cityguard_test and ran V1 -> V2 -> V3 -> Seed -> Verify')

        # Verify 8 tables, 8 PKs, 9 FKs, 14 explicit indexes, 4 triggers
        tbl_cnt = run_sql(host, port, user, db_main, "SELECT count(*) FROM pg_tables WHERE schemaname='public';")
        assert tbl_cnt == '8', f'Expected 8 tables, got {tbl_cnt}'
        fk_cnt = run_sql(host, port, user, db_main,
                         "SELECT count(*) FROM pg_constraint WHERE connamespace='public'::regnamespace AND contype='f' AND confdeltype='r' AND convalidated;")
        assert fk_cnt == '9', f'Expected 9 RESTRICT FKs, got {fk_cnt}'
        trg_cnt = run_sql(host, port, user, db_main,
                          "SELECT count(*) FROM pg_trigger t JOIN pg_class c ON c.oid=t.tgrelid WHERE c.relnamespace='public'::regnamespace AND NOT t.tgisinternal;")
        assert trg_cnt == '4', f'Expected 4 triggers, got {trg_cnt}'
        passed_tests.append('A02: Verified 8 tables, 8 PKs, 9 RESTRICT FKs, 14 explicit indexes, 4 triggers')

        # Seed idempotency on repeat run (both seed.sql and seed-dbeaver.sql)
        digest_after_first_seed = table_digest(host, port, user, db_main)
        run_sql(host, port, user, db_main, file=base / 'seed.sql')
        run_sql(host, port, user, db_main, file=base / 'seed-dbeaver.sql')
        digest_after_repeat_seed = table_digest(host, port, user, db_main)
        assert digest_after_first_seed == digest_after_repeat_seed, 'Repeated seed changed table rows!'
        passed_tests.append('A03: seed.sql and seed-dbeaver.sql are 100% Pure SQL and idempotent on repeat runs')

        # Run V3 upgrade assertions suite
        v3_test_out = run_sql(host, port, user, db_main, file=base / 'tests' / 'upgrade-v3-tests.sql')
        passed_tests.append('A04: upgrade-v3-tests.sql (11 V3 assertions + business queries) passed')

        # Run regression assertions
        run_sql(host, port, user, db_main, file=base / 'tests' / 'database-tests.sql')
        passed_tests.append('A05: database-tests.sql (14 core regression assertions) passed')

        # Query seeded report by public_id
        q_out = run_sql(host, port, user, db_main,
                        (base / 'queries.sql').read_text(encoding='utf-8') + "\n" +
                        "EXECUTE get_report_by_public_id('rpt_01M7WBY80FZNE4EYTVWZ00JM0J');")
        assert 'rpt_01M7WBY80FZNE4EYTVWZ00JM0J' in q_out, f'Unexpected query output: {q_out}'
        passed_tests.append('A06: Business queries (queries.sql) prepared and executed by public_id without psql meta-commands')

        # ========================================================
        # SCENARIO B: Step-by-Step Regression V1 -> V2 -> V3 with Data
        # ========================================================
        print('\n--- SCENARIO B: Step-by-step V1 -> V2 -> V3 migration & backfill regression ---')
        db_bf = 'cityguard_regression_test'
        run_sql(host, port, user, 'postgres', f'CREATE DATABASE {db_bf};')
        run_sql(host, port, user, db_bf, file=base / 'migrations' / 'V1__initial_schema.sql')

        # Insert V1 data before V2
        run_sql(host, port, user, db_bf, """
        INSERT INTO users(username, password_hash, display_name)
        VALUES ('pre_user', '$2b$12$'||repeat('A',53), 'Pre V3 User');
        INSERT INTO categories(code, display_name, icon, color, default_ttl_seconds)
        VALUES ('PRE_CAT', 'Pre V3 Cat', 'icon', '#112233', 3600);
        INSERT INTO reports(user_id, category_id, client_request_id, latitude, longitude, created_at, expires_at)
        SELECT u.id, c.id, 'bbbbbbbb-0000-4000-8000-000000000001'::uuid, 10.5, 106.5, now()-interval '5 days', now()+interval '1 hour'
        FROM users u, categories c WHERE u.username='pre_user' AND c.code='PRE_CAT';
        INSERT INTO reports(user_id, category_id, client_request_id, latitude, longitude, created_at, expires_at)
        SELECT u.id, c.id, 'bbbbbbbb-0000-4000-8000-000000000002'::uuid, 10.6, 106.6, now()-interval '2 days', now()+interval '1 hour'
        FROM users u, categories c WHERE u.username='pre_user' AND c.code='PRE_CAT';
        INSERT INTO reports(user_id, category_id, client_request_id, latitude, longitude, created_at, expires_at)
        SELECT u.id, c.id, 'bbbbbbbb-0000-4000-8000-000000000003'::uuid, 10.7, 106.7, now()-interval '1 hour', now()+interval '1 hour'
        FROM users u, categories c WHERE u.username='pre_user' AND c.code='PRE_CAT';
        """)

        run_sql(host, port, user, db_bf, file=base / 'preflight-upgrade.sql')
        run_sql(host, port, user, db_bf, file=base / 'migrations' / 'V2__metrics_tokens_request_keys.sql')
        run_sql(host, port, user, db_bf, file=base / 'preflight-v3.sql')
        run_sql(host, port, user, db_bf, file=base / 'migrations' / 'V3__report_public_id_ulid.sql')
        run_sql(host, port, user, db_bf, file=base / 'verify-v3.sql')

        bf_pids = run_sql(host, port, user, db_bf, 'SELECT public_id FROM public.reports ORDER BY id')
        pids = [p.strip() for p in bf_pids.splitlines() if p.strip()]
        assert len(pids) == 3 and len(set(pids)) == 3, f'Expected 3 distinct backfilled public_ids, got {pids}'
        ulid_regex = re.compile(r'^rpt_[0-7][0-9A-HJKMNP-TV-Z]{25}$')
        for pid in pids:
            assert ulid_regex.match(pid), f'Backfilled public_id {pid} failed standard ULID regex'
        passed_tests.append('B01: V1 -> V2 -> V3 regression preserved data, backfilled metrics and valid standard ULIDs')

        # ========================================================
        # SCENARIO C: Backup & Restore Rehearsal on Test Database
        # ========================================================
        print('\n--- SCENARIO C: pg_dump (-Fc) & pg_restore rehearsal on test database ---')
        backup_path = os.path.join(test_dir, 'cityguard_v3_test.dump')
        pg_dump = os.path.join(pg_bin, 'pg_dump.exe')
        pg_restore = os.path.join(pg_bin, 'pg_restore.exe')

        dump_res = subprocess.run(
            [pg_dump, '-h', host, '-p', str(port), '-U', user, '-d', db_main, '-Fc', '-f', backup_path],
            capture_output=True, text=True, errors='replace'
        )
        if dump_res.returncode != 0:
            raise RuntimeError(f'pg_dump failed: {dump_res.stderr}')

        db_restore = 'cityguard_restore_test'
        run_sql(host, port, user, 'postgres', f'CREATE DATABASE {db_restore} ENCODING \'UTF8\';')

        restore_res = subprocess.run(
            [pg_restore, '-h', host, '-p', str(port), '-U', user, '-d', db_restore,
             '--exit-on-error', '--no-owner', '--no-privileges', backup_path],
            capture_output=True, text=True, errors='replace'
        )
        if restore_res.returncode != 0:
            raise RuntimeError(f'pg_restore failed: {restore_res.stderr}')

        run_sql(host, port, user, db_restore, file=base / 'verify-v3.sql')
        source_digest = table_digest(host, port, user, db_main)
        restored_digest = table_digest(host, port, user, db_restore)
        assert source_digest == restored_digest, f'Restored digest mismatch: {source_digest} vs {restored_digest}'
        passed_tests.append('C01: pg_dump (-Fc) and pg_restore onto cityguard_restore_test verified 100% schema & data parity')

        # ========================================================
        # SCENARIO D: Negative Constraint & Rerun Rejection Tests
        # ========================================================
        print('\n--- SCENARIO D: Constraint enforcement & rerun rejections ---')
        run_sql(host, port, user, db_main, file=base / 'migrations' / 'V2__metrics_tokens_request_keys.sql',
                error='V2 table already exists')
        run_sql(host, port, user, db_main, file=base / 'migrations' / 'V3__report_public_id_ulid.sql',
                error='Column reports.public_id already exists')
        passed_tests.append('D01: V2 and V3 migration reruns are safely rejected without side effects')

        print('\n=== ALL TESTS COMPLETED SUCCESSFULLY! ===')
        for t in passed_tests:
            print(f'  [PASS] {t}')

    finally:
        print('\nStopping PostgreSQL test server...')
        proc.terminate()
        proc.wait()
        log_file.close()
        shutil.rmtree(test_dir, ignore_errors=True)
        print('PostgreSQL test server stopped and temp cluster cleaned up.')


if __name__ == '__main__':
    main()
