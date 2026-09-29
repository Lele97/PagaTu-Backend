# PagaTu v1.0.5

> **Release Date:** 2026-09-29  
> **Branch:** [release/pagatu-v1.0.5](https://github.com/Lele97/PagaTu-Backend/tree/release/pagatu-v1.0.5)  
> **Tag:** [pagatu-v1.0.5](https://github.com/Lele97/PagaTu-Backend/releases/tag/pagatu-v1.0.5)  
> **Qodana Quality Gate:** ✅ Passed

---

## 📦 Overview

This release includes updates to all PagaTu microservices with bug fixes, improvements, and new features.

### Services

| Service | Description | Version | Docker Image |
|---------|-------------|---------|--------------|
| **auth** | Authentication & Authorization | 1.0.5 | `ghcr.io/Lele97/PagaTu-Backend/auth:1.0.5` |
| **coffee** | Coffee/Expense Tracking | 1.0.5 | `ghcr.io/Lele97/PagaTu-Backend/coffee:1.0.5` |
| **mail** | Email Notifications | 1.0.5 | `ghcr.io/Lele97/PagaTu-Backend/mail:1.0.5` |
| **gateway-service** | API Gateway | 1.0.5 | `ghcr.io/Lele97/PagaTu-Backend/gateway:1.0.5` |
| **eureka-server** | Service Discovery | 1.0.5 | `ghcr.io/Lele97/PagaTu-Backend/eureka:1.0.5` |

---

## 🏷️ Badges

### Build & Release
![GitHub Release](https://img.shields.io/github/v/release/Lele97/PagaTu-Backend?label=Latest%20Release&style=flat-square)
![GitHub Release Date](https://img.shields.io/github/release-date/Lele97/PagaTu-Backend?style=flat-square)
![GitHub All Releases](https://img.shields.io/github/downloads/Lele97/PagaTu-Backend/total?style=flat-square)

### CI/CD Status
![Build Status](https://img.shields.io/github/actions/workflow/status/Lele97/PagaTu-Backend/ci.yml?branch=main&label=Build&style=flat-square)
![Tests](https://img.shields.io/github/actions/workflow/status/Lele97/PagaTu-Backend/ci.yml?branch=main&label=Tests&style=flat-square)
![Qodana Quality Gate](https://img.shields.io/badge/Qodana-Passed-brightgreen?style=flat-square&logo=jetbrains&logoColor=white)

### Code Quality
![Code Coverage](https://img.shields.io/codecov/c/github/Lele97/PagaTu-Backend?style=flat-square)
![Lines of Code](https://img.shields.io/tokei/lines/github/Lele97/PagaTu-Backend?style=flat-square)
![Code Size](https://img.shields.io/github/languages/code-size/Lele97/PagaTu-Backend?style=flat-square)

### Repository
![License](https://img.shields.io/github/license/Lele97/PagaTu-Backend?style=flat-square)
![Stars](https://img.shields.io/github/stars/Lele97/PagaTu-Backend?style=flat-square)
![Forks](https://img.shields.io/github/forks/Lele97/PagaTu-Backend?style=flat-square)
![Issues](https://img.shields.io/github/issues/Lele97/PagaTu-Backend?style=flat-square)
![Pull Requests](https://img.shields.io/github/issues-pr/Lele97/PagaTu-Backend?style=flat-square)

### Tech Stack
![Java](https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.5-6DB33F?style=flat-square&logo=spring-boot&logoColor=white)
![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2024.0.1-6DB33F?style=flat-square&logo=spring&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-4169E1?style=flat-square&logo=postgresql&logoColor=white)
![NATS](https://img.shields.io/badge/NATS-2.15.0-FF6C37?style=flat-square&logo=nats&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.9.9-C71A36?style=flat-square&logo=apache-maven&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-✓-2496ED?style=flat-square&logo=docker&logoColor=white)

---

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Docker & Docker Compose
- Maven 3.9+ (or use included wrapper)

### Using Docker Compose (Recommended)
```bash
# Clone the release branch
git clone -b release/pagatu-v1.0.5 https://github.com/Lele97/PagaTu-Backend.git
cd PagaTu-Backend

# Start all services
docker-compose up -d

# Check service health
docker-compose ps
```

### Local Development
```bash
# Build all services
./mvnw clean install -DskipTests

# Run specific service
cd auth && ../mvnw spring-boot:run
```

### Service Endpoints (Default Ports)
| Service | Port | Health Check |
|---------|------|--------------|
| Gateway | 8080 | `GET /actuator/health` |
| Auth | 8081 | `GET /actuator/health` |
| Coffee | 8082 | `GET /actuator/health` |
| Mail | 8083 | `GET /actuator/health` |
| Eureka | 8761 | `GET /` |
| NATS Monitor | 8222 | `GET /healthz` |
| Mailpit UI | 8025 | `GET /` |

---

## 📋 What's New in v1.0.5

### 🔧 Changes
- Automated release from develop branch
- All services updated to version 1.0.5

### 🐛 Bug Fixes
- See [closed issues](https://github.com/Lele97/PagaTu-Backend/issues?q=is%3Aissue+is%3Aclosed+milestone%3A%22v1.0.5%22) for details

### ✨ New Features
- See [merged PRs](https://github.com/Lele97/PagaTu-Backend/pulls?q=is%3Apr+is%3Amerged+milestone%3A%22v1.0.5%22) for details

---

## 🔧 Configuration

### Environment Variables
Key environment variables for each service:

#### Auth Service
```env
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/auth
NATS_SERVER=nats://localhost:4222
JWT_SECRET=your-secret-key
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://localhost:8761/eureka/
```

#### Coffee Service
```env
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/coffee
NATS_SERVER=nats://localhost:4222
JWT_SECRET=your-secret-key
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://localhost:8761/eureka/
```

#### Mail Service
```env
SPRING_MAIL_HOST=localhost
SPRING_MAIL_PORT=1025
NATS_SERVER=nats://localhost:4222
JWT_SECRET=your-secret-key
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://localhost:8761/eureka/
```

---

## 🧪 Testing

```bash
# Run all tests
./mvnw test

# Run tests for specific service
./mvnw test -pl auth
./mvnw test -pl coffee
./mvnw test -pl mail
```

Test reports available in `target/surefire-reports/` for each service.

---

## 📚 Documentation

- [API Documentation (Swagger)](http://localhost:8080/swagger-ui.html) (via Gateway)
- [Eureka Dashboard](http://localhost:8761)
- [NATS Monitoring](http://localhost:8222)
- [Mailpit UI](http://localhost:8025)

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'feat: add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.

---

## 🙏 Acknowledgments

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Spring Cloud](https://spring.io/projects/spring-cloud)
- [NATS](https://nats.io/)
- [PostgreSQL](https://www.postgresql.org/)
- [Mailpit](https://github.com/axllent/mailpit)

---

**Full Changelog:** [https://github.com/Lele97/PagaTu-Backend/compare/pagatu-v1.0.4...pagatu-v1.0.5](https://github.com/Lele97/PagaTu-Backend/compare/pagatu-v1.0.4...pagatu-v1.0.5)

---

*Generated on 2026-09-29 by PagaTu Release Automation*
