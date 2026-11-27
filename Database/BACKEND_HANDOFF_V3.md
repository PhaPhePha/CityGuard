# Bàn giao kỹ thuật CityGuard Database V3 cho Backend (TV5) & Team Lead (TV1)

## 1. Mục tiêu và phạm vi phiên bản V3

CityGuard V3 nâng cấp định danh báo cáo và chuẩn hóa toàn bộ quy trình khởi tạo schema từ con số 0:
- Định danh công khai dễ đọc, thân thiện với người dùng và URL, có dạng: `rpt_` + **ULID chuẩn 26 ký tự Crockford Base32**.
- **Bảo toàn 100% cấu trúc khóa chính nội bộ**: `reports.id BIGINT GENERATED ALWAYS AS IDENTITY` và toàn bộ các quan hệ khóa ngoại (Foreign Keys) liên quan (`report_votes`, `comments`, `report_metrics`).
- **Giữ nguyên `client_request_id UUID`**: Tiếp tục phục vụ chống gửi trùng lặp yêu cầu (idempotency / deduplication) trên toàn bảng `reports`.
- **Chuẩn hóa chuỗi Migration Versioned (`V1 -> V2 -> V3`)**: Bổ sung `migrations/V1__initial_schema.sql` và khôi phục `schema.sql` dạng Pure SQL để có thể dựng mới hoàn toàn 8 bảng V3 từ một database trống.

---

## 2. Đặc tả kỹ thuật `reports.public_id`

| Thuộc tính | Giá trị / Ràng buộc | Ghi chú |
|---|---|---|
| **Tên cột** | `public_id` | Nằm trên bảng `public.reports` |
| **Kiểu dữ liệu** | `VARCHAR(30)` | Độ dài cố định đúng 30 ký tự |
| **Bắt buộc** | `NOT NULL` | Mọi bản ghi báo cáo đều phải có `public_id` |
| **Duy nhất** | `UNIQUE` | Ràng buộc `reports_public_id_uq` |
| **Định dạng** | `CHECK (public_id ~ '^rpt_[0-7][0-9A-HJKMNP-TV-Z]{25}$')` | Ràng buộc `reports_public_id_format_check` |

### Quy chuẩn định dạng ULID:
- **Tiền tố (Prefix)**: `rpt_` (4 ký tự chữ thường).
- **Phần thân ULID**: 26 ký tự Base32 chuẩn **Crockford**:
  - Bảng ký tự cho phép: `0123456789ABCDEFGHJKMNPQRSTVWXYZ`.
  - **Không chứa** các ký tự dễ gây nhầm lẫn: `I`, `L`, `O`, `U`.
  - Ký tự đầu tiên của ULID thuộc `[0-7]` (giới hạn 128-bit của chuẩn ULID, chống tràn số).
  - Định dạng chuẩn là chữ in hoa (**UPPERCASE**).
- **Tổng độ dài**: Đúng 30 ký tự (`4` + `26` = `30`).

---

## 3. Trách nhiệm Backend & Quy trình tạo báo cáo mới

1. **Khởi tạo ULID tại Backend**:
   - Khi client gửi request tạo báo cáo mới, Backend sinh một ULID chuẩn (48-bit timestamp ms + 80-bit crypto-randomness, ví dụ dùng thư viện `com.github.f4b6a3:ulid-creator` trên Java/Spring Boot).
   - Ghép tiền tố `rpt_` vào trước chuỗi ULID in hoa (ví dụ: `rpt_01M7WBY80FZNE4EYTVWZ00JM0J`).
   - Client vẫn gửi kèm header/field `client_request_id` (UUID).

2. **Gọi câu lệnh `create_report`**:
   - Backend truyền đầy đủ `client_request_id` (UUID) và `public_id` (VARCHAR(30)).
   - Database kiểm tra ràng buộc format CHECK và UNIQUE.
   - `create_report` trả về đồng thời `id`, `public_id`, `status`, `expires_at`.

3. **Xử lý idempotent retry**:
   - Nếu client gửi lại cùng `client_request_id`, mệnh đề `ON CONFLICT (client_request_id) DO NOTHING` được kích hoạt.
   - Backend thực hiện `report_retry(user_id, client_request_id)` ở câu lệnh thứ hai để lấy lại dữ liệu báo cáo đã tạo trước đó (bao gồm cả `public_id`), đối chiếu payload để trả HTTP 200/201 (trùng lặp an toàn) hoặc HTTP 409 (xung đột payload).

