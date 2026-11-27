# Kết quả kiểm thử CityGuard V3 — Chuẩn hóa Khởi tạo, Migration & Docker Compose (25/11/2026)

## 1. Môi trường kiểm thử độc lập
1. **Cụm PostgreSQL 17.6 Native Windows (Disposable Cluster)**:
   - Binary: `PostgreSQL 17.6 on x86_64-windows, compiled by msvc-19.44.35217, 64-bit`.
   - Cổng cô lập: `127.0.0.1:55460`, chạy qua `python tests/run-v3-tests.py`.
2. **Môi trường Docker Compose (`postgres:17-alpine`)**:
   - Docker Engine: `29.5.2` (context `desktop-linux`).
   - Image & Phiên bản: `PostgreSQL 17.12 on x86_64-pc-linux-musl, compiled by gcc (Alpine 15.2.0) 15.2.0, 64-bit`.
   - Cổng ánh xạ: `127.0.0.1:5433:5432` (cách ly hoàn toàn với cổng `5432` local), volume riêng `cityguard_v3_pgdata`, chạy qua `python tests/run-docker-tests.py`.
3. **Bảo vệ an toàn**:
   - Không kết nối cổng `5432`, không tác động tới `cityguard_db`, không thay đổi các file backup hiện có.
   - Không sử dụng `docker compose down -v` hay `docker system prune --volumes`.

---

## 2. Kết quả kiểm thử thực tế trên cụm PostgreSQL 17.6 Native (`tests/run-v3-tests.py`)

