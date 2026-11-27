"""Disposable PostgreSQL 17 cluster ONLY. Creates fresh test databases; never drops any.

python database/tests/run-upgrade-tests.py --psql PATH --host 127.0.0.1 --port 55449 --user OWNER
Refuses port 5432. This runner does not start/stop a server or read production credentials.
"""
import argparse
import os
from pathlib import Path
import re
import secrets
import subprocess
from concurrent.futures import ThreadPoolExecutor

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--psql', required=True)
parser.add_argument('--host', choices=['127.0.0.1', 'localhost'], default='127.0.0.1')
parser.add_argument('--port', type=int, required=True)
parser.add_argument('--user', required=True)
parser.add_argument('--resume-v2-main', action='store_true',
                    help='Diagnostic only: reuse existing disposable cityguard_test already migrated to V2; does not reset/drop it')
args = parser.parse_args()
if args.port == 5432:
    parser.error('Refusing normal local development port 5432; use a disposable test cluster')
base = Path(__file__).resolve().parents[1]


def sql(db, statement=None, file=None, error=None):
    command = [args.psql, '-X', '-w', '-h', args.host, '-p', str(args.port),
               '-U', args.user, '-d', db, '-v', 'ON_ERROR_STOP=1', '-At']
    # Windows argv may transcode non-ASCII SQL. Send statements as UTF-8 stdin.
    command += ['-f', str(file)] if file else ['-f', '-']
    environment = dict(os.environ, PGCLIENTENCODING='UTF8', PGCONNECT_TIMEOUT='5')
    result = subprocess.run(command, input=None if file else statement,
                            capture_output=True, text=True, encoding='utf-8', env=environment)
    if error:
        if result.returncode == 0 or error not in result.stderr:
            raise RuntimeError('Expected rejection not observed: ' + error)
        return ''
    if result.returncode:
        # Redact fixture hashes before surfacing PostgreSQL diagnostics.
        diagnostic = re.sub(r'\$2[aby]\$\d{2}\$[./A-Za-z0-9]{53}', '[HASH_REDACTED]', result.stderr)
        diagnostic = re.sub(r'\b[0-9a-f]{64}\b', '[DIGEST_REDACTED]', diagnostic)
        raise RuntimeError('psql failed for ' + str(file or 'test statement') +
                           ': ' + diagnostic)
    return result.stdout.strip()


def fresh(db):
    if sql('postgres', "SELECT count(*) FROM pg_database WHERE datname='" + db + "'") != '0':
        raise RuntimeError('Refusing to overwrite existing test database ' + db)
    sql('postgres', 'CREATE DATABASE ' + db)
    sql(db, file=base / 'schema.sql')


def snapshot(db):
    return [sql(db, 'SELECT count(*),md5(coalesce(string_agg(row_to_json(t)::text,\'\' ORDER BY id),\'\')) '
                    'FROM public.' + table + ' t')
            for table in ['users', 'categories', 'reports', 'report_votes', 'comments', 'admin_logs']]


