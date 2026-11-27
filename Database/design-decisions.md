# Quyết định thiết kế CityGuard Database V1 và V2

> V2 supersedes V1 aggregate-cache and request-key decisions below. Sections 1-10 retain historical V1 rationale; section 11 is the current V2 contract. See upgrade-plan.md for operating details.

## 1. Cơ sở và hiện trạng đã kiểm tra

Đã đọc nội dung XML (đoạn văn và ô bảng) của `CityGuard_SRS_SoBo.docx` và `CityGuard_WBS.docx` bằng thư viện chuẩn Python ZIP/XML. Hai file thực tế nằm ở gốc workspace, không phải `docs/`. Không diễn giải hình prototype thành yêu cầu dữ liệu bổ sung.

Workspace tại thời điểm kiểm tra chỉ có hai DOCX: chưa có Backend, Android, entity/model, database configuration, migration framework hay Git repository. Spring Boot/NestJS chỉ là lựa chọn còn mở OD-01, không phải công nghệ đã triển khai. Vì vậy convention đề xuất là bảng số nhiều `snake_case`, PK `bigint identity`, FK `<entity>_id`, thời gian `timestamptz` và mã nghiệp vụ chữ hoa.

## 2. Truy vết yêu cầu bắt buộc

| Nguồn | Yêu cầu | Nơi đáp ứng |
|---|---|---|
| FR-AUTH-01..04, NFR-SEC-01 | Đăng ký username hoặc email; duy nhất; hash | users, unique index lower(), CHECK; Backend xác thực và quản lý session |
| FR-MAP-03/05, NFR-PERF-02 | bbox, category, giới hạn marker | reports_map_bbox_idx, map_bbox trong queries.sql |
| FR-REP-02..07 | Một category, vị trí/thời gian, ảnh URL, retry an toàn | reports, FK, CHECK, UNIQUE(user_id,client_request_id) |
| FR-REPORT-02..05, NFR-SEC-03 | Vote hiện hành duy nhất; counts; nhãn | report_votes, unique pair, upsert; Backend tính score/label |
| FR-COM-02..04, NFR-REL-01 | Comment, phân trang; chống trùng retry | comments, request key, cursor index; soft delete đã sẵn sàng |
| FR-PROF-01..03 | Thống kê, My Reports, reports đã xác nhận | report/user index; profile_summary và my_reports |
| FR-LIFE-01..03 | 5 status, TTL, giữ lịch sử | reports status CHECK, expires_at, expire_reports |
| FR-ADMIN-02..08, NFR-ADMIN-03 | Thống kê, khóa user, moderation, audit | users.status, comments.is_hidden, reports.status, admin_logs |
| NFR-REL-02, NFR-SCALE-01 | Backend tính reliability, cấu hình category/TTL | không lưu score/counters; categories có TTL và presentation metadata |
| WBS 6.1..6.3; từ điển 5.1.1..5.3.1 | ERD/schema, versioned migration, seed/spatial | Bộ file database/ và quy trình baseline V1 |

NFR-PERF-01 (~2 giây API staging) là mục tiêu chưa đo, không thể bảo đảm chỉ từ schema. Auth, session/logout, HTTPS, owner checks, rate limit và API 401/403 cần Backend; database schema không triển khai các chức năng đó.

## 3. Mâu thuẫn tài liệu và phương án tạm

| Điểm | Bằng chứng | Xử lý tạm thời cần Leader xác nhận |
|---|---|---|
| 4 hay 6 category | SRS 7.1: TRAFFIC_JAM, ROAD_BLOCK, ROAD_DAMAGE, FLOOD; FR-MAP-05, 6.1, AT-04: 6 loại có Broken Traffic Light/Fallen Tree | Seed 6 loại; giữ mã FLOOD cho Flooding; thêm BROKEN_TRAFFIC_LIGHT/FALLEN_TREE. Không thêm category khác |
| Admin trong hay ngoài MVP | SRS 1.2/2.6/3.8 có Admin; 8.1 lại loại trừ Web/admin dashboard; WBS có workstream Admin và D-09 admin_logs | Chỉ chuẩn bị schema dùng chung và audit, đúng phạm vi user giao; không triển khai UI/API |
| Mã WBS | Cây tổng thể DB 6.x, QA 7.x, bàn giao 8.x; từ điển 4.5 dùng DB 5.x, QA 6.x, bàn giao 7.x | Dùng cả mã và tên gói khi truy vết; không sửa tài liệu nguồn |
| Mốc và quyền dashboard | WBS có M5/M7 release candidate; SRS 2.3 mô tả Admin xem dashboard, UC-09 có cả Moderator | Không dùng DB để chốt lịch hoặc quyền API; Leader xác nhận Moderator được xem dashboard hay không |
| HIDDEN “về sau” | SRS 7.2/7.4 diễn đạt moderation giai đoạn sau, trong khi 3.8 yêu cầu hiện tại | Có status/log sẵn; Android chỉ nhận dữ liệu công khai theo Backend |

