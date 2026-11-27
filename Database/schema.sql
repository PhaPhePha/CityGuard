-- CityGuard V1 Baseline Schema (Pure SQL)
-- Authoritative baseline matching migrations/V1__initial_schema.sql.
-- Creates the 6 foundational V1 tables, constraints, indexes, triggers, and foreign keys.
-- No psql meta-commands, no hardcoded role ownership, no sample data.

BEGIN;

SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '120s';
SET LOCAL search_path = public, pg_catalog;

CREATE FUNCTION public.set_updated_at() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at := statement_timestamp();
    RETURN NEW;
END;
$$;

CREATE TABLE public.users (
    id bigint NOT NULL,
    username character varying(50),
    email character varying(254),
    password_hash text NOT NULL,
    display_name character varying(100) NOT NULL,
    avatar_url text,
    role character varying(16) DEFAULT 'USER'::character varying NOT NULL,
    status character varying(16) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT users_check CHECK (((username IS NOT NULL) OR (email IS NOT NULL))),
    CONSTRAINT users_display_name_check CHECK (((char_length(btrim((display_name)::text)) >= 1) AND (char_length(btrim((display_name)::text)) <= 100))),
    CONSTRAINT users_email_check CHECK (((email IS NULL) OR (((email)::text = btrim((email)::text)) AND ((email)::text ~ '^[^[:space:]@]+@[^[:space:]@]+[.][^[:space:]@]+$'::text)))),
    CONSTRAINT users_password_hash_check CHECK (((password_hash ~ '^\$argon2id\$v=19\$m=[0-9]+,t=[0-9]+,p=[0-9]+\$[A-Za-z0-9+/]+\$[A-Za-z0-9+/]+$'::text) OR (password_hash ~ '^\$2[aby]\$[0-9]{2}\$[./A-Za-z0-9]{53}$'::text))),
    CONSTRAINT users_role_check CHECK (((role)::text = ANY ((ARRAY['USER'::character varying, 'MODERATOR'::character varying, 'ADMIN'::character varying])::text[]))),
    CONSTRAINT users_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'LOCKED'::character varying])::text[]))),
    CONSTRAINT users_username_check CHECK (((username IS NULL) OR (((username)::text = btrim((username)::text)) AND ((username)::text ~ '^[A-Za-z0-9_]{3,50}$'::text))))
);

ALTER TABLE public.users ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE public.categories (
    id bigint NOT NULL,
    code character varying(40) NOT NULL,
    display_name character varying(100) NOT NULL,
    icon character varying(80) NOT NULL,
    color character varying(7) NOT NULL,
    default_ttl_seconds integer NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    CONSTRAINT categories_code_check CHECK (((code)::text ~ '^[A-Z][A-Z0-9_]*$'::text)),
    CONSTRAINT categories_color_check CHECK (((color)::text ~ '^#[0-9A-Fa-f]{6}$'::text)),
    CONSTRAINT categories_default_ttl_seconds_check CHECK ((default_ttl_seconds > 0)),
    CONSTRAINT categories_display_name_check CHECK ((btrim((display_name)::text) <> ''::text)),
    CONSTRAINT categories_icon_check CHECK ((btrim((icon)::text) <> ''::text))
);

ALTER TABLE public.categories ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.categories_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE public.reports (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    category_id bigint NOT NULL,
    client_request_id uuid NOT NULL,
    description character varying(2000),
    image_url text,
    latitude double precision NOT NULL,
    longitude double precision NOT NULL,
    address_snapshot character varying(500),
    status character varying(20) DEFAULT 'PENDING_VERIFY'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    CONSTRAINT reports_check CHECK ((isfinite(created_at) AND isfinite(expires_at) AND (expires_at > created_at))),
    CONSTRAINT reports_description_check CHECK (((description IS NULL) OR (btrim((description)::text) <> ''::text))),
    CONSTRAINT reports_image_url_check CHECK (((image_url IS NULL) OR (image_url ~ '^https://[^[:space:]]+$'::text))),
    CONSTRAINT reports_latitude_check CHECK (((latitude >= ('-90'::integer)::double precision) AND (latitude <= (90)::double precision))),
    CONSTRAINT reports_longitude_check CHECK (((longitude >= ('-180'::integer)::double precision) AND (longitude <= (180)::double precision))),
    CONSTRAINT reports_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING_VERIFY'::character varying, 'ACTIVE'::character varying, 'EXPIRED'::character varying, 'RESOLVED'::character varying, 'HIDDEN'::character varying])::text[])))
);

ALTER TABLE public.reports ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.reports_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE public.report_votes (
    id bigint NOT NULL,
    report_id bigint NOT NULL,
    user_id bigint NOT NULL,
    vote_type character varying(12) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT report_votes_vote_type_check CHECK (((vote_type)::text = ANY ((ARRAY['CONFIRMED'::character varying, 'NO_LONGER'::character varying])::text[])))
);

