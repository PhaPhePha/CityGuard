
### `SourceCode/backend/README.md`

```md
# CityGuard Backend

Backend cung cấp REST API cho ứng dụng Android CityGuard. Hiện tại service dùng in-memory store để
chạy được workflow local trong khi PostgreSQL chưa được triển khai. Khi có database, chỉ cần thay
`ReportServiceImpl` bằng repository/service dùng JPA hoặc JDBC, giữ nguyên DTO và endpoint.

## Chạy local

```bash
./mvnw test
./mvnw spring-boot:run
```

Health check: `GET http://localhost:8080/api/v1/health`

Swagger: `http://localhost:8080/swagger-ui.html`

## API workflow hiện tại

- `POST /api/v1/auth/register` và `POST /api/v1/auth/login`
- `GET /api/v1/reports?bbox=minLng,minLat,maxLng,maxLat&type=TRAFFIC_JAM`
- `GET /api/v1/reports/{id}`
- `POST /api/v1/reports`
- `POST /api/v1/reports/{id}/vote`
- `POST /api/v1/reports/{id}/comments`
- `GET /api/v1/reports/mine`

Response thành công dùng `{ success, message, data }`. Client gửi `Authorization: Bearer <token>`
và backend demo dùng thêm `X-User-Id` để giữ context user cho tới khi middleware JWT hoàn chỉnh.

## Điểm thay thế khi triển khai PostgreSQL

Database cần giữ các khóa nghiệp vụ: `request_id` unique để retry không tạo duplicate, một vote/user/report,
`expires_at`, `creator_id`, tọa độ report và soft-delete/moderation status. Không đưa logic này về Android.

## Công nghệ

- Framework: Spring Boot
- Ngôn ngữ: Java
- Database: PostgreSQL
- Authentication: JWT hoặc ...
- API format: REST JSON

## Quy tắc bảo mật

- Không commit file .env.
- Không commit API key, password hoặc secret.
- Chỉ commit file .env.example với giá trị mẫu.
- Mỗi thành viên tự tạo file cấu hình local trên máy của mình.

## Các module chính

- auth: Đăng ký, đăng nhập, logout và token
- user: Thông tin người dùng và profile
- category: Các loại sự cố
- report: Tạo, xem và quản lý report
- vote: Xác nhận report hoặc phản hồi không còn nữa
- reliability: Tính độ tin cậy của report
- comment: Bình luận cộng đồng
- storage: Upload và quản lý ảnh

## Cấu trúc mã nguồn

```text
src/
├── main/
└── test/