4. **Xử lý API công khai**:
   - Các API chi tiết báo cáo sử dụng `public_id` làm path parameter: `GET /api/v1/reports/{public_id}`.
   - Backend sử dụng prepared statement `get_report_by_public_id(public_id)` để truy vấn nhanh qua unique index.
   - Các API bình chọn, bình luận nội bộ: Backend ánh xạ `public_id` sang `reports.id` (BIGINT) hoặc sử dụng `reports.id` đã có trong context để thực hiện vote/comment theo protocol V2.

---

## 4. Danh mục Prepared Statements cập nhật (queries.sql)

### 4.1. `create_report`
```sql
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
```
- **Tham số**:
  - `$1`: `user_id` (bigint)
  - `$2`: `category_id` (bigint)
  - `$3`: `client_request_id` (uuid)
  - `$4`: `public_id` (varchar(30)) — *Mới bổ sung ở V3*
  - `$5`: `description` (text)
  - `$6`: `image_url` (text)
  - `$7`: `latitude` (double precision)
  - `$8`: `longitude` (double precision)
  - `$9`: `address_snapshot` (text)

### 4.2. `get_report_by_public_id` *(Mới ở V3)*
```sql
PREPARE get_report_by_public_id(varchar(30)) AS
SELECT r.id,r.public_id,r.user_id,r.category_id,r.client_request_id,r.description,r.image_url,
       r.latitude,r.longitude,r.address_snapshot,r.status,r.created_at,r.updated_at,r.expires_at,
       m.confirm_count,m.no_longer_count,m.trust_score,m.trust_label,m.computed_at
FROM reports r LEFT JOIN report_metrics m ON m.report_id=r.id
WHERE r.public_id=$1 AND r.status <> 'HIDDEN';
```

### 4.3. `map_bbox`, `my_reports`, `report_retry`
Đều đã được bổ sung cột `public_id` trong danh sách `SELECT`.

---

## 5. Phân tích tích hợp Flyway & Điểm cần phối hợp với TV5

> [!WARNING]
> **Trạng thái kiểm thử Flyway: `NOT TESTED`**
> Trên môi trường hiện tại không cài đặt Flyway CLI, Maven/Gradle và không có source Backend của TV5, nên toàn bộ các migration `V1`, `V2`, `V3` mới chỉ được kiểm thử thực tế qua **`psql` (`PostgreSQL 17.6`) và `bootstrap-v3.py`**. Chưa chạy kiểm thử thực tế qua Flyway engine. Không tự khẳng định tương thích tuyệt đối với Flyway trước khi chạy test tích hợp cùng Backend của TV5.

### 5.1. Đính chính về cấu hình `spring.flyway.execute-in-transaction` trong Spring Boot / Flyway
- **Tính hợp lệ của cấu hình**: Tài liệu chính thức của Spring Boot (Spring Boot 3.x `FlywayProperties`) **có hỗ trợ** thuộc tính:
  ```properties
  spring.flyway.execute-in-transaction=true # Giá trị mặc định là true
  ```
  *(Bên cạnh đó, Flyway cũng hỗ trợ cấu hình ở mức từng file script thông qua file `<Tên_Migration>.sql.conf` chứa `executeInTransaction=false` hoặc cấu hình `flyway.executeInTransaction` của Flyway).*
- **Kiểm tra phiên bản thực tế**: Khi tích hợp, TV5 cần đối chiếu với phiên bản Spring Boot và `flyway-core` / `flyway-database-postgresql` thực tế đang khai báo trong `pom.xml` / `build.gradle` của Backend.
- **Phân biệt giữa "Cấu hình hợp lệ" và "Cấu hình được khuyến nghị"**:
  - Việc đặt `spring.flyway.execute-in-transaction=false` là **cấu hình hợp lệ về mặt cú pháp** trong Spring Boot khi muốn tắt transaction bọc ngoài của Flyway để tránh xung đột với lệnh `BEGIN;` / `COMMIT;` có sẵn bên trong file SQL.
  - Tuy nhiên, theo kiến trúc **được khuyến nghị chính thức** bởi Flyway, khi chuyển sang dùng Flyway làm công cụ quản lý migration chính thức thì **nên ưu tiên giữ mặc định `spring.flyway.execute-in-transaction=true` để Flyway tự quản lý transaction** (xem phân tích chi tiết tại mục 5.2 và 5.3 bên dưới).

