# CityGuard Infrastructure

Thư mục này chứa cấu hình môi trường chạy, Docker, CI/CD và các script hỗ trợ dự án.

## Thành phần dự kiến

- Docker Compose
- PostgreSQL container
- Backend service
- Image storage: ...
- GitHub Actions
- Development environment

## Cấu trúc

- docker/: Dockerfile và docker-compose.yml
- environments/: Cấu hình môi trường và .env.example
- scripts/: Script khởi động hoặc hỗ trợ phát triển
- ci/: Cấu hình kiểm tra tự động nếu cần

## Chạy môi trường local

Lệnh khởi động dự kiến:

```bash
...