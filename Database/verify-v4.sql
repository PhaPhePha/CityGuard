-- READ ONLY Post-Upgrade Verification for CityGuard Database V4.
-- Pure SQL: usable in psql (-v ON_ERROR_STOP=1) or DBeaver Execute SQL Script.
-- Validates all 10 tables, primary keys, 13 RESTRICT foreign keys (including composite FK),
-- unique/check constraints, triggers, indexes, and users.password_hash nullability.
-- Makes zero changes to data or schema.

BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY;

DO $verify_v4$
DECLARE
    item record;
    relation regclass;
    cnt integer;
BEGIN
    -- 1. Verify all 10 tables exist as ordinary tables
    FOR item IN SELECT * FROM (VALUES
        ('users'), ('categories'), ('reports'), ('report_votes'),
        ('comments'), ('admin_logs'), ('report_metrics'), ('refresh_tokens'),
        ('user_firebase_accounts'), ('user_auth_providers')
    ) AS t(table_name) LOOP
        relation := to_regclass('public.' || item.table_name);
        IF relation IS NULL OR NOT EXISTS (
            SELECT 1 FROM pg_class WHERE oid = relation AND relkind = 'r'
        ) THEN
            RAISE EXCEPTION 'Missing V4 table: public.%', item.table_name;
        END IF;
    END LOOP;

    -- 2. Verify Primary Keys on all 10 tables
    FOR item IN SELECT * FROM (VALUES
        ('users','id',true), ('categories','id',true), ('reports','id',true),
        ('report_votes','id',true), ('comments','id',true), ('admin_logs','id',true),
        ('report_metrics','report_id',false), ('refresh_tokens','id',true),
        ('user_firebase_accounts','id',true), ('user_auth_providers','id',true)
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

    -- 3. Verify single-column Foreign Keys with ON DELETE RESTRICT (12 FKs)
    FOR item IN SELECT * FROM (VALUES
        ('reports','user_id','users'),
        ('reports','category_id','categories'),
        ('report_votes','report_id','reports'),
        ('report_votes','user_id','users'),
        ('comments','report_id','reports'),
        ('comments','user_id','users'),
        ('admin_logs','actor_id','users'),
        ('report_metrics','report_id','reports'),
        ('refresh_tokens','user_id','users'),
        ('user_firebase_accounts','user_id','users'),
        ('user_auth_providers','user_id','users'),
        ('user_auth_providers','firebase_account_id','user_firebase_accounts')
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

    -- 4. Verify Composite Foreign Key: user_auth_providers(firebase_account_id, user_id)
    -- -> user_firebase_accounts(id, user_id) ON DELETE RESTRICT
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint c
        WHERE c.conrelid = 'public.user_auth_providers'::regclass
          AND c.confrelid = 'public.user_firebase_accounts'::regclass
          AND c.contype = 'f'
          AND c.convalidated
          AND c.confdeltype = 'r'
          AND c.conname = 'user_auth_providers_composite_user_firebase_fkey'
    ) THEN
        RAISE EXCEPTION 'Missing validated Composite RESTRICT FK: user_auth_providers(firebase_account_id, user_id) -> user_firebase_accounts(id, user_id)';
    END IF;

    -- 5. Verify users.password_hash is nullable
    IF EXISTS (
        SELECT 1 FROM pg_attribute
        WHERE attrelid = 'public.users'::regclass
          AND attname = 'password_hash'
          AND attnotnull
          AND NOT attisdropped
    ) THEN
        RAISE EXCEPTION 'users.password_hash is still NOT NULL; expected nullable in V4';
    END IF;

    -- 6. Verify users_password_hash_check constraint exists and allows NULL
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint c
        WHERE c.conrelid = 'public.users'::regclass
          AND c.conname = 'users_password_hash_check'
          AND c.contype = 'c'
          AND pg_get_constraintdef(c.oid) LIKE '%password_hash IS NULL%'
    ) THEN
        RAISE EXCEPTION 'Missing or incorrect users_password_hash_check constraint';
    END IF;

    -- 7. Verify Unique Constraints on V4 tables
    FOR item IN SELECT * FROM (VALUES
        ('user_firebase_accounts', 'user_firebase_accounts_user_id_uq'),
        ('user_firebase_accounts', 'user_firebase_accounts_firebase_uid_uq'),
        ('user_firebase_accounts', 'user_firebase_accounts_id_user_id_uq'),
        ('user_auth_providers', 'uq_user_auth_provider_identity'),
        ('user_auth_providers', 'uq_user_auth_provider_per_user')
    ) AS uq(table_name, constraint_name) LOOP
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint c
            WHERE c.conrelid = to_regclass('public.' || item.table_name)
              AND c.conname = item.constraint_name
              AND c.contype = 'u'
        ) THEN
            RAISE EXCEPTION 'Missing UNIQUE constraint % on %', item.constraint_name, item.table_name;
        END IF;
    END LOOP;

    -- 8. Verify Check constraints on V4 tables
    FOR item IN SELECT * FROM (VALUES
        ('user_firebase_accounts', 'user_firebase_accounts_firebase_uid_check'),
        ('user_auth_providers', 'user_auth_providers_provider_check'),
        ('user_auth_providers', 'user_auth_providers_provider_uid_check'),
        ('user_auth_providers', 'user_auth_providers_email_check')
    ) AS chk(table_name, constraint_name) LOOP
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint c
            WHERE c.conrelid = to_regclass('public.' || item.table_name)
              AND c.conname = item.constraint_name
              AND c.contype = 'c'
        ) THEN
            RAISE EXCEPTION 'Missing CHECK constraint % on %', item.constraint_name, item.table_name;
        END IF;
    END LOOP;

    -- 9. Verify updated_at triggers on V4 tables
    FOR item IN SELECT * FROM (VALUES
        ('user_firebase_accounts', 'user_firebase_accounts_updated_at'),
        ('user_auth_providers', 'user_auth_providers_updated_at')
    ) AS trg(table_name, trigger_name) LOOP
        IF NOT EXISTS (
            SELECT 1 FROM pg_trigger t
            WHERE t.tgrelid = to_regclass('public.' || item.table_name)
              AND t.tgname = item.trigger_name
              AND NOT t.tgisinternal
        ) THEN
            RAISE EXCEPTION 'Missing updated_at trigger % on %', item.trigger_name, item.table_name;
        END IF;
    END LOOP;

    -- 10. Verify explicit B-tree indexes
    FOR item IN SELECT * FROM (VALUES
        ('user_firebase_accounts_user_id_idx'),
        ('user_auth_providers_user_id_idx'),
        ('user_auth_providers_firebase_account_id_idx')
    ) AS idx(index_name) LOOP
        IF NOT EXISTS (
            SELECT 1 FROM pg_class c
            JOIN pg_namespace n ON n.oid = c.relnamespace
            WHERE c.relname = item.index_name
              AND c.relkind = 'i'
              AND n.nspname = 'public'
        ) THEN
            RAISE EXCEPTION 'Missing index: %', item.index_name;
        END IF;
    END LOOP;

    RAISE NOTICE 'Post-Upgrade V4 Verification PASSED: All 10 tables, 13 RESTRICT FKs, Composite FK, nullability, unique/check constraints, triggers, and indexes are valid.';
END;
$verify_v4$;

COMMIT;