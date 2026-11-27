-- V3: apply schema.sql THEN migrations/V2__metrics_tokens_request_keys.sql THEN migrations/V3__report_public_id_ulid.sql first.
-- psql -X -v ON_ERROR_STOP=1 -d cityguard_test -f database/queries.sql
-- PREPARE registers templates in the current connection; it DOES NOT run writes.
-- Backend should use driver bind parameters; never interpolate client text.

-- west,south,east,north,category_ids (NULL=all),status,limit.
-- Supports antimeridian: west > east means crossing +180/-180.
-- Public map only allows PENDING_VERIFY or ACTIVE, and always filters TTL.
PREPARE map_bbox(double precision,double precision,double precision,double precision,bigint[],text,integer) AS
SELECT r.id,r.public_id,category_id,description,image_url,latitude,longitude,address_snapshot,status,created_at,expires_at,
       m.confirm_count,m.no_longer_count,m.trust_score,m.trust_label,m.computed_at
FROM reports r LEFT JOIN report_metrics m ON m.report_id=r.id
WHERE status IN ('PENDING_VERIFY','ACTIVE') AND status=$6 AND expires_at > now()
  AND $1 BETWEEN -180 AND 180 AND $3 BETWEEN -180 AND 180
  AND $2 BETWEEN -90 AND 90 AND $4 BETWEEN -90 AND 90 AND $2 <= $4
  AND latitude BETWEEN $2 AND $4
  AND (($1 <= $3 AND longitude BETWEEN $1 AND $3)
       OR ($1 > $3 AND (longitude >= $1 OR longitude <= $3)))
  AND ($5 IS NULL OR category_id=ANY($5))
ORDER BY r.created_at DESC,r.id DESC
LIMIT LEAST(GREATEST(COALESCE($7,100),1),500);
-- Example: EXECUTE map_bbox(106.65,10.70,106.75,10.85,NULL,'ACTIVE',100);
-- To show both live statuses, Backend executes an equivalent query with
-- status IN ('PENDING_VERIFY','ACTIVE') instead of status=$6.

-- Query a single report by its public readable identifier (rpt_ + ULID).
PREPARE get_report_by_public_id(varchar(30)) AS
SELECT r.id,r.public_id,r.user_id,r.category_id,r.client_request_id,r.description,r.image_url,
       r.latitude,r.longitude,r.address_snapshot,r.status,r.created_at,r.updated_at,r.expires_at,
       m.confirm_count,m.no_longer_count,m.trust_score,m.trust_label,m.computed_at
FROM reports r LEFT JOIN report_metrics m ON m.report_id=r.id
WHERE r.public_id=$1 AND r.status <> 'HIDDEN';

-- My Reports/history: actor comes from authenticated session, not request body.
PREPARE my_reports(bigint,text,timestamptz,bigint,integer) AS
SELECT id,public_id,category_id,description,status,created_at,expires_at
FROM reports WHERE user_id=$1 AND ($2 IS NULL OR status=$2)
  AND ($3 IS NULL OR (created_at,id)<($3,$4))
ORDER BY created_at DESC,id DESC LIMIT LEAST(GREATEST(COALESCE($5,50),1),100);

-- Insert once; Backend generates public_id (rpt_ + ULID) and client_request_id (UUID).
-- TTL snapshot uses current category config and server time.
PREPARE create_report(bigint,bigint,uuid,varchar(30),text,text,double precision,double precision,text) AS
WITH created AS (
INSERT INTO reports(user_id,category_id,client_request_id,public_id,description,image_url,latitude,longitude,address_snapshot,expires_at)
SELECT u.id,c.id,$3,$4,$5,$6,$7,$8,$9,now()+make_interval(secs=>c.default_ttl_seconds)
FROM users u CROSS JOIN categories c
WHERE u.id=$1 AND u.status='ACTIVE' AND c.id=$2 AND c.is_active
ON CONFLICT (client_request_id) DO NOTHING RETURNING id,public_id,status,expires_at
), initialized AS (
 INSERT INTO report_metrics(report_id) SELECT id FROM created RETURNING report_id
)
SELECT c.id,c.public_id,c.status,c.expires_at FROM created c JOIN initialized m ON m.report_id=c.id;
-- On conflict, run a SECOND statement at READ COMMITTED (not same-snapshot CTE).
PREPARE report_retry(bigint,uuid) AS
SELECT id,public_id,category_id,description,image_url,latitude,longitude,address_snapshot,status,expires_at
FROM reports WHERE user_id=$1 AND client_request_id=$2;
-- Compare submitted payload with original; same key + different payload => HTTP 409.
-- If no row on insert OR lookup, reject inactive user/category or retry transaction.
-- Global key belonging to ANOTHER user => conflict, never return their report.

