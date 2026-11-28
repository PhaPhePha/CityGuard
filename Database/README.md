# CityGuard Database V4 (Firebase Authentication & Social Login)

PostgreSQL 17+. One shared database for Android/Backend/Admin; clients access it through Backend. No extensions or PostGIS required. Never run migrations or reset scripts on the active `cityguard_db` without explicit approval.

## Authoritative schema and files

- `migrations/V1__initial_schema.sql`: Pure SQL V1 versioned migration creating the 6 foundational tables (`users`, `categories`, `reports`, `report_votes`, `comments`, `admin_logs`), constraints, 7 `ON DELETE RESTRICT` foreign keys, 12 indexes, `set_updated_at()` function, and 4 triggers.
- `schema.sql`: restored V1 baseline DDL identical to `migrations/V1__initial_schema.sql`. Do not use as a scratch query file.
- `preflight-upgrade.sql`: read-only V1 catalog, type, FK, coordinate CHECK and duplicate validation before V2.
- `migrations/V2__metrics_tokens_request_keys.sql`: applies once after V1; adds `report_metrics` and `refresh_tokens`, global `client_request_id` uniqueness per table, and backfills metrics counts.
- `preflight-v3.sql`: read-only V2 catalog and state validation before applying V3.
- `migrations/V3__report_public_id_ulid.sql`: applies once after V2; adds `reports.public_id` (format `rpt_` + 26-char Crockford Base32 ULID, NOT NULL, UNIQUE, CHECK). Preserves internal `reports.id BIGINT PK` and all foreign keys.
- `verify-v3.sql`: read-only Pure SQL verification script confirming all 8 tables and V3 constraints.
- `preflight-v4.sql`: read-only V3 catalog and state validation before applying V4.
- `migrations/V4__firebase_auth.sql`: applies once after V3; adds Firebase Authentication & OAuth Provider support (`user_firebase_accounts` and `user_auth_providers`), composite foreign key preventing cross-user mapping, and enables `users.password_hash` to be nullable for OAuth-only users.
- `verify-v4.sql`: read-only Pure SQL post-upgrade verification confirming all 10 tables, 10 PKs, 13 RESTRICT FKs (including composite FK), triggers, and nullable `password_hash`.
- `docker-compose.yml` & `.env.example`: Dev/Test Docker Compose setup running PostgreSQL 17 on `127.0.0.1:5433` (isolated from local port `5432`), automatically initializing `V1 -> V2 -> V3 -> V4 -> Verify` on fresh volume, with an optional `--profile demo` service to load `seed.sql`.
- `bootstrap-v4.py`: automated test database initializer (`Empty DB -> V1 -> Preflight V2 -> V2 -> Preflight V3 -> V3 -> Preflight V4 -> V4 -> Seed Demo -> Verify V4`).
- `queries.sql`: prepared query examples for report lifecycle and queries.
- `seed.sql`: 100% Pure SQL development/test fixtures with valid Crockford Base32 ULIDs.
- `update-demo-passwords.sql`: separate demo password reset; not a migration or seed dependency.
- `erd.md`, `design-decisions.md`: ERD and architectural rationale.
- `BACKEND_HANDOFF_V4.md`: detailed technical handoff document and integration guide for the Backend team (TV5).
- `tests/upgrade-v4-tests.sql`: V4 integration assertions (multi-provider, uniqueness, composite FK cross-mapping prevention, nullability).
- `tests/run-v4-tests.py`: end-to-end Python test runner testing clean bootstrap, V3→V4 data preservation, functional constraints, rollback, and `pg_dump`/`pg_restore`.

Tables (10): `users`, `categories`, `reports`, `report_votes`, `comments`, `admin_logs`, `report_metrics`, `refresh_tokens`, `user_firebase_accounts`, `user_auth_providers`.

## V4 Firebase Authentication & Social Provider Design