## 4. Giả định và trade-off kỹ thuật (chưa phải rule đã duyệt)

1. **TTL**: tính giây; tạm 2h kẹt xe, 4h vật cản, 7 ngày đường hư, 24h đèn hỏng/cây đổ, 6h ngập. Backend tạo `expires_at` từ cấu hình hiện hành. Thay TTL category không đổi report cũ vì expires_at là snapshot nghiệp vụ.
2. **Trạng thái đầu**: `PENDING_VERIFY`; map được phép hiển thị cả PENDING_VERIFY/ACTIVE còn TTL để report mới xuất hiện (AT-05). Chưa tự chuyển ACTIVE/RESOLVED theo vote; ngưỡng và tập chuyển trạng thái cần chốt OD-04. CHECK giới hạn tập giá trị, không áp đặt state machine chưa thống nhất.
3. **Ảnh/mô tả**: nullable vì SRS OD-05 còn hỏi bắt buộc ảnh hay không; mô tả tối đa 2.000 ký tự, comment 2.000, địa chỉ 500 là giới hạn kỹ thuật đề xuất. URL ảnh https; seed dùng placeholder không phải ảnh tải được. Backend kiểm tra host, ownership, loại file/kích thước; không fetch URL tùy ý. Icon là key logic cần map với Android assets.
4. **User**: ít nhất một username/email; cả hai unique không phân biệt hoa thường, trim input. Username ASCII 3–50 ký tự, không chứa `@` để tránh nhập nhằng login username/email. Regex email chỉ kiểm tra cơ bản, Backend chuẩn hóa/xác thực. Display name bắt buộc; user chỉ có một role. LOCKED không làm ẩn report cũ. Backend kiểm tra khóa tại login và mọi thao tác ghi, bao gồm token đã cấp.
5. **Hash**: chấp nhận encoded Argon2id/bcrypt, kiểm tra hình dạng không chứng minh hash đúng. Backend dùng thư viện chuẩn, salt ngẫu nhiên và policy cost; không hash ở Android rồi coi đó là password an toàn. Nếu chọn thuật toán khác phải tạo migration điều chỉnh CHECK.
6. **Self-vote/vote hết hạn**: schema chưa cấm vì chưa chốt; Backend phải xác nhận policy, không dựa vào FK để coi thao tác đã được phân quyền. Chỉ giữ vote hiện hành, chưa có toàn bộ lịch sử lần đổi vote. SRS “lịch sử các lần xác nhận” được hiểu tạm là danh sách report đã vote hiện hành; nếu cần event history phải bổ sung migration sau khi thống nhất.
7. **Soft delete/moderation**: comment `deleted_at` độc lập với `is_hidden`; restore moderation không hồi sinh comment user đã xóa. Nội dung vẫn trong DB, chưa phải xóa dữ liệu cá nhân vĩnh viễn. Report hết TTL giữ nguyên; không có xóa vật lý ở flow MVP.
8. **Thời gian**: server là nguồn timestamp; timestamptz lưu thời điểm độc lập timezone, client hiển thị Asia/Ho_Chi_Minh. Trigger cập nhật updated_at; không dùng thời gian Android cho created_at. Soft delete không tự refresh TTL report.

## 5. Quan hệ, 3NF và index

6 bảng; không có 1–1. User/category 1–N report; user/report 1–N comment và vote; user 1–N admin_log. User–report N–N qua votes, unique cặp xác định một trạng thái hiện hành. FK đều RESTRICT, không cascade phá lịch sử; vô hiệu hóa category bằng is_active thay vì xóa. Unique constraints tự tạo index, không tạo trùng.

