-- PostgreSQL V3 integration assertions. Dedicated test database ONLY.
\set ON_ERROR_STOP on
BEGIN;
DO $$ BEGIN
 IF current_database() = 'cityguard_db' OR current_database() !~ '^cityguard_([a-z0-9_]*test[a-z0-9_]*|v3_dev)$' THEN
   RAISE EXCEPTION 'Dedicated test/dev-docker database required (current: %)', current_database();
 END IF;
END $$;
\ir ../queries.sql

CREATE OR REPLACE FUNCTION pg_temp.expect_error(statement text, expected_state text)
RETURNS void LANGUAGE plpgsql AS $$
DECLARE actual_state text;
BEGIN
 BEGIN EXECUTE statement;
 EXCEPTION WHEN OTHERS THEN
  GET STACKED DIAGNOSTICS actual_state=RETURNED_SQLSTATE;
  IF actual_state=expected_state THEN RETURN; END IF;
  RAISE EXCEPTION 'Expected SQLSTATE %, received %',expected_state,actual_state;
 END;
 RAISE EXCEPTION 'Expected SQLSTATE %, statement succeeded',expected_state;
END $$;

DO $v3_tests$
DECLARE
  rec record;
  user_a bigint;
  user_b bigint;
  cat_id bigint;
  row_data record;
  created_rep bigint;
  retrieved_rep record;
  count_val bigint;
  valid_pid1 text := 'rpt_01M7ZZZZZZZZZZZZZZZZZZZZZ1';
  valid_pid2 text := 'rpt_01M7ZZZZZZZZZZZZZZZZZZZZZ2';
  fixture_hash text := '$2b$12$'||repeat('A',53);
