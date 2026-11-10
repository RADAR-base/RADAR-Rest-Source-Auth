# RADAR-Rest-Source-Auth — Architecture

## Overview

RADAR-Rest-Source-Auth lets a RADAR-base participant grant (OAuth) consent so that a third-party
wearable/health API (FitBit, Garmin, Oura, Huawei Health, Google Health, …) can be polled by a
[RADAR REST connector](https://github.com/RADAR-base/RADAR-REST-Connector). It has two independently
deployed halves:

- **`authorizer-app-backend`** — a Kotlin/JAX-RS (Jersey) service that drives the OAuth1/OAuth2
  dance, stores tokens in PostgreSQL, and exposes a REST API for connectors and the admin UI.
- **`authorizer-app`** — an Angular 15 single-page app: the participant-facing consent flow plus an
  admin dashboard for managing registered users.

Both authenticate against **ManagementPortal** (RADAR-base's identity/project service), which is the
source of truth for projects and subjects. Redis is used only for distributed locking during
concurrent token refresh; Liquibase manages schema migrations.

```
Participant browser ──▶ authorizer-app (Angular)  ──▶ authorizer-app-backend (Jersey) ──▶ 3rd-party OAuth provider
                                │                              │
                                └──────────── OAuth2 ──────────┴──▶ ManagementPortal (auth + project/subject data)
                                                                     │
                                                              PostgreSQL + Redis
```

---

## Repository Layout

```
RADAR-Rest-Source-Auth/
├── authorizer-app-backend/          # Gradle module — the Kotlin backend (only Gradle subproject)
│   ├── src/main/java/org/radarbase/authorizer/
│   │   ├── Main.kt                  # Entry point — loads authorizer.yml, starts Grizzly/Jersey
│   │   ├── api/                     # Request/response DTOs + mappers
│   │   ├── config/                  # Configuration data classes
│   │   ├── doa/                     # Repositories + JPA entities (Hibernate)
│   │   ├── enhancer/                # HK2 DI bindings, Jersey resource-config wiring
│   │   ├── lifecycle/               # Scheduled background jobs (stale-registration cleanup, MP user sync)
│   │   ├── resources/                # JAX-RS REST endpoints (controllers)
│   │   ├── service/                 # Business logic + per-provider OAuth implementations
│   │   └── util/                    # HMAC-256, OAuth1 signing, PKCE helpers
│   ├── src/main/resources/db/       # Liquibase changelogs
│   ├── src/test/                    # JUnit5 tests
│   └── authorizer.yml               # Runtime configuration template
├── authorizer-app/                  # Angular 15 frontend (separate yarn/npm project, not in Gradle build)
│   ├── src/app/
│   │   ├── auth/                    # Login (OAuth2 login-page) — services, models, routing
│   │   ├── admin/                   # Admin dashboard: users list, user dialog, containers, pipes, models
│   │   └── shared/                  # Toolbar, message-box, authorization-page/-complete-page, shared services
│   └── src/assets/i18n/             # Translations (admin, auth, shared)
├── docker/                          # docker-compose env files (authorizer.yml.template, MP oauth clients)
├── docker-compose.yml               # Full local stack: MP + both Postgres DBs + backend + frontend + traefik + redis
├── gradle/                          # Gradle wrapper + version catalog (libs.versions.toml)
├── settings.gradle.kts              # Gradle root — only includes authorizer-app-backend
├── build.gradle.kts                 # Applies radar-kotlin/radar-root-project convention plugins
└── docs/                            # This file + registration-api.md
```

Note: there is no `buildSrc`; Gradle conventions (Kotlin compilation, Sentry, dependency management)
come from the external `org.radarbase.radar-*` plugins declared in `gradle/libs.versions.toml`.

---

## Backend: Key Packages

| Package | Purpose |
|---------|---------|
| `api` | DTOs (`RestOauth2AccessToken`, `RequestTokenPayload`, `RestSourceUserDTO`, `RegistrationResponse`, `ShareableClientDetail`, `RestGoogleHealthIdentity`, …) and mappers (`RestSourceUserMapper`, `RestSourceClientMapper`) |
| `config` | `AuthorizerConfig` (root), `AuthorizerServiceConfig`, `AuthConfig`, `RestSourceClient(s)`, `RedisConfig`, `OAuthVersion` |
| `doa` | `RestSourceUserRepository` (+ `Impl`), `RegistrationRepository` |
| `doa.entity` | `RestSourceUser`, `RegistrationState` — JPA entities backed by PostgreSQL |
| `enhancer` | `AuthorizerResourceEnhancer` (HK2 bindings), `ManagementPortalEnhancerFactory`/`ManagementPortalResourceEnhancer` (radar-jersey MP integration), `JedisResourceEnhancer` (Redis client binding) |
| `lifecycle` | `RegistrationLifecycleManager` (expires/cleans stale registration tokens), `UserSyncLifecycleManager` (periodically removes users deleted from ManagementPortal) |
| `resources` | `RegistrationResource`, `RestSourceUserResource`, `SourceClientResource`, `ProjectResource` |
| `service` | Per-provider authorization services, `RegistrationService`, `RestSourceUserService`, `RestSourceClientService`, `LockService`/`RedisLockService`, `MPClientFactory` |
| `util` | `Hmac256Secret` (registration secret hashing), `OauthSignature` (OAuth1 request signing), `PkceUtil` (PKCE code verifier/challenge) |

---

## REST API

### `/registrations` — OAuth flow initiation

| Method | Path | Auth | Action |
|--------|------|------|--------|
| POST | `/registrations` | `SUBJECT_UPDATE` | Create a state token for a user; if not persistent, also returns the provider authorize URL |
| GET | `/registrations/{token}` | none | Fetch minimal registration details (used by the frontend to bootstrap the consent page) |
| POST | `/registrations/{token}` | HMAC secret in body | Validate the persistent-token secret and return the provider authorize URL |
| POST | `/registrations/{token}/authorize` | none | Exchange the provider's auth code for tokens, persist the user, delete the registration |
| DELETE | `/registrations/{token}` | `SUBJECT_UPDATE` | Cancel/delete a registration |

### `/users` — User (rest-source authorization) management

| Method | Path | Permission | Action |
|--------|------|------------|--------|
| GET | `/users` | `SUBJECT_READ` | Paginated query by project/source-type/search/authorized |
| POST | `/users` | `SUBJECT_UPDATE` | Create a user record |
| POST | `/users/{id}` | `SUBJECT_UPDATE` | Update a user |
| GET | `/users/{id}` | `SUBJECT_READ` | Get a user (cached 300s) |
| DELETE | `/users/{id}` | `SUBJECT_UPDATE` | Delete a user |
| POST | `/users/{id}/reset` | `SUBJECT_UPDATE` | Reset authorization (bumps `version`/`timesReset`) |
| GET | `/users/{id}/token` | `MEASUREMENT_CREATE` | Ensure/return a valid access token (refreshes if needed) |
| POST | `/users/{id}/token` | `MEASUREMENT_CREATE` | Force-refresh the token |
| POST | `/users/{id}/token/sign` | `MEASUREMENT_READ` | Sign an outgoing request (OAuth1 providers) |

### `/source-clients` — OAuth client configuration

| Method | Path | Auth | Action |
|--------|------|------|--------|
| GET | `/source-clients` | `SOURCETYPE_READ` | List all configured sources (cached 1h) |
| GET | `/source-clients/{type}` | `SOURCETYPE_READ` | Get config for one source type |
| DELETE | `/source-clients/{type}/authorization/{serviceUserId}` | `SUBJECT_UPDATE` | Revoke + deregister by provider user ID (used by the connector) |
| GET | `/source-clients/{type}/authorization/{serviceUserId}` | `MEASUREMENT_READ` | Look up the RADAR user by provider user ID |
| POST | `/source-clients/{type}/deregister` | none | Webhook: provider-initiated deregistration (validates provided access token before deleting) |

### `/projects` — ManagementPortal passthrough (read-only, cached)

`GET /projects`, `GET /projects/{projectId}`, `GET /projects/{projectId}/users` — thin proxies over
`RadarProjectService` used by the admin frontend to populate project/subject pickers.

See `docs/registration-api.md` for the end-to-end registration/consent sequence.

---

## Authorization Service Architecture

All provider integrations implement `RestSourceAuthorizationService`. Jersey binds one instance per
`sourceType` (HK2 named binding); `DelegatedRestSourceAuthorizationService` is bound as the
unqualified implementation and routes every call to the named instance matching `sourceType`.

```
RestSourceAuthorizationService (interface)
├── DelegatedRestSourceAuthorizationService        → router, injected wherever the interface is required
├── OAuth2RestSourceAuthorizationService (base)     → FitBit ("FitBit", default OAuth2 + Basic Auth)
│   ├── OuraAuthorizationService                    → Oura (+ custom user-ID fetch, custom revoke)
│   ├── HuaweiAuthorizationService                  → Huawei (+ form-param auth, JWT id_token parsing)
│   ├── GoogleHealthAuthorizationService             → GoogleHealth (+ PKCE, identity fetch, cascades
│   │                                                   deregistration of any legacy FitBit auth for the
│   │                                                   same participant on successful consent)
│   └── GarminOAuth2AuthorizationService             → Garmin, when `oauthVersion: OAUTH2` (+ PKCE)
└── OAuth1RestSourceAuthorizationService (base)
    └── GarminOauth1AuthorizationService             → Garmin, when `oauthVersion` is unset/OAUTH1 (default;
                                                          + user-ID API call, deregistration scheduler)
```

`AuthorizerResourceEnhancer` picks the Garmin implementation at startup based on the `oauthVersion`
field of the configured Garmin `RestSourceClient` — only one is bound.

### Adding a New Source

1. Create `XyzAuthorizationService` extending `OAuth2RestSourceAuthorizationService` or
   `OAuth1RestSourceAuthorizationService` (or implement `RestSourceAuthorizationService` directly for a
   fully custom flow, as `GarminOauth1AuthorizationService` does).
2. Add `const val XYZ_AUTH = "Xyz"` to `DelegatedRestSourceAuthorizationService.Companion`.
3. Bind it in `AuthorizerResourceEnhancer.enhance()` with `.named(XYZ_AUTH)`.
4. Add the source client block to `authorizer.yml` (`restSourceClients`), including
   `authorizationEndpoint`, `tokenEndpoint`, `clientId`/`clientSecret` (or rely on
   `{SOURCETYPE}_CLIENT_*` env vars).
5. If the provider requires PKCE, add the source type to `RestSourceClient.usesPkce`.

---

## Database

### Tables

**`rest_source_user`** — one row per authorized user per source type.

| Column | Type | Notes |
|--------|------|-------|
| `id` | bigint PK | sequence-generated |
| `project_id`, `user_id` | varchar | RADAR identifiers (ManagementPortal project/subject) |
| `source_id` | varchar (UUID) | Kafka record key |
| `source_type` | varchar | "FitBit", "Garmin", "Oura", "Huawei", "GoogleHealth", … |
| `external_user_id` | varchar | provider's user ID; unique together with `source_type` |
| `authorized` | boolean | current auth status |
| `access_token`, `refresh_token` | varchar(2000) | |
| `expires_in`, `expires_at` | int / timestamp | `expires_at` computed from `expires_in` |
| `token_type` | varchar | |
| `start_date`, `end_date` | timestamp | data collection window |
| `version`, `times_reset` | varchar / bigint | reset tracking |
| `created_at` | timestamp | |

**`registration`** — short-lived state tokens for the OAuth flow.

| Column | Type | Notes |
|--------|------|-------|
| `token` | varchar PK | state param in the OAuth authorize URL |
| `user_id` | FK → `rest_source_user` | |
| `salt`, `secret_hash` | bytea, nullable | HMAC-256 for persistent (long-lived) tokens |
| `code_verifier` | varchar, nullable | PKCE verifier, for providers where `usesPkce == true` |
| `created_at`, `expires_at` | timestamp | TTL enforced by `RegistrationLifecycleManager` |
| `persistent` | boolean | long-lived (secret-protected) vs ephemeral single-use |

Migrations live under `authorizer-app-backend/src/main/resources/db/changelog/changes/`, wired
through `db.changelog-master.xml` and run automatically at startup (via radar-jersey/Liquibase).

---

## Configuration

`authorizer.yml` (parsed into `AuthorizerConfig`):

```yaml
service:
  baseUri: http://0.0.0.0:8085/rest-sources/backend/
  advertisedBaseUri: http://example.org/rest-sources/backend/
  frontendBaseUri: http://example.org/rest-sources/authorizer/   # optional; else derived from advertisedBaseUri
  # callbackUrl = {frontendBaseUri|derived}/users:new
  enableCors: false
  syncProjectsIntervalMin: 30
  syncParticipantsIntervalMin: 1440   # UserSyncLifecycleManager cadence
  tokenExpiryTimeInMinutes: 15        # ephemeral registration TTL
  persistentTokenExpiryInMin: 4320    # 3 days

auth:
  managementPortalUrl: https://...
  clientId: radar_rest_sources_auth_backend
  clientSecret: <secret>

database:
  driver: org.postgresql.Driver
  url: jdbc:postgresql://localhost:5432/restsourceauthorizer
  user: radarcns
  password: radarcns

redis:
  uri: redis://localhost:6379
  lockPrefix: radar-rest-sources-backend/lock

restSourceClients:
  - sourceType: FitBit
    authorizationEndpoint: https://www.fitbit.com/oauth2/authorize
    tokenEndpoint: https://api.fitbit.com/oauth2/token
    clientId: ...
    clientSecret: ...
  - sourceType: Garmin
    oauthVersion: OAUTH1   # or OAUTH2, selects which service implementation gets bound
    ...
```

Secrets/endpoints can be overridden per source via env vars: `{SOURCETYPE}_CLIENT_ID`,
`{SOURCETYPE}_CLIENT_SECRET`, `{SOURCETYPE}_CLIENT_AUTH_URL`, `{SOURCETYPE}_CLIENT_TOKEN_URL`.

---

## Background Jobs (`lifecycle`)

Both run as Jersey `ApplicationEventListener`s started on `INITIALIZATION_APP_FINISHED` and cancelled
on `DESTROY_FINISHED`, using Jersey's `@BackgroundScheduler` executor:

- **`RegistrationLifecycleManager`** — periodically deletes expired rows from `registration`
  (interval = 4× `tokenExpiryTimeInMinutes`, first run delayed by one interval to let Liquibase finish).
- **`UserSyncLifecycleManager`** — every `syncParticipantsIntervalMin`, lists all `rest_source_user`
  rows, groups by project, fetches current subjects from ManagementPortal, and deletes any local user
  whose subject no longer exists in MP for that project.

---

## Dependency Injection

Framework: HK2 (Jersey's built-in DI). All bindings live in `AuthorizerResourceEnhancer.enhance()`
(plus `ManagementPortalResourceEnhancer`/`JedisResourceEnhancer` for MP client + Redis wiring, pulled
in via radar-jersey).

- Most services/repositories are bound as singletons.
- `DelegatedRestSourceAuthorizationService` receives an `IterableProvider<RestSourceAuthorizationService>`
  and looks up the HK2-named binding matching `sourceType` at call time — this is how new providers
  plug in without touching the router.

---

## Frontend (`authorizer-app`)

Angular 15 SPA, module-per-feature:

- **`auth/`** — login page; exchanges credentials for a ManagementPortal OAuth2 token (`@auth0/angular-jwt`
  for JWT handling) via `auth/services`.
- **`admin/`** — dashboard for staff: users list (`admin/components/users-list`), user create/edit dialog
  (`admin/containers/user-dialog`), backed by `admin/services` calling the backend `/users`,
  `/source-clients`, `/projects` endpoints.
- **`shared/`** — toolbar, message-box, and the actual **participant consent flow**
  (`shared/containers/authorization-page`, `authorization-complete-page`) that a participant lands on
  after following a registration link; talks to `/registrations/{token}` and
  `/registrations/{token}/authorize`.
- `assets/i18n/{admin,auth,shared}` — `ngx-translate` translation files.

Built independently of the Gradle build (`yarn`/`npm` + `ng build`); see `authorizer-app/README.md`
and `authorizer-app/Dockerfile`. `BASE_HREF`, `BACKEND_BASE_URL`, and `AUTH_*` env vars configure the
built app at container start (see `docker-compose.yml`).

---

## Local Development Stack (`docker-compose.yml`)

Services, fronted by Traefik on `:8080`:

| Service | Role |
|---------|------|
| `managementportal` + `mp-postgresql` | Identity/project/subject provider |
| `rest-auth-postgresql` | Backend's own Postgres DB (`restsourceauthorizer`) |
| `radar-rest-sources-backend` | This repo's `authorizer-app-backend`, built from `authorizer-app-backend/Dockerfile` |
| `radar-rest-sources-authorizer` | This repo's `authorizer-app` frontend |
| `redis` | Distributed lock backend for token refresh |
| `traefik` | Reverse proxy routing `/managementportal`, `/rest-sources/backend`, `/rest-sources/authorizer` |

Configure via `docker/etc/rest-source-authorizer/authorizer.yml` (copy from the `.template`) before
`docker-compose up -d --build`.

---

## Key Dependencies

| Library | Purpose |
|---------|---------|
| radar-jersey | JAX-RS + Jersey + Hibernate + ManagementPortal client integration (external convention library) |
| ktor-client | Async HTTP client used by provider services for token exchange/refresh/revoke calls |
| kotlinx.serialization | JSON (de)serialization for API DTOs |
| PostgreSQL / Hibernate (JPA) | Persistence for `rest_source_user` / `registration` |
| Jedis | Redis client, used by `RedisLockService` for distributed token-refresh locking |
| Liquibase | DB schema migrations |
| Sentry | Error monitoring (opt-in via `SENTRY_DSN`, see `authorizer-app-backend/README.md`) |
| Angular 15 + Angular Material + ngx-translate | Frontend SPA framework, UI components, i18n |
