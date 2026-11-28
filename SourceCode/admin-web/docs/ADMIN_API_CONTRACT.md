# CITYGUARD ADMIN WEB – API CONTRACT SPECIFICATION

> **Phiên bản tài liệu:** 1.0.0 (Dành cho TV5 Backend & TV6 Admin Web Frontend)
>
> **Người lập:** TV6 (Frontend Admin Web)
>
> **Người nhận & xác nhận:** TV5 (Backend REST API) & TV1 (Leader)
>
> **Căn cứ:** `CityGuard_SRS_SoBo.docx`, `CityGuard_WBS.docx`, Database CityGuard V3 Schema (`BACKEND_HANDOFF_V3.md`).

---

## 1. QUY ƯỚC TRẠNG THÁI CONTRACT

Để tránh ngộ nhận giữa tài liệu đặc tả và mã nguồn thực tế, toàn bộ endpoint trong bảng này được phân loại theo 5 trạng thái chuẩn:
- **EXPECTED:** Endpoint dự kiến được trích xuất từ tài liệu SRS sơ bộ mục 6.4 và WBS 5.0.
- **CONFIRMED:** Endpoint đã được TV5 xác nhận qua biên bản họp hoặc trao đổi kỹ thuật.
- **IMPLEMENTED:** Endpoint đã tồn tại trên server Backend và được TV6 kiểm thử tích hợp thực tế thành công.
- **MISSING:** Endpoint cần thiết cho Admin Web nhưng Backend hiện chưa triển khai.
- **MISMATCH:** Endpoint có sự bất tương thích về HTTP Method, URL path, DTO payload, hoặc kiểu định danh (ví dụ: dùng ID số nguyên thay vì `public_id`).

---

## 2. BẢNG TỔNG HỢP API CONTRACT (ADMIN WEB ↔ BACKEND)

| STT | Chức năng | HTTP Method | Endpoint dự kiến (SRS) | Endpoint đề xuất thực tế | Quyền hạn | Trạng thái hiện tại |
|---|---|---|---|---|---|---|
| 1 | **Đăng nhập Admin/Moderator** | `POST` | `/api/v1/admin/auth/login` | `/api/v1/admin/auth/login` | Public | **EXPECTED** / **MISSING** |
| 2 | **Thống kê Dashboard** | `GET` | `/api/v1/admin/dashboard/summary` | `/api/v1/admin/dashboard/summary` | `ADMIN`, `MODERATOR` | **EXPECTED** / **MISSING** |
| 3 | **Danh sách Danh mục Sự cố** | `GET` | `/api/v1/admin/categories` | `/api/v1/admin/categories` | `ADMIN`, `MODERATOR` | **EXPECTED** / **MISSING** |
| 4 | **Danh sách Báo cáo** | `GET` | `/api/v1/admin/reports` | `/api/v1/admin/reports` | `ADMIN`, `MODERATOR` | **EXPECTED** / **MISSING** |
| 5 | **Chi tiết Báo cáo** | `GET` | `/api/v1/admin/reports/{id}` | `/api/v1/admin/reports/{public_id}` | `ADMIN`, `MODERATOR` | **MISMATCH** *(SRS ghi {id}, V3 dùng public_id)* |
| 6 | **Cập nhật Trạng thái Báo cáo** | `PATCH` | `/api/v1/admin/reports/{id}/status` | `/api/v1/admin/reports/{public_id}/status` | `ADMIN`, `MODERATOR` | **MISMATCH** *(SRS ghi {id}, V3 dùng public_id)* |
| 7 | **Danh sách Người dùng** | `GET` | `/api/v1/admin/users` | `/api/v1/admin/users` | `ADMIN` | **EXPECTED** / **MISSING** |
| 8 | **Chi tiết Người dùng** | `GET` | `/api/v1/admin/users/{id}` | `/api/v1/admin/users/{id}` | `ADMIN` | **EXPECTED** / **MISSING** |
| 9 | **Khóa/Mở khóa Người dùng** | `PATCH` | `/api/v1/admin/users/{id}/status` | `/api/v1/admin/users/{id}/status` | `ADMIN` | **EXPECTED** / **MISSING** |
| 10 | **Danh sách Bình luận** | `GET` | `/api/v1/admin/comments` | `/api/v1/admin/comments` | `ADMIN`, `MODERATOR` | **EXPECTED** / **MISSING** |
| 11 | **Ẩn/Khôi phục Bình luận** | `PATCH` | `/api/v1/admin/comments/{id}/status` | `/api/v1/admin/comments/{id}/status` | `ADMIN`, `MODERATOR` | **EXPECTED** / **MISSING** |
| 12 | **Nhật ký Quản trị** | `GET` | `/api/v1/admin/logs` | `/api/v1/admin/logs` | `ADMIN` *(chờ TV1/5 chốt)* | **EXPECTED** / **MISSING** |