Đạt mục tiêu 3NF: thuộc tính user/category chỉ lưu tại bảng của chúng; report không sao chép tên category/user; không lưu số đếm phụ thuộc các dòng khác. Address snapshot và expires_at là sự kiện tại thời điểm tạo report, không phải bản sao địa chỉ/TTL hiện tại. `admin_logs` là sự kiện độc lập; cặp target_type/target_id là tham chiếu logic đa hình. PostgreSQL không thể FK một cột tới 4 bảng tùy target_type; Backend phải kiểm tra target tồn tại trong transaction. Chọn giải pháp này thay vì 4 FK nullable cho MVP, đổi lại mất bảo đảm FK của target. Actor vẫn có FK.

| Index | Truy vấn / lý do |
|---|---|
| users lower(username/email), unique từng giá trị không null | đăng ký/login không phân biệt hoa thường |
| reports(user_id,created_at,id) | My Reports, user history, count; FK user |
| reports(status,created_at,id) | Community/admin theo status và thời gian |
| reports(category_id,status,created_at,id) | filter category/status; FK category |
| reports(latitude,longitude) partial live statuses | bbox chỉ đọc report có thể hiển thị |
| reports(expires_at,id) partial live statuses | scheduler chọn report đến hạn |
| votes UNIQUE(report_id,user_id) | chống duplicate, aggregate vote và FK report |
| votes(user_id,updated_at,id) | profile report đã xác nhận; FK user |
| comments(report_id,created_at,id) | cursor public/moderation; FK report |
| comments UNIQUE(user_id,request_id) | idempotency và FK user, không cần index user thêm |
| admin_logs recent / actor / target | nhật ký mới nhất, lọc actor/đối tượng, FK actor |

Không tạo index riêng role, comment.is_hidden hay từng status ít giá trị; không tạo spatial table, bảng counter hoặc bảng session khi chưa chọn cơ chế auth. Truy vấn không filter status có thể cần sort/scan; đo workload trước khi thêm index. PK/unique đã là index.

## 6. Spatial: PostgreSQL thường và PostGIS

Chọn `double precision latitude/longitude` + CHECK khoảng + B-tree cho MVP, không extension. PostgreSQL từ chối NaN/infinity qua CHECK khoảng. bbox lọc latitude/longitude và trạng thái/category, sort thời gian, limit tối đa 500. Query mẫu hỗ trợ vượt kinh tuyến 180; Backend validate bbox, category list, status whitelist và limit trước khi gọi. `LIMIT` kết quả không đồng nghĩa giới hạn số dòng phải scan.

B-tree `(latitude,longitude)` tận dụng tốt cận latitude, không tương đương chỉ mục 2D; bbox rộng hoặc dữ liệu dày có thể scan/sort nhiều. OR khi vượt kinh tuyến có thể kém hiệu quả; có thể tách UNION ALL hai vùng không chồng nhau sau khi đo. Chạy EXPLAIN (ANALYZE, BUFFERS) ở test với dữ liệu đại diện trước khi cam kết hiệu năng.

PostGIS thích hợp khi cần radius/khoảng cách mét, nearest neighbor, polygon hoặc bbox lớn trên tập dữ liệu lớn. Khi được duyệt: cài package PostGIS tương thích bản PostgreSQL trên server (hoặc image PostGIS), DBA chạy `CREATE EXTENSION postgis` trên DB đích, migration thêm `geometry(Point,4326)` và GiST, backfill `ST_SetSRID(ST_MakePoint(longitude,latitude),4326)` rồi chuyển query sang `&& ST_MakeEnvelope(...)`. Dùng geography/ST_DWithin khi cần khoảng cách mét. Chỉ duy trì một nguồn tọa độ hoặc generated geometry, tránh hai bản sao lệch nhau. Hiện không cài extension, không bật PostGIS.

## 7. Reliability và tính nhất quán

Không lưu confirmed_count/no_longer_count/report_count/trust_score. SQL aggregate từ vote unique, Backend kết hợp created_at/expires_at để tính score/label cùng một service cho mọi client. SRS 7.3 quy định baseline nhãn: không vote → Đang xác minh; Cao score >=70 và >=5 vote; Trung bình 40–69 hoặc chưa đủ bằng chứng Cao; Thấp <40 khi đã có vote. Công thức cụ thể, ý nghĩa “report mới”, decay theo thời gian và ngưỡng RESOLVED chưa được chốt; database không tự gán.

