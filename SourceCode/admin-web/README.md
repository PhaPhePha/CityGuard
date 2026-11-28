# CityGuard Admin Web

Admin Web là giao diện quản trị của hệ thống CityGuard. Website cho phép quản trị viên theo dõi, kiểm duyệt và quản lý dữ liệu từ ứng dụng Android.

## Chức năng dự kiến

- Đăng nhập quản trị viên.
- Xem tổng quan hệ thống.
- Quản lý báo cáo giao thông.
- Xem báo cáo trên bản đồ.
- Lọc báo cáo theo loại, trạng thái và độ tin cậy.
- Ẩn hoặc xóa báo cáo không hợp lệ.
- Quản lý người dùng.
- Khóa hoặc mở khóa tài khoản.
- Quản lý bình luận.
- Xem nhật ký hoạt động quản trị.

## Công nghệ dự kiến

- React
- Vite
- JavaScript hoặc TypeScript
- React Router
- Axios
- Tailwind CSS hoặc Material UI
- Chart.js hoặc Recharts
- Kết nối với CityGuard Backend API

## Chạy phiên bản hiện tại

Frontend hiện dùng React, TypeScript, Vite, React Router, Axios và Tailwind CSS.
Các chức năng đã tích hợp gồm đăng nhập Admin/Moderator, tổng quan, danh mục,
kiểm duyệt báo cáo và bình luận, quản lý người dùng và nhật ký quản trị.
Các mục trong phần dự kiến ở trên mô tả phạm vi ban đầu.

Cài dependencies và chạy frontend từ thư mục này:

```powershell
npm ci
npm run dev
```

Mở http://localhost:3000. Mặc định dev gọi Admin API tại
http://localhost:8080. Backend phải có các endpoint Admin tương ứng và kết nối
PostgreSQL. Đăng nhập bằng tài khoản được cấp cho môi trường đó.

Để đổi cấu hình riêng trên máy, đặt `VITE_API_BASE_URL` hoặc `VITE_USE_MOCK`
trong `.env.development.local`. Đặt `VITE_USE_MOCK=true` để chọn Mock API.
Các biến `VITE_*` được đưa vào bundle trình duyệt, vì vậy chỉ chứa cấu hình
công khai. Khi build triển khai, cung cấp URL API chính thức qua
`VITE_API_BASE_URL`. Địa chỉ trong `.env.production` là cấu hình mẫu.
Hosting cần phục vụ `index.html` cho các đường dẫn của React Router.

Kiểm tra trước khi gửi Pull Request:

```powershell
npx --no-install tsc --noEmit
npm run build
npm test
```

Vitest kiểm tra workflow mock. Kiểm tra tích hợp dùng Spring Boot và
PostgreSQL test riêng để xác nhận các thao tác ghi dữ liệu.