def fixture(db):
    # Reuse real demo hashes without printing them; no plaintext password fixture.
    seed = (base / 'seed.sql').read_text(encoding='utf-8')
    for table in ['categories', 'users']:
        section = re.search(r'INSERT INTO ' + table + r'\b.*?ON CONFLICT.*?;', seed, re.S)
        if not section:
            raise RuntimeError('Seed fixture section not found: ' + table)
        sql(db, section.group(0))
    sql(db, """
      INSERT INTO reports(user_id,category_id,client_request_id,description,latitude,longitude,expires_at)
      SELECT u.id,c.id,'aaaaaaaa-0000-4000-8000-000000000001','V1 preserved fixture',10.77,106.7,now()+interval '1 hour'
      FROM users u,categories c WHERE u.username='demo_user_a' AND c.code='TRAFFIC_JAM';
      INSERT INTO reports(user_id,category_id,client_request_id,latitude,longitude,expires_at)
      SELECT u.id,c.id,'aaaaaaaa-0000-4000-8000-000000000002',-10,179.9,now()+interval '1 hour'
      FROM users u,categories c WHERE u.username='demo_user_a' AND c.code='ROAD_DAMAGE';
      INSERT INTO report_votes(report_id,user_id,vote_type)
      SELECT r.id,u.id,v.kind FROM (VALUES ('demo_user_b','CONFIRMED'),('demo_moderator','NO_LONGER')) v(name,kind)
      JOIN users u ON u.username=v.name CROSS JOIN reports r
      WHERE r.client_request_id='aaaaaaaa-0000-4000-8000-000000000001';
      INSERT INTO comments(report_id,user_id,client_request_id,body)
      SELECT r.id,u.id,'bbbbbbbb-0000-4000-8000-000000000001','Preserved comment'
      FROM reports r,users u WHERE r.client_request_id='aaaaaaaa-0000-4000-8000-000000000001' AND u.username='demo_user_b';
      INSERT INTO admin_logs(actor_id,action,target_type,target_id)
      SELECT u.id,'TEST_EVENT','REPORT',r.id FROM users u,reports r
      WHERE u.username='demo_admin' AND r.client_request_id='aaaaaaaa-0000-4000-8000-000000000001';
    """)


migration = base / 'migrations' / 'V2__metrics_tokens_request_keys.sql'
preflight = base / 'preflight-upgrade.sql'
if args.resume_v2_main:
    if sql('cityguard_test', "SELECT to_regclass('public.report_metrics') IS NOT NULL") != 't':
        raise RuntimeError('Resume requires an existing disposable V2 main test database')
    print('RESUME: initial preservation assertions were not rerun in this invocation')
else:
    fresh('cityguard_test')
    fixture('cityguard_test')
    before = snapshot('cityguard_test')
    sql('cityguard_test', file=preflight)
    if before != snapshot('cityguard_test'):
        raise RuntimeError('Preflight changed source data')
    sql('cityguard_test', file=migration)
    if before != snapshot('cityguard_test'):
        raise RuntimeError('Migration changed V1 rows')
    print('PASS: preflight read-only, V1->V2 migration, all six V1 tables preserved by row JSON digest')
sql('cityguard_test', file=migration, error='V2 table already exists')
print('PASS: migration rerun rejected without overwriting V2')
sql('cityguard_test', file=base / 'tests' / 'upgrade-tests.sql')
sql('cityguard_test', file=base / 'tests' / 'database-tests.sql')
print('PASS: V2 integration assertions and existing regression assertions')
sql('cityguard_test', file=base / 'seed.sql')
after_seed = snapshot('cityguard_test')
metrics_before = sql('cityguard_test', 'SELECT md5(string_agg(row_to_json(t)::text,\'\' ORDER BY report_id)) FROM report_metrics t')
sql('cityguard_test', file=base / 'seed.sql')
if after_seed != snapshot('cityguard_test') or metrics_before != sql('cityguard_test', 'SELECT md5(string_agg(row_to_json(t)::text,\'\' ORDER BY report_id)) FROM report_metrics t'):
    raise RuntimeError('Repeated seed changed existing rows')
sql('cityguard_test', file=base / 'tests' / 'upgrade-tests.sql')
print('PASS: V2 seed twice, unchanged V1 rows on repeat, cache consistent after seed')

# Two real sessions compete for the same report and obey the parent-lock protocol.
query_text = (base / 'queries.sql').read_text(encoding='utf-8')
owner_id = sql('cityguard_test', "SELECT id FROM users WHERE username='demo_user_a'")
category_id = sql('cityguard_test', "SELECT id FROM categories WHERE code='TRAFFIC_JAM'")
sql('cityguard_test', query_text + '\n' +
    f"EXECUTE create_report({owner_id},{category_id},'cccccccc-0000-4000-8000-000000000001',NULL,NULL,10,106,NULL);")
