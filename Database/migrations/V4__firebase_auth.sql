-- V4 immutable migration. Apply ONCE after V3 and an approved backup.
-- PostgreSQL 17+. Standalone Pure SQL: usable by psql, DBeaver, or migration runners.
-- Stop on first error. Adds Firebase Authentication & OAuth Provider support.
-- Preserves internal BIGINT PK users.id, foreign keys, and all existing data.
BEGIN;

SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '120s';
SET LOCAL search_path = public, pg_catalog;

-- 1. Pause application writers on users to prevent schema mutation deadlocks
LOCK TABLE public.users IN SHARE ROW EXCLUSIVE MODE;

-- 2. Preflight verification: Ensure V3 baseline exists and V4 is not already applied
DO $preflight_v4$
BEGIN
    IF current_setting('server_version_num')::integer < 170000 THEN
        RAISE EXCEPTION 'V4 is validated for PostgreSQL 17+; inspect compatibility before upgrading';
    END IF;

    -- Ensure V3 baseline is present (reports.public_id, report_metrics, refresh_tokens)
    IF to_regclass('public.report_metrics') IS NULL OR to_regclass('public.refresh_tokens') IS NULL THEN
        RAISE EXCEPTION 'V2/V3 baseline missing: report_metrics or refresh_tokens not found';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_attribute
        WHERE attrelid = 'public.reports'::regclass
          AND attname = 'public_id'
          AND NOT attisdropped
    ) THEN
        RAISE EXCEPTION 'V3 baseline missing: reports.public_id not found; apply V3 migration first';
    END IF;

    -- Abort if V4 tables already exist to prevent duplicate application / drift
    IF to_regclass('public.user_firebase_accounts') IS NOT NULL OR to_regclass('public.user_auth_providers') IS NOT NULL THEN
        RAISE EXCEPTION 'V4 tables already exist: schema drift or migration already applied; do not rerun blindly';
    END IF;
END;
$preflight_v4$;

-- 3. Update public.users: Allow password_hash to be NULL for OAuth/Social users
ALTER TABLE public.users ALTER COLUMN password_hash DROP NOT NULL;

-- Drop and recreate users_password_hash_check to explicitly permit NULL
ALTER TABLE public.users DROP CONSTRAINT IF EXISTS users_password_hash_check;
ALTER TABLE public.users ADD CONSTRAINT users_password_hash_check
    CHECK (
        password_hash IS NULL
        OR (password_hash ~ '^\$argon2id\$v=19\$m=[0-9]+,t=[0-9]+,p=[0-9]+\$[A-Za-z0-9+/]+\$[A-Za-z0-9+/]+$'::text)
        OR (password_hash ~ '^\$2[aby]\$[0-9]{2}\$[./A-Za-z0-9]{53}$'::text)
    );

-- 4. Table 1: public.user_firebase_accounts
CREATE TABLE public.user_firebase_accounts (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    firebase_uid character varying(128) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,

    CONSTRAINT user_firebase_accounts_pkey PRIMARY KEY (id),
    CONSTRAINT user_firebase_accounts_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE RESTRICT,
    CONSTRAINT user_firebase_accounts_user_id_uq UNIQUE (user_id),
    CONSTRAINT user_firebase_accounts_firebase_uid_uq UNIQUE (firebase_uid),
    CONSTRAINT user_firebase_accounts_firebase_uid_check CHECK (btrim((firebase_uid)::text) <> ''::text),
    CONSTRAINT user_firebase_accounts_id_user_id_uq UNIQUE (id, user_id)
);

ALTER TABLE public.user_firebase_accounts ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.user_firebase_accounts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TRIGGER user_firebase_accounts_updated_at
    BEFORE UPDATE ON public.user_firebase_accounts
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

-- 5. Table 2: public.user_auth_providers
CREATE TABLE public.user_auth_providers (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    firebase_account_id bigint,
    provider character varying(32) NOT NULL,
    provider_uid character varying(128) NOT NULL,
    email character varying(254),
    display_name character varying(100),
    avatar_url text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,

    CONSTRAINT user_auth_providers_pkey PRIMARY KEY (id),
    CONSTRAINT user_auth_providers_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE RESTRICT,
    CONSTRAINT user_auth_providers_firebase_account_fkey
        FOREIGN KEY (firebase_account_id) REFERENCES public.user_firebase_accounts(id) ON DELETE RESTRICT,
    CONSTRAINT user_auth_providers_composite_user_firebase_fkey
        FOREIGN KEY (firebase_account_id, user_id) REFERENCES public.user_firebase_accounts(id, user_id) ON DELETE RESTRICT,
    CONSTRAINT user_auth_providers_provider_check
        CHECK ((provider)::text = ANY (ARRAY['GOOGLE'::character varying, 'FACEBOOK'::character varying]::text[])),
    CONSTRAINT user_auth_providers_provider_uid_check
        CHECK (btrim((provider_uid)::text) <> ''::text),
    CONSTRAINT user_auth_providers_email_check
        CHECK ((email IS NULL) OR (((email)::text = btrim((email)::text)) AND ((email)::text ~ '^[^[:space:]@]+@[^[:space:]@]+[.][^[:space:]@]+$'::text))),
    CONSTRAINT uq_user_auth_provider_identity UNIQUE (provider, provider_uid),
    CONSTRAINT uq_user_auth_provider_per_user UNIQUE (user_id, provider)
);

ALTER TABLE public.user_auth_providers ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.user_auth_providers_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TRIGGER user_auth_providers_updated_at
    BEFORE UPDATE ON public.user_auth_providers
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

-- 6. Indexes for high-performance querying
CREATE INDEX user_firebase_accounts_user_id_idx ON public.user_firebase_accounts USING btree (user_id);
CREATE INDEX user_auth_providers_user_id_idx ON public.user_auth_providers USING btree (user_id);
CREATE INDEX user_auth_providers_firebase_account_id_idx ON public.user_auth_providers USING btree (firebase_account_id) WHERE (firebase_account_id IS NOT NULL);

-- 7. Documentation comments
COMMENT ON TABLE public.user_firebase_accounts IS
    'Maps internal users.id to Firebase Project authentication identity (firebase_uid). Exactly 1 Firebase account per user.';
COMMENT ON TABLE public.user_auth_providers IS
    'Stores specific third-party identity provider credentials (Google sub, Facebook id) linked to a user. Composite foreign key prevents cross-user mapping.';

COMMIT;
