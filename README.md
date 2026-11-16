# CityGuard

Ứng dụng hỗ trợ cộng đồng báo cáo, chia sẻ và xác minh các sự cố giao thông đô thị như kẹt xe, đường hư, ngập nước, vật cản.

## Cấu trúc thư mục

- SourceCode/android: Android app
- SourceCode/backend: Backend REST API
- SourceCode/admin-web: Admin Web
- Database: Database schema và migration
- Infra: Docker, CI/CD và môi trường chạy
- TaiLieu: Prototype và tài liệu thiết kế

## Bộ quy tắc
Toàn bộ quy tắc khi thực hiện dự án nằm trong CONTRIBUTING.md

## Chạy workflow local

1. Chạy backend tại `SourceCode/backend` bằng `./mvnw spring-boot:run`.
2. Mở `SourceCode/android` bằng Android Studio và chạy debug emulator.
3. Debug emulator dùng `http://10.0.2.2:8080/`; thiết bị thật cần override `BASE_URL` theo IP máy chạy backend.

Backend hiện dùng in-memory store để unblock workflow Android trong lúc Database chưa được triển khai. API contract,
request id idempotency, vote, comment, bbox và My Reports đã được giữ ổn định để thay implementation bằng PostgreSQL sau.
