# Condominio Web

REST API for condominium management built with Spring Boot 3.5 / Java 21.

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 21 (Virtual Threads / Project Loom) |
| Framework | Spring Boot 3.5.14 |
| Server | Undertow (replaces Tomcat) |
| Persistence | Spring Data JPA + Hibernate + PostgreSQL 16 |
| Migrations | Liquibase |
| Cache | Two-level: L1 Caffeine (in-process) + L2 Redis 7 |
| Security | Spring Security + JWT (RS256) |
| Mapping | MapStruct 1.6 |
| Metrics | Micrometer + Prometheus |
| Build | Maven 3.9 |

## Prerequisites

- Java 21+
- Maven 3.9+
- Docker (required for integration tests and BDD tests via Testcontainers)
- PostgreSQL 16 (for running the application locally)
- Redis 7 (for running the application locally)

## Quick Start

### 1. Clone and build

```bash
git clone <repo-url>
cd condominio-web
mvn verify
```

### 2. Configure environment variables

Copy the defaults or export before running:

| Variable | Default | Description |
|---|---|---|
| `DB_USERNAME` | `postgres` | PostgreSQL username |
| `DB_PASSWORD` | `postgres` | PostgreSQL password |
| `REDIS_HOST` | `localhost` | Redis hostname |
| `REDIS_PORT` | `6379` | Redis port |
| `JWT_SECRET` | *(dev key)* | Base64-encoded RS256 secret — **replace in production** |
| `JWT_ISSUER` | `http://localhost:8080` | Token issuer claim |
| `JWT_ACCESS_EXPIRATION` | `3600` | Access token TTL (seconds) |
| `JWT_REFRESH_EXPIRATION` | `86400` | Refresh token TTL (seconds) |
| `MAIL_HOST` | `localhost` | SMTP host |
| `MAIL_PORT` | `587` | SMTP port |
| `SERVER_PORT` | `8080` | HTTP port |

### 3. Run

```bash
mvn spring-boot:run
```

The API is available at `http://localhost:8080/api`.

### 4. Docker

```bash
docker build -t condominio-web .
docker run -p 8080:8080 \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=postgres \
  -e REDIS_HOST=redis \
  condominio-web
```

## Running Tests

```bash
# Unit + integration tests (requires Docker for Testcontainers)
mvn test

# Full verification with coverage (80% minimum)
mvn verify

# End-to-end BDD tests (full stack, no mocks)
mvn verify -Pe2e
```

## Project Structure

```
src/main/java/com/pmrodrigues/
├── CondominiApplication.java
├── commons/
│   ├── cache/          # Two-level cache (Caffeine + Redis)
│   ├── config/         # JPA auditing, cache, metrics, web config
│   ├── controller/     # EstadoController
│   ├── dto/            # Shared DTOs (ApiResponse, EnderecoDTO, EstadoDTO…)
│   ├── embeddable/     # Endereco JPA embeddable
│   ├── filter/         # MDC context filter
│   ├── interceptor/    # Request-ID interceptor
│   ├── mapper/         # EstadoMapper, EnderecoMapper
│   ├── model/          # Estado entity
│   ├── repository/     # EstadoRepository
│   ├── service/        # EstadoService, MailService, MeterService
│   ├── tenant/         # TenantContext (ThreadLocal condominioId)
│   └── util/           # Exceptions helper
├── condominio/
│   ├── config/         # TenantFilterAspect
│   ├── controller/     # ApartamentoController, BlocoController, CondominioController
│   ├── dto/            # Domain DTOs
│   ├── mapper/         # MapStruct mappers
│   ├── model/          # Apartamento, Bloco, Condominio entities
│   ├── repository/     # JPA repositories
│   ├── service/        # Application services
│   └── specification/  # JPA Specification predicates
└── security/
    ├── config/         # SecurityConfig, JwtConfig, AuthorizationServerConfig
    ├── controller/     # AuthController, UserController, AuthExceptionHandler
    ├── dto/            # Auth DTOs
    ├── filter/         # CustomBearerTokenFilter
    ├── mapper/         # UserMapper
    ├── model/          # User entity
    ├── repository/     # UserRepository
    └── service/        # JwtService, TokenBlacklistService, UserService…
```

## Architecture Decisions

### Multi-tenancy
Tables `blocos` and `apartamentos` are HASH-partitioned by `condominio_id` (4 buckets). Every entity has a `condominio_id` FK set automatically via `@PrePersist` from `TenantContext`. A Hibernate `@Filter` activated by `TenantFilterAspect` restricts all queries to the current tenant without requiring caller changes.

### Two-level Cache
- **L1 (Caffeine)**: In-process, sub-millisecond. TTL 5–10 min, evicted on write.
- **L2 (Redis)**: Shared across instances. TTL 30 min–1 h. Keys are prefixed with `condominioId` for tenant isolation.
- Cache keys include `TenantContext.getCondominioId()` so tenants never see each other's cached data.

### JWT Authentication
- Access tokens (RS256, 1 h TTL) carry `roles` and `condominioId` claims.
- Refresh tokens are opaque UUIDs stored in Redis; rotation on every refresh.
- Blacklisted JTIs are stored in Redis; `CustomBearerTokenFilter` rejects blacklisted tokens before Spring Security processes them.

### Soft Delete
All domain entities (`Apartamento`, `Bloco`, `Condominio`, `User`) use `@SQLDelete` + `@SQLRestriction` to mark rows as `deleted = true` instead of physically removing them.

### Observability
- All service methods carry `@Timed` (Micrometer). Metrics are exported via `/api/actuator/prometheus`.
- Structured logging with SLF4J + Logback; every request carries a `correlationId` and `requestId` in MDC, visible in log lines.
- Actuator endpoints: `health`, `info`, `metrics`, `caches`, `prometheus`.

## API Endpoints

All endpoints require a `Bearer` JWT unless noted. ADMIN role required for `POST`, `PUT`, `DELETE`.

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/auth/login` | Authenticate; returns access + refresh tokens |
| `POST` | `/api/auth/refresh` | Rotate refresh token; returns new tokens |
| `POST` | `/api/auth/logout` | Blacklist current access token |
| `GET` | `/api/estados` | List estados (optionally filter by `uf` or `nome`) |
| `GET` | `/api/estados/{id}` | Get estado by ID |
| `GET` | `/api/condominios` | List condominios (filter: `nome`, `cnpj`) |
| `GET/POST` | `/api/condominios` | List / create condominios |
| `GET/PUT/DELETE` | `/api/condominios/{id}` | Get / update / delete condominio |
| `GET/POST` | `/api/blocos` | List / create blocos |
| `GET/PUT/DELETE` | `/api/blocos/{id}` | Get / update / delete bloco |
| `GET/POST` | `/api/apartamentos` | List / create apartamentos |
| `GET/PUT/DELETE` | `/api/apartamentos/{id}` | Get / update / delete apartamento |
| `GET/POST` | `/api/users` | List / create users |
| `GET/PUT/DELETE` | `/api/users/{id}` | Get / update / delete user |

All responses follow the `ApiResponse<T>` envelope:

```json
{
  "request_id": "uuid",
  "timestamp": "2026-05-26T14:00:00Z",
  "data": { ... }
}
```

## Database Migrations

Migrations live in `src/main/resources/db/changelog/changes/` and are registered in `db.changelog-master.yaml`. Liquibase runs automatically on startup.

To add a migration: create `NNNN-description.sql` and add an entry to the master YAML.