BEGIN
  -- 1. Check schema catalog: public_id exists on reports, varchar(30), NOT NULL
  SELECT attname, format_type(atttypid, atttypmod) as type_name, attnotnull
  INTO rec
  FROM pg_attribute
  WHERE attrelid = 'public.reports'::regclass AND attname = 'public_id' AND NOT attisdropped;

  IF rec.attname IS NULL THEN
    RAISE EXCEPTION 'Column public.reports.public_id is missing';
  END IF;
  IF rec.type_name <> 'character varying(30)' THEN
    RAISE EXCEPTION 'Expected character varying(30), got %', rec.type_name;
  END IF;
  IF NOT rec.attnotnull THEN
    RAISE EXCEPTION 'Column public.reports.public_id must be NOT NULL';
  END IF;
  RAISE NOTICE 'PASS V3-01: public_id column signature (VARCHAR(30) NOT NULL)';

  -- 2. Check UNIQUE constraint exists on public_id
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint c
    JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = ANY(c.conkey)
    WHERE c.conrelid = 'public.reports'::regclass
      AND c.contype = 'u'
      AND a.attname = 'public_id'
  ) THEN
    RAISE EXCEPTION 'UNIQUE constraint missing on reports.public_id';
  END IF;
  RAISE NOTICE 'PASS V3-02: public_id UNIQUE constraint verified';

  -- 3. Check CHECK constraint exists on public_id
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint c
    WHERE c.conrelid = 'public.reports'::regclass
      AND c.contype = 'c'
      AND pg_get_constraintdef(c.oid) ~ 'public_id.*~'
  ) THEN
    RAISE EXCEPTION 'CHECK constraint missing on reports.public_id';
  END IF;
  RAISE NOTICE 'PASS V3-03: public_id format CHECK constraint verified';

  -- Setup test users & category
  INSERT INTO users(username, password_hash, display_name)
  VALUES ('v3_user_a', fixture_hash, 'V3 User A') RETURNING id INTO user_a;
  INSERT INTO users(username, password_hash, display_name)
  VALUES ('v3_user_b', fixture_hash, 'V3 User B') RETURNING id INTO user_b;
  INSERT INTO categories(code, display_name, icon, color, default_ttl_seconds)
  VALUES ('V3_CAT', 'V3 Category', 'v3_icon', '#336699', 3600) RETURNING id INTO cat_id;

  -- 4. Test format validation: reject invalid formats
  -- 4a. Missing prefix 'rpt_'
  PERFORM pg_temp.expect_error(format(
    'INSERT INTO reports(user_id, category_id, client_request_id, public_id, latitude, longitude, expires_at)
     VALUES (%s, %s, ''d0000000-0000-4000-8000-000000000001'', ''01M7WBY80FZNE4EYTVWZ00JM0J'', 10.7, 106.7, now()+interval ''1 hour'')',
    user_a, cat_id), '23514');

  -- 4b. Too short (25 chars)
  PERFORM pg_temp.expect_error(format(
    'INSERT INTO reports(user_id, category_id, client_request_id, public_id, latitude, longitude, expires_at)
     VALUES (%s, %s, ''d0000000-0000-4000-8000-000000000001'', ''rpt_01M7WBY80FZNE4EYTVWZ00JM0'', 10.7, 106.7, now()+interval ''1 hour'')',
    user_a, cat_id), '23514');

  -- 4c. Too long (31 chars)
  PERFORM pg_temp.expect_error(format(
    'INSERT INTO reports(user_id, category_id, client_request_id, public_id, latitude, longitude, expires_at)
     VALUES (%s, %s, ''d0000000-0000-4000-8000-000000000001'', ''rpt_01M7WBY80FZNE4EYTVWZ00JM0J99'', 10.7, 106.7, now()+interval ''1 hour'')',
    user_a, cat_id), '22001');

  -- 4d. Invalid character 'I' (not in Crockford Base32)
  PERFORM pg_temp.expect_error(format(
    'INSERT INTO reports(user_id, category_id, client_request_id, public_id, latitude, longitude, expires_at)
     VALUES (%s, %s, ''d0000000-0000-4000-8000-000000000001'', ''rpt_01M7WBY80FZNE4EYTVWZ00JM0I'', 10.7, 106.7, now()+interval ''1 hour'')',
    user_a, cat_id), '23514');

  -- 4e. Invalid character 'L' (not in Crockford Base32)
  PERFORM pg_temp.expect_error(format(
    'INSERT INTO reports(user_id, category_id, client_request_id, public_id, latitude, longitude, expires_at)
     VALUES (%s, %s, ''d0000000-0000-4000-8000-000000000001'', ''rpt_01M7WBY80FZNE4EYTVWZ00JM0L'', 10.7, 106.7, now()+interval ''1 hour'')',
    user_a, cat_id), '23514');

  -- 4f. Invalid character 'O' (not in Crockford Base32)
  PERFORM pg_temp.expect_error(format(
    'INSERT INTO reports(user_id, category_id, client_request_id, public_id, latitude, longitude, expires_at)
     VALUES (%s, %s, ''d0000000-0000-4000-8000-000000000001'', ''rpt_01M7WBY80FZNE4EYTVWZ00JM0O'', 10.7, 106.7, now()+interval ''1 hour'')',
    user_a, cat_id), '23514');

  -- 4g. Invalid character 'U' (not in Crockford Base32)
  PERFORM pg_temp.expect_error(format(
    'INSERT INTO reports(user_id, category_id, client_request_id, public_id, latitude, longitude, expires_at)
     VALUES (%s, %s, ''d0000000-0000-4000-8000-000000000001'', ''rpt_01M7WBY80FZNE4EYTVWZ00JM0U'', 10.7, 106.7, now()+interval ''1 hour'')',
    user_a, cat_id), '23514');

  -- 4h. First ULID char > 7 (overflows 128-bit ULID)
  PERFORM pg_temp.expect_error(format(
    'INSERT INTO reports(user_id, category_id, client_request_id, public_id, latitude, longitude, expires_at)
     VALUES (%s, %s, ''d0000000-0000-4000-8000-000000000001'', ''rpt_81M7WBY80FZNE4EYTVWZ00JM0J'', 10.7, 106.7, now()+interval ''1 hour'')',
    user_a, cat_id), '23514');

  -- 4i. NULL public_id rejected
  PERFORM pg_temp.expect_error(format(
    'INSERT INTO reports(user_id, category_id, client_request_id, public_id, latitude, longitude, expires_at)
     VALUES (%s, %s, ''d0000000-0000-4000-8000-000000000001'', NULL, 10.7, 106.7, now()+interval ''1 hour'')',
    user_a, cat_id), '23502');

  RAISE NOTICE 'PASS V3-04: format CHECK rejects invalid prefixes, lengths, non-Base32 chars (I,L,O,U), overflow digits, and NULL';

  -- 5. Test create_report prepared statement with valid public_id
  EXECUTE format(
    'EXECUTE create_report(%s, %s, ''d0000000-0000-4000-8000-000000000001''::uuid, %L, ''V3 description'', NULL, 10.77, 106.70, ''TP.HCM'')',
    user_a, cat_id, valid_pid1) INTO row_data;

  created_rep := row_data.id;
  IF created_rep IS NULL THEN
    RAISE EXCEPTION 'create_report failed to return id';
  END IF;
  IF row_data.public_id <> valid_pid1 THEN
    RAISE EXCEPTION 'create_report returned wrong public_id: expected %, got %', valid_pid1, row_data.public_id;
  END IF;
  RAISE NOTICE 'PASS V3-05: create_report succeeded with valid public_id';

  -- 6. Test uniqueness: duplicate public_id rejected
  PERFORM pg_temp.expect_error(format(
    'INSERT INTO reports(user_id, category_id, client_request_id, public_id, latitude, longitude, expires_at)
     VALUES (%s, %s, ''d0000000-0000-4000-8000-000000000002'', %L, 10.7, 106.7, now()+interval ''1 hour'')',
    user_b, cat_id, valid_pid1), '23505');
  RAISE NOTICE 'PASS V3-06: duplicate public_id rejected by UNIQUE constraint';

  -- 7. Test query by public_id: get_report_by_public_id
  EXECUTE format('EXECUTE get_report_by_public_id(%L)', valid_pid1) INTO retrieved_rep;
  IF retrieved_rep.id <> created_rep THEN
    RAISE EXCEPTION 'get_report_by_public_id failed: expected id %, got %', created_rep, retrieved_rep.id;
  END IF;
  IF retrieved_rep.public_id <> valid_pid1 THEN
    RAISE EXCEPTION 'get_report_by_public_id public_id mismatch';
  END IF;
  IF retrieved_rep.confirm_count <> 0 OR retrieved_rep.no_longer_count <> 0 THEN
    RAISE EXCEPTION 'get_report_by_public_id metrics mismatch';
  END IF;
  RAISE NOTICE 'PASS V3-07: get_report_by_public_id returned matching report and initialized metrics';

  -- 8. Test report_retry with public_id
  EXECUTE format('EXECUTE report_retry(%s, ''d0000000-0000-4000-8000-000000000001''::uuid)', user_a) INTO retrieved_rep;
  IF retrieved_rep.public_id <> valid_pid1 THEN
    RAISE EXCEPTION 'report_retry public_id mismatch';
  END IF;
  RAISE NOTICE 'PASS V3-08: report_retry returns public_id correctly';

  -- 9. Test Foreign Keys & other V2 tables:
  -- 9a. report_votes -> reports.id (BIGINT FK intact)
  INSERT INTO report_votes(report_id, user_id, vote_type) VALUES (created_rep, user_b, 'CONFIRMED');
  SELECT count(*) INTO count_val FROM report_votes WHERE report_id = created_rep AND user_id = user_b;
  IF count_val <> 1 THEN RAISE EXCEPTION 'Vote insert failed'; END IF;

  -- 9b. comments -> reports.id (BIGINT FK intact)
  INSERT INTO comments(report_id, user_id, client_request_id, body)
  VALUES (created_rep, user_b, 'd0000000-0000-4000-8000-000000000003'::uuid, 'Comment on V3 report');
  SELECT count(*) INTO count_val FROM comments WHERE report_id = created_rep;
  IF count_val <> 1 THEN RAISE EXCEPTION 'Comment insert failed'; END IF;

  -- 9c. report_metrics -> reports.id (BIGINT PK/FK intact)
  EXECUTE format('EXECUTE refresh_report_metrics(%s)', created_rep);
  SELECT confirm_count INTO count_val FROM report_metrics WHERE report_id = created_rep;
  IF count_val <> 1 THEN RAISE EXCEPTION 'Metrics update failed'; END IF;

  -- 9d. refresh_tokens table still functional
  INSERT INTO refresh_tokens(user_id, token_hash, expires_at)
  VALUES (user_a, repeat('a', 64), now() + interval '7 days');
  SELECT count(*) INTO count_val FROM refresh_tokens WHERE user_id = user_a;
  IF count_val <> 1 THEN RAISE EXCEPTION 'Refresh token insert failed'; END IF;

  RAISE NOTICE 'PASS V3-09: All FKs (votes, comments, metrics) and other V2 tables remain fully functional';

  -- 10. Test map_bbox includes public_id
  UPDATE reports SET status = 'ACTIVE' WHERE id = created_rep;
  EXECUTE format('EXECUTE map_bbox(106.6, 10.7, 106.8, 10.8, NULL, ''ACTIVE'', 10)') INTO retrieved_rep;
  IF retrieved_rep.public_id <> valid_pid1 THEN
    RAISE EXCEPTION 'map_bbox public_id mismatch';
  END IF;
  RAISE NOTICE 'PASS V3-10: map_bbox returns public_id correctly';

  -- 11. Test my_reports includes public_id
  EXECUTE format('EXECUTE my_reports(%s, NULL, NULL, NULL, 10)', user_a) INTO retrieved_rep;
  IF retrieved_rep.public_id <> valid_pid1 THEN
    RAISE EXCEPTION 'my_reports public_id mismatch';
  END IF;
  RAISE NOTICE 'PASS V3-11: my_reports returns public_id correctly';

  RAISE NOTICE 'ALL V3 INTEGRATION ASSERTIONS PASSED SUCCESSFULLY!';
END;
$v3_tests$;

ROLLBACK;
