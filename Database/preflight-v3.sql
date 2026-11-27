-- READ ONLY. Run with psql -X -v ON_ERROR_STOP=1 or as a complete DBeaver script.
-- Validates a V2 database before upgrading to V3; no data changes.
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY;
SELECT current_database() AS database_name, current_user AS database_user,
       current_setting('server_version') AS server_version;

DO $preflight_v3$
BEGIN
    IF current_setting('server_version_num')::integer < 170000 THEN
        RAISE EXCEPTION 'V3 is validated for PostgreSQL 17+; inspect compatibility before upgrading';
    END IF;

    -- Ensure V2 baseline is present
    IF to_regclass('public.report_metrics') IS NULL OR to_regclass('public.refresh_tokens') IS NULL THEN
        RAISE EXCEPTION 'Missing V2 baseline tables (report_metrics, refresh_tokens)';
    END IF;

    IF to_regclass('public.reports') IS NULL THEN
        RAISE EXCEPTION 'Missing public.reports table';
    END IF;

    IF EXISTS (
        SELECT 1 FROM pg_attribute
        WHERE attrelid = 'public.reports'::regclass
          AND attname = 'public_id'
          AND NOT attisdropped
    ) THEN
        RAISE EXCEPTION 'Column reports.public_id already exists; inspect applied version';
    END IF;
END;
$preflight_v3$;

COMMIT;
