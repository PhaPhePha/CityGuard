-- V3 Pure SQL seed: apply baseline V1 then V2 then V3 migration first. Development/demo/test only.
-- 100% Pure SQL (no psql backslash meta-commands); compatible with psql, DBeaver, and JDBC runners.
-- Per-account BCrypt cost 12 hashes. No plaintext passwords are stored here.
-- Existing accounts are preserved; password resets use update-demo-passwords.sql.
BEGIN;

SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '120s';
SET LOCAL search_path = public, pg_catalog;

-- Seed must not run on system databases or attach demo content to a conflicting pre-existing account.
DO $seed_guard$
BEGIN
 IF current_database() IN ('postgres','template0','template1') THEN
  RAISE EXCEPTION 'Refusing to seed PostgreSQL system database %', current_database(); END IF;
 IF to_regclass('public.report_metrics') IS NULL OR to_regclass('public.refresh_tokens') IS NULL THEN
  RAISE EXCEPTION 'Apply V2 migration before this seed'; END IF;
 IF NOT EXISTS (
  SELECT 1 FROM pg_attribute
  WHERE attrelid='public.reports'::regclass AND attname='public_id' AND NOT attisdropped
 ) THEN
  RAISE EXCEPTION 'Apply V3 migration before this seed'; END IF;
END;
$seed_guard$;
LOCK TABLE public.users,public.categories,public.reports,public.report_votes,public.comments
 IN SHARE ROW EXCLUSIVE MODE;
DO $demo_identity$
BEGIN
 IF EXISTS (
  SELECT 1 FROM public.users u JOIN (VALUES
   ('demo_user_a','user.a@example.invalid'),('demo_user_b','user.b@example.invalid'),
   ('demo_moderator','moderator@example.invalid'),('demo_admin','admin@example.invalid'),
   ('demo_locked','locked@example.invalid')
  ) e(username,email) ON lower(u.username)=lower(e.username) OR lower(u.email)=lower(e.email)
  WHERE u.username IS DISTINCT FROM e.username OR u.email IS DISTINCT FROM e.email
 ) THEN RAISE EXCEPTION 'Demo username/email conflict; seed aborted without overwriting users'; END IF;
 IF EXISTS (
  SELECT 1 FROM public.reports r JOIN public.users u ON u.id=r.user_id
  WHERE r.client_request_id IN (
   '10000000-0000-4000-8000-000000000001'::uuid,'10000000-0000-4000-8000-000000000002'::uuid,
   '10000000-0000-4000-8000-000000000003'::uuid,'10000000-0000-4000-8000-000000000004'::uuid,
   '10000000-0000-4000-8000-000000000005'::uuid,'10000000-0000-4000-8000-000000000006'::uuid)
  AND u.username<>'demo_user_a'
 ) THEN RAISE EXCEPTION 'Demo report request key owned by another user; seed aborted'; END IF;
 IF EXISTS (
  SELECT 1 FROM public.reports r JOIN public.users u ON u.id=r.user_id
  WHERE r.public_id IN (
   'rpt_01M7WBY80FZNE4EYTVWZ00JM0J','rpt_01M7WD2VWFWZHEHDHRKW7KJ0P8',
   'rpt_01M77TFDTF03ZVCGPW33ZW0T32','rpt_01M7W3BJYFK4V98Z640GXVESNM',
   'rpt_01M7WA7A6FN0ECW33ERQCN8TKR','rpt_01M7WA7F2QB19EYCH95NW9NGGJ')
  AND u.username<>'demo_user_a'
 ) THEN RAISE EXCEPTION 'Demo report public_id owned by another user; seed aborted'; END IF;
 IF EXISTS (
  SELECT 1 FROM public.comments c JOIN public.users u ON u.id=c.user_id
  JOIN public.reports r ON r.id=c.report_id JOIN public.users owner ON owner.id=r.user_id
  WHERE c.client_request_id IN (
   '20000000-0000-4000-8000-000000000001'::uuid,'20000000-0000-4000-8000-000000000002'::uuid,
   '20000000-0000-4000-8000-000000000003'::uuid)
  AND (u.username<>'demo_user_b' OR owner.username<>'demo_user_a'
       OR r.client_request_id<>'10000000-0000-4000-8000-000000000001'::uuid)
 ) THEN RAISE EXCEPTION 'Demo comment request key conflicts with existing content; seed aborted'; END IF;
