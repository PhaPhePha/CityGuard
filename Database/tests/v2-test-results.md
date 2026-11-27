# Kết quả kiểm thử CityGuard V2 ngày 19 tháng 11 năm 2026

## Phạm vi thực thi

Đã chạy PostgreSQL **17.6 Windows**, cluster mới dành riêng cho kiểm thử, chỉ lắng nghe `127.0.0.1:55449`, role `cityguard_v2_test_owner`. Không kết nối cổng 5432, không đọc/ghi `cityguard_db` đang sử dụng, không chạy migration trên dữ liệu thật. Cluster test dùng trust local trong phiên ngắn, đã dừng sau kiểm thử; không phải cấu hình auth triển khai khuyến nghị.

Lượt cuối chạy đầy đủ từ database V1 mới, không dùng chế độ resume:

```text
python -u database/tests/run-upgrade-tests.py
  --psql "C:\Program Files\PostgreSQL\17\bin\psql.exe"
  --host 127.0.0.1 --port 55449 --user cityguard_v2_test_owner
```

Đây là câu lệnh test, không phải hướng dẫn kết nối server đang dùng. Muốn lặp lại cần tự khởi tạo cluster test an toàn và đảm bảo chưa có database `cityguard_test`; runner không ghi đè hoặc drop database. Những scenario khác dùng hậu tố ngẫu nhiên để không đụng dữ liệu thử cũ. Test SQL tự rollback fixture; identity sequence có thể tăng.

## Kết quả thực tế

| Kiểm tra | Kết quả |
|---|---|
| Baseline V1 trên database sạch | PASS |
| Preflight chỉ đọc | PASS; snapshot dữ liệu 6 bảng không đổi |
| V1 có users/categories/reports/votes/comments/admin_logs → V2 | PASS, COMMIT |
| Bảo toàn dữ liệu V1 | PASS; count và digest JSON toàn bộ từng dòng của cả 6 bảng trước/sau giống nhau |
| Migration chạy lại | Bị từ chối có chủ đích khi bảng V2 đã có, không ghi đè |
| Kiểu dữ liệu/nullability/identity bảng mới | PASS cho đầy đủ 11 cột |
| Metrics backfill | PASS, tất cả report kể cả không có vote; count khớp aggregation |
| Metrics PK/FK/default count/CHECK âm | PASS |
| Global UNIQUE reports/comments | PASS; hai user khác nhau dùng cùng key bị từ chối |
| Query tạo report/comment và retry | PASS, dùng chính prepared statements V2; report khởi tạo metrics 0 |
| Owner filtering của report_retry | PASS, không trả report của owner khác |
| Vote unique/upsert/metrics transaction | PASS; đổi vote cập nhật hai count, invalidate trust; retry cùng vote giữ trust đã tính |
| Lỗi sau khi ghi vote và metrics | PASS; fault injection rollback cả vote lẫn count |
| Hai kết nối cùng vote | PASS; hai user CONFIRMED trên cùng report, count cuối là 2 dưới parent lock |
| Refresh token | PASS: issue, UNIQUE hash, FK, định dạng hash, owner revoke, rotation tuần tự, token revoked/expired bị loại, account LOCKED không được issue |
| Bộ regression ban đầu tương thích V2 | PASS, đủ 14 mục |
| Seed V2 chạy hai lần | PASS; lần lặp giữ nguyên snapshot 6 bảng và cache metrics |
| Cache sau seed | PASS qua upgrade-tests lần hai |
| Duplicate request key trong reports | Preflight và migration đều từ chối, dữ liệu giữ nguyên, không có bảng V2 commit dở |
| Duplicate request key trong comments | Tương tự, PASS |
| Schema drift latitude thành numeric | Preflight và migration đều từ chối, không sửa dữ liệu |
| CHECK latitude bị nới thành -190..190 | Preflight và migration đều từ chối |
| Global unique index tương đương đã tồn tại | PASS; được dùng lại, không thêm global unique index trùng |
| Vote UNIQUE thiếu nhưng dữ liệu không trùng | PASS; tạo lại đúng một constraint sau kiểm tra duplicate |
| Server test sau kiểm thử | Đã dừng thành công |

Không drop database trong runner. Fixture hash hình dạng chỉ dùng ở SQL test và rollback, không được mô tả là tài khoản đăng nhập được. Số 12.34/nhãn test trong kiểm thử invalidation không phải công thức hoặc nhãn nghiệp vụ.

## Kiểm tra tĩnh đã thực hiện

- SHA-256 baseline `schema.sql` vẫn là `a455018108862eb5332de610d1edbebfc097e6d72f02416ddba6f81d6711be03`: file không đổi.
- Block kiểm tra catalog trong preflight và migration giống nhau.
- Payload seed psql và seed DBeaver giống nhau; bản DBeaver chỉ thêm guard database, không chứa lệnh backslash.
- Không còn conflict target `(user_id,client_request_id)` trong query/seed/test V2; constraint legacy vẫn được giữ theo kế hoạch tương thích.
- Các file SQL không có lệnh DROP TABLE/TRUNCATE.
- `queries.sql` có 21 PREPARE; PostgreSQL đã parse/prepare trong các test thực tế.
- Runner Python kiểm tra cú pháp bằng AST; không cần dependency Python bên thứ ba.

## Lỗi trong quá trình thử đã được giải quyết

Sandbox chặn TCP local và vòng đời process nền, nên phiên test phải được cho phép chạy ngoài sandbox; server/run-test/stop nằm trong cùng phiên. Không mở quyền truy cập database đang sử dụng.

SQL fixture tiếng Việt qua Windows command-line arguments từng lỗi UTF-8; runner đã đổi sang stdin UTF-8 và PGCLIENTENCODING=UTF8. Một fixture FK của test V1 dùng cùng UUID với report đã tồn tại nên V2 báo UNIQUE trước FK; đã đổi fixture FK sang UUID độc lập. Sau các sửa này, lượt test cuối chạy đầy đủ đạt.

## Chưa kiểm chứng và giới hạn

- Chưa chạy trên `cityguard_db` đang dùng, chưa kiểm tra duplicate/catalog/dung lượng/quyền thực tế của database đó; cần phê duyệt và preflight tại thời điểm triển khai.
- Chưa chạy backup/restore rehearsal trên dữ liệu thật. `upgrade-plan.md` có bước bắt buộc, không tuyên bố đã thực hiện.
- Chưa có source Backend để test Java transaction, DTO, API login/logout, integration migration framework, hash encoder, rotation đồng thời hoặc access-token revocation.
- Chưa test trên UI DBeaver. Đã đối chiếu payload SQL seed DBeaver với seed psql; phần psql đã thực thi thành công.
- Chưa benchmark dữ liệu lớn hoặc chứng minh mục tiêu API ~2s. Migration cần thời gian khóa/backfill; timeout phải được đánh giá trên môi trường triển khai.
- Không có trigger tự duy trì count/trust. Hai-session test chỉ chứng minh protocol parent-lock đã cung cấp; writer bỏ qua protocol vẫn có thể làm cache stale.
- Thang điểm/công thức/nhãn, TTL và SHA-256 encoding/rotation policy vẫn cần Backend xác nhận. Chưa có token family/reuse detection.
- Chưa loại bỏ constraint ghép legacy vì chưa xác nhận toàn bộ caller/dependency ngoài workspace.
