-- V2 immutable migration. Apply ONCE after V1 and an approved backup.
-- PostgreSQL 17+. Standalone SQL: usable by psql or DBeaver Execute SQL Script.
-- Stop on first error. Existing V1 rows are never updated or deleted.
BEGIN;
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '120s';
SET LOCAL search_path = public, pg_catalog;
-- Pause application writers first. These locks also stabilize duplicate checks/backfill.
LOCK TABLE public.users, public.categories, public.reports, public.report_votes,
           public.comments, public.admin_logs IN SHARE ROW EXCLUSIVE MODE;

DO $preflight$
DECLARE
    item record;
    relation regclass;
    key_number smallint;
    actual_type text;
    duplicate_detail text;
BEGIN
    IF current_setting('server_version_num')::integer < 170000 THEN
        RAISE EXCEPTION 'V2 is validated for PostgreSQL 17+; inspect compatibility before upgrading';
    END IF;
    IF to_regclass('public.report_metrics') IS NOT NULL OR to_regclass('public.refresh_tokens') IS NOT NULL THEN
        RAISE EXCEPTION 'V2 table already exists: inspect applied version/partial schema; do not rerun blindly';
    END IF;
    FOR item IN SELECT * FROM (VALUES
        ('users'),('categories'),('reports'),('report_votes'),('comments'),('admin_logs')
    ) AS required(table_name) LOOP
        relation := to_regclass('public.' || item.table_name);
        IF relation IS NULL THEN RAISE EXCEPTION 'Missing V1 table public.%', item.table_name; END IF;
        IF NOT EXISTS (SELECT 1 FROM pg_class WHERE oid=relation AND relkind='r') THEN
            RAISE EXCEPTION 'Expected ordinary V1 table public.%',item.table_name;
        END IF;
        SELECT attnum INTO key_number FROM pg_attribute
        WHERE attrelid=relation AND attname='id' AND NOT attisdropped
          AND atttypid='bigint'::regtype AND attidentity='a' AND attnotnull;
        IF key_number IS NULL OR NOT EXISTS (
            SELECT 1 FROM pg_constraint WHERE conrelid=relation AND contype='p'
                AND conkey=ARRAY[key_number]::smallint[]
        ) THEN RAISE EXCEPTION 'Expected BIGINT GENERATED ALWAYS identity PK: %.id', item.table_name; END IF;
    END LOOP;

    FOR item IN SELECT * FROM (VALUES
        ('reports','user_id','bigint'),('reports','category_id','bigint'),
        ('reports','client_request_id','uuid'),('reports','latitude','double precision'),
        ('reports','longitude','double precision'),('reports','expires_at','timestamp with time zone'),
        ('reports','created_at','timestamp with time zone'),('reports','status','character varying(20)'),
        ('report_votes','report_id','bigint'),('report_votes','user_id','bigint'),
        ('report_votes','vote_type','character varying(12)'),
        ('comments','report_id','bigint'),('comments','user_id','bigint'),
        ('comments','client_request_id','uuid'),('admin_logs','actor_id','bigint')
    ) AS required(table_name,column_name,type_name) LOOP
        SELECT format_type(atttypid,atttypmod) INTO actual_type FROM pg_attribute
        WHERE attrelid=to_regclass('public.'||item.table_name) AND attname=item.column_name
          AND NOT attisdropped AND attnotnull;
        IF actual_type IS DISTINCT FROM item.type_name THEN
            RAISE EXCEPTION 'Incompatible %.%: expected NOT NULL %, found %',
                item.table_name,item.column_name,item.type_name,actual_type;
        END IF;
    END LOOP;

    FOR item IN SELECT * FROM (VALUES
        ('reports','user_id','users'),('reports','category_id','categories'),
        ('report_votes','report_id','reports'),('report_votes','user_id','users'),
        ('comments','report_id','reports'),('comments','user_id','users'),
        ('admin_logs','actor_id','users')
    ) AS required(table_name,column_name,parent_name) LOOP
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint c
            JOIN pg_attribute child ON child.attrelid=c.conrelid AND child.attname=item.column_name
            JOIN pg_attribute parent ON parent.attrelid=c.confrelid AND parent.attname='id'
            WHERE c.conrelid=to_regclass('public.'||item.table_name)
              AND c.confrelid=to_regclass('public.'||item.parent_name)
              AND c.contype='f' AND c.convalidated AND c.confdeltype='r'
              AND c.conkey=ARRAY[child.attnum]::smallint[]
              AND c.confkey=ARRAY[parent.attnum]::smallint[]
        ) THEN RAISE EXCEPTION 'Missing validated RESTRICT FK: %.% -> %.id',
            item.table_name,item.column_name,item.parent_name; END IF;
    END LOOP;

    -- Match range CHECK definitions from the existing baseline, not just names.
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid='public.reports'::regclass
        AND contype='c' AND convalidated
        AND pg_get_constraintdef(oid) ~ 'latitude >=[^0-9-]*-90([^0-9.]|$)'
        AND pg_get_constraintdef(oid) ~ 'latitude <=[^0-9-]*90([^0-9.]|$)'
        AND pg_get_constraintdef(oid) LIKE '% AND %' AND pg_get_constraintdef(oid) NOT LIKE '% OR %')
       OR NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid='public.reports'::regclass
        AND contype='c' AND convalidated
        AND pg_get_constraintdef(oid) ~ 'longitude >=[^0-9-]*-180([^0-9.]|$)'
        AND pg_get_constraintdef(oid) ~ 'longitude <=[^0-9-]*180([^0-9.]|$)'
        AND pg_get_constraintdef(oid) LIKE '% AND %' AND pg_get_constraintdef(oid) NOT LIKE '% OR %') THEN
        RAISE EXCEPTION 'Expected validated latitude/longitude range CHECKs; inspect actual definitions';
    END IF;

    FOR item IN SELECT * FROM (VALUES ('reports'),('comments')) AS target(table_name) LOOP
        EXECUTE format('SELECT string_agg(client_request_id::text || '' (rows='' || n || '')'', '', '')
            FROM (SELECT client_request_id,count(*) n FROM public.%I
            GROUP BY client_request_id HAVING count(*)>1 ORDER BY client_request_id LIMIT 10) d',item.table_name)
            INTO duplicate_detail;
        IF duplicate_detail IS NOT NULL THEN
            RAISE EXCEPTION 'Duplicate client_request_id in %: %',item.table_name,duplicate_detail
                USING HINT='No values were changed. Resolve with the data owner before retrying.';
        END IF;
    END LOOP;
    IF EXISTS (SELECT 1 FROM public.report_votes GROUP BY report_id,user_id HAVING count(*)>1) THEN
        RAISE EXCEPTION 'Duplicate (report_id,user_id) in report_votes; no automatic cleanup';
    END IF;
END;
$preflight$;

-- Recognize an existing equivalent usable unique index instead of duplicating it.
DO $unique_keys$
DECLARE item record;
BEGIN
    FOR item IN SELECT * FROM (VALUES
        ('reports',ARRAY['client_request_id']::text[],'reports_client_request_id_uq'),
        ('comments',ARRAY['client_request_id']::text[],'comments_client_request_id_uq'),
        ('report_votes',ARRAY['report_id','user_id']::text[],'report_votes_report_user_uq')
    ) AS required(table_name,column_names,constraint_name) LOOP
        IF NOT EXISTS (
            SELECT 1 FROM pg_index i
            WHERE i.indrelid=to_regclass('public.'||item.table_name)
              AND i.indisunique AND i.indisvalid AND i.indisready AND i.indimmediate
              AND i.indpred IS NULL AND i.indexprs IS NULL
              AND (SELECT array_agg(a.attname::text ORDER BY a.attname)
                   FROM unnest(i.indkey::smallint[]) WITH ORDINALITY k(attnum,ord)
                   JOIN pg_attribute a ON a.attrelid=i.indrelid AND a.attnum=k.attnum
                   WHERE k.ord<=i.indnkeyatts)
                  = (SELECT array_agg(n ORDER BY n) FROM unnest(item.column_names) n)
        ) THEN
            EXECUTE format('ALTER TABLE public.%I ADD CONSTRAINT %I UNIQUE (%s)',
                item.table_name,item.constraint_name,
                (SELECT string_agg(quote_ident(n),',') FROM unnest(item.column_names) n));
        END IF;
    END LOOP;
END;
$unique_keys$;
-- Keep old (user_id,client_request_id) constraints/indexes for legacy Backend
-- dependencies until their removal is separately approved. V2 queries do not use them.

CREATE TABLE public.report_metrics (
    report_id bigint PRIMARY KEY REFERENCES public.reports(id) ON DELETE RESTRICT,
    confirm_count bigint NOT NULL DEFAULT 0 CHECK (confirm_count >= 0),
    no_longer_count bigint NOT NULL DEFAULT 0 CHECK (no_longer_count >= 0),
    trust_score numeric(5,2),
    trust_label varchar(32),
    computed_at timestamptz CHECK (computed_at IS NULL OR isfinite(computed_at))
);
COMMENT ON TABLE public.report_metrics IS
    'Derived cache. Source of truth is report_votes. Backend must maintain counts in the vote transaction.';
COMMENT ON COLUMN public.report_metrics.computed_at IS
    'Timestamp of Backend trust calculation; NULL means not calculated or invalidated. Backfilled counts alone do not set it.';

CREATE TABLE public.refresh_tokens (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES public.users(id) ON DELETE RESTRICT,
    token_hash varchar(64) NOT NULL UNIQUE CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    expires_at timestamptz NOT NULL CHECK (isfinite(expires_at)),
    revoked_at timestamptz CHECK (revoked_at IS NULL OR isfinite(revoked_at))
);
COMMENT ON COLUMN public.refresh_tokens.token_hash IS
    'Proposed encoding: SHA-256 lowercase hexadecimal of a high-entropy random token; confirm with Backend. Never store raw tokens.';
CREATE INDEX refresh_tokens_user_idx ON public.refresh_tokens (user_id);
CREATE INDEX refresh_tokens_expiration_idx ON public.refresh_tokens (expires_at);

-- Backfill ALL reports, including zero-vote/history/hidden reports, without
-- changing votes, report status or fabricating trust score/label/formula.
INSERT INTO public.report_metrics(report_id,confirm_count,no_longer_count)
SELECT r.id,count(v.id) FILTER (WHERE v.vote_type='CONFIRMED'),
            count(v.id) FILTER (WHERE v.vote_type='NO_LONGER')
FROM public.reports r LEFT JOIN public.report_votes v ON v.report_id=r.id
GROUP BY r.id;
COMMIT;