Aggregate vote chỉ cho các report trong trang/bbox đã giới hạn; không N+1 query cho từng marker khi scale. Dùng subquery group theo report_id sau khi chọn trang; không join trực tiếp nhiều bảng 1–N rồi count làm nhân bản hàng. Nếu tương lai cache counters, cần transaction atomic cùng vote hoặc materialized view có lịch refresh và semantics chấp nhận stale; chưa cần ở MVP.

TTL không tự chạy khi đồng hồ đổi: trigger chỉ chạy khi ghi. Backend scheduler gọi expire_reports theo batch; map luôn kiểm tra expires_at để không lộ report quá hạn nếu scheduler chậm. Giữ HIDDEN/RESOLVED, chỉ expire hai trạng thái live; định nghĩa quá hạn `expires_at <= now()`. Job nên dùng transaction ngắn, giám sát overdue_unprocessed và retry deadlock. Khi job chưa chạy, lịch sử hiển thị status lưu có thể chậm; Backend có thể diễn giải effective status EXPIRED cho hai trạng thái live quá TTL.

## 8. Idempotency, concurrency và moderation

- Android tạo UUID cho một thao tác report/comment, lưu lại khi retry. Cùng user+key luôn cùng payload; không cấp key mới cho mỗi lần bấm gửi lại. Unique bảo đảm ngay cả hai request đồng thời.
- INSERT ON CONFLICT DO NOTHING; nếu không có RETURNING, SELECT bằng owner+key ở statement sau trong READ COMMITTED. Không dùng CTE insert+fallback cùng snapshot vì có thể không thấy dòng vừa commit từ transaction cạnh tranh. So sánh payload gốc (và report_id với comment); khác nội dung → 409. Retry comment đã xóa/ẩn không tự restore. Chuẩn hóa null/chuỗi/tọa độ trước khi so sánh; report payload/category/vị trí cần giữ bất biến trong MVP để so sánh có ý nghĩa.
- Vote PUT upsert theo (report_id,user_id); không thêm count. Gửi cùng vote không ghi lại timestamp. Request cũ bị retry SAU một lần đổi vote mới có thể ghi đè lựa chọn mới: unique chỉ chống duplicate, không bảo đảm thứ tự ý định. MVP client serialize các PUT vote và hủy retry cũ khi đổi lựa chọn; nếu cần bảo đảm nhiều thiết bị, chốt optimistic version/If-Match rồi thêm migration.
- Backend authorize trước ghi; user_id/actor_id lấy từ session. Để thao tác khóa không đua với create/vote/comment, transaction ghi khóa hàng user bằng `SELECT ... FOR UPDATE`, kiểm tra ACTIVE rồi ghi; transaction lock/unlock dùng cùng hàng. Tất cả đường ghi phải tuân thủ, schema chưa enforce policy đa bảng này.
- Moderation phải update đối tượng và insert admin_logs trong cùng transaction (queries.sql có hide_report nguyên tử). Nếu log lỗi, rollback cả thay đổi. Không tạo trigger audit dựa trên current_user vì một DB login được dùng cho nhiều end-user. Backend kiểm tra quyền Admin/Moderator; SQL CHECK role không thay thế authorization.
- Không tạo trạng thái trước/sau dạng JSON chứa dữ liệu nhạy cảm. Reason tùy chọn, tránh ghi password/token/URL ký tạm. Runtime chỉ được INSERT/SELECT admin_logs, không UPDATE/DELETE; migration owner riêng. Đây là policy cần DBA cấp quyền, không phải tính bất biến được schema tự bảo đảm.

## 9. Bảo mật, vận hành và các việc còn mở

Database không kết nối trực tiếp Android/Admin Web. Backend dùng tài khoản runtime ít quyền, không phải owner/superuser; cấp SELECT danh sách cột users không gồm password_hash cho luồng đọc thông thường, auth dùng đường truy cập riêng. Mọi public query loại HIDDEN và comment deleted/hidden; history riêng user có thể có report bị ẩn nếu nhóm duyệt. Không SELECT * users ra API. Parameter binding, HTTPS, secrets ngoài repo; không log tọa độ/token tùy tiện. Chưa áp dụng RLS vì chưa có cơ chế xác thực session DB; phải review endpoint permissions khi Backend ra đời.