1. **`user_firebase_accounts` (1-1 with `users`)**:
   - `id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY`
   - `user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE RESTRICT`
   - `firebase_uid VARCHAR(128) NOT NULL UNIQUE` (Firebase Project UID)
   - `UNIQUE (id, user_id)` (supports Composite FK)
2. **`user_auth_providers` (1-N with `users`)**:
   - `id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY`
   - `user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT`
   - `firebase_account_id BIGINT REFERENCES user_firebase_accounts(id) ON DELETE RESTRICT`
   - `provider VARCHAR(32) NOT NULL CHECK (provider IN ('GOOGLE', 'FACEBOOK'))`
   - `provider_uid VARCHAR(128) NOT NULL` (Google Sub / Facebook ID)
   - `email VARCHAR(254)`, `display_name VARCHAR(100)`, `avatar_url TEXT`
   - `UNIQUE (provider, provider_uid)`: guarantees a social identity is bound to at most one CityGuard user.
   - `UNIQUE (user_id, provider)`: guarantees a user links at most one account per provider.
   - **Composite FK**: `FOREIGN KEY (firebase_account_id, user_id) REFERENCES user_firebase_accounts(id, user_id) ON DELETE RESTRICT` guarantees zero cross-user mapping at the database kernel level.
3. **`users.password_hash` Nullable**:
   - Allows users registered purely via Google/Facebook to have `password_hash = NULL`.
   - Local password authentication continues to enforce BCrypt/Argon2id format via `users_password_hash_check`.

## Docker Compose (Dev/Test Environment on Port 5433)

`database/docker-compose.yml` provisions an isolated PostgreSQL 17 instance bound strictly to `127.0.0.1:5433` so it never conflicts with a local PostgreSQL service on port `5432` (`cityguard_db`).

### 1. Fresh Database Initialization (Zero to V4)
```powershell
cd database
Copy-Item .env.example .env
# Edit .env and set CITYGUARD_DB_PASSWORD (never commit .env to Git)

# Validate configuration:
docker-compose config

# Start PostgreSQL 17 and initialize V1 -> V2 -> V3 -> V4 -> Verify on a fresh volume:
docker-compose up -d
```

### 2. Upgrading an Existing V3 Docker Container to V4 (In-place, Zero Data Loss)
If your container is already running with an existing volume containing V3 data, Docker skips `/docker-entrypoint-initdb.d`. To upgrade safely without dropping your volume:

```powershell
# Step 1: Backup current database inside container
docker-compose exec postgres pg_dump -U cityguard -d cityguard_v3_dev -Fc -f /var/lib/postgresql/data/backup_before_v4.dump

# Step 2: Run read-only Preflight V4
docker-compose exec postgres psql -U cityguard -d cityguard_v3_dev -v ON_ERROR_STOP=1 -f /docker-entrypoint-initdb.d/06_preflight_v4.sql

# Step 3: Apply V4 migration
docker-compose exec postgres psql -U cityguard -d cityguard_v3_dev -v ON_ERROR_STOP=1 -f /docker-entrypoint-initdb.d/07_V4__firebase_auth.sql

# Step 4: Run Post-Upgrade V4 Verification
docker-compose exec postgres psql -U cityguard -d cityguard_v3_dev -v ON_ERROR_STOP=1 -f /docker-entrypoint-initdb.d/08_verify_v4.sql
```

### 3. Load Demo Seed Data (Optional `--profile demo`)
```powershell
docker-compose --profile demo run --rm seed-demo
```

## Automated Test Database Bootstrap (`bootstrap-v4.py`)

To initialize a fresh test database on an existing PostgreSQL server from zero (`V1 -> V2 -> V3 -> V4 -> Demo Seed -> Verification V4`):

```powershell
python database/bootstrap-v4.py `
  --psql "C:\Program Files\PostgreSQL\17\bin\psql.exe" `
  --host 127.0.0.1 `
  --port <test_port> `
  --user <db_user> `
  --dbname cityguard_v4_test `
  --create-db
```