END;
$demo_identity$;
-- Provisional TTL values, NOT approved business rules.
INSERT INTO categories (code, display_name, icon, color, default_ttl_seconds) VALUES
 ('TRAFFIC_JAM','Kẹt xe','traffic_jam','#EF4444',7200),
 ('ROAD_BLOCK','Vật cản','road_block','#F97316',14400),
 ('ROAD_DAMAGE','Đường hư','road_damage','#A16207',604800),
 ('BROKEN_TRAFFIC_LIGHT','Đèn giao thông hỏng','broken_traffic_light','#EAB308',86400),
 ('FALLEN_TREE','Cây đổ','fallen_tree','#16A34A',86400),
 ('FLOOD','Ngập nước','flood','#2563EB',21600)
ON CONFLICT (code) DO NOTHING;

INSERT INTO users (username,email,password_hash,display_name,role,status) VALUES
 ('demo_user_a','user.a@example.invalid', '$2b$12$ab6c/.yK54zFN8zSlvgTIuZ98Mw34jGOcUbBQAsRXgjFAPYwrQkRi','Người dùng mẫu A','USER','ACTIVE'),
 ('demo_user_b','user.b@example.invalid', '$2b$12$IOu3l.GG4gk9BUx.8CKFI.42eDtOIDNRbBo4w/DniVUpY.fAlR/Xq','Người dùng mẫu B','USER','ACTIVE'),
 ('demo_moderator','moderator@example.invalid', '$2b$12$RtgClWXsM8VWf5YcyFD6VO22q0BpMpUMHNebBqRwQ2XGbdQ9MoofS','Kiểm duyệt mẫu','MODERATOR','ACTIVE'),
 ('demo_admin','admin@example.invalid', '$2b$12$aVJQUMKlTkwIIpbRlqDFKuy/UAJ/1nxBVlvXKNmEHvQWuwzTYEn5.','Quản trị mẫu','ADMIN','ACTIVE'),
 ('demo_locked','locked@example.invalid', '$2b$12$WTf3pjs1nauYEuGDV0I6Z.IntoQIlYoRLjmJ3AKwOSdmFbbuxG17y','Tài khoản khóa mẫu','USER','LOCKED')
ON CONFLICT DO NOTHING;

INSERT INTO reports (user_id,category_id,client_request_id,public_id,description,image_url,latitude,longitude,address_snapshot,status,created_at,expires_at)
SELECT u.id,c.id,v.request_id::uuid,v.public_id,v.description,'https://example.invalid/cityguard/demo.jpg',
       v.lat,v.lon,v.address,v.status,now()+v.created_offset,now()+v.expires_offset
FROM (VALUES
 ('10000000-0000-4000-8000-000000000001','rpt_01M7WBY80FZNE4EYTVWZ00JM0J','TRAFFIC_JAM','Xe đông, di chuyển chậm',10.7769,106.7009,'Khu vực Quận 1, TP.HCM','ACTIVE',interval '-30 minutes',interval '90 minutes'),
 ('10000000-0000-4000-8000-000000000002','rpt_01M7WD2VWFWZHEHDHRKW7KJ0P8','ROAD_BLOCK','Vật cản trên đường',10.7820,106.6950,'TP.HCM - vị trí minh họa','PENDING_VERIFY',interval '-10 minutes',interval '230 minutes'),
 ('10000000-0000-4000-8000-000000000003','rpt_01M77TFDTF03ZVCGPW33ZW0T32','ROAD_DAMAGE','Mặt đường hư hỏng',10.7900,106.7100,'TP.HCM - vị trí minh họa','EXPIRED',interval '-8 days',interval '-1 day'),
 ('10000000-0000-4000-8000-000000000004','rpt_01M7W3BJYFK4V98Z640GXVESNM','FLOOD','Nước đã rút',10.7600,106.6800,'TP.HCM - vị trí minh họa','RESOLVED',interval '-3 hours',interval '3 hours'),
 ('10000000-0000-4000-8000-000000000005','rpt_01M7WA7A6FN0ECW33ERQCN8TKR','BROKEN_TRAFFIC_LIGHT','Nội dung chờ kiểm tra',10.7700,106.6900,'TP.HCM - vị trí minh họa','HIDDEN',interval '-1 hour',interval '23 hours'),
 ('10000000-0000-4000-8000-000000000006','rpt_01M7WA7F2QB19EYCH95NW9NGGJ','FALLEN_TREE','Cây đổ chắn lối đi',10.8000,106.7200,'TP.HCM - vị trí minh họa','ACTIVE',interval '-1 hour',interval '23 hours')
) AS v(request_id,public_id,code,description,lat,lon,address,status,created_offset,expires_offset)
JOIN categories c ON c.code=v.code
JOIN users u ON u.username='demo_user_a'
ON CONFLICT (client_request_id) DO NOTHING;

