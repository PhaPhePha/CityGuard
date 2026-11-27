-- V2 regression suite. Run only on dedicated cityguard_test. No seed needed. All fixture rows roll back.
\set ON_ERROR_STOP on
BEGIN;
DO $$ BEGIN
 IF current_database() = 'cityguard_db' OR current_database() !~ '^cityguard_([a-z0-9_]*test[a-z0-9_]*|v3_dev)$' THEN
   RAISE EXCEPTION 'Refusing tests outside dedicated cityguard test/dev-docker database (current: %)', current_database();
 END IF;
END $$;

CREATE FUNCTION pg_temp.expect_error(statement text, expected_state text) RETURNS void
LANGUAGE plpgsql AS $$
DECLARE actual_state text;
BEGIN
 BEGIN
   EXECUTE statement;
 EXCEPTION WHEN OTHERS THEN
   GET STACKED DIAGNOSTICS actual_state = RETURNED_SQLSTATE;
   IF actual_state=expected_state THEN RETURN; END IF;
   RAISE EXCEPTION 'Expected SQLSTATE %, got % for %',expected_state,actual_state,statement;
 END;
 RAISE EXCEPTION 'Expected SQLSTATE %, but statement succeeded: %',expected_state,statement;
END $$;

DO $$
DECLARE
 a bigint; b bigint; cat bigint; rep bigint; expired_rep bigint; cm bigint; n bigint;
 -- SHAPE-ONLY fixture hash, deliberately NOT a login account/password.
 -- Test-only data always rolls back. Demo seed has separately verified BCrypt hashes.
 fixture_hash text := '$2b$12$' || repeat('A',53);
 request_id uuid := 'ffffffff-0000-4000-8000-000000000001';
