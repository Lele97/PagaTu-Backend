<div align="center">

# ☕ PagaTu <kbd>v1.0.6</kbd>

**Enterprise microservices platform that gamifies the "who buys breakfast?" ritual.**

[Documentation](https://github.com/Lele97/PagaTu-Backend#readme) · [Source](https://github.com/Lele97/PagaTu-Backend) · [Releases](https://github.com/Lele97/PagaTu-Backend/releases) · [Report an issue](https://github.com/Lele97/PagaTu-Backend/issues)

![Release](https://img.shields.io/github/v/release/Lele97/PagaTu-Backend?display_name=tag&label=Release&style=flat-square)
![Java](https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring%20Boot](https://img.shields.io/badge/Spring%20Boot-3.4-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Qodana](https://img.shields.io/badge/Qodana-Passed-brightgreen?style=flat-square&logo=jetbrains&logoColor=white)

</div>

---

## 📌 Release Info

| | |
|:--|:--|
| **Version** | <kbd>1.0.6</kbd> |
| **Released** | 2026-09-29 17:24 UTC |
| **Tag** | [`pagatu-v1.0.6`](https://github.com/Lele97/PagaTu-Backend/releases/tag/pagatu-v1.0.6) |
| **Branch** | [`release/pagatu-v1.0.6`](https://github.com/Lele97/PagaTu-Backend/tree/release/pagatu-v1.0.6) |
| **Commit** | `0f76ab3` |
| **Previous** | [`pagatu-v1.0.5`](https://github.com/Lele97/PagaTu-Backend/releases/tag/pagatu-v1.0.5) |
| **Quality gate** | ✅ Qodana — Passed |
| **Commits** | 4 non-merge commits |

## ✨ What's New in v1.0.6

### 🚀 Features

- feat(release): rewrite release README generator

### ♻️ Refactoring & Performance

- refactor(auth): remove unused event classes

### 🧹 Chores & Maintenance

- chore: remove unused imports, commented-out code and stray artifact

---

## 📊 Impact

| Metric | Value |
|:--|--:|
| Commits | **4** |
| Files changed | **18** |
| Lines added | **+464** |
| Lines removed | **−663** |
| Net delta | **+-199** |
| New features | **1** |
| Bug fixes | **0
0** |

---

## 🧩 Services in this Release

| Module | Artifact | Port | Image | Responsibility |
|:--|:--|--:|:--|:--|
| `auth` |     <artifactId>auth</artifactId>:1.0.6 | 8081 | pagatu-auth | Identity, JWT, OAuth2, email verification, rate-limited password reset |
| `coffee` |     <artifactId>coffee</artifactId>:1.0.6 | 8082 | pagatu-coffee | Core domain: turn rotation, groups, invitations, payments, gamification |
| `mail` |     <artifactId>mail</artifactId>:1.0.6 | 8083 | pagatu-mail | Transactional HTML email delivery (9 Thymeleaf templates) via NATS events |
| `gateway-service` |     <artifactId>gateway-service</artifactId>:1.0.6 | 8080 | pagatu-gateway-service | Reactive API gateway: JWT edge auth, routing, circuit breakers, OpenAPI |
| `eureka-server` |     <artifactId>eureka-server</artifactId>:1.0.6 | 8761 | pagatu-eureka-server | Service discovery and health registry |

**Architecture:** `Client → API Gateway (JWT + Circuit Breaker) → {Auth, Coffee, Mail}`
with `Eureka` service discovery, `PostgreSQL` per service (Flyway migrations),
and `NATS` carrying domain events through a **transactional outbox** pattern.

---

## 🐳 Container Images

| Image | Tag | Platforms | Pull |
|:--|:--|:--|:--|
| \`pagatu-auth\` | \`:1.0.6\` | \`linux/amd64\`, \`linux/arm64\` | \`docker pull Lele97/pagatu-auth:1.0.6\` |
| \`pagatu-coffee\` | \`:1.0.6\` | \`linux/amd64\`, \`linux/arm64\` | \`docker pull Lele97/pagatu-coffee:1.0.6\` |
| \`pagatu-mail\` | \`:1.0.6\` | \`linux/amd64\`, \`linux/arm64\` | \`docker pull Lele97/pagatu-mail:1.0.6\` |
| \`pagatu-gateway-service\` | \`:1.0.6\` | \`linux/amd64\`, \`linux/arm64\` | \`docker pull Lele97/pagatu-gateway-service:1.0.6\` |
| \`pagatu-eureka-server\` | \`:1.0.6\` | \`linux/amd64\`, \`linux/arm64\` | \`docker pull Lele97/pagatu-eureka-server:1.0.6\` |

### ☸️ Kubernetes rollout

```bash
kubectl rollout restart deploy -n pagatu
kubectl rollout status  deploy -n pagatu
```

### 🧪 Local stack

```bash
bash script/start-docker.sh          # build + run everything
bash script/start-docker.sh logs    # tail all service logs
```

| Endpoint | URL |
|:--|:--|
| Gateway | http://localhost:8080 |
| Eureka dashboard | http://localhost:8761 |
| Mailpit inbox | http://localhost:8025 |
| PostgreSQL | localhost:5433 |
| NATS monitoring | http://localhost:8222 |

---

## 🛡️ Quality & Verification

| Gate | Tool | Result |
|:--|:--|:--|
| Compilation | Maven (`mvnw clean compile`) | 5/5 modules |
| Static analysis | Qodana (jvm-community) | ✅ Passed |
| Unit tests | JUnit 5 + Mockito | run in CI |
| Integration tests | Testcontainers (PostgreSQL, NATS) | run in CI |
| Contract tests | Pact (Gateway ↔ services) | run in CI |
| Container scan | Trivy | run in CI |
| Dependency audit | OWASP dependency-check | run in CI |

API documentation is exposed per service via **OpenAPI / Swagger UI**
(`/swagger-ui.html`) and aggregated at the gateway.

---

## 🏷️ Badges

### Release

| [![badge](https://img.shields.io/github/v/release/Lele97/PagaTu-Backend?display_name=tag&label=Release&style=flat-square)](https://github.com/Lele97/PagaTu-Backend) | [![badge](https://img.shields.io/github/release-date/Lele97/PagaTu-Backend?display_name=released&label=Date&style=flat-square)](https://github.com/Lele97/PagaTu-Backend) |
| [![badge](https://img.shields.io/github/downloads/Lele97/PagaTu-Backend/total?style=flat-square&label=Downloads)](https://github.com/Lele97/PagaTu-Backend) | [![badge](https://img.shields.io/github/tag/Lele97/PagaTu-Backend?style=flat-square&label=Tag)](https://github.com/Lele97/PagaTu-Backend) |

### CI/CD

| [![badge](https://img.shields.io/github/actions/workflow/status/Lele97/PagaTu-Backend/ci.yml?branch=main&label=Build&style=flat-square)](https://github.com/Lele97/PagaTu-Backend) | [![badge](https://img.shields.io/github/actions/workflow/status/Lele97/PagaTu-Backend/ci.yml?branch=main&label=Tests&style=flat-square)](https://github.com/Lele97/PagaTu-Backend) |
| [![badge](https://img.shields.io/badge/Qodana-Passed-brightgreen?style=flat-square&logo=jetbrains&logoColor=white)](https://github.com/Lele97/PagaTu-Backend) | [![badge](https://img.shields.io/badge/CI-passing-brightgreen?style=flat-square&label=CI)](https://github.com/Lele97/PagaTu-Backend) |

### Code

| [![badge](https://img.shields.io/codecov/c/github/Lele97/PagaTu-Backend?style=flat-square&label=Coverage)](https://github.com/Lele97/PagaTu-Backend) | [![badge](https://img.shields.io/tokei/lines/github/Lele97/PagaTu-Backend?style=flat-square&label=Lines)](https://github.com/Lele97/PagaTu-Backend) |
| [![badge](https://img.shields.io/github/languages/code-size/Lele97/PagaTu-Backend?style=flat-square&label=Size)](https://github.com/Lele97/PagaTu-Backend) | [![badge](https://img.shields.io/github/languages/top/Lele97/PagaTu-Backend?style=flat-square&label=Top%20Language)](https://github.com/Lele97/PagaTu-Backend) |

### Repository

| [![badge](https://img.shields.io/github/license/Lele97/PagaTu-Backend?style=flat-square&label=License)](https://github.com/Lele97/PagaTu-Backend) | [![badge](https://img.shields.io/github/stars/Lele97/PagaTu-Backend?style=flat-square&label=Stars)](https://github.com/Lele97/PagaTu-Backend) |
| [![badge](https://img.shields.io/github/forks/Lele97/PagaTu-Backend?style=flat-square&label=Forks)](https://github.com/Lele97/PagaTu-Backend) | [![badge](https://img.shields.io/github/watchers/Lele97/PagaTu-Backend?style=flat-square&label=Watchers)](https://github.com/Lele97/PagaTu-Backend) |
| [![badge](https://img.shields.io/github/issues/Lele97/PagaTu-Backend?style=flat-square&label=Issues)](https://github.com/Lele97/PagaTu-Backend) | [![badge](https://img.shields.io/github/issues-pr/Lele97/PagaTu-Backend?style=flat-square&label=PRs)](https://github.com/Lele97/PagaTu-Backend) |

### Tech Stack

| [![badge](https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://github.com/Lele97/PagaTu-Backend) | [![badge](https://img.shields.io/badge/Spring%20Boot-3.4-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://github.com/Lele97/PagaTu-Backend) |
| [![badge](https://img.shields.io/badge/Spring%20Cloud-2024.0-6DB33F?style=flat-square&logo=spring&logoColor=white)](https://github.com/Lele97/PagaTu-Backend) | [![badge](https://img.shields.io/badge/PostgreSQL-15-4169E1?style=flat-square&logo=postgresql&logoColor=white)](https://github.com/Lele97/PagaTu-Backend) |
| [![badge](https://img.shields.io/badge/NATS-FF6C37?style=flat-square&logo=nats&logoColor=white)](https://github.com/Lele97/PagaTu-Backend) | [![badge](https://img.shields.io/badge/Flyway-CC0B2F?style=flat-square&logo=flywaydb&logoColor=white)](https://github.com/Lele97/PagaTu-Backend) |
| [![badge](https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white)](https://github.com/Lele97/PagaTu-Backend) | [![badge](https://img.shields.io/badge/Kubernetes-326CE5?style=flat-square&logo=kubernetes&logoColor=white)](https://github.com/Lele97/PagaTu-Backend) |
| [![badge](https://img.shields.io/badge/Apache%20Maven-C71A36?style=flat-square&logo=apache-maven&logoColor=white)](https://github.com/Lele97/PagaTu-Backend) | [![badge](https://img.shields.io/badge/JUnit5-25A162?style=flat-square&logo=junit5&logoColor=white)](https://github.com/Lele97/PagaTu-Backend) |
| [![badge](https://img.shields.io/badge/React-61DAFB?style=flat-square&logo=react&logoColor=black)](https://github.com/Lele97/PagaTu-Backend) | [![badge](https://img.shields.io/badge/Thymeleaf-005C0F?style=flat-square&logo=thymeleaf&logoColor=white)](https://github.com/Lele97/PagaTu-Backend) |

---

## 👥 Contributors

     3	Lele97
     1	github-actions[bot]

[View all contributors →](https://github.com/Lele97/PagaTu-Backend/graphs/contributors)

---

## 🔐 Upgrade Notes

1. Pull the release branch and rebuild all modules:
   ```bash
   ./mvnw clean install
   ```
2. Apply database migrations — Flyway runs automatically in the `prod` profile
   (`validate-on-migrate: true`); check the migration list above before promoting.
3. Rebuild and push multi-arch images, then roll the deployments:
   ```bash
   bash script/buildone.sh all
   ```
4. No configuration changes are required unless listed under
   **Breaking Changes**.

> Environment variables consumed by the services are unchanged unless a
> breaking change is documented above.

---

## 🔗 Useful Links

| Resource | Link |
|:--|:--|
| Release notes | [pagatu-v1.0.6](https://github.com/Lele97/PagaTu-Backend/releases/tag/pagatu-v1.0.6) |
| Release branch | [`release/pagatu-v1.0.6`](https://github.com/Lele97/PagaTu-Backend/tree/release/pagatu-v1.0.6) |
| Full diff | [pagatu-v1.0.6...main](https://github.com/Lele97/PagaTu-Backend/compare/pagatu-v1.0.6...main) |
| Pull requests | [Merged PRs](https://github.com/Lele97/PagaTu-Backend/pulls) |
| Closed issues | [Closed issues](https://github.com/Lele97/PagaTu-Backend/issues) |
| CI runs | [Actions](https://github.com/Lele97/PagaTu-Backend/actions) |
| All releases | [Tags](https://github.com/Lele97/PagaTu-Backend/tags) |

---

<div align="center">

**Full changelog:** [pagatu-v1.0.5 → pagatu-v1.0.6](https://github.com/Lele97/PagaTu-Backend/compare/pagatu-v1.0.5...pagatu-v1.0.6)

_Generated on 2026-09-29 17:24 UTC by PagaTu release automation._

</div>