### 5.2. Phân tích ảnh hưởng của `BEGIN;` / `COMMIT;` trong `V1`, `V2`, `V3` khi chạy bằng Flyway
Hiện tại, cả 3 file trong `database/migrations/` (`V1__initial_schema.sql`, `V2__metrics_tokens_request_keys.sql`, `V3__report_public_id_ulid.sql`) đều chứa:
- `BEGIN;` ở đầu file và `COMMIT;` ở cuối file (để đảm bảo tính nguyên tử khi chạy độc lập qua `psql`, DBeaver hoặc `/docker-entrypoint-initdb.d/`).
- `SET LOCAL lock_timeout = '5s';`, `SET LOCAL statement_timeout = '120s';`, `SET LOCAL search_path = public, pg_catalog;`.
- `LOCK TABLE ... IN SHARE ROW EXCLUSIVE MODE;` (trong `V2` và `V3`).

Tác động theo từng chế độ cấu hình của Flyway:
1. **Khi giữ mặc định `spring.flyway.execute-in-transaction=true` mà trong file SQL vẫn còn `BEGIN;` / `COMMIT;`**:
   - Flyway mở sẵn một JDBC transaction (`connection.setAutoCommit(false)`).
   - Khi gặp `BEGIN;` trong file SQL, PostgreSQL phát cảnh báo `WARNING: there is already a transaction in progress` (SQLSTATE `25001`).
   - Khi gặp `COMMIT;` ở cuối file SQL, câu lệnh SQL thô này sẽ **đóng và commit sớm transaction trên PostgreSQL** trước khi Flyway kết thúc hàm quản lý transaction của JDBC và trước khi Flyway ghi lịch sử vào bảng `flyway_schema_history`.
   - Tài liệu chính thức của Flyway khuyến cáo không đặt `BEGIN` / `COMMIT` thủ công bên trong file SQL khi Flyway đang quản lý transaction (`execute-in-transaction=true`).
2. **Khi đặt `spring.flyway.execute-in-transaction=false` (hoặc dùng file `.sql.conf` `executeInTransaction=false`)**:
   - Flyway không mở transaction JDBC bọc ngoài; khối `BEGIN; ... COMMIT;` bên trong từng file `V1`, `V2`, `V3` sẽ trực tiếp mở và đóng transaction trên PostgreSQL (do đó các lệnh `SET LOCAL` và `LOCK TABLE` bên trong file vẫn nằm trong khối transaction và chạy hợp lệ).
   - Điểm hạn chế: Thao tác ghi dòng lịch sử vào `flyway_schema_history` của Flyway sẽ nằm ở một transaction riêng sau khi script đã `COMMIT;`, thay vì được commit/rollback chung một transaction với DDL của migration.

### 5.3. Hai phương án chuẩn hóa cho TV5 & TV1 khi tích hợp chính thức

- **Phương án 1 (Khuyến nghị ưu tiên — Để Flyway quản lý transaction)**:
  - Giữ cấu hình mặc định được khuyến nghị: `spring.flyway.execute-in-transaction=true`.
  - Trước khi đưa `V1`, `V2`, `V3` vào Flyway của Backend, **bỏ dòng `BEGIN;` ở đầu và `COMMIT;` ở cuối file**.
  - Vì PostgreSQL hỗ trợ Transactional DDL 100%, mọi câu lệnh `SET LOCAL ...`, `LOCK TABLE ... IN SHARE ROW EXCLUSIVE MODE`, `DO $$ ... $$`, `CREATE/ALTER TABLE` và backfill trong `V1`, `V2`, `V3` đều chạy nguyên tử hoàn hảo bên trong transaction do Flyway mở, đồng thời bảng `flyway_schema_history` cũng được cập nhật chung trong chính transaction đó.
  - Với các trường hợp chạy thủ công qua `psql`, truyền thêm cờ `--single-transaction` (`-1`) sẽ cho hành vi tương đương 100%.
  - *(Lưu ý: Hiện tại các file trong `database/migrations/` vẫn giữ `BEGIN;` / `COMMIT;` để phục vụ chạy độc lập qua `psql`, DBeaver và Docker `/docker-entrypoint-initdb.d/` cho đến khi TV5 chính thức tích hợp Flyway).*