INSERT INTO report_votes (report_id,user_id,vote_type)
SELECT r.id,u.id,v.vote_type
FROM (VALUES
 ('10000000-0000-4000-8000-000000000001','CONFIRMED'),
 ('10000000-0000-4000-8000-000000000004','NO_LONGER')
) v(request_id,vote_type)
JOIN reports r ON r.client_request_id=v.request_id::uuid
JOIN users owner ON owner.id=r.user_id AND owner.username='demo_user_a'
JOIN users u ON u.username='demo_user_b'
ON CONFLICT (report_id,user_id) DO NOTHING;

INSERT INTO comments (report_id,user_id,client_request_id,body,is_hidden,deleted_at)
SELECT r.id,u.id,v.request_id::uuid,v.body,v.hidden,CASE WHEN v.deleted THEN now() END
FROM (VALUES
 ('20000000-0000-4000-8000-000000000001','Tôi cũng thấy xe đông ở đây.',false,false),
 ('20000000-0000-4000-8000-000000000002','Bình luận mẫu đã ẩn.',true,false),
 ('20000000-0000-4000-8000-000000000003','Bình luận mẫu đã xóa mềm.',false,true)
) v(request_id,body,hidden,deleted)
JOIN users u ON u.username='demo_user_b'
JOIN reports r ON r.client_request_id='10000000-0000-4000-8000-000000000001'::uuid
JOIN users owner ON owner.id=r.user_id AND owner.username='demo_user_a'
ON CONFLICT (client_request_id) DO NOTHING;
-- Synchronize only the six demo reports; leave unrelated cache rows untouched.
INSERT INTO public.report_metrics(report_id,confirm_count,no_longer_count)
SELECT r.id,count(v.id) FILTER (WHERE v.vote_type='CONFIRMED'),
            count(v.id) FILTER (WHERE v.vote_type='NO_LONGER')
FROM public.reports r JOIN public.users u ON u.id=r.user_id
LEFT JOIN public.report_votes v ON v.report_id=r.id
WHERE u.username='demo_user_a' AND r.client_request_id IN (
 '10000000-0000-4000-8000-000000000001'::uuid,'10000000-0000-4000-8000-000000000002'::uuid,
 '10000000-0000-4000-8000-000000000003'::uuid,'10000000-0000-4000-8000-000000000004'::uuid,
 '10000000-0000-4000-8000-000000000005'::uuid,'10000000-0000-4000-8000-000000000006'::uuid)
GROUP BY r.id
ON CONFLICT (report_id) DO UPDATE SET
 confirm_count=EXCLUDED.confirm_count,no_longer_count=EXCLUDED.no_longer_count,
 trust_score=NULL,trust_label=NULL,computed_at=NULL
WHERE (report_metrics.confirm_count,report_metrics.no_longer_count)
 IS DISTINCT FROM (EXCLUDED.confirm_count,EXCLUDED.no_longer_count);
COMMIT;
-- Deliberately no fake admin logs: seeding fixtures is not a moderation action.
-- Re-running preserves existing rows; time-relative reports do not refresh automatically.
