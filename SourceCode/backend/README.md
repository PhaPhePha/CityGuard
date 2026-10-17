
### `SourceCode/backend/README.md`

```md
# CityGuard Backend

Backend cung cấp REST API cho ứng dụng Android CityGuard.

## Công nghệ

- Framework: ...
- Ngôn ngữ: ...
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