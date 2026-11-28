-- PostgreSQL V4 integration assertions. Dedicated test database ONLY.
\set ON_ERROR_STOP on
BEGIN;

DO $$ BEGIN
 IF current_database() = 'cityguard_db' OR current_database() !~ '^cityguard_([a-z0-9_]*test[a-z0-9_]*|v3_dev|v4_test)$' THEN
   RAISE EXCEPTION 'Dedicated test/dev database required (current: %)', current_database();
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

DO $v4_tests$
DECLARE
  rec record;
  user_local bigint;
  user_social bigint;
  user_other bigint;
  user_no_email bigint;
  fb_acc_id bigint;
  fb_acc_other_id bigint;
  rep_id bigint;
  fixture_hash text := '$2b$12$'||repeat('A',53);
  invalid_hash text := 'plain_text_password';
  cat_id bigint;
  prov_count int;
  updated_ts timestamptz;
BEGIN
  -- 1. Check schema catalog: users.password_hash is nullable
  SELECT attname, attnotnull INTO rec
  FROM pg_attribute
  WHERE attrelid = 'public.users'::regclass AND attname = 'password_hash' AND NOT attisdropped;

  IF rec.attname IS NULL THEN
    RAISE EXCEPTION 'Column public.users.password_hash is missing';
  END IF;
  IF rec.attnotnull THEN
    RAISE EXCEPTION 'Column public.users.password_hash must be nullable in V4';
  END IF;
  RAISE NOTICE 'PASS V4-01: users.password_hash is nullable';

  -- 2. Verify V4 tables exist
  IF to_regclass('public.user_firebase_accounts') IS NULL OR to_regclass('public.user_auth_providers') IS NULL THEN
    RAISE EXCEPTION 'Missing V4 tables';
  END IF;
  RAISE NOTICE 'PASS V4-02: V4 tables exist';

  -- 3. Setup test data
  -- 3.1. Insert local user with valid password
  INSERT INTO users(username, email, password_hash, display_name)
  VALUES ('v4_user_local', 'local@cityguard.test', fixture_hash, 'Local User')
  RETURNING id INTO user_local;

  -- 3.2. Insert social user with password_hash NULL (Criterion 7)
  INSERT INTO users(username, email, password_hash, display_name)
  VALUES ('v4_user_social', 'social@cityguard.test', NULL, 'Social User')
  RETURNING id INTO user_social;
  RAISE NOTICE 'PASS V4-03: User created with password_hash NULL';

  -- 3.3. Insert social user with email NULL and valid username (Criterion 8)
  INSERT INTO users(username, email, password_hash, display_name)
  VALUES ('v4_user_no_email', NULL, NULL, 'No Email User')
  RETURNING id INTO user_no_email;
  RAISE NOTICE 'PASS V4-04: User created with email NULL and valid username';

  -- 3.4. Verify constraint: both username and email NULL is REJECTED
  PERFORM pg_temp.expect_error(
    'INSERT INTO users(username, email, password_hash, display_name) VALUES (NULL, NULL, NULL, ''Invalid User'')',
    '23514' -- check_violation
  );
  RAISE NOTICE 'PASS V4-05: User with both username and email NULL is rejected';

  -- 3.5. Verify constraint: invalid password_hash format is REJECTED
  PERFORM pg_temp.expect_error(
    'INSERT INTO users(username, email, password_hash, display_name) VALUES (''v4_bad_pass'', ''badpass@test.com'', '''||invalid_hash||''', ''Bad Pass'')',
    '23514' -- check_violation
  );
  RAISE NOTICE 'PASS V4-06: Invalid password_hash format is rejected';

  -- 3.6. Insert third user for cross-mapping and uniqueness tests
  INSERT INTO users(username, email, password_hash, display_name)
  VALUES ('v4_user_other', 'other@cityguard.test', fixture_hash, 'Other User')
  RETURNING id INTO user_other;

  -- 4. Multi-provider linking for a single user (Criterion 3)
  -- Create Firebase account for user_social
  INSERT INTO user_firebase_accounts(user_id, firebase_uid)
  VALUES (user_social, 'firebase_uid_social_123')
  RETURNING id INTO fb_acc_id;

  -- Link Google provider
  INSERT INTO user_auth_providers(user_id, firebase_account_id, provider, provider_uid, email, display_name)
  VALUES (user_social, fb_acc_id, 'GOOGLE', 'google_sub_1001', 'social@gmail.com', 'Social Google');

  -- Link Facebook provider
  INSERT INTO user_auth_providers(user_id, firebase_account_id, provider, provider_uid, email, display_name)
  VALUES (user_social, fb_acc_id, 'FACEBOOK', 'facebook_id_2002', 'social@facebook.com', 'Social Facebook');

  SELECT count(*) INTO prov_count FROM user_auth_providers WHERE user_id = user_social;
  IF prov_count <> 2 THEN
    RAISE EXCEPTION 'Expected 2 providers linked to user_social, got %', prov_count;
  END IF;
  RAISE NOTICE 'PASS V4-07: Single user successfully linked to both Google and Facebook';

  -- 5. Duplicate provider identity rejection (Criterion 4 & Criterion 6)
  -- Another user (user_other) tries to link the SAME Google identity
  INSERT INTO user_firebase_accounts(user_id, firebase_uid)
  VALUES (user_other, 'firebase_uid_other_999')
  RETURNING id INTO fb_acc_other_id;

  PERFORM pg_temp.expect_error(
    'INSERT INTO user_auth_providers(user_id, firebase_account_id, provider, provider_uid) ' ||
    'VALUES ('||user_other||', '||fb_acc_other_id||', ''GOOGLE'', ''google_sub_1001'')',
    '23505' -- unique_violation on uq_user_auth_provider_identity
  );
  RAISE NOTICE 'PASS V4-08: Duplicate provider identity across users blocked by UNIQUE constraint';

  -- Same user tries to link a second Google account
  PERFORM pg_temp.expect_error(
    'INSERT INTO user_auth_providers(user_id, firebase_account_id, provider, provider_uid) ' ||
    'VALUES ('||user_social||', '||fb_acc_id||', ''GOOGLE'', ''google_sub_different'')',
    '23505' -- unique_violation on uq_user_auth_provider_per_user
  );
  RAISE NOTICE 'PASS V4-09: Linking two accounts of same provider to one user blocked';

  -- 6. Firebase UID uniqueness & 1-1 with user (Criterion 5)
  -- Another user tries to use the same Firebase UID
  PERFORM pg_temp.expect_error(
    'INSERT INTO user_firebase_accounts(user_id, firebase_uid) VALUES ('||user_local||', ''firebase_uid_social_123'')',
    '23505' -- unique_violation on user_firebase_accounts_firebase_uid_uq
  );
  RAISE NOTICE 'PASS V4-10: Duplicate firebase_uid across users blocked by UNIQUE constraint';

  -- Same user tries to create a second Firebase account
  PERFORM pg_temp.expect_error(
    'INSERT INTO user_firebase_accounts(user_id, firebase_uid) VALUES ('||user_social||', ''firebase_uid_second'')',
    '23505' -- unique_violation on user_firebase_accounts_user_id_uq
  );
  RAISE NOTICE 'PASS V4-11: Multiple Firebase accounts per user blocked';

  -- 7. Cross-mapping prevention via Composite FK (Criterion 10)
  -- Attempt to insert provider with user_id = user_other but firebase_account_id = fb_acc_id (which belongs to user_social)
  PERFORM pg_temp.expect_error(
    'INSERT INTO user_auth_providers(user_id, firebase_account_id, provider, provider_uid) ' ||
    'VALUES ('||user_other||', '||fb_acc_id||', ''FACEBOOK'', ''fb_id_cross_map'')',
    '23503' -- foreign_key_violation on user_auth_providers_composite_user_firebase_fkey
  );
  RAISE NOTICE 'PASS V4-12: Cross-mapping between mismatched user and Firebase account blocked by Composite FK';

  -- 8. ON DELETE RESTRICT on users (Criterion 10)
  PERFORM pg_temp.expect_error(
    'DELETE FROM users WHERE id = ' || user_social,
    '23503' -- foreign_key_violation
  );
  RAISE NOTICE 'PASS V4-13: Deleting user with linked Firebase/providers blocked by RESTRICT FK';

  -- 9. Trigger set_updated_at test
  UPDATE user_auth_providers
  SET display_name = 'Social Google Updated'
  WHERE user_id = user_social AND provider = 'GOOGLE'
  RETURNING updated_at INTO updated_ts;

  IF updated_ts IS NULL THEN
    RAISE EXCEPTION 'updated_at trigger failed to set timestamp';
  END IF;
  RAISE NOTICE 'PASS V4-14: set_updated_at trigger functions correctly on V4 tables';

  -- 10. Non-interference with Reports, Votes, Comments (Criterion 9)
  SELECT id INTO cat_id FROM categories WHERE code = 'INFRASTRUCTURE' LIMIT 1;
  IF cat_id IS NULL THEN
    SELECT id INTO cat_id FROM categories LIMIT 1;
  END IF;

  INSERT INTO reports(user_id, category_id, client_request_id, public_id, description, latitude, longitude, address_snapshot, expires_at)
  VALUES (user_social, cat_id, '40000000-0000-4000-8000-000000000001'::uuid, 'rpt_01M7ZZZZZZZZZZZZZZZZZZZZ91', 'V4 Test Report', 10.762622, 106.660172, 'HCMC Test', now() + interval '1 day')
  RETURNING id INTO rep_id;

  INSERT INTO report_metrics(report_id) VALUES (rep_id);

  INSERT INTO report_votes(report_id, user_id, vote_type)
  VALUES (rep_id, user_local, 'CONFIRMED');

  INSERT INTO comments(report_id, user_id, client_request_id, body)
  VALUES (rep_id, user_social, '40000000-0000-4000-8000-000000000002'::uuid, 'Comment from social user');

  RAISE NOTICE 'PASS V4-15: Reports, votes, and comments operate flawlessly with V4 social users';

  RAISE NOTICE 'ALL V4 INTEGRATION ASSERTIONS COMPLETED SUCCESSFULLY.';
END;
$v4_tests$;

COMMIT;
