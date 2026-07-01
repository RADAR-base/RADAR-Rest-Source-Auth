# RADAR-Rest-Source-Auth — Architecture

## Overview

Kotlin/JAX-RS via Jersey backend that manages OAuth consent and token lifecycle for third-party wearable/health APIs. Stores tokens in PostgreSQL; uses Redis for distributed locking during concurrent refresh.

---

## Directory Structure

```
RADAR-Rest-Source-Auth/
├── authorizer-app-backend/          # Main application module
│   ├── src/main/java/org/radarbase/authorizer/
│   │   ├── Main.kt                  # Entry point — loads config, starts Grizzly/Jersey
│   │   ├── api/                     # Request/response DTOs
│   │   ├── config/                  # Configuration data classes
│   │   ├── doa/                     # Hibernate repositories + JPA entities
│   │   ├── enhancer/                # HK2 DI binders and Jersey setup
│   │   ├── lifecycle/               # Startup/shutdown hooks (registration cleanup)
│   │   ├── resources/               # JAX-RS REST endpoints (controllers)
│   │   ├── service/                 # Business logic and auth implementations
│   │   └── util/                    # HMAC-256, OAuth1 signing utilities
│   ├── src/main/resources/db/       # Liquibase migrations
│   └── authorizer.yml               # Runtime configuration template
├── buildSrc/                        # Custom Gradle plugins
├── docker/                          # Docker configs and compose files
└── docs/                            # Architecture and reference docs
```

---

## Key Packages

| Package | Purpose |
|---------|---------|
| `api` | DTOs: `RestOauth2AccessToken`, `RequestTokenPayload`, `RestSourceUserDTO`, etc. |
| `config` | `AuthorizerConfig` (root), `AuthorizerServiceConfig`, `RestSourceClient`, `RedisConfig` |
| `doa` | Repositories (`RestSourceUserRepository`, `RegistrationRepository`) and entities |
| `doa.entity` | `RestSourceUser`, `RegistrationState` — JPA entities backed by PostgreSQL |
| `enhancer` | `AuthorizerResourceEnhancer` — HK2 bindings for all services |
| `resources` | `RegistrationResource`, `RestSourceUserResource`, `SourceClientResource`, `ProjectResource` |
| `service` | Authorization services (OAuth2/OAuth1/Huawei/Garmin/Oura), user and client services |

---

## REST API

### `/registrations` — OAuth flow initiation

| Method | Path | Action |
|--------|------|--------|
| POST | `/registrations` | Create ephemeral state token for a user |
| GET | `/registrations/{token}` | Fetch registration details |
| POST | `/registrations/{token}` | Get OAuth authorize URL (validates HMAC secret) |
| POST | `/registrations/{token}/authorize` | Exchange auth code for tokens |
| DELETE | `/registrations/{token}` | Cancel registration |

### `/users` — User account management

| Method | Path | Action |
|--------|------|--------|
| GET/POST | `/users` | List / create users |
| GET/POST/DELETE | `/users/{id}` | Get / update / delete user |
| POST | `/users/{id}/reset` | Reset authorization |
| GET/POST | `/users/{id}/token` | Check / refresh token |

### `/source-clients` — OAuth client configuration

| Method | Path | Action |
|--------|------|--------|
| GET | `/source-clients` | List all configured sources |
| GET | `/source-clients/{type}` | Get config for a source type |
| POST | `/source-clients/{type}/deregister` | Webhook for provider-initiated deregistration |

---

## Authorization Service Architecture

All authorization implementations share the `RestSourceAuthorizationService` interface. `DelegatedRestSourceAuthorizationService` routes calls to the named implementation by sourceType.

```
RestSourceAuthorizationService (interface)
├── OAuth2RestSourceAuthorizationService     → FitBit (default OAuth2 + Basic Auth)
│   ├── OuraAuthorizationService             → Oura (+ custom user ID fetch, custom revoke)
│   └── HuaweiAuthorizationService           → Huawei (+ form-param auth, JWT id_token parsing)
└── OAuth1RestSourceAuthorizationService
    └── GarminSourceAuthorizationService     → Garmin (+ user ID API call, deregistration scheduler)
```

### Adding a New Source

1. Create `XyzAuthorizationService` extending the appropriate base.
2. Add `const val XYZ_AUTH = "Xyz"` to `DelegatedRestSourceAuthorizationService.Companion`.
3. Bind in `AuthorizerResourceEnhancer.enhance()` with `.named(XYZ_AUTH)`.
4. Add the source client block to `authorizer.yml`.

---

## Database

### Tables

**`rest_source_user`** — One row per authorized user per source type.

| Column | Type | Notes |
|--------|------|-------|
| `id` | bigint PK | |
| `project_id`, `user_id` | varchar | RADAR identifiers |
| `source_id` | UUID | Kafka record key (unique) |
| `source_type` | varchar | "FitBit", "Garmin", "Oura", "Huawei", … |
| `external_user_id` | varchar | Provider's user ID |
| `authorized` | boolean | Current auth status |
| `access_token` | varchar(2000) | |
| `refresh_token` | varchar(2000) | |
| `expires_at` | timestamp | Computed from `expires_in` |
| `start_date`, `end_date` | timestamp | Data collection window |
| `version`, `times_reset` | int | Reset tracking |

**`registration`** — Short-lived state tokens for the OAuth flow.

| Column | Type | Notes |
|--------|------|-------|
| `token` | varchar PK | State param in OAuth URL |
| `user_id` | FK → rest_source_user | |
| `salt`, `secret_hash` | bytea | HMAC-256 for persistent tokens |
| `created_at`, `expires_at` | timestamp | TTL |
| `persistent` | boolean | Long-lived vs ephemeral |

Migrations managed by Liquibase under `src/main/resources/db/changelog/`.

---

## Configuration

`authorizer.yml` (parsed into `AuthorizerConfig`):

```yaml
service:
  baseUri: http://0.0.0.0:8085/rest-sources/backend/
  advertisedBaseUri: http://example.org/rest-sources/backend/
  # callbackUrl derived from advertisedBaseUri or frontendBaseUri

auth:
  managementPortalUrl: https://...
  clientId: radar_rest_sources_auth
  clientSecret: <secret>

database:
  driver: org.postgresql.Driver
  url: jdbc:postgresql://localhost:5432/managementportal
  user: radar
  password: radar_test

redis:
  uri: redis://localhost:6379
  lockPrefix: radar-rest-sources-backend/lock

restSourceClients:
  - sourceType: FitBit
    ...
```

Secrets can be overridden via env vars: `{SOURCETYPE}_CLIENT_ID`, `{SOURCETYPE}_CLIENT_SECRET`.

---

## Dependency Injection

Framework: HK2 (Jersey's DI). All bindings in `AuthorizerResourceEnhancer.enhance()`.

- Services bound as singletons.
- `DelegatedRestSourceAuthorizationService` receives an `IterableProvider<RestSourceAuthorizationService>` and dispatches by the HK2 named binding that matches the sourceType string.

---

## Key Dependencies

| Library | Purpose |
|---------|---------|
| radar-jersey | JAX-RS + Jersey + Hibernate integration |
| ktor-client | Async HTTP for token exchange calls |
| kotlinx.serialization | JSON (de)serialization for API responses |
| postgresql / Hibernate | ORM + DB |
| Jedis | Redis client for distributed token-refresh locking |
| Liquibase | DB migrations |