---

## 3. CHI TIẾT REQUEST & RESPONSE CONTRACT

### 3.1. Đăng nhập Admin (`POST /api/v1/admin/auth/login`)
- **Headers:** `Content-Type: application/json`
- **Request Body:**
  ```json
  {
    "identifier": "admin@example.invalid", // Chấp nhận username hoặc email
    "password": "SamplePassword123!"
  }
  ```
- **Response thành công (HTTP 200):**
  ```json
  {
    "success": true,
    "message": "Đăng nhập thành công",
    "data": {
      "token": "eyJhbGciOi...",
      "tokenType": "Bearer",
      "user": {
        "id": 4,
        "username": "demo_admin",
        "email": "admin@example.invalid",
        "displayName": "Demo Admin",
        "role": "ADMIN",
        "status": "ACTIVE"
      }
    }
  }
  ```
- **Response lỗi:**
  - `401 Unauthorized`: Sai tài khoản/mật khẩu.
  - `403 Forbidden`: Người dùng có role `USER` hoặc tài khoản có status `LOCKED`.

---

### 3.2. Dashboard Summary (`GET /api/v1/admin/dashboard/summary`)
- **Headers:** `Authorization: Bearer <token>`
- **Response thành công (HTTP 200):**
  ```json
  {
    "success": true,
    "data": {
      "totalUsers": 120,
      "totalReports": 450,
      "activeReports": 85,
      "pendingReports": 24
    }
  }
  ```

---

### 3.3. Danh mục Sự cố (`GET /api/v1/admin/categories`)
- **Headers:** `Authorization: Bearer <token>`
- **Response thành công (HTTP 200):** Trả về chính xác 6 danh mục trong CityGuard V3:
  ```json
  {
    "success": true,
    "data": [
      { "id": 1, "code": "TRAFFIC_JAM", "name": "Kẹt xe", "color": "#EF4444" },
      { "id": 2, "code": "ROAD_BLOCK", "name": "Rào chắn / Cấm đường", "color": "#F97316" },
      { "id": 3, "code": "ROAD_DAMAGE", "name": "Đường hư hỏng / Ổ gà", "color": "#F59E0B" },
      { "id": 4, "code": "FLOOD", "name": "Ngập nước", "color": "#3B82F6" },
      { "id": 5, "code": "BROKEN_TRAFFIC_LIGHT", "name": "Đèn tín hiệu hỏng", "color": "#8B5CF6" },
      { "id": 6, "code": "FALLEN_TREE", "name": "Cây đổ", "color": "#10B981" }
    ]
  }
  ```
> *Lưu ý:* Frontend không tự thêm các danh mục không có trong database như "Tai nạn" hay "Khác". Bộ lọc sự cố trên Admin Web phải được tải động từ API này.

---

### 3.4. Báo cáo (Report Moderation)

#### Quy tắc Xử lý Điểm Tin cậy (Trust Score & Trust Label):
1. **Không tính toán ở Frontend:** Điểm tin cậy được tính toán hoàn toàn bởi hệ thống phân tích backend / thuật toán crowd-verification.
2. **Xử lý giá trị `null`:**
   - Khi `trustScore = null`: Frontend hiển thị chữ "Chưa có đánh giá", **không hiển thị** thanh đo tỷ lệ %.
   - Khi `trustLabel = null`: Frontend hiển thị nhãn trung tính "Đang xác minh".

#### Bảng Quy Tắc Chuyển Đổi Trạng Thái (Report State Machine):
| Trạng thái hiện tại | Trạng thái chuyển đổi cho phép | Trạng thái xác nhận (Contract) | Ghi chú |
|---|---|---|---|
| `ACTIVE` | `HIDDEN` | **CONFIRMED** | Ẩn sự cố do vi phạm/spam (bắt buộc nhập lý do) |
| `ACTIVE` | `RESOLVED` | **CONFIRMED** | Sự cố đã được xử lý/giải tỏa hiện trường |
| `PENDING_VERIFY` | `HIDDEN` | **CONFIRMED** | Ẩn sự cố rác ngay từ khi mới tạo |
| `PENDING_VERIFY` | `RESOLVED` | **CONFIRMED** | Sự cố được giải quyết |
| `HIDDEN` | `ACTIVE` | **CONFIRMED** | Khôi phục hiển thị báo cáo sau khi xem xét khiếu nại |
| `RESOLVED` | `HIDDEN` | **CONFIRMED** | Ẩn báo cáo kể cả khi đã giải quyết |
| `EXPIRED` | `HIDDEN` | **CONFIRMED** | Ẩn báo cáo đã hết hạn nếu phát hiện vi phạm |
| `*` sang bất kỳ trạng thái khác | Ví dụ: `EXPIRED` sang `PENDING_VERIFY` | **PENDING (Chờ TV5 xác nhận)** | Chưa được phép thực hiện trên UI hoặc Mock |

