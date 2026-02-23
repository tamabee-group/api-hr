# 🐝 Tamabee HR — Backend API

<p align="center">
  <strong>REST API quản lý nhân sự multi-tenant — xây dựng trên Spring Boot 3.5 + Java 21.</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=spring-boot" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk" alt="Java" />
  <img src="https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql" alt="PostgreSQL" />
  <img src="https://img.shields.io/badge/Flyway-Migration-CC0200?logo=flyway" alt="Flyway" />
  <img src="https://img.shields.io/badge/Docker-Ready-2496ED?logo=docker" alt="Docker" />
  <img src="https://img.shields.io/badge/Swagger-OpenAPI-85EA2D?logo=swagger" alt="Swagger" />
</p>

---

## 📋 Mục lục

- [Tổng quan](#-tổng-quan)
- [Multi-Tenant Architecture](#-multi-tenant-architecture)
- [Tech Stack](#-tech-stack)
- [Cấu trúc dự án](#-cấu-trúc-dự-án)
- [Yêu cầu hệ thống](#-yêu-cầu-hệ-thống)
- [Cài đặt & Chạy](#-cài-đặt--chạy)
- [Biến môi trường](#-biến-môi-trường)
- [Database & Migration](#-database--migration)
- [API Documentation](#-api-documentation)
- [Code Quality & Pre-commit](#-code-quality--pre-commit)
- [Conventional Commits](#-conventional-commits)
- [Testing](#-testing)
- [Deployment](#-deployment)
- [Monitoring](#-monitoring)

---

## 🌟 Tổng quan

Backend API của hệ thống Tamabee HR — ứng dụng quản lý nhân sự SaaS multi-tenant. Mỗi công ty (tenant) có database riêng biệt, tự động tạo khi đăng ký. Hệ thống sử dụng Spring Boot 3.5 với Java 21, PostgreSQL 16, và Flyway cho database migration.

---

## 🏗 Multi-Tenant Architecture

```
                         ┌──────────────┐
                         │  HTTP Request │
                         │  (X-Tenant)   │
                         └──────┬───────┘
                                │
                         ┌──────▼───────┐
                         │ TenantFilter  │  Đọc tenant từ JWT/header
                         └──────┬───────┘
                                │
                         ┌──────▼───────┐
                         │TenantContext  │  ThreadLocal lưu tenant hiện tại
                         └──────┬───────┘
                                │
                    ┌───────────▼────────────┐
                    │ TenantRoutingDataSource │  extends AbstractRoutingDataSource
                    └───────────┬────────────┘
                                │
              ┌─────────────────┼─────────────────┐
              │                 │                  │
       ┌──────▼──────┐  ┌──────▼──────┐  ┌───────▼──────┐
       │  Master DB   │  │  Tenant A   │  │  Tenant B    │
       │  (tamabee_   │  │  (tenant_   │  │  (tenant_    │
       │   master)    │  │   acme)     │  │   globex)    │
       └─────────────┘  └─────────────┘  └──────────────┘
```

### Luồng hoạt động

1. **TenantFilter** — Servlet filter đọc tenant ID từ JWT token hoặc request header
2. **TenantContext** — Lưu tenant ID vào `ThreadLocal` cho request hiện tại
3. **TenantRoutingDataSource** — Kế thừa `AbstractRoutingDataSource`, route query tới đúng database
4. **TenantDataSourceManager** — Quản lý connection pool cho từng tenant
5. **TenantDataSourceLoader** — Load tất cả tenant datasource khi application startup
6. **TenantDatabaseInitializer** — Tạo database + chạy Flyway migration cho tenant mới
7. **TenantProvisioningService** — Orchestrate toàn bộ quá trình tạo tenant mới
8. **RegionContext** — Hỗ trợ routing theo region (multi-region)

### Database Schema

| Database             | Nội dung                                                                                                                   |
| -------------------- | -------------------------------------------------------------------------------------------------------------------------- |
| **tamabee_master**   | companies, users, plans, subscriptions, tenant_databases                                                                   |
| **tenant\_\<slug\>** | employees, departments, contracts, attendance, leaves, shifts, payroll, payslips, adjustments, wallets, holidays, settings |

---

## 🛠 Tech Stack

### Core

| Công nghệ            | Version  | Mục đích                                |
| -------------------- | -------- | --------------------------------------- |
| **Spring Boot**      | 3.5      | Java framework                          |
| **Java**             | 21 (LTS) | Programming language                    |
| **Spring Security**  | 6.x      | Authentication & Authorization          |
| **Spring Data JPA**  | 3.x      | ORM, database access (Hibernate)        |
| **Spring WebSocket** | —        | Real-time notification (STOMP protocol) |
| **Spring AOP**       | —        | Cross-cutting concerns (logging, audit) |
| **Spring Mail**      | —        | Email sending (SMTP)                    |
| **PostgreSQL**       | 16       | Relational database                     |
| **Flyway**           | —        | Database migration (master + tenant)    |
| **Lombok**           | —        | Boilerplate reduction                   |

### Security & Auth

| Công nghệ    | Version | Mục đích                                   |
| ------------ | ------- | ------------------------------------------ |
| **JJWT**     | 0.12    | JWT token generation & validation          |
| **Bucket4j** | 8.x     | Rate limiting (in-memory, không cần Redis) |

### API & Documentation

| Công nghệ             | Version | Mục đích                    |
| --------------------- | ------- | --------------------------- |
| **SpringDoc OpenAPI** | 2.3     | Swagger UI + OpenAPI 3 spec |

### Export & Reporting

| Công nghệ              | Version | Mục đích                                    |
| ---------------------- | ------- | ------------------------------------------- |
| **OpenPDF**            | 2.0     | PDF export (hỗ trợ CJK fonts: 日本語, 中文) |
| **Apache Commons CSV** | 1.12    | CSV export                                  |

### Monitoring & Observability

| Công nghệ           | Mục đích                        |
| ------------------- | ------------------------------- |
| **Spring Actuator** | Health check, metrics endpoints |
| **Micrometer**      | Metrics abstraction             |
| **Prometheus**      | Metrics collection              |
| **Grafana**         | Monitoring dashboard            |
| **Node Exporter**   | VPS system metrics              |

### Code Quality

| Công nghệ      | Version | Mục đích               |
| -------------- | ------- | ---------------------- |
| **Checkstyle** | 10.x    | Code style enforcement |
| **SpotBugs**   | 4.x     | Static bug analysis    |
| **JaCoCo**     | 0.8     | Code coverage report   |

### Testing

| Công nghệ                | Mục đích                   |
| ------------------------ | -------------------------- |
| **JUnit 5**              | Unit & integration testing |
| **Spring Boot Test**     | Spring context testing     |
| **Spring Security Test** | Security testing           |
| **jqwik**                | Property-based testing     |
| **H2**                   | In-memory test database    |

### Infrastructure

| Công nghệ          | Mục đích                                                |
| ------------------ | ------------------------------------------------------- |
| **Docker**         | Multi-stage build (Maven → JRE Alpine)                  |
| **Docker Compose** | Orchestration (API + PostgreSQL + Prometheus + Grafana) |
| **Nginx**          | Reverse proxy, SSL termination                          |

---

## 📁 Cấu trúc dự án

```
api-hr/
├── src/
│   ├── main/
│   │   ├── java/com/tamabee/api_hr/
│   │   │   ├── ApiHrApplication.java          # Main entry point
│   │   │   │
│   │   │   ├── config/                        # ⚙️ Spring configurations
│   │   │   │   # SecurityConfig, WebSocketConfig, CorsConfig,
│   │   │   │   # FlywayMultiTenantConfig, SwaggerConfig, ...
│   │   │   │
│   │   │   ├── datasource/                    # 🏗️ Multi-tenant datasource
│   │   │   │   ├── TenantContext.java          # ThreadLocal tenant holder
│   │   │   │   ├── TenantFilter.java           # HTTP filter → resolve tenant
│   │   │   │   ├── TenantRoutingDataSource.java# AbstractRoutingDataSource
│   │   │   │   ├── TenantDataSourceManager.java# Connection pool management
│   │   │   │   ├── TenantDataSourceLoader.java # Load datasources on startup
│   │   │   │   ├── TenantDatabaseInitializer.java # Create DB + run migration
│   │   │   │   ├── TenantProvisioningService.java # Provision new tenant
│   │   │   │   └── RegionContext.java          # Region-based routing
│   │   │   │
│   │   │   ├── controller/                    # 🌐 REST controllers
│   │   │   ├── service/                       # 💼 Business logic
│   │   │   ├── repository/                    # 🗄️ Spring Data JPA repositories
│   │   │   ├── entity/                        # 📦 JPA entities
│   │   │   ├── dto/                           # 📨 Data Transfer Objects
│   │   │   ├── mapper/                        # 🔄 Entity ↔ DTO mappers
│   │   │   ├── enums/                         # 📋 Enums
│   │   │   ├── exception/                     # ❌ Custom exceptions & handlers
│   │   │   ├── validation/                    # ✅ Custom validators
│   │   │   ├── scheduler/                     # ⏰ Scheduled tasks
│   │   │   ├── util/                          # 🔧 Utility classes
│   │   │   └── constants/                     # 📌 App constants
│   │   │
│   │   └── resources/
│   │       ├── db/
│   │       │   ├── master/                    # Flyway migrations — Master DB
│   │       │   │   ├── V1__init.sql
│   │       │   │   ├── V2__seed_data.sql
│   │       │   │   └── ...
│   │       │   └── tenant/                    # Flyway migrations — Tenant DB
│   │       │       ├── V1__init.sql
│   │       │       ├── V2__seed_data.sql
│   │       │       └── ...
│   │       ├── fonts/                         # Fonts cho PDF export (CJK)
│   │       ├── templates/                     # Email templates
│   │       ├── application.yaml               # Main config
│   │       ├── application-prod.yaml          # Production config
│   │       └── banner.txt                     # Custom startup banner
│   │
│   └── test/                                  # 🧪 Tests
│
├── config/
│   └── checkstyle/
│       └── checkstyle.xml                     # Checkstyle rules
│
├── .githooks/                                 # Git hooks
│   ├── pre-commit                             # Checkstyle + SpotBugs
│   └── commit-msg                             # Conventional commits
│
├── grafana/                                   # Grafana provisioning
│   └── provisioning/
├── prometheus/
│   └── prometheus.yml                         # Prometheus scrape config
├── nginx/                                     # Nginx config templates
│
├── docker-compose.yml                         # Production Docker Compose
├── Dockerfile                                 # Multi-stage build
├── pom.xml                                    # Maven dependencies
├── mvnw / mvnw.cmd                            # Maven wrapper
├── .env.example                               # Dev environment template
└── .env.production.example                    # Production environment template
```

---

## 💻 Yêu cầu hệ thống

### Development

- **Java** 21 (LTS) — khuyến nghị [Eclipse Temurin](https://adoptium.net/)
- **Maven** 3.9+ (hoặc dùng `./mvnw` wrapper đi kèm)
- **PostgreSQL** 16+

### Production

- **Docker** & **Docker Compose**
- **VPS**: Ubuntu 24.04, 8GB RAM, 4 Core (khuyến nghị)

---

## 🚀 Cài đặt & Chạy

### 1. Cấu hình môi trường

```bash
# Copy template biến môi trường
cp .env.example .env

# Sửa .env với thông tin thật
# (database, JWT secret, mail, ...)
```

### 2. Tạo database

```bash
# Tạo Master database
createdb tamabee_master

# Tenant databases sẽ tự động tạo khi có company mới đăng ký
```

### 3. Cài đặt Git hooks

```bash
git config core.hooksPath .githooks
chmod +x .githooks/*
```

### 4. Chạy application

```bash
# Development
./mvnw spring-boot:run

# Hoặc build JAR rồi chạy
./mvnw clean package -DskipTests
java -jar target/api-hr-0.0.1-SNAPSHOT.jar
```

API chạy tại: `http://localhost:8081`

---

## 🔐 Biến môi trường

### Development (`.env`)

```env
# ---- Database ----
DATABASE_URL=jdbc:postgresql://localhost:5432/tamabee_master
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=your_password

# ---- SSH Tunnel (optional, kết nối remote DB) ----
DATABASE_TUNNEL_ENABLED=false
SSH_HOST=your_vps_ip
SSH_PORT=22
SSH_USERNAME=your_ssh_user
SSH_PASSWORD=your_ssh_password
LOCAL_PORT=5433
REMOTE_HOST=localhost
REMOTE_PORT=5432

# ---- JWT ----
JWT_SECRET=YourSecretKeyAtLeast64CharactersLongForSecurity
JWT_ACCESS_TOKEN_EXPIRATION=3600000         # 1 giờ
JWT_REFRESH_TOKEN_EXPIRATION=2592000000     # 30 ngày

# ---- Mail (Gmail App Password) ----
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password

# ---- App ----
ADMIN_EMAIL=admin@example.com
FRONTEND_URL=http://localhost:3000
BASE_URL=http://localhost:8081

# ---- Google Calendar (optional) ----
GOOGLE_CALENDAR_API_KEY=
```

### Production (`.env`)

```env
# ---- PostgreSQL ----
POSTGRES_DB=tamabee_master
POSTGRES_USER=postgres
POSTGRES_PASSWORD=strong_random_password

# ---- JWT ----
JWT_SECRET=random_64_char_secret_key

# ---- Mail ----
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=gmail_app_password

# ---- App ----
ADMIN_EMAIL=admin@example.com
FRONTEND_URL=https://your-domain.com

# ---- Grafana ----
GRAFANA_USER=admin
GRAFANA_PASSWORD=strong_grafana_password
```

---

## 🗄 Database & Migration

### Flyway Multi-Tenant

Flyway được cấu hình custom (không dùng auto-run của Spring Boot) để hỗ trợ multi-tenant:

```
src/main/resources/db/
├── master/          # Migrations cho Master DB
│   ├── V1__init.sql
│   └── V2__seed_data.sql
└── tenant/          # Migrations cho mỗi Tenant DB
    ├── V1__init.sql
    └── V2__seed_data.sql
```

- **Master DB**: Chạy migration từ `db/master/` khi application startup
- **Tenant DB**: Chạy migration từ `db/tenant/` khi tạo tenant mới hoặc khi startup (cho tất cả tenant đã tồn tại)
- `spring.flyway.enabled=false` — Disable auto-run, `FlywayMultiTenantConfig` quản lý thủ công
- `spring.jpa.hibernate.ddl-auto=none` — Flyway quản lý schema, không dùng Hibernate auto DDL

### Tạo migration mới

```sql
-- Master DB: src/main/resources/db/master/V{n}__description.sql
-- Tenant DB: src/main/resources/db/tenant/V{n}__description.sql
```

---

## 📖 API Documentation

Swagger UI tự động generate từ code:

```
http://localhost:8081/swagger-ui.html
```

OpenAPI 3 JSON spec:

```
http://localhost:8081/v3/api-docs
```

---

## 🔍 Code Quality & Pre-commit

### Pre-commit Hook

Khi commit file `.java`, hook tự động chạy:

```
🔍 Pre-commit checks (api-hr)...

[1/2] Checkstyle...    → Kiểm tra coding style
✅ Checkstyle OK

[2/2] SpotBugs...      → Phát hiện bug tiềm ẩn
✅ SpotBugs OK

🎉 All checks passed!
```

> Nếu không có file `.java` nào được staged, hook sẽ skip.

### Setup Git Hooks

```bash
# Cấu hình git sử dụng .githooks/ thay vì .git/hooks/
git config core.hooksPath .githooks
chmod +x .githooks/*
```

### Code Quality Tools

| Tool           | Mục đích                                      | Chạy thủ công                           |
| -------------- | --------------------------------------------- | --------------------------------------- |
| **Checkstyle** | Coding style (indentation, naming, imports)   | `./mvnw checkstyle:check`               |
| **SpotBugs**   | Static analysis (null pointer, resource leak) | `./mvnw spotbugs:check`                 |
| **JaCoCo**     | Code coverage report                          | `./mvnw verify` → `target/site/jacoco/` |

### Maven Lifecycle

Checkstyle chạy ở phase `validate`, SpotBugs + JaCoCo chạy ở phase `verify`:

```bash
./mvnw validate    # Checkstyle only
./mvnw verify      # Checkstyle + compile + test + SpotBugs + JaCoCo
```

---

## 📝 Conventional Commits

Commit message được validate bởi `commit-msg` hook.

**Format:**

```
type(scope): message
```

**Types:**

| Type       | Mô tả                              |
| ---------- | ---------------------------------- |
| `feat`     | Tính năng mới                      |
| `fix`      | Sửa bug                            |
| `docs`     | Documentation                      |
| `style`    | Format code (không thay đổi logic) |
| `refactor` | Refactor                           |
| `test`     | Thêm/sửa test                      |
| `chore`    | Maintenance, dependencies          |
| `perf`     | Performance                        |
| `ci`       | CI/CD                              |
| `build`    | Build system                       |

**Ví dụ:**

```bash
git commit -m "feat(auth): add JWT refresh token rotation"
git commit -m "fix(payroll): correct overtime calculation"
git commit -m "docs: update API documentation"
git commit -m "chore: upgrade Spring Boot to 3.5"
```

---

## 🧪 Testing

```bash
./mvnw test                        # Chạy tất cả tests
./mvnw test -Dtest=AuthServiceTest # Chạy test cụ thể
./mvnw test -Dtest="*PayrollTest"  # Chạy tests theo pattern
./mvnw verify                      # Tests + code quality + coverage
```

| Tool                     | Mục đích                                                   |
| ------------------------ | ---------------------------------------------------------- |
| **JUnit 5**              | Unit & integration testing                                 |
| **Spring Boot Test**     | Spring context, `@SpringBootTest`                          |
| **Spring Security Test** | `@WithMockUser`, security testing                          |
| **jqwik**                | Property-based testing                                     |
| **H2**                   | In-memory database cho test                                |
| **JaCoCo**               | Coverage → HTML report tại `target/site/jacoco/index.html` |

---

## 🐳 Deployment

### Docker Compose (Production)

```bash
# Cấu hình biến môi trường
cp .env.production.example .env
nano .env  # Điền giá trị thật

# Build và khởi động tất cả services
docker compose up -d --build

# Xem logs
docker compose logs -f api-hr

# Restart
docker compose restart api-hr

# Tắt
docker compose down
```

### Services

| Service         | Port | RAM   | Mô tả                         |
| --------------- | ---- | ----- | ----------------------------- |
| `api-hr`        | 8081 | 3GB   | Spring Boot API               |
| `postgres`      | 5432 | 2GB   | PostgreSQL 16 (chỉ localhost) |
| `prometheus`    | 9091 | 512MB | Metrics collector             |
| `grafana`       | 3001 | 512MB | Monitoring dashboard          |
| `node-exporter` | —    | 128MB | VPS system metrics            |

> Tất cả ports chỉ expose `127.0.0.1` (localhost). Nginx trên VPS làm reverse proxy ra ngoài.

### Dockerfile — Multi-stage Build

```
Stage 1 (builder):  maven:3.9-eclipse-temurin-21
  → Cache dependencies (Docker layer caching)
  → Build JAR (skip tests + checkstyle)

Stage 2 (runtime):  eclipse-temurin:21-jre-alpine
  → Non-root user (tamabee)
  → G1GC, 75% MaxRAMPercentage
  → Health check via Actuator
```

### JVM Tuning

```
-XX:+UseG1GC                    # G1 Garbage Collector
-XX:MaxRAMPercentage=75.0       # 75% container RAM limit
-XX:+UseStringDeduplication     # Giảm memory cho duplicate strings
-Djava.security.egd=file:/dev/./urandom  # Faster random
-Dfile.encoding=UTF-8           # UTF-8 encoding
```

---

## 📊 Monitoring

### Stack: Prometheus + Grafana

```
Spring Actuator ──▶ Prometheus ──▶ Grafana
     :9090/actuator      :9091         :3001
```

- **Spring Actuator** expose metrics tại `/actuator/prometheus`
- **Prometheus** scrape mỗi 15s, lưu data 30 ngày (max 5GB)
- **Grafana** hiển thị dashboard

### Metrics có sẵn

- JVM: heap memory, GC pauses, threads, class loading
- HTTP: request count, latency (p50, p95, p99), error rate
- Database: connection pool (active, idle, pending)
- System: CPU, RAM, disk (qua Node Exporter)

### Truy cập Grafana (qua SSH tunnel)

```bash
ssh -L 3001:localhost:3001 user@your-vps-ip
# Mở browser: http://localhost:3001
# Login: admin / (password trong .env)
```

### Health Check

```bash
curl http://localhost:8081/actuator/health
# {"status":"UP"}
```

---

## 📄 License

Private — © 2025 Tamabee. All rights reserved.
