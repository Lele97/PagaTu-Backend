# Pagatu — CV/Resume Versions (Ready to Paste)

---

## 🎯 **Full CV Entry (≈1,800 chars / ~300 words)**

**Pagatu — Enterprise Microservices Platform for Group Breakfast Rotation Tracking**  
*Personal flagship project • End-to-end: architecture, implementation, testing, containerization, K8s deployment, AI-augmented dev workflow*

Designed and built a **production-grade, 5-service microservices platform** (Spring Boot 3.4 / Java 17 / Spring Cloud 2024) that transforms an informal "who buys breakfast?" ritual into a **transparent, gamified, event-driven system** with transactional outbox reliability, distributed rate limiting, circuit-breaker resilience, and an **AI-agent-augmented development lifecycle** that cut boilerplate ~35% and lifted test coverage from ~60% → >85%.

**Backend (5 Services):** Auth (JWT, OAuth2, email verification, password reset with rate limiting, token cleanup batch), **Coffee/Core** (turn-tracking, groups, invitations, payments, monthly skip/karma/award gamification — 11 Flyway migrations), Mail (9 Thymeleaf HTML templates, reactive WebClient, 7 NATS consumers), Gateway (Spring Cloud Gateway reactive, JWT GlobalFilter, Resilience4j circuit breakers, CORS, OpenAPI aggregation), Eureka (service discovery). **Patterns:** Transactional Outbox (dual-write + scheduled NATS publisher), event-driven decoupling (Coffee→Mail via NATS), centralized JWT at gateway, schema evolution discipline (Flyway validate-on-migrate).

**DevOps:** Multi-stage Dockerfiles, docker buildx (linux/amd64,linux/arm64), docker-compose (dev/prod parity), Kubernetes Deployments + Services + Ingress (Helm-ready), zero-downtime `kubectl rollout restart`, ConfigMap/Secret injection, Actuator health endpoints. Single-command local stack (`start-docker.sh`).

**AI & Agentic Workflow:** Integrated GitHub Actions / local IDE (Continue.dev + Ollama / Copilot). **Agentic scaffolding** (controllers, entities, migrations, services, DTOs). **Prompt-engineered test generation** (JUnit 5 + MockMvc + Testcontainers + WireMock) covering turn rotation, invitation state machine, skip limits, pay-for rules, round reset, awards — coverage **60% → >85%**. **Flyway from natural language** (spec → migration + code + test). **Living docs** (OpenAPI, README, ADR from commits). **AI code review gate** (N+1, missing @Transactional, JWT gaps, OWASP Top 10). **RAG onboarding CLI agent** (Python + LangChain) — ramp-up days → hours.

**Testing:** Unit (JUnit 5, Mockito), Integration (Testcontainers PostgreSQL/NATS), Contract (Pact Gateway↔Auth/Coffee/Mail), External mock (WireMock), Security (Trivy, OWASP dependency-check). ~200+ test methods.

**Stack:** Java 17, Spring Boot 3.4, Spring Cloud 2024, PostgreSQL, Flyway, NATS, Spring Cloud Gateway, Eureka, Bucket4j, Redis-ready, Caffeine, Docker, K8s, Helm, JWT, OpenAPI, JUnit 5, Testcontainers, WireMock, Pact, GitHub Actions, Trivy.

---

## 📄 **Condensed CV Bullet Points (for Experience Section)**

- **Architected & delivered a 5-service Spring Boot 3.4 microservices platform** (Auth, Coffee, Mail, Gateway, Eureka) on Java 17 + Spring Cloud 2024, deployed to local Kubernetes with Helm-ready manifests and multi-arch Docker images.
- **Core domain (Coffee service):** Implemented turn-tracking rotation, group/invitation lifecycle, payment registration with pay-for-other, monthly skip/karma/award gamification — evolved via **11 Flyway migrations** with `validate-on-migrate` in prod.
- **Reliability patterns:** **Transactional Outbox** (dual-write + scheduled NATS publisher with retry/cleanup) for guaranteed event delivery; **Resilience4j circuit breakers** at Gateway; centralized JWT validation (GlobalFilter); distributed rate limiting (Bucket4j + Redis-ready).
- **Event-driven async:** Coffee publishes 7 domain events (NextPayment, SkipPayment, PayFor, Invitation, TurnReminder…) → Mail consumes via NATS & sends 9 Thymeleaf HTML emails with reactive WebClient fan-out to fetch user data.
- **AI-augmented dev lifecycle:** Built a **prompt library (50+)** for agentic scaffolding, test generation (JUnit/Testcontainers/WireMock → **coverage 60%→85%+**), Flyway migration from NL requirements, OpenAPI/ADR generation, **AI code review gate** (N+1, @Transactional, JWT, OWASP), and a **RAG onboarding CLI agent** (LangChain) cutting ramp-up from days to hours.
- **Testing rigor:** Unit/Integration (Testcontainers), Contract (Pact), Security (Trivy/OWASP), ~200+ test methods; CI pipeline: build → test → scan → multi-arch push → K8s rollout.

---

## 🔗 **LinkedIn "Projects" Section (≈800 chars)**

**Pagatu — Enterprise Microservices Platform**  
*Spring Boot 3.4 • Java 17 • Spring Cloud 2024 • Kubernetes • AI-Augmented Development*

Built a production-grade **5-service microservices platform** that gamifies group breakfast rotation tracking. End-to-end ownership: architecture, implementation, testing, containerization, K8s deployment, and AI-agent-augmented workflow.

**Architecture:** Auth (JWT/OAuth2, rate-limited reset, token cleanup), **Coffee/Core** (turn rotation, groups, payments, skip/karma/awards — 11 Flyway migrations), Mail (9 Thymeleaf templates, reactive, 7 NATS consumers), Gateway (reactive, JWT filter, Resilience4j circuit breakers, OpenAPI), Eureka (discovery).

**Key Patterns:** Transactional Outbox for reliable NATS event delivery; event-driven decoupling (Coffee→Mail); centralized JWT at edge; schema evolution discipline.

**DevOps:** Multi-stage + multi-arch Docker (buildx), docker-compose parity, K8s Deployments/Ingress (Helm-ready), zero-downtime rollouts, single-command local stack.

**AI Innovation:** Systematic prompt library for **agentic scaffolding** (−35% boilerplate), **test generation** (JUnit/Testcontainers → **coverage 60%→85%+**), **Flyway from natural language**, **living docs generation**, **AI code review gate** (N+1, @Transactional, OWASP), **RAG onboarding agent** (LangChain) — **ramp-up days→hours**.

**Testing:** Unit/Integration (Testcontainers), Contract (Pact), Security (Trivy), ~200 tests. CI: build→test→scan→push→deploy.

**Stack:** Java 17, Spring Boot 3.4, PostgreSQL, NATS, Docker, K8s, Helm, JWT, Bucket4j, Caffeine, JUnit 5, Testcontainers, Pact, WireMock, GitHub Actions.

---

## 🏷️ **One-Liner for "Technical Highlights" / Skills Cloud**

`Spring Boot 3.4` `Java 17` `Spring Cloud 2024` `Microservices` `Transactional Outbox` `NATS` `Kubernetes` `Helm` `Docker multi-arch` `Resilience4j` `JWT` `Flyway` `Pact Contract Testing` `Testcontainers` `Generative AI` `Prompt Engineering` `Agentic Workflows` `LangChain` `RAG` `CI/CD` `GitHub Actions` `Trivy` `OpenAPI`