"""CityGuard V3 Automated Test Database Bootstrap Runner.

Initializes an approved, empty test database from zero:
  Empty Test Database -> V1 -> Preflight V2 -> V2 -> Preflight V3 -> V3 -> Seed Demo -> Verification

Safety Rules:
- Strictly refuses 'cityguard_db' and PostgreSQL system databases ('postgres', 'template0', 'template1').
- Only allows database names matching '^cityguard_[a-z0-9_]*test[a-z0-9_]*$'.
- Never contains hardcoded passwords; uses environment variables, .pgpass, or interactive psql prompt.
- Stops immediately on the first migration or verification error.
"""
import argparse
import os
from pathlib import Path
import re
import subprocess
import sys

ALLOWED_DB_PATTERN = re.compile(r'^cityguard_[a-z0-9_]*test[a-z0-9_]*$')
FORBIDDEN_DBS = {'cityguard_db', 'postgres', 'template0', 'template1'}

BASE_DIR = Path(__file__).resolve().parent


def validate_target_db(dbname: str) -> None:
    if dbname.lower() in FORBIDDEN_DBS:
        raise ValueError(
            f"SAFETY BLOCK: Refusing to run bootstrap on protected database '{dbname}'. "
            "Only dedicated test databases (e.g. 'cityguard_test', 'cityguard_v3_test') are allowed."
        )
    if not ALLOWED_DB_PATTERN.match(dbname):
        raise ValueError(
            f"SAFETY BLOCK: Database name '{dbname}' is outside the allowed test naming pattern "
            f"'{ALLOWED_DB_PATTERN.pattern}'."
        )


def run_psql(psql_bin: str, host: str, port: int, user: str, dbname: str,
             statement: str = None, sql_file: Path = None) -> str:
    cmd = [
        psql_bin, '-X', '-h', host, '-p', str(port),
        '-U', user, '-d', dbname, '-v', 'ON_ERROR_STOP=1', '-At'
    ]
    if sql_file is not None:
        cmd.extend(['-f', str(sql_file)])
    else:
        cmd.extend(['-f', '-'])

    env = dict(os.environ, PGCLIENTENCODING='UTF8', PGCONNECT_TIMEOUT='10')
    res = subprocess.run(
        cmd,
        input=None if sql_file is not None else statement,
        capture_output=True,
        text=True,
        encoding='utf-8',
        errors='replace',
        env=env,
    )
    if res.returncode != 0:
        target_label = sql_file.name if sql_file else 'SQL check'
        raise RuntimeError(f"[{target_label}] failed (exit {res.returncode}):\n{res.stderr.strip()}")
    return res.stdout.strip()


def bootstrap_database(psql_bin: str, host: str, port: int, user: str, dbname: str,
                       create_db: bool = False, skip_seed: bool = False) -> None:
    validate_target_db(dbname)

    print(f"=== CityGuard V3 Bootstrap Target: {dbname} ({host}:{port}, user={user}) ===")

    if create_db:
        exists = run_psql(
            psql_bin, host, port, user, 'postgres',
            statement=f"SELECT count(*) FROM pg_database WHERE datname = '{dbname}';"
        )
        if exists != '0':
            raise RuntimeError(
                f"Test database '{dbname}' already exists. Refusing to drop or overwrite it."
            )
        print(f"[0/7] Creating fresh test database '{dbname}'...")
        run_psql(psql_bin, host, port, user, 'postgres', statement=f'CREATE DATABASE "{dbname}" ENCODING \'UTF8\';')

    # Verify target database is empty before applying V1
    existing_tables = run_psql(
        psql_bin, host, port, user, dbname,
        statement="SELECT count(*) FROM pg_tables WHERE schemaname = 'public';"
    )
    if existing_tables != '0':
        raise RuntimeError(
            f"Target database '{dbname}' is not empty (found {existing_tables} table(s) in schema 'public'). "
            "Bootstrap from zero requires an empty test database."
        )

    steps = [
        ('[1/7] Applying V1 Initial Schema', BASE_DIR / 'migrations' / 'V1__initial_schema.sql'),
        ('[2/7] Running Preflight V1 -> V2', BASE_DIR / 'preflight-upgrade.sql'),
        ('[3/7] Applying V2 Migration (metrics, tokens, request keys)', BASE_DIR / 'migrations' / 'V2__metrics_tokens_request_keys.sql'),
        ('[4/7] Running Preflight V2 -> V3', BASE_DIR / 'preflight-v3.sql'),
        ('[5/7] Applying V3 Migration (reports.public_id ULID)', BASE_DIR / 'migrations' / 'V3__report_public_id_ulid.sql'),
    ]

    if not skip_seed:
        steps.append(('[6/7] Applying Demo Seed Data (Pure SQL)', BASE_DIR / 'seed.sql'))

    steps.append(('[7/7] Running Post-Initialization Schema & Constraint Verification', BASE_DIR / 'verify-v3.sql'))

    for label, path in steps:
        if not path.exists():
            raise FileNotFoundError(f"Missing required SQL file: {path}")
        print(f"{label} ({path.name})...")
        run_psql(psql_bin, host, port, user, dbname, sql_file=path)

    summary = run_psql(
        psql_bin, host, port, user, dbname,
        statement="""
        SELECT
            (SELECT count(*) FROM pg_tables WHERE schemaname = 'public') AS tables,
            (SELECT count(*) FROM public.users) AS users,
            (SELECT count(*) FROM public.categories) AS categories,
            (SELECT count(*) FROM public.reports) AS reports,
            (SELECT count(*) FROM public.report_votes) AS votes,
            (SELECT count(*) FROM public.comments) AS comments,
            (SELECT count(*) FROM public.report_metrics) AS metrics;
        """
    )
    t_cnt, u_cnt, c_cnt, r_cnt, v_cnt, cm_cnt, m_cnt = summary.split('|')
    print(
        f"=== Bootstrap SUCCEEDED on '{dbname}' ===\n"
        f"  Tables={t_cnt} | Users={u_cnt} | Categories={c_cnt} | Reports={r_cnt} | "
        f"Votes={v_cnt} | Comments={cm_cnt} | Metrics={m_cnt}"
    )


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('--psql', default=os.environ.get('PG_PSQL_BIN', r'C:\Program Files\PostgreSQL\17\bin\psql.exe'),
                        help='Path to psql executable')
    parser.add_argument('--host', default=os.environ.get('PGHOST', '127.0.0.1'),
                        help='PostgreSQL host (default: 127.0.0.1)')
    parser.add_argument('--port', type=int, default=int(os.environ.get('PGPORT', '55460')),
                        help='PostgreSQL port (default: 55460)')
    parser.add_argument('--user', default=os.environ.get('PGUSER', 'postgres'),
                        help='PostgreSQL user')
    parser.add_argument('--dbname', default=os.environ.get('PGDATABASE', 'cityguard_v3_test'),
                        help="Target test database name (must match '^cityguard_[a-z0-9_]*test[a-z0-9_]*$')")
    parser.add_argument('--create-db', action='store_true',
                        help='Create the target test database if it does not exist yet')
    parser.add_argument('--skip-seed', action='store_true',
                        help='Skip loading demo seed data after V3 migration')
    args = parser.parse_args()

    try:
        bootstrap_database(
            psql_bin=args.psql,
            host=args.host,
            port=args.port,
            user=args.user,
            dbname=args.dbname,
            create_db=args.create_db,
            skip_seed=args.skip_seed,
        )
    except Exception as exc:
        print(f"\nBOOTSTRAP ABORTED: {exc}", file=sys.stderr)
        sys.exit(1)


if __name__ == '__main__':
    main()