ALTER TABLE public.report_votes ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.report_votes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE public.comments (
    id bigint NOT NULL,
    report_id bigint NOT NULL,
    user_id bigint NOT NULL,
    client_request_id uuid NOT NULL,
    body character varying(2000) NOT NULL,
    is_hidden boolean DEFAULT false NOT NULL,
    deleted_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT comments_body_check CHECK (((char_length(btrim((body)::text)) >= 1) AND (char_length(btrim((body)::text)) <= 2000)))
);

ALTER TABLE public.comments ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.comments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE public.admin_logs (
    id bigint NOT NULL,
    actor_id bigint NOT NULL,
    action character varying(60) NOT NULL,
    target_type character varying(16) NOT NULL,
    target_id bigint NOT NULL,
    reason character varying(1000),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT admin_logs_action_check CHECK ((btrim((action)::text) <> ''::text)),
    CONSTRAINT admin_logs_target_id_check CHECK ((target_id > 0)),
    CONSTRAINT admin_logs_target_type_check CHECK (((target_type)::text = ANY ((ARRAY['USER'::character varying, 'REPORT'::character varying, 'COMMENT'::character varying, 'CATEGORY'::character varying])::text[])))
);

ALTER TABLE public.admin_logs ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.admin_logs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.categories
    ADD CONSTRAINT categories_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.categories
    ADD CONSTRAINT categories_code_key UNIQUE (code);

ALTER TABLE ONLY public.reports
    ADD CONSTRAINT reports_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.reports
    ADD CONSTRAINT reports_user_id_client_request_id_key UNIQUE (user_id, client_request_id);

ALTER TABLE ONLY public.report_votes
    ADD CONSTRAINT report_votes_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.report_votes
    ADD CONSTRAINT report_votes_report_id_user_id_key UNIQUE (report_id, user_id);

ALTER TABLE ONLY public.comments
    ADD CONSTRAINT comments_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.comments
    ADD CONSTRAINT comments_user_id_client_request_id_key UNIQUE (user_id, client_request_id);

ALTER TABLE ONLY public.admin_logs
    ADD CONSTRAINT admin_logs_pkey PRIMARY KEY (id);

CREATE UNIQUE INDEX users_email_ci_uq ON public.users USING btree (lower((email)::text)) WHERE (email IS NOT NULL);

CREATE UNIQUE INDEX users_username_ci_uq ON public.users USING btree (lower((username)::text)) WHERE (username IS NOT NULL);

CREATE INDEX reports_category_status_recent_idx ON public.reports USING btree (category_id, status, created_at DESC, id DESC);

CREATE INDEX reports_expiration_idx ON public.reports USING btree (expires_at, id) WHERE ((status)::text = ANY ((ARRAY['PENDING_VERIFY'::character varying, 'ACTIVE'::character varying])::text[]));

CREATE INDEX reports_map_bbox_idx ON public.reports USING btree (latitude, longitude) WHERE ((status)::text = ANY ((ARRAY['PENDING_VERIFY'::character varying, 'ACTIVE'::character varying])::text[]));

CREATE INDEX reports_status_recent_idx ON public.reports USING btree (status, created_at DESC, id DESC);

CREATE INDEX reports_user_history_idx ON public.reports USING btree (user_id, created_at DESC, id DESC);

CREATE INDEX report_votes_user_idx ON public.report_votes USING btree (user_id, updated_at DESC, id DESC);

CREATE INDEX comments_report_page_idx ON public.comments USING btree (report_id, created_at DESC, id DESC);

CREATE INDEX admin_logs_actor_idx ON public.admin_logs USING btree (actor_id, created_at DESC, id DESC);

CREATE INDEX admin_logs_recent_idx ON public.admin_logs USING btree (created_at DESC, id DESC);

CREATE INDEX admin_logs_target_idx ON public.admin_logs USING btree (target_type, target_id, created_at DESC, id DESC);

CREATE TRIGGER users_updated_at BEFORE UPDATE ON public.users FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE TRIGGER reports_updated_at BEFORE UPDATE ON public.reports FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE TRIGGER report_votes_updated_at BEFORE UPDATE ON public.report_votes FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE TRIGGER comments_updated_at BEFORE UPDATE ON public.comments FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

ALTER TABLE ONLY public.reports
    ADD CONSTRAINT reports_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.reports
    ADD CONSTRAINT reports_category_id_fkey FOREIGN KEY (category_id) REFERENCES public.categories(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.report_votes
    ADD CONSTRAINT report_votes_report_id_fkey FOREIGN KEY (report_id) REFERENCES public.reports(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.report_votes
    ADD CONSTRAINT report_votes_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.comments
    ADD CONSTRAINT comments_report_id_fkey FOREIGN KEY (report_id) REFERENCES public.reports(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.comments
    ADD CONSTRAINT comments_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.admin_logs
    ADD CONSTRAINT admin_logs_actor_id_fkey FOREIGN KEY (actor_id) REFERENCES public.users(id) ON DELETE RESTRICT;

COMMIT;