Backup mã hóa, thử restore trên DB riêng, retention vị trí/ảnh và yêu cầu xóa/anonymize account cần nhóm chốt. RESTRICT chủ động ngăn hard delete user có lịch sử; không đồng nghĩa đáp ứng mọi chính sách xóa dữ liệu. URL chỉ lưu object URL ổn định, không lưu signed URL hết hạn lâu dài. DB và object storage không cùng transaction: Backend cleanup ảnh mồ côi sau create thất bại, retry dùng lại object cùng thao tác.

Chưa có test concurrency hai kết nối, plan benchmark, API integration, login/hash verification, backup/restore hoặc permission runtime thật. Không thay DB đang hoạt động. Leader cần chốt: 6 category/mã FLOOD, TTL, trạng thái đầu và ngưỡng chuyển trạng thái, công thức reliability, bắt buộc ảnh, giới hạn text, self-vote/vote hết hạn, quyền dashboard Moderator, retention, event history vote, stack auth/hash/migration.

## 10. Demo password revision (explicit user request)

Seed now stores individual BCrypt cost 12 hashes instead of a supplied shared hash. Random salts generated locally; 5/5 correct-password checks and 5/5 wrong-password rejection checks passed with libcrypt via local Perl. No full plaintext passwords were written. These are development/demo credentials only. update-demo-passwords.sql updates exact demo username/email matches after explicit approval; no schema changes. Earlier seed SQL test results describe the previous seed version, not this revision. No SQL for this revision has been executed on the live database; the earlier localhost:5432 connection required credentials.

## 11. V2 theo yêu cầu Backend

V2 chủ động thay đổi hai quyết định V1: bổ sung cache `report_metrics` thay cho chỉ tính động, và request key unique toàn cục riêng từng bảng reports/comments thay vì chỉ unique theo user. Thêm `refresh_tokens` để Backend có nơi lưu trạng thái refresh/revoke. Không thay schema.sql baseline, tọa độ, bảng/cột hoặc dòng V1; database mới phải chạy V1 rồi V2.

Metrics dùng PK/FK BIGINT report_id, hai count BIGINT mặc định 0 không âm, score NUMERIC(5,2), label VARCHAR(32), computed_at TIMESTAMPTZ nullable. Không chốt thang điểm/nhãn/công thức; backfill count không gán score hoặc timestamp tính trust. Không thêm index report_id trùng PK. Bảng cache là phi chuẩn hóa có chủ đích để đọc map, chỉ nhất quán khi mọi đường ghi vote tuân thủ transaction/parent lock trong upgrade-plan.md. Không dùng CHECK để giả vờ kiểm tra aggregate đa bảng. Retry không đổi count giữ score; count thay đổi invalidate trust về NULL. Score phụ thuộc thời gian cần worker riêng sau khi chốt công thức.

Refresh tokens dùng PK identity BIGINT, user_id BIGINT FK RESTRICT; token_hash UNIQUE VARCHAR(64), CHECK lowercase hex; expires_at/revoked_at TIMESTAMPTZ với thời điểm hữu hạn. Chọn SHA-256 cho token entropy cao là **đề xuất cần Backend xác nhận**, khác với password dùng BCrypt/Argon2id. Expiry do Backend cung cấp; không tự chọn TTL, không seed token, không thêm pgcrypto/PostGIS. Index bổ sung đúng user_id và expires_at. Rotation/revoke dùng transaction; chưa có family/reuse tracking.

Migration nhận diện unique index tương đương từ catalog và không tạo trùng. Constraint ghép cũ được giữ tạm cho dependency Backend ngoài workspace, không còn dùng làm conflict target trong file V2. Chỉ loại bỏ bằng migration sau khi Backend duyệt; giữ hiệu năng/FK lookup theo user. Duplicate giữa hai user khiến nâng cấp dừng, không tự sửa/xóa dữ liệu.

Các điểm còn mở: Backend thực tế/framework migration, thang điểm/công thức/nhãn, strategy score refresh, SHA-256 encoding, TTL token, scope logout, rotation/reuse policy, thời điểm bỏ legacy constraints, serialization/version cho stale vote retry, và tích hợp Java/API. Bản V2 này không tự giải quyết hay thay đổi các quy tắc chưa được duyệt.