-- Required vote protocol in ONE short READ COMMITTED transaction:
-- BEGIN -> lock_vote_report (must return 1 row) -> put_vote -> refresh_report_metrics
-- -> read counts/label -> COMMIT. On any error ROLLBACK.
-- Serialize ALL vote insert/update/delete paths with this parent row lock.
-- SQL writers/fixtures bypassing this protocol must explicitly resync metrics.
PREPARE lock_vote_report(bigint,bigint) AS
SELECT r.id FROM reports r JOIN users u ON u.id=$1
WHERE r.id=$2 AND r.status<>'HIDDEN' AND u.status='ACTIVE'
FOR UPDATE OF u,r;

PREPARE put_vote(bigint,bigint,text) AS
INSERT INTO report_votes(report_id,user_id,vote_type) VALUES ($1,$2,$3)
ON CONFLICT (report_id,user_id) DO UPDATE SET vote_type=EXCLUDED.vote_type
WHERE report_votes.vote_type IS DISTINCT FROM EXCLUDED.vote_type
RETURNING id,vote_type,updated_at;
-- Same vote yields no RETURNING row; read existing vote and return it as success.
-- Backend must authorize active user and visible report before this statement.

-- Separate statement after put_vote, so READ COMMITTED sees the new vote.
-- Repeated identical votes do not invalidate a previously calculated score.
PREPARE refresh_report_metrics(bigint) AS
INSERT INTO report_metrics(report_id,confirm_count,no_longer_count)
SELECT r.id,count(v.id) FILTER (WHERE v.vote_type='CONFIRMED'),
            count(v.id) FILTER (WHERE v.vote_type='NO_LONGER')
FROM reports r LEFT JOIN report_votes v ON v.report_id=r.id WHERE r.id=$1 GROUP BY r.id
ON CONFLICT (report_id) DO UPDATE
SET confirm_count=EXCLUDED.confirm_count,no_longer_count=EXCLUDED.no_longer_count,
    trust_score=NULL,trust_label=NULL,computed_at=NULL
WHERE (report_metrics.confirm_count,report_metrics.no_longer_count)
      IS DISTINCT FROM (EXCLUDED.confirm_count,EXCLUDED.no_longer_count)
RETURNING report_id,confirm_count,no_longer_count;

-- Aggregate from authoritative vote rows, no cached counters that can drift.
PREPARE report_vote_counts(bigint) AS
SELECT count(*) FILTER (WHERE vote_type='CONFIRMED') AS confirmed_count,
       count(*) FILTER (WHERE vote_type='NO_LONGER') AS no_longer_count,
       count(*) AS total_votes
FROM report_votes WHERE report_id=$1;

PREPARE create_comment(bigint,bigint,uuid,text) AS
INSERT INTO comments(report_id,user_id,client_request_id,body) VALUES ($1,$2,$3,$4)
ON CONFLICT (client_request_id) DO NOTHING RETURNING id,created_at;
PREPARE comment_retry(bigint,uuid) AS
SELECT id,report_id,body,created_at FROM comments WHERE user_id=$1 AND client_request_id=$2;
-- Compare report_id AND body for 409; keep key even after soft delete.
-- A key owned by another user must NOT reveal their comment or be accepted as a retry.

PREPARE visible_comments(bigint,timestamptz,bigint,integer) AS
SELECT c.id,c.user_id,u.display_name,c.body,c.created_at,c.updated_at
FROM comments c JOIN users u ON u.id=c.user_id
JOIN reports r ON r.id=c.report_id AND r.status <> 'HIDDEN'
WHERE c.report_id=$1 AND NOT c.is_hidden AND c.deleted_at IS NULL
  AND ($2 IS NULL OR (c.created_at,c.id)<($2,$3))
ORDER BY c.created_at DESC,c.id DESC LIMIT LEAST(GREATEST(COALESCE($4,50),1),100);

PREPARE delete_own_comment(bigint,bigint) AS
UPDATE comments SET deleted_at=statement_timestamp()
WHERE id=$1 AND user_id=$2 AND deleted_at IS NULL RETURNING id;

