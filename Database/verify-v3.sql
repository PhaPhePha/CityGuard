-- READ ONLY Post-Initialization Verification for CityGuard V3.
-- Pure SQL: usable in psql (-v ON_ERROR_STOP=1) or DBeaver Execute SQL Script.
-- Verifies all 8 tables, primary keys, 9 RESTRICT foreign keys, unique/check constraints,
-- 14 explicit indexes, 4 updated_at triggers, and report public_id integrity.

BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY;

DO $verify_v3$
DECLARE
    item record;
    relation regclass;
    cnt integer;
BEGIN
    -- 1. Verify all 8 tables exist as ordinary tables
    FOR item IN SELECT * FROM (VALUES
        ('users'), ('categories'), ('reports'), ('report_votes'),
        ('comments'), ('admin_logs'), ('report_metrics'), ('refresh_tokens')
    ) AS t(table_name) LOOP
        relation := to_regclass('public.' || item.table_name);
        IF relation IS NULL OR NOT EXISTS (
            SELECT 1 FROM pg_class WHERE oid = relation AND relkind = 'r'
        ) THEN
            RAISE EXCEPTION 'Missing V3 table: public.%', item.table_name;
        END IF;
    END LOOP;

    -- 2. Verify Primary Keys on all 8 tables
    FOR item IN SELECT * FROM (VALUES
        ('users','id',true), ('categories','id',true), ('reports','id',true),
        ('report_votes','id',true), ('comments','id',true), ('admin_logs','id',true),
        ('report_metrics','report_id',false), ('refresh_tokens','id',true)
    ) AS pk(table_name, col_name, is_identity) LOOP
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint c
            JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = ANY(c.conkey)
            WHERE c.conrelid = to_regclass('public.' || item.table_name)
              AND c.contype = 'p'
              AND a.attname = item.col_name
              AND a.atttypid = 'bigint'::regtype
              AND a.attnotnull
              AND (NOT item.is_identity OR a.attidentity = 'a')
        ) THEN
            RAISE EXCEPTION 'Invalid or missing PRIMARY KEY on %.%', item.table_name, item.col_name;
        END IF;
    END LOOP;

    -- 3. Verify all 9 Foreign Keys with ON DELETE RESTRICT
    FOR item IN SELECT * FROM (VALUES
        ('reports','user_id','users'),
        ('reports','category_id','categories'),
        ('report_votes','report_id','reports'),
        ('report_votes','user_id','users'),
        ('comments','report_id','reports'),
        ('comments','user_id','users'),
        ('admin_logs','actor_id','users'),
        ('report_metrics','report_id','reports'),
        ('refresh_tokens','user_id','users')
    ) AS fk(table_name, col_name, parent_table) LOOP
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint c
            JOIN pg_attribute child ON child.attrelid = c.conrelid AND child.attname = item.col_name
            JOIN pg_attribute parent ON parent.attrelid = c.confrelid AND parent.attname = 'id'
            WHERE c.conrelid = to_regclass('public.' || item.table_name)
              AND c.confrelid = to_regclass('public.' || item.parent_table)
              AND c.contype = 'f'
              AND c.convalidated
              AND c.confdeltype = 'r'
              AND c.conkey = ARRAY[child.attnum]::smallint[]
              AND c.confkey = ARRAY[parent.attnum]::smallint[]
        ) THEN
            RAISE EXCEPTION 'Missing validated RESTRICT FK: %.% -> %.id', item.table_name, item.col_name, item.parent_table;
        END IF;
    END LOOP;

    -- 4. Verify V3 reports.public_id column, UNIQUE, and format CHECK
    IF NOT EXISTS (
        SELECT 1 FROM pg_attribute
        WHERE attrelid = 'public.reports'::regclass
          AND attname = 'public_id'
          AND format_type(atttypid, atttypmod) = 'character varying(30)'
          AND attnotnull
          AND NOT attisdropped
    ) THEN
        RAISE EXCEPTION 'Invalid signature for reports.public_id (expected VARCHAR(30) NOT NULL)';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint c
        JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = ANY(c.conkey)
        WHERE c.conrelid = 'public.reports'::regclass
          AND c.contype = 'u'
          AND a.attname = 'public_id'
    ) THEN
        RAISE EXCEPTION 'Missing UNIQUE constraint on reports.public_id';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint c
        WHERE c.conrelid = 'public.reports'::regclass
          AND c.contype = 'c'
          AND c.convalidated
          AND pg_get_constraintdef(c.oid) ~ 'public_id.*~'
    ) THEN
        RAISE EXCEPTION 'Missing format CHECK constraint on reports.public_id';
    END IF;

    -- 5. Verify all 14 explicit indexes exist
    FOR item IN SELECT * FROM (VALUES
        ('users_email_ci_uq'),
        ('users_username_ci_uq'),
        ('reports_category_status_recent_idx'),
        ('reports_expiration_idx'),
        ('reports_map_bbox_idx'),
        ('reports_status_recent_idx'),
        ('reports_user_history_idx'),
        ('report_votes_user_idx'),
        ('comments_report_page_idx'),
        ('admin_logs_actor_idx'),
        ('admin_logs_recent_idx'),
        ('admin_logs_target_idx'),
        ('refresh_tokens_user_idx'),
        ('refresh_tokens_expiration_idx')
    ) AS idx(index_name) LOOP
        IF to_regclass('public.' || item.index_name) IS NULL THEN
            RAISE EXCEPTION 'Missing expected index: public.%', item.index_name;
        END IF;
    END LOOP;

    -- 6. Verify all 4 updated_at triggers exist
    FOR item IN SELECT * FROM (VALUES
        ('users','users_updated_at'),
        ('reports','reports_updated_at'),
        ('report_votes','report_votes_updated_at'),
        ('comments','comments_updated_at')
    ) AS trg(table_name, trigger_name) LOOP
        IF NOT EXISTS (
            SELECT 1 FROM pg_trigger
            WHERE tgrelid = to_regclass('public.' || item.table_name)
              AND tgname = item.trigger_name
              AND NOT tgisinternal
        ) THEN
            RAISE EXCEPTION 'Missing trigger % on public.%', item.trigger_name, item.table_name;
        END IF;
    END LOOP;

    -- 7. Verify any existing reports satisfy public_id format and non-nullability
    IF EXISTS (
        SELECT 1 FROM public.reports
        WHERE public_id IS NULL OR public_id !~ '^rpt_[0-7][0-9A-HJKMNP-TV-Z]{25}$'
    ) THEN
        RAISE EXCEPTION 'Found invalid or NULL public_id rows in public.reports';
    END IF;
END;
$verify_v3$;

COMMIT;