BEGIN
 INSERT INTO users(username,password_hash,display_name)
 VALUES ('cg_test_a',fixture_hash,'Test A') RETURNING id INTO a;
 INSERT INTO users(email,password_hash,display_name,role)
 VALUES ('cg-test-b@example.invalid',fixture_hash,'Test B','ADMIN') RETURNING id INTO b;
 INSERT INTO categories(code,display_name,icon,color,default_ttl_seconds)
 VALUES ('CG_TEST','Test','test','#123ABC',3600) RETURNING id INTO cat;
 INSERT INTO reports(user_id,category_id,client_request_id,public_id,latitude,longitude,expires_at,status)
 VALUES (a,cat,request_id,'rpt_01M7WBY80FZNE4EYTVWZ00JM01',10.77,106.70,now()+interval '1 hour','ACTIVE') RETURNING id INTO rep;
 RAISE NOTICE 'PASS 01 valid users/category/report inserts';

 PERFORM pg_temp.expect_error(format(
 'INSERT INTO users(username,password_hash,display_name) VALUES (%L,%L,%L)',
 'CG_TEST_A',fixture_hash,'Duplicate'),'23505');
 PERFORM pg_temp.expect_error(format(
 'INSERT INTO users(email,password_hash,display_name) VALUES (%L,%L,%L)',
 'CG-TEST-B@EXAMPLE.INVALID',fixture_hash,'Duplicate'),'23505');
 RAISE NOTICE 'PASS 02 case-insensitive username/email UNIQUE';

 PERFORM pg_temp.expect_error(format(
 'INSERT INTO reports(user_id,category_id,client_request_id,public_id,latitude,longitude,expires_at) VALUES (-1,%s,%L,''rpt_01M7WBY80FZNE4EYTVWZ00JM02'',10,106,now()+interval ''1 hour'')',cat,'ffffffff-0000-4000-8000-000000000099'::uuid),'23503');
 RAISE NOTICE 'PASS 03 foreign key violation';
 PERFORM pg_temp.expect_error(format(
 'INSERT INTO comments(report_id,user_id,client_request_id,body) VALUES (%s,%s,%L,NULL)',rep,a,request_id),'23502');
 RAISE NOTICE 'PASS 04 NOT NULL';

 INSERT INTO report_votes(report_id,user_id,vote_type) VALUES (rep,b,'CONFIRMED');
 PERFORM pg_temp.expect_error(format(
 'INSERT INTO report_votes(report_id,user_id,vote_type) VALUES (%s,%s,''NO_LONGER'')',rep,b),'23505');
 INSERT INTO report_votes(report_id,user_id,vote_type) VALUES (rep,b,'NO_LONGER')
 ON CONFLICT (report_id,user_id) DO UPDATE SET vote_type=EXCLUDED.vote_type;
 SELECT count(*) INTO n FROM report_votes WHERE report_id=rep AND user_id=b AND vote_type='NO_LONGER';
 IF n<>1 THEN RAISE EXCEPTION 'Vote upsert failed'; END IF;
 RAISE NOTICE 'PASS 05-06 duplicate vote rejected, change vote succeeds';

 INSERT INTO comments(report_id,user_id,client_request_id,body)
 VALUES (rep,a,request_id,'Test comment') RETURNING id INTO cm;
 UPDATE comments SET deleted_at=now() WHERE id=cm;
 SELECT count(*) INTO n FROM comments WHERE id=cm AND deleted_at IS NULL AND NOT is_hidden;
 IF n<>0 THEN RAISE EXCEPTION 'Soft deleted comment visible'; END IF;
 IF NOT EXISTS (SELECT 1 FROM comments WHERE id=cm AND deleted_at IS NOT NULL) THEN
  RAISE EXCEPTION 'Soft deleted comment lost'; END IF;
 UPDATE comments SET deleted_at=NULL,is_hidden=true WHERE id=cm;
 IF EXISTS (SELECT 1 FROM comments WHERE id=cm AND deleted_at IS NULL AND NOT is_hidden) THEN
  RAISE EXCEPTION 'Hidden comment visible'; END IF;
 RAISE NOTICE 'PASS 07 comment soft delete and hide';

 SELECT count(*) INTO n FROM reports WHERE id=rep AND latitude BETWEEN 10.7 AND 10.8
 AND longitude BETWEEN 106.6 AND 106.8 AND status IN ('ACTIVE','PENDING_VERIFY') AND expires_at>now();
 IF n<>1 THEN RAISE EXCEPTION 'bbox missed fixture'; END IF;
 IF EXISTS (SELECT 1 FROM reports WHERE id=rep AND longitude BETWEEN 100 AND 101) THEN
  RAISE EXCEPTION 'bbox returned out-of-region fixture'; END IF;
 RAISE NOTICE 'PASS 08 bbox includes inside and excludes outside';
 SELECT count(*) INTO n FROM reports WHERE id=rep AND category_id=cat;
 IF n<>1 THEN RAISE EXCEPTION 'Category filter failed'; END IF;
 SELECT count(*) INTO n FROM reports WHERE id=rep AND status='ACTIVE';
 IF n<>1 THEN RAISE EXCEPTION 'Status filter failed'; END IF;
 RAISE NOTICE 'PASS 09-10 category and status';

 INSERT INTO reports(user_id,category_id,client_request_id,public_id,latitude,longitude,created_at,expires_at)
 VALUES (a,cat,'ffffffff-0000-4000-8000-000000000002','rpt_01M7WBY80FZNE4EYTVWZ00JM03',10.77,106.70,now()-interval '2 hours',now()-interval '1 hour')
 RETURNING id INTO expired_rep;
 IF EXISTS (SELECT 1 FROM reports WHERE id=expired_rep AND status IN ('ACTIVE','PENDING_VERIFY') AND expires_at>now()) THEN
  RAISE EXCEPTION 'Overdue report visible on live map'; END IF;
 UPDATE reports SET status='EXPIRED' WHERE id=expired_rep AND expires_at<=now() AND status IN ('ACTIVE','PENDING_VERIFY');
 IF NOT EXISTS (SELECT 1 FROM reports WHERE id=expired_rep AND status='EXPIRED') THEN
  RAISE EXCEPTION 'Expiration failed'; END IF;
 RAISE NOTICE 'PASS 11 expiration retains history';

 WITH changed AS (UPDATE reports SET status='HIDDEN' WHERE id=rep RETURNING id)
 INSERT INTO admin_logs(actor_id,action,target_type,target_id)
 SELECT b,'HIDE_REPORT','REPORT',id FROM changed;
 IF NOT EXISTS (SELECT 1 FROM admin_logs WHERE actor_id=b AND target_type='REPORT' AND target_id=rep) THEN
  RAISE EXCEPTION 'Audit log missing'; END IF;
 SELECT count(*) INTO n FROM reports WHERE user_id=a;
 IF n<>2 THEN RAISE EXCEPTION 'Profile/Admin count incorrect'; END IF;
 IF EXISTS (SELECT 1 FROM reports WHERE id=rep AND status IN ('ACTIVE','PENDING_VERIFY')) THEN
  RAISE EXCEPTION 'Hidden report visible'; END IF;
 RAISE NOTICE 'PASS 12 admin statistics, moderation and audit';

 PERFORM pg_temp.expect_error(format(
 'INSERT INTO reports(user_id,category_id,client_request_id,public_id,latitude,longitude,expires_at) VALUES (%s,%s,%L,''rpt_01M7WBY80FZNE4EYTVWZ00JM04'',10,106,now()+interval ''1 hour'')',a,cat,request_id),'23505');
 INSERT INTO reports(user_id,category_id,client_request_id,public_id,latitude,longitude,expires_at)
 VALUES (a,cat,request_id,'rpt_01M7WBY80FZNE4EYTVWZ00JM05',10,106,now()+interval '1 hour') ON CONFLICT (client_request_id) DO NOTHING;
 SELECT count(*) INTO n FROM reports WHERE user_id=a AND client_request_id=request_id;
 IF n<>1 THEN RAISE EXCEPTION 'Report retry duplicate'; END IF;
 INSERT INTO comments(report_id,user_id,client_request_id,body)
 VALUES (rep,a,request_id,'Test comment') ON CONFLICT (client_request_id) DO NOTHING;
 SELECT count(*) INTO n FROM comments WHERE user_id=a AND client_request_id=request_id;
 IF n<>1 THEN RAISE EXCEPTION 'Comment retry duplicate'; END IF;
 RAISE NOTICE 'PASS 13 report/comment retry';

 PERFORM pg_temp.expect_error(format('UPDATE reports SET latitude=91 WHERE id=%s',rep),'23514');
 PERFORM pg_temp.expect_error(format('UPDATE reports SET longitude=''NaN''::float8 WHERE id=%s',rep),'23514');
 PERFORM pg_temp.expect_error(format('UPDATE reports SET expires_at=created_at WHERE id=%s',rep),'23514');
 PERFORM pg_temp.expect_error(format('UPDATE reports SET status=''UNKNOWN'' WHERE id=%s',rep),'23514');
 PERFORM pg_temp.expect_error(format('UPDATE comments SET body=''   '' WHERE id=%s',cm),'23514');
 PERFORM pg_temp.expect_error(format('DELETE FROM users WHERE id=%s',a),'23503');
 RAISE NOTICE 'PASS 14 coordinates, TTL, status, body and restrictive delete';
END $$;
ROLLBACK;
\echo 'All SQL assertions passed; fixture rows rolled back. Identity sequences may advance.'