#### A. Danh sách báo cáo (`GET /api/v1/admin/reports`)
- **Query Parameters:**
  - `keyword` (string, optional): Tìm kiếm theo description hoặc address_snapshot.
  - `categoryId` (number, optional): Lọc theo ID danh mục.
  - `status` (string, optional): `PENDING_VERIFY`, `ACTIVE`, `EXPIRED`, `RESOLVED`, `HIDDEN`.
  - `page` (number, default: 0), `size` (number, default: 10), `sortBy`, `direction`.
- **Response thành công (HTTP 200):**
  ```json
  {
    "success": true,
    "data": {
      "items": [
        {
          "publicId": "rpt_01M6P4YPXFBG21T29EZJ31RR0Q",
          "categoryId": 1,
          "categoryCode": "TRAFFIC_JAM",
          "categoryDisplayName": "Kẹt xe",
          "categoryColor": "#EF4444",
          "description": "Ùn tắc nghiêm trọng tại ngã tư",
          "imageUrl": "https://example.com/images/jam.jpg",
          "latitude": 10.7769,
          "longitude": 106.7009,
          "addressSnapshot": "Quận 1, TP.HCM",
          "status": "ACTIVE",
          "confirmCount": 15,
          "noLongerCount": 2,
          "trustScore": 88.50,
          "trustLabel": "Cao",
          "createdAt": "2026-11-20T10:00:00Z",
          "expiresAt": "2026-11-20T14:00:00Z"
        }
      ],
      "page": 0,
      "size": 10,
      "totalItems": 450,
      "totalPages": 45,
      "isLast": false
    }
  }
  ```

#### B. Chi tiết báo cáo (`GET /api/v1/admin/reports/{public_id}`)
- **Path Parameter:** `{public_id}` là chuỗi định danh ULID 30 ký tự (`rpt_...`).
- **Response thành công (HTTP 200):** Bổ sung thông tin người tạo báo cáo (`reporterId`, `reporterUsername`, `reporterDisplayName`).

#### C. Cập nhật trạng thái (`PATCH /api/v1/admin/reports/{public_id}/status`)
- **Request Body:**
  ```json
  {
    "status": "HIDDEN", // PENDING_VERIFY | ACTIVE | EXPIRED | RESOLVED | HIDDEN
    "reason": "Nội dung phản cảm hoặc thông tin sai lệch"
  }
  ```

---

### 3.4. Quản lý Người dùng (User Management - Role ADMIN)

#### A. Danh sách người dùng (`GET /api/v1/admin/users`)
- **Query Parameters:** `search`, `role` (USER | MODERATOR | ADMIN), `status` (ACTIVE | LOCKED), `page`, `size`.
- **Lưu ý bảo mật:** Tuyệt đối không trả `password_hash` ra API.

#### B. Khóa/Mở khóa tài khoản (`PATCH /api/v1/admin/users/{id}/status`)
- **Request Body:**
  ```json
  {
    "status": "LOCKED", // ACTIVE | LOCKED
    "reason": "Tài khoản spam nhiều báo cáo giả mạo"
  }
  ```

---

### 3.5. Quản lý Bình luận (Comment Moderation)
- **Danh sách:** `GET /api/v1/admin/comments?reportPublicId=&isHidden=&page=&size=`
- **Cập nhật:** `PATCH /api/v1/admin/comments/{id}/status`
  ```json
  {
    "isHidden": true,
    "reason": "Ngôn từ vi phạm tiêu chuẩn cộng đồng"
  }
  ```
  *(Frontend và Backend thống nhất chỉ thay đổi cờ `is_hidden`, không đụng tới `deleted_at` và không xóa vật lý)*.

---

### 3.6. Nhật ký Quản trị (`GET /api/v1/admin/logs`)
- **Query Parameters:** `actorId`, `action`, `targetType`, `page`, `size`.
- **Response:** Danh sách log gồm `actorId`, `actorUsername`, `action`, `targetType`, `targetId`, `reason`, `createdAt`.

---

## 4. CÁC ĐIỂM CẦN TV5 VÀ TV1 XÁC NHẬN CHÍNH THỨC

1. **Chuẩn hóa Public ID trong URL Báo cáo:** Xác nhận TV5 sẽ triển khai API path nhận `public_id` (`/reports/{public_id}`) thay vì ID nội bộ BIGINT.
2. **Cơ chế xác thực phiên (Auth Contract):** TV5 sẽ cấp token JWT qua Authorization Bearer Header hay qua HttpOnly Cookie?
3. **Phân quyền xem Audit Logs:** TV1 và TV5 chốt rõ `MODERATOR` có được cấp quyền đọc `admin_logs` hay chỉ duy nhất `ADMIN` mới được đọc.
