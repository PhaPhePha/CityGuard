-- READ ONLY. Run with psql -X -v ON_ERROR_STOP=1 or as a complete DBeaver script.
-- Validates an unupgraded V1 database; no credentials or token hashes are printed.
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY;
SELECT current_database() AS database_name, current_user AS database_user,
       current_setting('server_version') AS server_version;

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

SELECT c.relname AS table_name, con.conname, pg_get_constraintdef(con.oid) AS definition
FROM pg_constraint con JOIN pg_class c ON c.oid=con.conrelid
WHERE con.conrelid IN ('public.reports'::regclass,'public.comments'::regclass,'public.report_votes'::regclass)
  AND con.contype IN ('p','u','f') ORDER BY c.relname,con.conname;
SELECT tablename,indexname,indexdef FROM pg_indexes
WHERE schemaname='public' ORDER BY tablename,indexname;
SELECT event_object_table,trigger_name,action_timing,event_manipulation
FROM information_schema.triggers WHERE trigger_schema='public'
ORDER BY event_object_table,trigger_name;
COMMIT;
