-- PostgreSQL V2 integration assertions. Dedicated cityguard_test ONLY.
-- psql -X -v ON_ERROR_STOP=1 -d cityguard_test -f database/tests/upgrade-tests.sql
\set ON_ERROR_STOP on
BEGIN;
DO $$ BEGIN
 IF current_database()<>'cityguard_test' THEN RAISE EXCEPTION 'Dedicated cityguard_test required'; END IF;
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

DO $tests$
DECLARE
 item record; a bigint; b bigint; cat bigint; rep bigint; old_token bigint; count_rows bigint;
 row_data record;
 fixture_hash text := '$2b$12$'||repeat('A',53); -- shape-only test account, always rolled back
 request_id uuid := 'eeeeeeee-0000-4000-8000-000000000001';
 comment_key uuid := 'eeeeeeee-0000-4000-8000-000000000002';
BEGIN
 FOR item IN SELECT * FROM (VALUES
  ('report_metrics','report_id','bigint',true),
  ('report_metrics','confirm_count','bigint',true),
  ('report_metrics','no_longer_count','bigint',true),
  ('report_metrics','trust_score','numeric(5,2)',false),
  ('report_metrics','trust_label','character varying(32)',false),
  ('report_metrics','computed_at','timestamp with time zone',false),
  ('refresh_tokens','id','bigint',true),('refresh_tokens','user_id','bigint',true),
  ('refresh_tokens','token_hash','character varying(64)',true),
  ('refresh_tokens','expires_at','timestamp with time zone',true),
  ('refresh_tokens','revoked_at','timestamp with time zone',false)
 ) e(table_name,column_name,type_name,required) LOOP
  IF NOT EXISTS (SELECT 1 FROM pg_attribute WHERE
   attrelid=to_regclass('public.'||item.table_name) AND attname=item.column_name
   AND format_type(atttypid,atttypmod)=item.type_name AND attnotnull=item.required AND NOT attisdropped)
  THEN RAISE EXCEPTION 'Wrong signature %.%',item.table_name,item.column_name; END IF;
 END LOOP;
 IF (SELECT count(*) FROM pg_attribute WHERE attrelid='public.report_metrics'::regclass AND attnum>0 AND NOT attisdropped)<>6
 OR (SELECT count(*) FROM pg_attribute WHERE attrelid='public.refresh_tokens'::regclass AND attnum>0 AND NOT attisdropped)<>5
 THEN RAISE EXCEPTION 'Unexpected V2 table columns'; END IF;
 IF NOT EXISTS (SELECT 1 FROM pg_attribute WHERE attrelid='public.refresh_tokens'::regclass AND attname='id' AND attidentity='a')
 THEN RAISE EXCEPTION 'Token identity missing'; END IF;
 RAISE NOTICE 'PASS V2-01 exact table types, nullability and identity';

 IF EXISTS (
  SELECT r.id FROM reports r LEFT JOIN report_votes v ON v.report_id=r.id
  LEFT JOIN report_metrics m ON m.report_id=r.id
  GROUP BY r.id,m.report_id,m.confirm_count,m.no_longer_count
  HAVING m.report_id IS NULL
   OR m.confirm_count<>count(v.id) FILTER (WHERE v.vote_type='CONFIRMED')
   OR m.no_longer_count<>count(v.id) FILTER (WHERE v.vote_type='NO_LONGER')
 ) THEN RAISE EXCEPTION 'Backfill/cache does not match source votes'; END IF;
 RAISE NOTICE 'PASS V2-02 existing backfill equals live vote aggregation';

 INSERT INTO users(username,password_hash,display_name) VALUES ('v2_test_a',fixture_hash,'V2 A') RETURNING id INTO a;
 INSERT INTO users(username,password_hash,display_name) VALUES ('v2_test_b',fixture_hash,'V2 B') RETURNING id INTO b;
 INSERT INTO categories(code,display_name,icon,color,default_ttl_seconds)
 VALUES ('V2_TEST','V2','test','#123456',3600) RETURNING id INTO cat;
 EXECUTE format('EXECUTE create_report(%s,%s,%L,NULL,NULL,10.7,106.7,NULL)',a,cat,request_id) INTO row_data;
 rep:=row_data.id;
 IF rep IS NULL OR NOT EXISTS (SELECT 1 FROM report_metrics WHERE report_id=rep
  AND confirm_count=0 AND no_longer_count=0 AND trust_score IS NULL AND computed_at IS NULL)
 THEN RAISE EXCEPTION 'Create report/zero metrics initialization failed'; END IF;
 PERFORM pg_temp.expect_error(format('INSERT INTO report_metrics(report_id) VALUES (%s)',rep),'23505');
 PERFORM pg_temp.expect_error('INSERT INTO report_metrics(report_id) VALUES (-1)','23503');
 PERFORM pg_temp.expect_error(format('UPDATE report_metrics SET confirm_count=-1 WHERE report_id=%s',rep),'23514');
 PERFORM pg_temp.expect_error(format('UPDATE report_metrics SET no_longer_count=-1 WHERE report_id=%s',rep),'23514');
 RAISE NOTICE 'PASS V2-03 metrics PK/FK/defaults/count CHECK and actual create_report';

 -- Global uniqueness must also reject a DIFFERENT user using the same key.
 PERFORM pg_temp.expect_error(format(
  'INSERT INTO reports(user_id,category_id,client_request_id,latitude,longitude,expires_at) VALUES (%s,%s,%L,10,106,now()+interval ''1 hour'')',b,cat,request_id),'23505');
 EXECUTE format('EXECUTE create_report(%s,%s,%L,NULL,NULL,10.7,106.7,NULL)',a,cat,request_id);
 SELECT count(*) INTO count_rows FROM reports WHERE client_request_id=request_id;
 IF count_rows<>1 THEN RAISE EXCEPTION 'Report retry duplicated row'; END IF;
 EXECUTE format('EXECUTE report_retry(%s,%L)',b,request_id) INTO row_data;
 IF row_data.id IS NOT NULL THEN RAISE EXCEPTION 'Report retry leaked another owner'; END IF;
 EXECUTE format('EXECUTE create_comment(%s,%s,%L,%L)',rep,a,comment_key,'V2 comment');
 EXECUTE format('EXECUTE create_comment(%s,%s,%L,%L)',rep,a,comment_key,'V2 comment');
 PERFORM pg_temp.expect_error(format(
  'INSERT INTO comments(report_id,user_id,client_request_id,body) VALUES (%s,%s,%L,''other user'')',rep,b,comment_key),'23505');
 IF (SELECT count(*) FROM comments WHERE client_request_id=comment_key)<>1 THEN RAISE EXCEPTION 'Comment retry duplicate'; END IF;
 RAISE NOTICE 'PASS V2-04 global report/comment keys, retry and owner filtering';

 EXECUTE format('EXECUTE lock_vote_report(%s,%s)',b,rep) INTO row_data;
 IF row_data.id IS NULL THEN RAISE EXCEPTION 'Vote parent lock failed'; END IF;
 EXECUTE format('EXECUTE put_vote(%s,%s,''CONFIRMED'')',rep,b);
 EXECUTE format('EXECUTE refresh_report_metrics(%s)',rep);
 IF NOT EXISTS (SELECT 1 FROM report_metrics WHERE report_id=rep AND confirm_count=1 AND no_longer_count=0)
 THEN RAISE EXCEPTION 'Confirm count mismatch'; END IF;
 UPDATE report_metrics SET trust_score=12.34,trust_label='test_only',computed_at=now() WHERE report_id=rep;
 EXECUTE format('EXECUTE put_vote(%s,%s,''CONFIRMED'')',rep,b);
 EXECUTE format('EXECUTE refresh_report_metrics(%s)',rep);
 IF NOT EXISTS (SELECT 1 FROM report_metrics WHERE report_id=rep AND trust_score=12.34)
 THEN RAISE EXCEPTION 'Identical retry invalidated computed score'; END IF;
 EXECUTE format('EXECUTE put_vote(%s,%s,''NO_LONGER'')',rep,b);
 EXECUTE format('EXECUTE refresh_report_metrics(%s)',rep);
 IF NOT EXISTS (SELECT 1 FROM report_metrics WHERE report_id=rep AND confirm_count=0 AND no_longer_count=1
  AND trust_score IS NULL AND trust_label IS NULL AND computed_at IS NULL)
 THEN RAISE EXCEPTION 'Changed vote did not invalidate score/update counts'; END IF;
 PERFORM pg_temp.expect_error(format('INSERT INTO report_votes(report_id,user_id,vote_type) VALUES (%s,%s,''CONFIRMED'')',rep,b),'23505');
 RAISE NOTICE 'PASS V2-05 actual vote/upsert/cache protocol, retry, invalidation and UNIQUE';

 -- Subtransaction fault injection: a later error must roll back both writes.
 BEGIN
  EXECUTE format('EXECUTE put_vote(%s,%s,''CONFIRMED'')',rep,b);
  EXECUTE format('EXECUTE refresh_report_metrics(%s)',rep);
  RAISE EXCEPTION 'simulated failure after both writes' USING ERRCODE='ZX001';
 EXCEPTION WHEN SQLSTATE 'ZX001' THEN NULL;
 END;
 IF NOT EXISTS (SELECT 1 FROM report_votes WHERE report_id=rep AND user_id=b AND vote_type='NO_LONGER')
 OR NOT EXISTS (SELECT 1 FROM report_metrics WHERE report_id=rep AND confirm_count=0 AND no_longer_count=1)
 THEN RAISE EXCEPTION 'Vote/cache writes did not roll back together'; END IF;
 RAISE NOTICE 'PASS V2-05b vote/cache atomic rollback on later failure';

 EXECUTE format('EXECUTE issue_refresh_token(%s,%L,%L)',a,repeat('a',64),now()+interval '1 hour') INTO row_data;
 old_token:=row_data.id;
 IF old_token IS NULL THEN RAISE EXCEPTION 'Token issuance failed'; END IF;
 PERFORM pg_temp.expect_error(format('INSERT INTO refresh_tokens(user_id,token_hash,expires_at) VALUES (%s,%L,now())',b,repeat('a',64)),'23505');
 PERFORM pg_temp.expect_error(format('INSERT INTO refresh_tokens(user_id,token_hash,expires_at) VALUES (-1,%L,now())',repeat('b',64)),'23503');
 PERFORM pg_temp.expect_error(format('INSERT INTO refresh_tokens(user_id,token_hash,expires_at) VALUES (%s,''raw-token'',now())',a),'23514');
 EXECUTE format('EXECUTE lock_refresh_token_user(%L)',repeat('a',64)) INTO row_data;
 IF row_data.id IS DISTINCT FROM a THEN RAISE EXCEPTION 'Token user lock failed'; END IF;
 EXECUTE format('EXECUTE valid_refresh_token(%L)',repeat('a',64)) INTO row_data;
 IF row_data.id IS DISTINCT FROM old_token THEN RAISE EXCEPTION 'Valid token not found'; END IF;
 EXECUTE format('EXECUTE revoke_refresh_token(%s,%L)',b,repeat('a',64));
 IF EXISTS (SELECT 1 FROM refresh_tokens WHERE id=old_token AND revoked_at IS NOT NULL)
 THEN RAISE EXCEPTION 'Another owner revoked token'; END IF;
 EXECUTE format('EXECUTE revoke_refresh_token(%s,%L)',a,repeat('a',64));
 EXECUTE format('EXECUTE issue_refresh_token(%s,%L,%L)',a,repeat('b',64),now()+interval '2 hours');
 EXECUTE format('EXECUTE valid_refresh_token(%L)',repeat('a',64)) INTO row_data;
 IF row_data.id IS NOT NULL THEN RAISE EXCEPTION 'Revoked token accepted'; END IF;
 UPDATE refresh_tokens SET expires_at=now()-interval '1 second' WHERE token_hash=repeat('b',64);
 EXECUTE format('EXECUTE valid_refresh_token(%L)',repeat('b',64)) INTO row_data;
 IF row_data.id IS NOT NULL THEN RAISE EXCEPTION 'Expired token accepted'; END IF;
 RAISE NOTICE 'PASS V2-06 tokens: issue, UNIQUE, FK, hash format, ownership, rotation and expiry';

 UPDATE users SET status='LOCKED' WHERE id=a;
 EXECUTE format('EXECUTE issue_refresh_token(%s,%L,%L)',a,repeat('c',64),now()+interval '1 hour');
 IF EXISTS (SELECT 1 FROM refresh_tokens WHERE token_hash=repeat('c',64)) THEN RAISE EXCEPTION 'Locked account issued token'; END IF;
 RAISE NOTICE 'PASS V2-07 locked-user token issuance rejected';
END;
$tests$;
ROLLBACK;
\echo 'V2 integration assertions passed; fixture rows rolled back (sequences may advance).'