-- Background job: repeat batches until zero rows; safe with multiple workers.
PREPARE expire_reports(integer) AS
WITH due AS (
 SELECT id FROM reports WHERE status IN ('PENDING_VERIFY','ACTIVE') AND expires_at<=now()
 ORDER BY expires_at,id LIMIT LEAST(GREATEST(COALESCE($1,1000),1),5000)
 FOR UPDATE SKIP LOCKED
)
UPDATE reports r SET status='EXPIRED' FROM due WHERE r.id=due.id RETURNING r.id;

-- One statement/snapshot: avoid joining raw votes/comments and multiplying counts.
PREPARE admin_dashboard AS
SELECT (SELECT count(*) FROM users) AS total_accounts,
       (SELECT count(*) FROM users WHERE role='USER') AS end_users,
       count(*) AS total_reports,
       count(*) FILTER (WHERE status='ACTIVE' AND expires_at>now()) AS active_reports,
       count(*) FILTER (WHERE status='PENDING_VERIFY' AND expires_at>now()) AS pending_reports,
       count(*) FILTER (WHERE status IN ('PENDING_VERIFY','ACTIVE') AND expires_at<=now()) AS overdue_unprocessed,
       count(*) FILTER (WHERE status='HIDDEN') AS hidden_reports
FROM reports;
PREPARE profile_summary(bigint) AS
SELECT (SELECT count(*) FROM reports WHERE user_id=$1) AS report_count,
       (SELECT count(*) FROM report_votes WHERE user_id=$1 AND vote_type='CONFIRMED') AS confirmed_reports;

-- Backend authorizes actor role, then runs this atomic update + audit insert.
-- $1 report id, $2 actor id, $3 reason. Role is checked again in SQL.
PREPARE hide_report(bigint,bigint,text) AS
WITH changed AS (
 UPDATE reports SET status='HIDDEN'
 WHERE id=$1 AND status<>'HIDDEN' AND EXISTS (
   SELECT 1 FROM users WHERE id=$2 AND status='ACTIVE' AND role IN ('ADMIN','MODERATOR'))
 RETURNING id
)
INSERT INTO admin_logs(actor_id,action,target_type,target_id,reason)
SELECT $2,'HIDE_REPORT','REPORT',id,$3 FROM changed RETURNING id;
-- Lock/unlock users and hide/restore comments follow the same atomic pattern.
-- Never expose prepared write templates directly to Android/Admin clients.

-- Token plaintext is generated and hashed by Backend; bind only lowercase hex hash.
-- Token lifetime is supplied explicitly by Backend, not chosen by these queries.
PREPARE issue_refresh_token(bigint,text,timestamptz) AS
INSERT INTO refresh_tokens(user_id,token_hash,expires_at)
SELECT id,$2,$3 FROM users WHERE id=$1 AND status='ACTIVE' AND $3>now()
RETURNING id,user_id,expires_at;

-- Use inside a transaction. Rotation callers must use the lock order users ->
-- refresh_tokens consistently and verify this returns exactly one row.
PREPARE lock_refresh_token_user(text) AS
SELECT u.id FROM users u WHERE u.status='ACTIVE' AND EXISTS (
 SELECT 1 FROM refresh_tokens t WHERE t.user_id=u.id AND t.token_hash=$1
   AND t.revoked_at IS NULL AND t.expires_at>now()) FOR UPDATE OF u;
PREPARE valid_refresh_token(text) AS
SELECT t.id,t.user_id,t.expires_at FROM refresh_tokens t JOIN users u ON u.id=t.user_id
WHERE t.token_hash=$1 AND t.revoked_at IS NULL AND t.expires_at>now() AND u.status='ACTIVE'
FOR UPDATE OF t;
PREPARE revoke_refresh_token(bigint,text) AS
UPDATE refresh_tokens SET revoked_at=statement_timestamp()
WHERE user_id=$1 AND token_hash=$2 AND revoked_at IS NULL RETURNING id;
PREPARE revoke_user_refresh_tokens(bigint) AS
UPDATE refresh_tokens SET revoked_at=statement_timestamp()
WHERE user_id=$1 AND revoked_at IS NULL RETURNING id;
-- Rotation: lock_refresh_token_user -> valid_refresh_token -> revoke old
-- (must affect 1 row) -> issue new hash -> COMMIT; otherwise ROLLBACK.
-- No family/reuse detection is claimed with these five token columns.
