#!/bin/bash

# PagaTu Release README Generator
# Creates a comprehensive README.md for a release version with badges

set -e

# Helper: compute previous patch version
prev_version() {
    local v="$1"
    IFS='.' read -r major minor patch <<< "$v"
    echo "${major}.${minor}.$((patch - 1))"
}

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Usage
usage() {
    cat << EOF
Usage: $0 <version> <branch_name> [output_file]

Generates a release README.md with badges and version info.

Arguments:
  version       Release version (e.g., 1.0.5)
  branch_name   Release branch name (e.g., release/pagatu-v1.0.5)
  output_file   Optional output file (default: RELEASE-README.md)

Example:
  $0 1.0.5 release/pagatu-v1.0.5
EOF
}

# Check arguments
if [ $# -lt 2 ]; then
    usage
    exit 1
fi

VERSION="$1"
BRANCH_NAME="$2"
OUTPUT_FILE="${3:-RELEASE-README.md}"

# Get current date
RELEASE_DATE=$(date -u +"%Y-%m-%d")
RELEASE_YEAR=$(date -u +"%Y")

# GitHub repo info (from git remote)
REPO_URL=$(git config --get remote.github.url 2>/dev/null || git config --get remote.origin.url 2>/dev/null)
if [[ "$REPO_URL" =~ github.com[:/](.+)/(.+)\.git ]]; then
    REPO_OWNER="${BASH_REMATCH[1]}"
    REPO_NAME="${BASH_REMATCH[2]}"
else
    REPO_OWNER="Lele97"
    REPO_NAME="PagaTu-Backend"
fi

GITHUB_URL="https://github.com/${REPO_OWNER}/${REPO_NAME}"
RELEASE_URL="${GITHUB_URL}/releases/tag/pagatu-v${VERSION}"
BRANCH_URL="${GITHUB_URL}/tree/${BRANCH_NAME}"
ISSUES_URL="${GITHUB_URL}/issues"
ACTIONS_URL="${GITHUB_URL}/actions"

echo -e "${BLUE}📝 Generating release README for v${VERSION}...${NC}"

# Generate README
cat > "$OUTPUT_FILE" << EOF
# PagaTu v${VERSION}

> **Release Date:** ${RELEASE_DATE}  
> **Branch:** [${BRANCH_NAME}](${BRANCH_URL})  
> **Tag:** [pagatu-v${VERSION}](${GITHUB_URL}/releases/tag/pagatu-v${VERSION})

---

## 📦 Overview

This release includes updates to all PagaTu microservices with bug fixes, improvements, and new features.

### Services

| Service | Description | Version | Docker Image |
|---------|-------------|---------|--------------|
| **auth** | Authentication & Authorization | ${VERSION} | \`ghcr.io/${REPO_OWNER}/${REPO_NAME}/auth:${VERSION}\` |
| **coffee** | Coffee/Expense Tracking | ${VERSION} | \`ghcr.io/${REPO_OWNER}/${REPO_NAME}/coffee:${VERSION}\` |
| **mail** | Email Notifications | ${VERSION} | \`ghcr.io/${REPO_OWNER}/${REPO_NAME}/mail:${VERSION}\` |
| **gateway-service** | API Gateway | ${VERSION} | \`ghcr.io/${REPO_OWNER}/${REPO_NAME}/gateway:${VERSION}\` |
| **eureka-server** | Service Discovery | ${VERSION} | \`ghcr.io/${REPO_OWNER}/${REPO_NAME}/eureka:${VERSION}\` |

---

## 🏷️ Badges

### Build & Release
![GitHub Release](https://img.shields.io/github/v/release/${REPO_OWNER}/${REPO_NAME}?label=Latest%20Release&style=flat-square)
![GitHub Release Date](https://img.shields.io/github/release-date/${REPO_OWNER}/${REPO_NAME}?style=flat-square)
![GitHub All Releases](https://img.shields.io/github/downloads/${REPO_OWNER}/${REPO_NAME}/total?style=flat-square)

### CI/CD Status
![Build Status](https://img.shields.io/github/actions/workflow/status/${REPO_OWNER}/${REPO_NAME}/ci.yml?branch=main&label=Build&style=flat-square)
![Tests](https://img.shields.io/github/actions/workflow/status/${REPO_OWNER}/${REPO_NAME}/ci.yml?branch=main&label=Tests&style=flat-square)
![Qodana](https://img.shields.io/github/actions/workflow/status/${REPO_OWNER}/${REPO_NAME}/ci.yml?branch=main&label=Qodana&style=flat-square)

### Code Quality
![Code Coverage](https://img.shields.io/codecov/c/github/${REPO_OWNER}/${REPO_NAME}?style=flat-square)
![Lines of Code](https://img.shields.io/tokei/lines/github/${REPO_OWNER}/${REPO_NAME}?style=flat-square)
![Code Size](https://img.shields.io/github/languages/code-size/${REPO_OWNER}/${REPO_NAME}?style=flat-square)

### Repository
![License](https://img.shields.io/github/license/${REPO_OWNER}/${REPO_NAME}?style=flat-square)
![Stars](https://img.shields.io/github/stars/${REPO_OWNER}/${REPO_NAME}?style=flat-square)
![Forks](https://img.shields.io/github/forks/${REPO_OWNER}/${REPO_NAME}?style=flat-square)
![Issues](https://img.shields.io/github/issues/${REPO_OWNER}/${REPO_NAME}?style=flat-square)
![Pull Requests](https://img.shields.io/github/issues-pr/${REPO_OWNER}/${REPO_NAME}?style=flat-square)

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
\`\`\`bash
# Clone the release branch
git clone -b ${BRANCH_NAME} ${GITHUB_URL}.git
cd ${REPO_NAME}

# Start all services
docker-compose up -d

# Check service health
docker-compose ps
\`\`\`

### Local Development
\`\`\`bash
# Build all services
./mvnw clean install -DskipTests

# Run specific service
cd auth && ../mvnw spring-boot:run
\`\`\`

### Service Endpoints (Default Ports)
| Service | Port | Health Check |
|---------|------|--------------|
| Gateway | 8080 | \`GET /actuator/health\` |
| Auth | 8081 | \`GET /actuator/health\` |
| Coffee | 8082 | \`GET /actuator/health\` |
| Mail | 8083 | \`GET /actuator/health\` |
| Eureka | 8761 | \`GET /\` |
| NATS Monitor | 8222 | \`GET /healthz\` |
| Mailpit UI | 8025 | \`GET /\` |

---

## 📋 What's New in v${VERSION}

### 🔧 Changes
- Automated release from develop branch
- All services updated to version ${VERSION}

### 🐛 Bug Fixes
- See [closed issues](${ISSUES_URL}?q=is%3Aissue+is%3Aclosed+milestone%3A%22v${VERSION}%22) for details

### ✨ New Features
- See [merged PRs](${GITHUB_URL}/pulls?q=is%3Apr+is%3Amerged+milestone%3A%22v${VERSION}%22) for details

---

## 🔧 Configuration

### Environment Variables
Key environment variables for each service:

#### Auth Service
\`\`\`env
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/auth
NATS_SERVER=nats://localhost:4222
JWT_SECRET=your-secret-key
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://localhost:8761/eureka/
\`\`\`

#### Coffee Service
\`\`\`env
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/coffee
NATS_SERVER=nats://localhost:4222
JWT_SECRET=your-secret-key
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://localhost:8761/eureka/
\`\`\`

#### Mail Service
\`\`\`env
SPRING_MAIL_HOST=localhost
SPRING_MAIL_PORT=1025
NATS_SERVER=nats://localhost:4222
JWT_SECRET=your-secret-key
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://localhost:8761/eureka/
\`\`\`

---

## 🧪 Testing

\`\`\`bash
# Run all tests
./mvnw test

# Run tests for specific service
./mvnw test -pl auth
./mvnw test -pl coffee
./mvnw test -pl mail
\`\`\`

Test reports available in \`target/surefire-reports/\` for each service.

---

## 📚 Documentation

- [API Documentation (Swagger)](http://localhost:8080/swagger-ui.html) (via Gateway)
- [Eureka Dashboard](http://localhost:8761)
- [NATS Monitoring](http://localhost:8222)
- [Mailpit UI](http://localhost:8025)

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (\`git checkout -b feature/amazing-feature\`)
3. Commit your changes (\`git commit -m 'feat: add amazing feature'\`)
4. Push to the branch (\`git push origin feature/amazing-feature\`)
5. Open a Pull Request

---

## 📄 License

Distributed under the MIT License. See \`LICENSE\` for more information.

---

## 🙏 Acknowledgments

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Spring Cloud](https://spring.io/projects/spring-cloud)
- [NATS](https://nats.io/)
- [PostgreSQL](https://www.postgresql.org/)
- [Mailpit](https://github.com/axllent/mailpit)

---

**Full Changelog:** [${GITHUB_URL}/compare/pagatu-v$(prev_version ${VERSION})...pagatu-v${VERSION}](${GITHUB_URL}/compare/pagatu-v$(prev_version ${VERSION})...pagatu-v${VERSION})

---

*Generated on ${RELEASE_DATE} by PagaTu Release Automation*
EOF

echo -e "${GREEN}✅ Release README generated: ${OUTPUT_FILE}${NC}"
echo -e "${YELLOW}📋 Preview:${NC}"
head -50 "$OUTPUT_FILE"
echo "..."
EOF