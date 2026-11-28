-- READ ONLY Preflight Verification before applying CityGuard Database V4.
-- Pure SQL: usable in psql (-v ON_ERROR_STOP=1) or DBeaver Execute SQL Script.
-- Validates that the current database is at a clean, healthy V3 baseline
-- and ready for V4 (Firebase Auth & Social Providers). Makes zero changes.

BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY;

SELECT current_database() AS database_name,
       current_user AS database_user,
       current_setting('server_version') AS server_version;

DO $preflight_v4$
DECLARE
    item record;
    relation regclass;
    user_count bigint;
    report_count bigint;
BEGIN
    -- 1. Check PostgreSQL Engine version (17+)
    IF current_setting('server_version_num')::integer < 170000 THEN
        RAISE EXCEPTION 'V4 requires PostgreSQL 17+; current version: %', current_setting('server_version');
    END IF;

    -- 2. Verify all 8 V3 baseline tables exist
    FOR item IN SELECT * FROM (VALUES
        ('users'), ('categories'), ('reports'), ('report_votes'),
        ('comments'), ('admin_logs'), ('report_metrics'), ('refresh_tokens')
    ) AS t(table_name) LOOP
        relation := to_regclass('public.' || item.table_name);
        IF relation IS NULL OR NOT EXISTS (
            SELECT 1 FROM pg_class WHERE oid = relation AND relkind = 'r'
        ) THEN
            RAISE EXCEPTION 'Missing V3 baseline table: public.%', item.table_name;
        END IF;
    END LOOP;

    -- 3. Verify V3 column reports.public_id exists with NOT NULL and UNIQUE constraint
    IF NOT EXISTS (
        SELECT 1 FROM pg_attribute
        WHERE attrelid = 'public.reports'::regclass
          AND attname = 'public_id'
          AND format_type(atttypid, atttypmod) = 'character varying(30)'
          AND attnotnull
          AND NOT attisdropped
    ) THEN
        RAISE EXCEPTION 'V3 baseline invalid: reports.public_id missing or not VARCHAR(30) NOT NULL';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint c
        JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = ANY(c.conkey)
        WHERE c.conrelid = 'public.reports'::regclass
          AND c.contype = 'u'
          AND a.attname = 'public_id'
    ) THEN
        RAISE EXCEPTION 'V3 baseline invalid: missing UNIQUE constraint on reports.public_id';
    END IF;

    -- 4. Verify public.users exists with expected columns
    FOR item IN SELECT * FROM (VALUES
        ('id', 'bigint'),
        ('username', 'character varying(50)'),
        ('email', 'character varying(254)'),
        ('password_hash', 'text'),
        ('display_name', 'character varying(100)'),
        ('role', 'character varying(16)'),
        ('status', 'character varying(16)')
    ) AS col(col_name, expected_type) LOOP
        IF NOT EXISTS (
            SELECT 1 FROM pg_attribute
            WHERE attrelid = 'public.users'::regclass
              AND attname = item.col_name
              AND format_type(atttypid, atttypmod) = item.expected_type
              AND NOT attisdropped
        ) THEN
            RAISE EXCEPTION 'Incompatible users.%: expected type %', item.col_name, item.expected_type;
        END IF;
    END LOOP;

    -- 5. Ensure V4 tables do NOT already exist (prevent duplicate upgrade / schema drift)
    IF to_regclass('public.user_firebase_accounts') IS NOT NULL THEN
        RAISE EXCEPTION 'Table public.user_firebase_accounts already exists; V4 is already applied or schema is drifted';
    END IF;

    IF to_regclass('public.user_auth_providers') IS NOT NULL THEN
        RAISE EXCEPTION 'Table public.user_auth_providers already exists; V4 is already applied or schema is drifted';
    END IF;

    -- 6. Collect baseline health statistics
    SELECT count(*) INTO user_count FROM public.users;
    SELECT count(*) INTO report_count FROM public.reports;

    RAISE NOTICE 'Preflight V4 check PASSED. Database is at a healthy V3 baseline (% users, % reports). Ready for V4.',
        user_count, report_count;
END;
$preflight_v4$;

COMMIT;