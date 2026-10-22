# CityGuard Team Rules

## 1. Phân công công việc

- Mỗi task phải có người phụ trách rõ ràng.
- Không tự ý sửa phần việc của thành viên khác nếu chưa trao đổi.
- Mỗi task cần ghi rõ:
  - Nội dung cần làm
  - Người phụ trách
  - Deadline
  - Kết quả cần đạt

## 2. Làm việc với Git

- Không commit trực tiếp vào branch `main`.
- Mỗi task tạo một branch riêng:

feature/login
feature/map
feature/report
fix/comment

- Trước khi code phải cập nhật code mới nhất:

git checkout main
git pull origin main

- Sau khi hoàn thành:

git add .
git commit -m "feat: add report creation screen"
git push origin feature/report

- Tạo Pull Request để leader hoặc thành viên khác review.
- Chỉ merge vào `main` sau khi đã kiểm tra và không có lỗi nghiêm trọng.

## 3. Quy tắc đặt tên

- Package Kotlin: viết thường.

com.cityguard.app.presentation.report

- Class: PascalCase.

ReportViewModel
ReportRepository

- Hàm và biến: camelCase.

createReport()
currentLocation

- Tên branch viết thường, dùng dấu gạch ngang.

feature/create-report
fix/login-error

## 4. Quy tắc commit

Dùng các tiền tố:

feat: thêm chức năng mới
fix: sửa lỗi
refactor: chỉnh sửa code nhưng không đổi chức năng
docs: cập nhật tài liệu
style: chỉnh format, giao diện
test: thêm hoặc sửa test
chore: cấu hình dự án

Ví dụ:

feat: add login screen
fix: validate report description
docs: update project structure

## 5. Quy tắc code

- Code phải đúng package đã thống nhất.
- Không viết toàn bộ logic trong `Activity`.
- UI đặt trong `presentation`.
- Model nghiệp vụ đặt trong `domain`.
- Xử lý dữ liệu đặt trong `data`.
- Code dùng chung đặt trong `core`.
- Không hard-code API URL, token hoặc mật khẩu.
- Không xóa code của người khác nếu chưa trao đổi.
- Code phải dễ đọc, tránh viết tắt tên biến.

## 6. Quy tắc giao diện

- Bám theo thiết kế Figma đã thống nhất.
- Dùng màu sắc, font chữ và kích thước trong theme chung.
- Không tự ý thay đổi luồng màn hình.
- Nếu thiết kế chưa rõ, hỏi leader trước khi triển khai.

## 7. Quy tắc kiểm tra trước khi tạo Pull Request

Trước khi gửi Pull Request, thành viên phải:

- Kiểm tra code có build được không.
- Kiểm tra chức năng chính.
- Kiểm tra lỗi giao diện cơ bản.
- Kiểm tra không làm hỏng chức năng cũ.
- Mô tả rõ mình đã thay đổi gì.
- Đính kèm ảnh hoặc video nếu có thay đổi giao diện.

## 8. Quy tắc review

- Mỗi Pull Request cần ít nhất một người review.
- Người review tập trung vào:
  - Đúng yêu cầu không
  - Có lỗi logic không
  - Có ảnh hưởng code khác không
  - Có đúng cấu trúc dự án không
- Không review theo cảm tính hoặc công kích cá nhân.
- Nếu có lỗi, ghi rõ vị trí và cách đề xuất sửa.

## 9. Họp và trao đổi

- Họp ngắn định kỳ để cập nhật tiến độ.
- Khi gặp lỗi quá 30 phút, báo lên nhóm để cùng xử lý.
- Mọi thay đổi lớn về chức năng, database hoặc cấu trúc thư mục phải được thông báo.
- Quyết định quan trọng phải được ghi lại trong nhóm hoặc tài liệu dự án.

## 10. Definition of Done

Một task chỉ được xem là hoàn thành khi:

- Đã code đúng yêu cầu.
- Đã kiểm tra chức năng.
- Không có lỗi build.
- Đã push lên branch riêng.
- Đã tạo Pull Request.
- Đã được review và merge.
- Đã cập nhật trạng thái task.