| Mã Test | Hạng mục kiểm thử | Chi tiết thực thi & Kiểm chứng | Kết quả |
|---|---|---|---|
| **S01** | **Chốt chặn an toàn `bootstrap-v3.py`** | Thử truyền `cityguard_db`, `postgres`, `template1`, `production_db`, `cityguard_prod` -> Tất cả đều bị chặn ngay bởi `validate_target_db()`. | **PASS** |
| **S02** | **Đồng bộ DDL V1 Pure SQL** | Đối chiếu nội dung `migrations/V1__initial_schema.sql` và `schema.sql`: khớp 100%, không chứa `COPY`, không chứa `OWNER TO postgres`, không có lệnh `\` của `psql`. | **PASS** |
| **A01** | **Bootstrap tự động từ Database trống** | `bootstrap-v3.py` tự tạo DB mới `cityguard_test` và chạy tuần tự: `V1 -> Preflight V2 -> V2 -> Preflight V3 -> V3 -> Seed -> Verify`. | **PASS** |
| **A02** | **Kiểm tra cấu trúc 8 bảng & Ràng buộc** | Xác nhận từ `pg_catalog`: đúng **8 bảng**, **8 Primary Keys** (`BIGINT`), **9 Foreign Keys (`ON DELETE RESTRICT`)**, **14 explicit indexes**, **4 triggers (`set_updated_at`)**. | **PASS** |
| **A03** | **Seed Pure SQL lần đầu & chạy lại** | Chạy `seed.sql` lần 1 (5 users, 6 categories, 6 reports, 2 votes, 3 comments, 6 metrics). Chạy lại `seed.sql` và `seed-dbeaver.sql`: MD5 JSON digest của toàn bộ 8 bảng giữ nguyên 100%. | **PASS** |
| **A04** | **Kiểm thử định danh `reports.public_id` (`upgrade-v3-tests.sql`)** | Vượt qua toàn bộ 11 mục assertion V3 (`VARCHAR(30) NOT NULL`, `UNIQUE`, `CHECK` từ chối thiếu tiền tố `rpt_`, sai độ dài, ký tự cấm `I, L, O, U`, ký tự đầu `>7`, chữ thường và `NULL`). | **PASS** |
| **A05** | **Regression Suite cốt lõi (`database-tests.sql`)** | Vượt qua toàn bộ 14 mục regression gốc. | **PASS** |
| **A06** | **Truy vấn nghiệp vụ chính (`queries.sql`)** | `PREPARE` toàn bộ 22 câu lệnh mẫu và thực thi truy vấn theo `public_id` (`get_report_by_public_id`), `create_report`, `report_retry`, `map_bbox`, `my_reports`, `put_vote` + `refresh_report_metrics`. | **PASS** |
| **B01** | **Regression nâng cấp từng bước `V1 -> V2 -> V3` có dữ liệu** | Trên DB `cityguard_regression_test`: nạp V1 -> chèn 3 report V1 -> chạy Preflight & V2 -> chạy Preflight & V3 -> cả 3 report cũ được backfill tự động mã `public_id` chuẩn ULID duy nhất, hợp lệ và bảo toàn dữ liệu gốc. | **PASS** |
| **C01** | **Diễn tập Backup & Restore (`pg_dump -Fc` / `pg_restore`)** | Chạy `pg_dump -Fc` trên `cityguard_test`, khôi phục bằng `pg_restore --exit-on-error --no-owner --no-privileges` sang DB mới `cityguard_restore_test`, chạy `verify-v3.sql` và so sánh MD5 JSON digest toàn bộ 8 bảng: khớp 100%. | **PASS** |
| **D01** | **Chống chạy đè Migration V2 & V3** | Thử chạy lại file `V2` và `V3` trên DB đã nâng cấp: cả hai đều chủ động từ chối ngay trong khối preflight mà không làm hỏng dữ liệu. | **PASS** |

---

## 3. Kết quả kiểm thử thực tế trên Docker Compose cổng 5433 (`tests/run-docker-tests.py`)

| Mã Test | Hạng mục kiểm thử Docker Compose | Chi tiết thực thi & Kiểm chứng | Kết quả |
|---|---|---|---|
| **D01** | **Kiểm tra cấu hình `docker compose config`** | Chạy `docker compose config --quiet` cho cả cấu hình mặc định và `--profile demo`: cú pháp YAML, volume mounts và biến môi trường hợp lệ 100%. | **PASS** |
| **D02** | **Khởi tạo PostgreSQL V3 từ volume mới (`cityguard_v3_pgdata`)** | Chạy `docker compose up -d --wait postgres`: container `cityguard-v3-postgres` tự động thực thi `01_V1` -> `02_preflight_upgrade` -> `03_V2` -> `04_preflight_v3` -> `05_V3` -> `06_verify_v3` và đạt trạng thái `healthy`. | **PASS** |
| **D03** | **Kết nối từ Windows Host qua `127.0.0.1:5433` & Kiểm tra 8 bảng** | Dùng `C:\Program Files\PostgreSQL\17\bin\psql.exe -h 127.0.0.1 -p 5433` kết nối vào container (`PostgreSQL 17.12`): xác nhận đủ **8 bảng** và **0 dòng demo** khi chưa bật profile `demo`. | **PASS** |
| **D04** | **Kiểm chứng `public_id`, FK, Constraints, Indexes, Triggers** | Chạy `verify-v3.sql`, `tests/upgrade-v3-tests.sql` và `tests/database-tests.sql` từ máy Windows qua `127.0.0.1:5433`: vượt qua toàn bộ assertions. | **PASS** |
| **D05** | **Nạp Seed Demo qua Docker Compose Profile (`--profile demo`)** | Chạy `docker compose --profile demo run --rm seed-demo`: nạp thành công 5 users, 6 categories, 6 reports (ULID chuẩn), 2 votes, 3 comments, 6 metrics và vượt qua `verify-v3.sql`. | **PASS** |
| **D06** | **Kiểm tra Volume giữ dữ liệu sau khi Restart Container** | Chạy `docker compose restart postgres`, chờ container `healthy` trở lại và đối chiếu MD5 JSON digest của toàn bộ 8 bảng: bảo toàn 100% dữ liệu và schema. | **PASS** |
| **D07** | **Kiểm tra Seed chạy lại không bị trùng (Idempotency)** | Chạy lại `docker compose --profile demo run --rm seed-demo` lần 2: MD5 JSON digest của toàn bộ 8 bảng giữ nguyên 100% (0 bản ghi trùng). | **PASS** |
| **D08** | **Thực thi truy vấn nghiệp vụ (`queries.sql`) qua `127.0.0.1:5433`** | Thực thi `PREPARE` toàn bộ 22 câu lệnh và chạy `EXECUTE get_report_by_public_id('rpt_01M7WBY80FZNE4EYTVWZ00JM0J')` từ Windows host: trả về đúng báo cáo demo và metrics. | **PASS** |

---

## 4. Tổng hợp trạng thái kiểm thử theo môi trường & công cụ

| Môi trường / Công cụ | Phương thức kiểm tra | Trạng thái | Ghi chú |
|---|---|---|---|
| **PostgreSQL 17.6 (Windows Native Cluster, cổng 55460)** | **Kiểm thử thực thi thực tế (Live Execution)** | **PASS (11/11 kịch bản)** | Đã kiểm thử DDL, DML, Prepared Queries, `pg_dump` và `pg_restore`. |
| **PostgreSQL 17.12 (Docker Compose `postgres:17-alpine`, cổng 5433)** | **Kiểm thử thực thi thực tế (Live Execution)** | **PASS (8/8 kịch bản)** | Đã kiểm thử init volume mới, kết nối từ Windows host qua `127.0.0.1:5433`, restart persistence và `--profile demo`. |
| **PostgreSQL 19** | **Chỉ phân tích tĩnh (Static Analysis)** | **NOT TESTED** | Chưa có binary/image PostgreSQL 19 trong phiên kiểm thử này; cần chạy thực tế khi có môi trường PG 19. |
| **Flyway CLI / Spring Boot Flyway** | **Chỉ phân tích tĩnh (Static Analysis)** | **NOT TESTED** | Chưa tích hợp và chưa chạy thực tế với source Backend của TV5. Đã đính chính tài liệu về `spring.flyway.execute-in-transaction` (default `true`) trong `BACKEND_HANDOFF_V3.md` và `README.md`. |
