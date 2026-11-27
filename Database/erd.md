# CityGuard V3 ERD (baseline V1 + migration V2 + migration V3)

```mermaid
erDiagram
    users ||--o{ reports : creates
    categories ||--o{ reports : classifies
    users ||--o{ report_votes : votes
    reports ||--o{ report_votes : receives
    users ||--o{ comments : writes
    reports ||--o{ comments : contains
    users ||--o{ admin_logs : performs
    reports ||--o| report_metrics : caches
    users ||--o{ refresh_tokens : owns
    users {
        bigint id PK
        varchar username "nullable; unique lower()"
        varchar email "nullable; unique lower()"
        text password_hash
        varchar display_name
        text avatar_url
        varchar role
        varchar status
        timestamptz created_at
        timestamptz updated_at
    }
    categories {
        bigint id PK
        varchar code UK
        varchar display_name
        varchar icon
        varchar color
        integer default_ttl_seconds
        boolean is_active
    }
    reports {
        bigint id PK
        varchar(30) public_id UK "Format: rpt_ + 26-char Crockford Base32 ULID"
        bigint user_id FK
        bigint category_id FK
        uuid client_request_id UK "Global in reports; idempotency key"
        varchar description
        text image_url
        float8 latitude
        float8 longitude
        varchar address_snapshot
        varchar status
        timestamptz created_at
        timestamptz updated_at
        timestamptz expires_at
    }
    report_votes {
        bigint id PK
        bigint report_id FK "UNIQUE report_id + user_id"
        bigint user_id FK
        varchar vote_type
        timestamptz created_at
        timestamptz updated_at
    }
    comments {
        bigint id PK
        bigint report_id FK
        bigint user_id FK
        uuid client_request_id UK "Global in comments; legacy user+key retained"
        varchar body
        boolean is_hidden
        timestamptz deleted_at
        timestamptz created_at
        timestamptz updated_at
    }
    admin_logs {
        bigint id PK
        bigint actor_id FK
        varchar action
        varchar target_type
        bigint target_id "logical reference, no FK"
        varchar reason
        timestamptz created_at
    }
    report_metrics {
        bigint report_id PK,FK
        bigint confirm_count "NOT NULL default 0 CHECK >=0"
        bigint no_longer_count "NOT NULL default 0 CHECK >=0"
        numeric trust_score "nullable numeric(5,2)"
        varchar trust_label "nullable varchar(32)"
        timestamptz computed_at "nullable trust calculation time"
    }
    refresh_tokens {
        bigint id PK "identity"
        bigint user_id FK
        varchar token_hash UK "64 lowercase hex characters"
        timestamptz expires_at
        timestamptz revoked_at "nullable"
    }
```

### Điểm nhấn cấu trúc V3:

1. **`reports.id` (BIGINT PK) được bảo toàn nguyên vẹn**:
   - Tất cả FK nội bộ (`report_votes.report_id`, `comments.report_id`, `report_metrics.report_id`) tiếp tục tham chiếu `reports.id`.
   - Hiệu năng JOIN và index foreign key không thay đổi.

2. **`reports.public_id` (VARCHAR(30) UK NOT NULL)**:
   - Định dạng: tiền tố `rpt_` (4 ký tự thường) kết hợp với chuỗi 26 ký tự ULID canonical chữ hoa chuẩn Crockford Base32 (`0-9`, `A-Z` loại trừ `I, L, O, U`, ký tự đầu thuộc `0-7`).
   - Ràng buộc: `CHECK (public_id ~ '^rpt_[0-7][0-9A-HJKMNP-TV-Z]{25}$')` và `UNIQUE (public_id)`.
   - Dùng làm định danh công khai cho API, định tuyến URL và hiển thị ứng dụng Android/Web (`GET /api/v1/reports/{public_id}`).

3. **`reports.client_request_id` (UUID UK NOT NULL)**:
   - Giữ nguyên toàn bộ cơ chế chống gửi trùng (idempotency key) toàn bảng đã được thiết lập ở V2.

4. **8 bảng cốt lõi**:
   - Vẫn gồm 8 bảng: `users`, `categories`, `reports`, `report_votes`, `comments`, `admin_logs`, `report_metrics`, `refresh_tokens`.
