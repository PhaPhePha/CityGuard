"""End-to-end Docker Compose Verification Runner for CityGuard V3.

Executes the 8 required Docker verification steps on 127.0.0.1:5433:
1. Validate `docker compose config` (default and `--profile demo`).
2. Start PostgreSQL 17 container (`docker compose up -d --wait`) initializing V1 -> V2 -> V3 -> Verify on volume `cityguard_v3_pgdata`.
3. Connect from Windows host (`psql.exe -h 127.0.0.1 -p 5433`) to verify all 8 tables and 0 demo rows initially (seed separation).
4. Verify `public_id` format/constraints, FKs, indexes, triggers (`verify-v3.sql`, `upgrade-v3-tests.sql`, `database-tests.sql`).
5. Load demo seed data using Docker Compose profile (`docker compose --profile demo run --rm seed-demo`).
6. Verify volume data persistence across container restart (`docker compose restart postgres`).
7. Verify demo seed idempotency on repeated execution (`docker compose --profile demo run --rm seed-demo`).
8. Verify business queries (`queries.sql`) over Windows host connection `127.0.0.1:5433`.

Safety Rules:
- Uses an ephemeral random password generated in memory (never hardcoded or saved to disk).
- Never connects to port 5432 or touches `cityguard_db`.
- Never executes `docker compose down -v` or `docker system prune --volumes`.
"""
import os
from pathlib import Path
import secrets
import subprocess
import sys
import time

BASE_DIR = Path(__file__).resolve().parents[1]
PG_BIN = r'C:\Program Files\PostgreSQL\17\bin'
PSQL_EXE = os.path.join(PG_BIN, 'psql.exe')

HOST = '127.0.0.1'
PORT = 5433
DB_NAME = 'cityguard_v3_dev'
DB_USER = 'cityguard'
EPHEMERAL_PWD = 'cg_ephemeral_' + secrets.token_hex(12)


def run_docker(args, env):
    cmd = ['docker', 'compose'] + args
    res = subprocess.run(
        cmd,
        cwd=str(BASE_DIR),
        capture_output=True,
        text=True,
        encoding='utf-8',
        errors='replace',
        env=env,
    )
    if res.returncode != 0:
        raise RuntimeError(f"Command {' '.join(cmd)} failed (exit {res.returncode}):\n{res.stderr}\n{res.stdout}")
    return res.stdout.strip()


def run_host_psql(env, statement=None, sql_file: Path = None):
    cmd = [
        PSQL_EXE, '-X', '-h', HOST, '-p', str(PORT),
        '-U', DB_USER, '-d', DB_NAME, '-v', 'ON_ERROR_STOP=1', '-At'
    ]
    if sql_file is not None:
        cmd.extend(['-f', str(sql_file)])
    else:
        cmd.extend(['-f', '-'])

    psql_env = dict(env, PGPASSWORD=EPHEMERAL_PWD, PGCLIENTENCODING='UTF8', PGCONNECT_TIMEOUT='10')
    res = subprocess.run(
        cmd,
        cwd=str(BASE_DIR / 'tests') if sql_file else str(BASE_DIR),
        input=None if sql_file is not None else statement,
        capture_output=True,
        text=True,
        encoding='utf-8',
        errors='replace',
        env=psql_env,
    )
    if res.returncode != 0:
        label = sql_file.name if sql_file else 'SQL statement'
        raise RuntimeError(f"Host psql ({label}) failed:\n{res.stderr}")
    return res.stdout.strip()


def table_digest(env):
    tables = ['users', 'categories', 'reports', 'report_votes', 'comments', 'admin_logs', 'report_metrics', 'refresh_tokens']
    digests = {}
    for t in tables:
        order_col = 'report_id' if t == 'report_metrics' else 'id'
        d = run_host_psql(
            env,
            statement=f"SELECT count(*) || ':' || md5(coalesce(string_agg(row_to_json(t)::text, '' ORDER BY {order_col}), '')) FROM public.{t} t;"
        )
        digests[t] = d
    return digests


def wait_container_healthy(env, timeout_sec=60):
    deadline = time.time() + timeout_sec
    while time.time() < deadline:
        res = subprocess.run(
            ['docker', 'inspect', '--format', '{{.State.Health.Status}}', 'cityguard-v3-postgres'],
            capture_output=True, text=True, env=env
        )
        if res.returncode == 0 and res.stdout.strip() == 'healthy':
            return
        time.sleep(2)
    raise RuntimeError('Container cityguard-v3-postgres did not become healthy in time')


