# CityGuard Database

Thư mục này chứa thiết kế và các file liên quan đến cơ sở dữ liệu CityGuard.

## Công nghệ

- Database: PostgreSQL
- Spatial data: PostGIS hoặc ...
- Migration tool: ...

## Các bảng chính

- users
- categories
- reports
- report_votes
- comments
- refresh_tokens

## Cấu trúc

- migrations/: Các file migration theo thứ tự phiên bản
- seed/: Dữ liệu mẫu phục vụ phát triển và demo
- schema/: ERD hoặc SQL schema nếu cần

## Quy tắc

- Tên bảng dùng snake_case.
- Mỗi migration phải có mã phiên bản.
- Không commit password, API key hoặc dữ liệu người dùng thật.
- Không lưu ảnh trực tiếp trong database; chỉ lưu URL và metadata.
