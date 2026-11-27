# Kết quả kiểm thử thực tế — 2026-11-05

## Môi trường và cách ly

- PostgreSQL 17.6 Windows, binaries trong `C:\Program Files\PostgreSQL\17\bin` (không có trong PATH ban đầu).
- Tạo cluster mới trong thư mục TEMP có tên `cityguard-db-test-<UUID>`, chỉ listen `127.0.0.1:55439`, role riêng `cityguard_test_owner`, DB `cityguard_test`. Không kết nối database dịch vụ có sẵn.
- `initdb` thành công. `pg_ctl start` lỗi tạo restricted token trong sandbox; khởi chạy trực tiếp `postgres.exe` bằng process nền ẩn thành công. Không yêu cầu mở quyền sandbox.
- Cluster test dùng trust local để kiểm tra ngắn hạn, không phải cấu hình triển khai khuyến nghị. README hướng dẫn SCRAM/password cho môi trường nhóm.

## Đã chạy và kết quả

| Kiểm tra | Kết quả thực tế |
|---|---|
| schema.sql trên DB mới | PASS: tạo đủ 6 bảng, index, function và 4 trigger; COMMIT |
| database-tests.sql trước seed | PASS: toàn bộ 14 mục, rollback fixtures |
| queries.sql | PASS: PostgreSQL PREPARE đủ 14 statement, xác nhận cú pháp/type/quan hệ |
| seed lần 1 | PASS: 6 category, 5 user, 6 report, 2 vote, 3 comment |
| seed lần 2 | PASS: mỗi INSERT thêm 0 dòng, counts giữ nguyên |
| Hash seed | Dùng pgcrypto chỉ trên DB test để tạo bcrypt từ password UUID ngẫu nhiên; kiểm tra crypt(password,hash)=hash trả true, không in/lưu password/hash vào source |
| map_bbox vùng TP.HCM, ACTIVE | Trả đúng 2 report ACTIVE còn TTL |
| map_bbox vùng vượt kinh tuyến 180 | Chạy thành công; trả 0 với seed ở Việt Nam (chưa có fixture +/-180) |
| admin_dashboard | 5 accounts, 3 USER, 6 reports, 2 active, 1 pending, 0 overdue, 1 hidden |
| report_vote_counts | Report mẫu 1 có confirmed=1, no_longer=0, total=1 |
| visible_comments | Chỉ trả 1 comment public; loại 1 hidden, 1 soft-deleted |
| put_vote đổi rồi retry cùng giá trị | Lần đổi trả 1 row, lần lặp không ghi thêm (0 row); transaction rollback |
| delete_own_comment ID không tồn tại | UPDATE 0, không tác động dữ liệu |
| expire_reports khi không có dòng đến hạn | UPDATE 0; case expire thật đã pass trong suite assertion |
| profile_summary sau đổi vote trong transaction | User B report_count=0, confirmed_reports=0, đúng trạng thái hiện hành |
| database-tests.sql sau seed | PASS lần hai, giữ dữ liệu seed sau rollback |

Các mục assertion gồm: insert hợp lệ, username/email unique không phân biệt hoa thường, FK, NOT NULL, vote trùng/update, comment soft-delete/hide, bbox trong/ngoài, category/status, TTL giữ lịch sử, thống kê/audit, report/comment request key retry, tọa độ không hợp lệ/NaN, expires_at, status/body CHECK và restrictive delete. Kiểm tra dùng RAISE EXCEPTION, không phụ thuộc cấu hình plpgsql.check_asserts.

`pgcrypto` **không phải dependency của schema hoặc seed**: chỉ được bật trong cluster test để kiểm tra hash thực vì chưa có Backend. Chưa kiểm chứng Argon2id bằng thư viện Backend. Hash fixture hình dạng trong suite chỉ phục vụ constraints và luôn rollback; không tuyên bố là tài khoản đăng nhập.

## Chưa kiểm chứng

- API login/logout/authorization, token của account bị khóa, xác thực hash qua Backend, Android/Admin UI vì chưa có source.
- Các prepared write create_report/create_comment/hide_report chưa được EXECUTE trực tiếp trong smoke test; thao tác insert, unique retry, audit và soft delete tương đương đã được assertion suite kiểm tra. Cần integration test đúng driver khi Backend có mặt.
- Race hai kết nối, deadlock, stale retry vote, 409 payload khác, cursor qua nhiều trang có timestamp trùng và fixture bbox antimeridian/boundary cần kiểm thử tích hợp bổ sung.
- Hiệu năng với dữ liệu lớn/EXPLAIN ANALYZE, mục tiêu API ~2s, quyền runtime/append-only audit, restore backup và PostgreSQL 14–16.
- Chưa có công thức reliability hay tiêu chí chuyển ACTIVE/RESOLVED được duyệt, do đó chưa thể test đúng nghiệp vụ các rule này.

Không chạy migration trên database thật; không xóa database/table hoặc dữ liệu hiện có. Cluster test được dừng sau kiểm thử; thư mục TEMP được giữ để kiểm tra khi cần.