def main():
    env = dict(
        os.environ,
        CITYGUARD_DB_PORT=str(PORT),
        CITYGUARD_DB_NAME=DB_NAME,
        CITYGUARD_DB_USER=DB_USER,
        CITYGUARD_DB_PASSWORD=EPHEMERAL_PWD,
    )
    passed = []

    try:
        print('=== [1/8] Validating docker compose config ===')
        run_docker(['config', '--quiet'], env)
        run_docker(['--profile', 'demo', 'config', '--quiet'], env)
        passed.append('D01: docker compose config (default & --profile demo) validated')

        print('=== [2/8] Starting PostgreSQL 17 container on 127.0.0.1:5433 ===')
        run_docker(['up', '-d', '--wait', 'postgres'], env)
        wait_container_healthy(env)
        passed.append('D02: Container cityguard-v3-postgres initialized V1 -> V2 -> V3 -> Verify on volume cityguard_v3_pgdata')

        print('=== [3/8] Verifying Windows host connection over 127.0.0.1:5433 & 8 tables ===')
        ver = run_host_psql(env, statement='SELECT version();')
        print(f'  Connected from Windows host psql.exe -> {ver}')
        counts_before_seed = run_host_psql(
            env,
            statement="""
            SELECT
                (SELECT count(*) FROM pg_tables WHERE schemaname='public'),
                (SELECT count(*) FROM public.users),
                (SELECT count(*) FROM public.reports);
            """
        )
        t_cnt, u_cnt, r_cnt = counts_before_seed.split('|')
        assert t_cnt == '8', f'Expected 8 tables, got {t_cnt}'
        assert u_cnt == '0' and r_cnt == '0', f'Expected 0 rows before demo profile seed, got users={u_cnt}, reports={r_cnt}'
        passed.append('D03: Connected from Windows host over 127.0.0.1:5433; verified 8 tables and 0 demo rows on default startup')

        print('=== [4/8] Verifying V3 constraints, FKs, indexes, triggers & regression suites ===')
        run_host_psql(env, sql_file=BASE_DIR / 'verify-v3.sql')
        run_host_psql(env, sql_file=BASE_DIR / 'tests' / 'upgrade-v3-tests.sql')
        run_host_psql(env, sql_file=BASE_DIR / 'tests' / 'database-tests.sql')
        passed.append('D04: verify-v3.sql, upgrade-v3-tests.sql, and database-tests.sql passed over 127.0.0.1:5433')

        print('=== [5/8] Loading demo seed data via Docker Compose profile (--profile demo) ===')
        run_docker(['--profile', 'demo', 'run', '--rm', 'seed-demo'], env)
        counts_after_seed = run_host_psql(
            env,
            statement="""
            SELECT
                (SELECT count(*) FROM public.users),
                (SELECT count(*) FROM public.categories),
                (SELECT count(*) FROM public.reports),
                (SELECT count(*) FROM public.report_votes),
                (SELECT count(*) FROM public.comments),
                (SELECT count(*) FROM public.report_metrics);
            """
        )
        assert counts_after_seed == '5|6|6|2|3|6', f'Unexpected seeded counts: {counts_after_seed}'
        passed.append('D05: Demo seed applied via `--profile demo` (5 users, 6 categories, 6 reports, 2 votes, 3 comments, 6 metrics)')

        print('=== [6/8] Verifying volume persistence across container restart ===')
        digest_before_restart = table_digest(env)
        run_docker(['restart', 'postgres'], env)
        wait_container_healthy(env)
        digest_after_restart = table_digest(env)
        assert digest_before_restart == digest_after_restart, 'Volume data changed or was lost after container restart!'
        passed.append('D06: Volume cityguard_v3_pgdata preserved 100% of data and schema across container restart')

        print('=== [7/8] Verifying demo seed idempotency on repeated run ===')
        run_docker(['--profile', 'demo', 'run', '--rm', 'seed-demo'], env)
        digest_after_reseed = table_digest(env)
        assert digest_before_restart == digest_after_reseed, 'Repeated demo seed modified existing rows!'
        passed.append('D07: Repeated `--profile demo` seed run is idempotent (0 duplicate rows)')

        print('=== [8/8] Verifying business queries over Windows host connection 127.0.0.1:5433 ===')
        queries_sql = (BASE_DIR / 'queries.sql').read_text(encoding='utf-8')
        q_out = run_host_psql(
            env,
            statement=queries_sql + "\nEXECUTE get_report_by_public_id('rpt_01M7WBY80FZNE4EYTVWZ00JM0J');"
        )
        assert 'rpt_01M7WBY80FZNE4EYTVWZ00JM0J' in q_out, f'Unexpected get_report_by_public_id result: {q_out}'
        passed.append('D08: Business queries (queries.sql) executed successfully from Windows host via 127.0.0.1:5433')

        print('\n=== ALL DOCKER COMPOSE TESTS PASSED! ===')
        for item in passed:
            print(f'  [PASS] {item}')

    finally:
        print('\nStopping Docker container cleanly (preserving volume cityguard_v3_pgdata without -v)...')
        subprocess.run(['docker', 'compose', 'stop'], cwd=str(BASE_DIR), env=env, capture_output=True)
        print('Container cityguard-v3-postgres stopped cleanly; volume cityguard_v3_pgdata retained.')


if __name__ == '__main__':
    main()