report_id = sql('cityguard_test', "SELECT id FROM reports WHERE client_request_id='cccccccc-0000-4000-8000-000000000001'")
voter_ids = [sql('cityguard_test', "SELECT id FROM users WHERE username='" + name + "'")
             for name in ['demo_user_b', 'demo_moderator']]


def competing_vote(voter):
    sql('cityguard_test', query_text + '\n' + f"""
      BEGIN;
      EXECUTE lock_vote_report({voter},{report_id});
      EXECUTE put_vote({report_id},{voter},'CONFIRMED');
      EXECUTE refresh_report_metrics({report_id});
      COMMIT;
    """)


with ThreadPoolExecutor(max_workers=2) as pool:
    list(pool.map(competing_vote, voter_ids))
if sql('cityguard_test', 'SELECT confirm_count FROM report_metrics WHERE report_id=' + report_id) != '2':
    raise RuntimeError('Concurrent vote cache lost an update')
print('PASS: two concurrent connections, distinct voters, parent lock prevents lost cache update')

suffix = '_' + secrets.token_hex(3)
for db, change, expected in [
 ('cityguard_v2_dup_reports', """INSERT INTO reports(user_id,category_id,client_request_id,latitude,longitude,expires_at)
 SELECT u.id,r.category_id,r.client_request_id,r.latitude,r.longitude,r.expires_at FROM users u,reports r
 WHERE u.username='demo_user_b' AND r.client_request_id='aaaaaaaa-0000-4000-8000-000000000001'""", 'Duplicate client_request_id in reports'),
 ('cityguard_v2_dup_comments', """INSERT INTO comments(user_id,report_id,client_request_id,body)
 SELECT u.id,c.report_id,c.client_request_id,'Duplicate key on another owner' FROM users u,comments c
 WHERE u.username='demo_user_a'""", 'Duplicate client_request_id in comments'),
 ('cityguard_v2_drift', 'ALTER TABLE reports ALTER COLUMN latitude TYPE numeric USING latitude::numeric', 'Incompatible reports.latitude'),
 ('cityguard_v2_bad_range', 'ALTER TABLE reports DROP CONSTRAINT reports_latitude_check; ALTER TABLE reports ADD CHECK(latitude BETWEEN -190 AND 190)', 'Expected validated latitude/longitude range CHECKs'),
]:
    db += suffix
    fresh(db)
    fixture(db)
    sql(db, change)
    state = snapshot(db)
    sql(db, file=preflight, error=expected)
    sql(db, file=migration, error=expected)
    if state != snapshot(db) or sql(db, "SELECT to_regclass('public.report_metrics') IS NULL") != 't':
        raise RuntimeError('Rejected upgrade left side effects: ' + db)
    print('PASS: ' + db + ' rejected by preflight AND migration, no data changes')

db = 'cityguard_v2_existing_unique' + suffix
fresh(db)
fixture(db)
sql(db, 'CREATE UNIQUE INDEX preexisting_global_request ON reports(client_request_id)')
sql(db, file=migration)
if sql(db, """SELECT count(*) FROM pg_index i JOIN pg_attribute a
 ON a.attrelid=i.indrelid AND a.attname='client_request_id'
 WHERE i.indrelid='reports'::regclass AND i.indisunique AND i.indnkeyatts=1 AND a.attnum=ANY(i.indkey)""") != '1':
    raise RuntimeError('Duplicated pre-existing unique index')
print('PASS: equivalent pre-existing UNIQUE index reused, no duplicate global index')

db = 'cityguard_v2_missing_vote_unique' + suffix
fresh(db)
fixture(db)
sql(db, 'ALTER TABLE report_votes DROP CONSTRAINT report_votes_report_id_user_id_key')
sql(db, file=migration)
if sql(db, "SELECT count(*) FROM pg_constraint WHERE conrelid='report_votes'::regclass AND contype='u'") != '1':
    raise RuntimeError('Missing vote UNIQUE was not restored exactly once')
print('PASS: missing vote UNIQUE restored after duplicate check')
print('All tests passed. Test databases retained; no database was dropped.')
