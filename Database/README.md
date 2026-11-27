# CityGuard Database V3 — Hướng dẫn Khởi tạo cho Thành viên Nhóm (PostgreSQL 17 & Docker Compose)

Thư mục `Database/` chứa toàn bộ cấu trúc cơ sở dữ liệu chính thức **CityGuard V3** (8 bảng, khóa chính nội bộ `BIGINT`, định danh công khai `reports.public_id` dạng `rpt_<ULID 26 ký tự>`, chuỗi migration `V1 → V2 → V3`, dữ liệu mẫu `seed.sql` và cấu hình Docker Compose).

> **Quy tắc an toàn**:
> - Tuyệt đối **không commit file `.env`**, mật khẩu thật, thư mục `backups/` hay file `.backup`/`.dump` lên Git.
> - Cổng Docker mặc định là **`5433`** (`127.0.0.1:5433`) để không bao giờ xung đột với PostgreSQL cài trực tiếp trên máy (`5432`).

---

## 1. Khởi tạo nhanh bằng Docker Compose (Khuyến nghị cho toàn bộ thành viên)

### Bước 1: Tạo file `.env` cục bộ từ `.env.example`
Mở PowerShell hoặc Terminal tại thư mục gốc của repository `CityGuard`:

```powershell
cd Database
Copy-Item .env.example .env
```

Mở file `Database/.env` vừa tạo và đổi `CITYGUARD_DB_PASSWORD` thành mật khẩu local của bạn (ví dụ `CityGuardDev2026!`):
```dotenv
CITYGUARD_DB_PORT=5433
CITYGUARD_DB_NAME=cityguard_v3_dev
CITYGUARD_DB_USER=cityguard
CITYGUARD_DB_PASSWORD=CityGuardDev2026!
```

### Bước 2: Khởi động PostgreSQL 17 và tự động chạy Migration `V1 → V2 → V3`
```powershell
# Kiểm tra cú pháp cấu hình Docker Compose
docker compose config

# Khởi động container PostgreSQL 17 ở chế độ nền
docker compose up -d

# Kiểm tra trạng thái container (đợi trạng thái Healthy)
docker compose ps
```
Khi volume `cityguard_v3_pgdata` mới hoàn toàn, container sẽ tự động chạy lần lượt 6 script trong `/docker-entrypoint-initdb.d/`:
1. `01_V1__initial_schema.sql` (`migrations/V1__initial_schema.sql`) — Tạo 6 bảng V1, constraints, FKs, indexes, triggers.
2. `02_preflight_upgrade.sql` (`preflight-upgrade.sql`) — Kiểm tra catalog V1 trước khi lên V2.
3. `03_V2__metrics_tokens_request_keys.sql` (`migrations/V2__metrics_tokens_request_keys.sql`) — Thêm `report_metrics`, `refresh_tokens`, unique constraints.
4. `04_preflight_v3.sql` (`preflight-v3.sql`) — Kiểm tra catalog V2 trước khi lên V3.
5. `05_V3__report_public_id_ulid.sql` (`migrations/V3__report_public_id_ulid.sql`) — Thêm `reports.public_id VARCHAR(30) UNIQUE NOT NULL` (`rpt_` + ULID 26 ký tự).
6. `06_verify_v3.sql` (`verify-v3.sql`) — Kiểm chứng đủ 8 bảng, 8 PK, 9 FK, 14 indexes, 4 triggers.

### Bước 3: Nạp dữ liệu mẫu Demo (`seed.sql`)
Dữ liệu mẫu được tách riêng qua profile `demo` để không trộn lẫn với schema migration:
```powershell
docker compose --profile demo run --rm seed-demo
```
*(Script `seed.sql` có tính idempotent — có thể chạy lại an toàn mà không gây lỗi trùng dữ liệu).*

### Bước 4: Thông tin kết nối từ Backend (`SourceCode/backend`) và DBeaver
- **Host**: `127.0.0.1` (hoặc `localhost`)
- **Port**: `5433`
- **Database**: `cityguard_v3_dev`
- **Username**: `cityguard`
- **Password**: Mật khẩu `CITYGUARD_DB_PASSWORD` trong file `Database/.env` của bạn
- **JDBC URL cho Spring Boot Backend**: `jdbc:postgresql://127.0.0.1:5433/cityguard_v3_dev`