- **Phương án 2 (Phương án tương thích nếu giữ nguyên `BEGIN;` / `COMMIT;` trong file `.sql`)**:
  - Sử dụng cấu hình hợp lệ `spring.flyway.execute-in-transaction=false` (hoặc cấu hình per-script `.sql.conf` `executeInTransaction=false` cho các file có `BEGIN;`/`COMMIT;`), và kiểm thử thực tế với connection pool (HikariCP) của Backend TV5 trên database thử nghiệm.

### 5.4. Quy trình áp dụng Flyway cho Database trống vs `cityguard_db` đã tồn tại
1. **Trên Database trống mới hoàn toàn (Docker Compose / CI / Máy Dev mới)**:
   - Cấu hình `spring.flyway.locations` (Cấu hình hợp lệ của Spring Boot) trỏ tới thư mục chứa `V1`, `V2`, `V3`.
   - Flyway chạy tuần tự `V1 -> V2 -> V3` tạo đủ 8 bảng.
   - Dữ liệu mẫu `database/seed.sql` được giữ tách biệt ngoài thư mục migration của Production; chỉ nạp riêng trên môi trường Dev/Test.
2. **Trên `cityguard_db` đã tồn tại sẵn**:
   - Không tự động bật Flyway hay chạy lại `V1` trên `cityguard_db`.
   - Sau khi `cityguard_db` đã được nâng cấp lên V3 và kiểm chứng bằng `verify-v3.sql`, nếu TV5 muốn gắn Flyway vào quản lý các bản `V4+` tiếp theo thì sử dụng các thuộc tính chuẩn được Spring Boot hỗ trợ:
     ```properties
     spring.flyway.baseline-on-migrate=true
     spring.flyway.baseline-version=3
     ```
   - Cần kiểm thử quy trình baseline này trên một bản sao (restore từ backup của `cityguard_db` sang database test) trước khi chạy trên `cityguard_db` thật.

### 5.5. Trạng thái kiểm thử theo công cụ và phiên bản PostgreSQL
- **PostgreSQL 17 (`17.6`) qua `psql` & `bootstrap-v3.py`**: **TESTED & PASS** (11/11 kịch bản: `V1 -> V2 -> V3 -> Seed -> Verify -> Queries -> pg_dump/pg_restore`).
- **PostgreSQL 19**: **NOT TESTED** (mới chỉ kiểm tra phân tích tĩnh do máy hiện tại chỉ có PostgreSQL 17.6; cần chạy thực tế khi có môi trường PG 19).
- **Flyway CLI / Spring Boot Flyway**: **NOT TESTED** (chưa có Flyway CLI hoặc source Backend TV5 trong workspace; cần kiểm thử thực tế khi tích hợp với nhánh của TV5).

---

## 6. Hướng dẫn áp dụng V3 lên database chính (`cityguard_db`)

> [!IMPORTANT]
> **Quy tắc an toàn**: Tuyệt đối không DROP/TRUNCATE `cityguard_db` và chỉ chạy migration lên `cityguard_db` sau khi đã backup thành công và được phê duyệt.

Các bước thực hiện khi được phê duyệt:
1. **Tạo bản sao lưu an toàn (`pg_dump -Fc`)**:
   ```powershell
   $pgBin = 'C:\Program Files\PostgreSQL\17\bin'
   $env:PGCLIENTENCODING = 'UTF8'
   & "$pgBin\pg_dump.exe" -h localhost -p 5432 -U <db_user> -d cityguard_db -Fc -f cityguard_before_v3.backup
   ```
2. **Chạy kiểm tra trước nâng cấp (Preflight V3)**:
   ```powershell
   & "$pgBin\psql.exe" -X -h localhost -p 5432 -U <db_user> -d cityguard_db -v ON_ERROR_STOP=1 -f database/preflight-v3.sql
   ```
3. **Thực thi Migration V3**:
   ```powershell
   & "$pgBin\psql.exe" -X -h localhost -p 5432 -U <db_user> -d cityguard_db -v ON_ERROR_STOP=1 -f database/migrations/V3__report_public_id_ulid.sql
   ```
4. **Kiểm chứng toàn diện cấu trúc V3 (`verify-v3.sql`)**:
   ```powershell
   & "$pgBin\psql.exe" -X -h localhost -p 5432 -U <db_user> -d cityguard_db -v ON_ERROR_STOP=1 -f database/verify-v3.sql
   ```