Ví dụ chạy Backend (`SourceCode/backend`) kết nối với Docker PostgreSQL V3 trong PowerShell:
```powershell
cd ..\SourceCode\backend
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://127.0.0.1:5433/cityguard_v3_dev"
$env:SPRING_DATASOURCE_USERNAME="cityguard"
$env:SPRING_DATASOURCE_PASSWORD="<Mật_khẩu_trong_file_.env>"
.\mvnw.cmd spring-boot:run
```

---

## 2. Khởi tạo trên PostgreSQL 17 cài trực tiếp trên máy (Không dùng Docker)

Nếu máy bạn cài PostgreSQL 17 native và muốn dựng một database test mới từ con số 0 bằng script tự động `bootstrap-v3.py`:

```powershell
python Database/bootstrap-v3.py `
  --psql "C:\Program Files\PostgreSQL\17\bin\psql.exe" `
  --host 127.0.0.1 `
  --port 5432 `
  --user postgres `
  --dbname cityguard_v3_test `
  --create-db
```
*(Lưu ý: `bootstrap-v3.py` có chốt chặn an toàn không bao giờ cho phép chạy đè lên database chính `cityguard_db`).*

---

## 3. Cấu trúc các file trong thư mục `Database/`

- `migrations/V1__initial_schema.sql`: Migration V1 tạo 6 bảng nền tảng (`users`, `categories`, `reports`, `report_votes`, `comments`, `admin_logs`), constraints, 7 `ON DELETE RESTRICT` FKs, 12 indexes, hàm `set_updated_at()` và 4 triggers.
- `migrations/V2__metrics_tokens_request_keys.sql`: Migration V2 bổ sung bảng `report_metrics`, `refresh_tokens`, ràng buộc unique toàn cục cho `client_request_id` và backfill bộ đếm vote.
- `migrations/V3__report_public_id_ulid.sql`: Migration V3 bổ sung cột `reports.public_id VARCHAR(30) UNIQUE NOT NULL` (`CHECK (public_id ~ '^rpt_[0-7][0-9A-HJKMNP-TV-Z]{25}$')`), giữ nguyên `reports.id BIGINT` cho các khóa ngoại nội bộ.
- `schema.sql`: Bản DDL V1 baseline chuẩn (đồng nhất với `V1__initial_schema.sql`).
- `preflight-upgrade.sql` & `preflight-v3.sql`: Các script kiểm tra điều kiện an toàn (read-only) trước khi chạy V2 và V3.
- `verify-v3.sql`: Script kiểm tra toàn diện cấu trúc V3 sau khi khởi tạo hoặc nâng cấp.
- `seed.sql` & `seed-dbeaver.sql`: Dữ liệu mẫu phục vụ phát triển và kiểm thử (6 danh mục sự cố, các tài khoản demo `USER`/`MODERATOR`/`ADMIN`/`LOCKED`, báo cáo mẫu có `public_id` chuẩn ULID).
- `queries.sql`: Tập hợp các câu truy vấn mẫu (`PREPARE`) chuẩn V3.
- `docker-compose.yml` & `.env.example`: Cấu hình khởi chạy nhanh PostgreSQL 17 qua Docker trên cổng `5433`.
- `bootstrap-v3.py`: Script Python tự động dựng database thử nghiệm mới (`Empty DB → V1 → V2 → V3 → Seed → Verify`).
- `BACKEND_HANDOFF_V3.md`, `erd.md`, `design-decisions.md`: Tài liệu bàn giao kỹ thuật cho Backend, sơ đồ ERD và quyết định thiết kế.
- `tests/`: Bộ kiểm thử tự động (`database-tests.sql`, `upgrade-tests.sql`, `upgrade-v3-tests.sql`, `run-v3-tests.py`, `run-docker-tests.py`, `v3-test-results.md`).

---

## 4. Lưu ý quan trọng về `/docker-entrypoint-initdb.d` của Docker PostgreSQL

- Các script gắn trong `/docker-entrypoint-initdb.d` **chỉ tự động chạy ở lần đầu tiên khi Docker volume (`cityguard_v3_pgdata`) hoàn toàn trống**.
- Khi dừng hoặc khởi động lại container (`docker compose stop` / `docker compose start` / `docker compose restart`), dữ liệu trong volume được giữ nguyên và các script init sẽ được bỏ qua.
- Nếu muốn xóa toàn bộ dữ liệu thử nghiệm trong Docker để dựng lại từ đầu, chạy `docker compose down -v` rồi `docker compose up -d` (chỉ thực hiện khi chắc chắn muốn tạo mới lại database dev/test trên cổng `5433`).
