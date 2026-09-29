#!/bin/bash

# ============================================================================
#  PagaTu — Release README Generator
# ----------------------------------------------------------------------------
#  Builds the release README used BOTH as the GitHub Release body AND as the
#  RELEASE-README.md committed on the release branch, so the two can never
#  drift apart (single source of truth).
#
#  Everything is derived from git metadata of the release range, so the
#  document is always consistent with what actually shipped.
# ============================================================================

set -euo pipefail

# ----------------------------------------------------------------------------
#  Colors (terminal only)
# ----------------------------------------------------------------------------
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

# ----------------------------------------------------------------------------
#  Usage
# ----------------------------------------------------------------------------
usage() {
    cat << EOF
Usage: $0 <version> <branch_name> [qodana_status] [output_file]

Generates the release README (badges, What's New from real commits,
services, deployment, contributors, upgrade notes).

Arguments:
  version         Release version, e.g. 1.0.5
  branch_name     Release branch, e.g. release/pagatu-v1.0.5
  qodana_status   Qodana quality gate outcome: success|success-with-problems|
                  failure  (default: success)
  output_file     Output path (default: RELEASE-README.md)

Example:
  $0 1.0.5 release/pagatu-v1.0.5 success
  $0 1.0.5 release/pagatu-v1.0.5 failure RELEASE-README.md
EOF
}

if [ $# -lt 2 ]; then
    usage
    exit 1
fi

VERSION="$1"
BRANCH_NAME="$2"
QODANA_STATUS="${3:-success}"
OUTPUT_FILE="${4:-RELEASE-README.md}"

TAG="pagatu-v${VERSION}"

# ----------------------------------------------------------------------------
#  Repository identity
# ----------------------------------------------------------------------------
REPO_URL=$(git config --get remote.github.url 2>/dev/null \
        || git config --get remote.origin.url 2>/dev/null || true)
if [[ "$REPO_URL" =~ github.com[:/]([^/]+)/([^/]+) ]]; then
    REPO_OWNER="${BASH_REMATCH[1]}"
    REPO_NAME="${BASH_REMATCH[2]}"
    REPO_NAME="${REPO_NAME%.git}"   # strip trailing .git
    REPO_NAME="${REPO_NAME%/}"
else
    REPO_OWNER="Lele97"
    REPO_NAME="PagaTu-Backend"
fi

GITHUB_URL="https://github.com/${REPO_OWNER}/${REPO_NAME}"
RELEASE_URL="${GITHUB_URL}/releases/tag/${TAG}"
BRANCH_URL="${GITHUB_URL}/tree/${BRANCH_NAME}"
COMPARE_URL="${GITHUB_URL}/compare/${TAG}...main"
ISSUES_URL="${GITHUB_URL}/issues"
PR_URL="${GITHUB_URL}/pulls"
ACTIONS_URL="${GITHUB_URL}/actions"
CONTRIBUTORS_URL="${GITHUB_URL}/graphs/contributors"

# ----------------------------------------------------------------------------
#  Dates
# ----------------------------------------------------------------------------
RELEASE_DATE=$(date -u +"%Y-%m-%d")
RELEASE_DATETIME=$(date -u +"%Y-%m-%d %H:%M UTC")

# ----------------------------------------------------------------------------
#  Qodana quality gate presentation
# ----------------------------------------------------------------------------
case "$QODANA_STATUS" in
    success)
        QODANA_BADGE="https://img.shields.io/badge/Qodana-Passed-brightgreen?style=flat-square&logo=jetbrains&logoColor=white"
        QODANA_ICON="✅"
        QODANA_TEXT="Passed"
        ;;
    success-with-problems)
        QODANA_BADGE="https://img.shields.io/badge/Qodana-Warnings-yellow?style=flat-square&logo=jetbrains&logoColor=white"
        QODANA_ICON="⚠️"
        QODANA_TEXT="Passed with warnings"
        ;;
    *)
        QODANA_BADGE="https://img.shields.io/badge/Qodana-Failed-red?style=flat-square&logo=jetbrains&logoColor=white"
        QODANA_ICON="❌"
        QODANA_TEXT="Failed"
        ;;
esac

# ----------------------------------------------------------------------------
#  Resolve the git range covered by this release
# ----------------------------------------------------------------------------
CURRENT_REF=""
for candidate in "refs/tags/${TAG}" "$TAG" "$BRANCH_NAME" "origin/$BRANCH_NAME" "HEAD"; do
    if git rev-parse --verify --quiet "$candidate" >/dev/null 2>&1; then
        CURRENT_REF="$candidate"
        break
    fi
done

if [ -z "$CURRENT_REF" ]; then
    echo -e "${RED}❌ Cannot resolve any git ref for ${TAG}. Aborting.${NC}" >&2
    exit 1
fi

# Previous release tag = highest pagatu-v* tag that is not the current one
ALL_TAGS=$(git tag -l "pagatu-v*" 2>/dev/null | sort -V || true)
PREV_TAG=$(printf '%s\n' "$ALL_TAGS" \
    | grep -v "^${TAG}$" \
    | grep -E '^pagatu-v[0-9]+\.[0-9]+\.[0-9]+$' \
    | tail -1 || true)

if [ -n "$PREV_TAG" ]; then
    RANGE="${PREV_TAG}..${CURRENT_REF}"
    IS_FIRST_RELEASE=false
else
    RANGE="$CURRENT_REF"
    IS_FIRST_RELEASE=true
fi

# Commits in the release, excluding merges and the automated release commit
mapfile -t COMMITS < <(
    git log "$RANGE" --no-merges --pretty=format:'%s' 2>/dev/null \
        | grep -viE '^chore\(release\):' || true
)

mapfile -t BREAKING < <(
    git log "$RANGE" --no-merges --pretty=format:'%s%n%b' 2>/dev/null \
        | grep -iE 'BREAKING[ -]CHANGE|^break:' || true
)

COMMIT_COUNT=$(git rev-list --count --no-merges "$RANGE" 2>/dev/null || echo 0)
SHORT_SHA=$(git rev-parse --short "$CURRENT_REF" 2>/dev/null || echo "unknown")

# Aggregate change statistics
SHORTSTAT=$(git diff --shortstat "$RANGE" 2>/dev/null || true)
FILES_CHANGED=$(printf '%s' "$SHORTSTAT" | grep -oE '[0-9]+ files? changed' | grep -oE '[0-9]+' || echo 0)
INSERTIONS=$(printf '%s' "$SHORTSTAT" | grep -oE '[0-9]+ insertions?\(\+\)' | grep -oE '[0-9]+' || echo 0)
DELETIONS=$(printf '%s' "$SHORTSTAT" | grep -oE '[0-9]+ deletions?\(-\)' | grep -oE '[0-9]+' || echo 0)
: "${FILES_CHANGED:=0}" "${INSERTIONS:=0}" "${DELETIONS:=0}"

# ----------------------------------------------------------------------------
#  Helpers
# ----------------------------------------------------------------------------

# render_badges <badge_url_1> <badge_url_2>  → two badges on one table row
badge_row() {
    printf '| [![badge](%s)](%s) | [![badge](%s)](%s) |\n' \
        "$1" "$GITHUB_URL" "$2" "$GITHUB_URL"
}

# count_of <array-name> → number of elements, safe under set -u when empty
count_of() {
    local -n _arr="$1"
    printf '%s' "${#_arr[@]}"
}

# render_section <emoji> <title> <regex>  → commit list, only if non-empty
render_commit_section() {
    local emoji="$1" title="$2" regex="$3"
    local items
    items=$(printf '%s\n' "${COMMITS[@]}" | grep -iE "$regex" || true)
    [ -z "$items" ] && return 0
    printf '### %s %s\n\n' "$emoji" "$title"
    printf '%s\n' "$items" | sort -u | sed 's/^/- /'
    printf '\n'
}

# Negative filter: everything that is NOT a conventional-commit category
OTHER_RE='^(feat|feature|fix|bug|hotfix|refactor|perf|style|revert|test|doc|docs|ci|build|chore)[[:space:]:]]'

# pom_field <dir> <tag>  → value of a top-level (non-parent) pom tag
pom_field() {
    awk -v tag="$2" '
        /<parent>/            { inpar = 1 }
        /<\/parent>/          { inpar = 0; next }
        !inpar && $0 ~ "<" tag ">" {
            gsub(/.*<" tag ">/, ""); gsub(/<\/" tag ">.*/, "")
            print; exit
        }
    ' "$1/pom.xml" 2>/dev/null || true
}

service_description() {
    case "$1" in
        auth)           echo "Identity, JWT, OAuth2, email verification, rate-limited password reset" ;;
        coffee)         echo "Core domain: turn rotation, groups, invitations, payments, gamification" ;;
        mail)           echo "Transactional HTML email delivery (9 Thymeleaf templates) via NATS events" ;;
        gateway-service) echo "Reactive API gateway: JWT edge auth, routing, circuit breakers, OpenAPI" ;;
        eureka-server)  echo "Service discovery and health registry" ;;
        *)              echo "Microservice" ;;
    esac
}

service_port() {
    case "$1" in
        gateway-service) echo "8080" ;;
        auth)            echo "8081" ;;
        coffee)          echo "8082" ;;
        mail)            echo "8083" ;;
        eureka-server)   echo "8761" ;;
        *)               echo "-" ;;
    esac
}

# Collect module → version from the reactor
collect_services() {
    local dir artifact version
    for dir in auth coffee mail gateway-service eureka-server; do
        [ -f "${dir}/pom.xml" ] || continue
        artifact=$(pom_field "$dir" artifactId)
        version=$(pom_field "$dir" version)
        [ -n "$artifact" ] || artifact="$dir"
        [ -n "$version" ] || version="${VERSION}"
        printf '| %s | %s:%s | %s | %s | %s |\n' \
            "\`$dir\`" "$artifact" "$version" \
            "$(service_port "$dir")" \
            "pagatu-${dir}" \
            "$(service_description "$dir")"
    done
}

# docker image table rows
collect_images() {
    local dir
    for dir in auth coffee mail gateway-service eureka-server; do
        [ -f "${dir}/Dockerfile" ] || continue
        printf '| \`pagatu-%s\` | \`:%s\` | \`linux/amd64\`, \`linux/arm64\` | \`docker pull %s/pagatu-%s:%s\` |\n' \
            "$dir" "$VERSION" "$REPO_OWNER" "$dir" "$VERSION"
    done
}

echo -e "${BLUE}📝 Generating release README for ${TAG}${NC}"
echo -e "${BLUE}   range     : ${RANGE}${NC}"
echo -e "${BLUE}   commits   : ${COMMIT_COUNT} (non-merge)${NC}"
echo -e "${BLUE}   qodana    : ${QODANA_ICON} ${QODANA_TEXT}${NC}"

# ----------------------------------------------------------------------------
#  Document
# ----------------------------------------------------------------------------
{
if [ -n "$PREV_TAG" ]; then
    PREV_CELL="[\`${PREV_TAG}\`](${GITHUB_URL}/releases/tag/${PREV_TAG})"
else
    PREV_CELL="_first release_"
fi

cat <<EOF
<div align="center">

# ☕ PagaTu <kbd>v${VERSION}</kbd>

**Enterprise microservices platform that gamifies the "who buys breakfast?" ritual.**

[Documentation](${GITHUB_URL}#readme) · [Source](${GITHUB_URL}) · [Releases](${GITHUB_URL}/releases) · [Report an issue](${ISSUES_URL})

![Release](https://img.shields.io/github/v/release/${REPO_OWNER}/${REPO_NAME}?display_name=tag&label=Release&style=flat-square)
![Java](https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring%20Boot](https://img.shields.io/badge/Spring%20Boot-3.4-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Qodana](${QODANA_BADGE})

</div>

---

## 📌 Release Info

| | |
|:--|:--|
| **Version** | <kbd>${VERSION}</kbd> |
| **Released** | ${RELEASE_DATETIME} |
| **Tag** | [\`${TAG}\`](${RELEASE_URL}) |
| **Branch** | [\`${BRANCH_NAME}\`](${BRANCH_URL}) |
| **Commit** | \`${SHORT_SHA}\` |
| **Previous** | ${PREV_CELL} |
| **Quality gate** | ${QODANA_ICON} Qodana — ${QODANA_TEXT} |
| **Commits** | ${COMMIT_COUNT} non-merge commits |

EOF

# ---------------------------------------------------------------- What's New
cat <<EOF
## ✨ What's New in v${VERSION}

EOF

FEATURES=$(printf '%s\n' "${COMMITS[@]}" | grep -iE '^feat|^feature:' || true)
FIXES=$(printf '%s\n' "${COMMITS[@]}"    | grep -iE '^fix|^bug|^hotfix' || true)
REFACTOR=$(printf '%s\n' "${COMMITS[@]}" | grep -iE '^refactor|^perf|^style|^revert' || true)
TESTS=$(printf '%s\n' "${COMMITS[@]}"    | grep -iE '^test' || true)
DOCS=$(printf '%s\n' "${COMMITS[@]}"     | grep -iE '^docs?' || true)
CI=$(printf '%s\n' "${COMMITS[@]}"       | grep -iE '^ci|^build' || true)
CHORES=$(printf '%s\n' "${COMMITS[@]}"   | grep -iE '^chore' || true)

render_commit_section "🚀" "Features"            '^feat|^feature:'
render_commit_section "🐛" "Bug Fixes"            '^fix|^bug|^hotfix'
render_commit_section "♻️"  "Refactoring & Performance" '^refactor|^perf|^style|^revert'
render_commit_section "🧪" "Tests"                '^test'
render_commit_section "📚" "Documentation"        '^docs?'
render_commit_section "🔧" "CI/CD & Build"        '^ci|^build'
render_commit_section "🧹" "Chores & Maintenance" '^chore'
render_commit_section "📦" "Other Changes"        "$OTHER_RE"

if [ "$(count_of COMMITS)" -eq 0 ]; then
    printf '_No user-facing commits in this range._\n\n'
fi

# ------------------------------------------------------------ Breaking change
if [ "$(count_of BREAKING)" -gt 0 ]; then
cat <<EOF
---

## 💥 Breaking Changes

> Review these before upgrading.

EOF
    printf '%s\n' "${BREAKING[@]}" | sort -u | sed 's/^/- /'
    printf '\n'
fi

# -------------------------------------------------------------------- Impact
cat <<EOF
---

## 📊 Impact

| Metric | Value |
|:--|--:|
| Commits | **${COMMIT_COUNT}** |
| Files changed | **${FILES_CHANGED}** |
| Lines added | **+${INSERTIONS}** |
| Lines removed | **−${DELETIONS}** |
| Net delta | **+$((INSERTIONS - DELETIONS))** |
| New features | **$(printf '%s\n' "$FEATURES" | grep -c . || echo 0)** |
| Bug fixes | **$(printf '%s\n' "$FIXES" | grep -c . || echo 0)** |

EOF

# ------------------------------------------------------------------ Services
cat <<EOF
---

## 🧩 Services in this Release

| Module | Artifact | Port | Image | Responsibility |
|:--|:--|--:|:--|:--|
EOF
collect_services

cat <<EOF

**Architecture:** \`Client → API Gateway (JWT + Circuit Breaker) → {Auth, Coffee, Mail}\`
with \`Eureka\` service discovery, \`PostgreSQL\` per service (Flyway migrations),
and \`NATS\` carrying domain events through a **transactional outbox** pattern.

EOF

# ----------------------------------------------------------------- Deployment
cat <<EOF
---

## 🐳 Container Images

| Image | Tag | Platforms | Pull |
|:--|:--|:--|:--|
EOF
collect_images

cat <<EOF

### ☸️ Kubernetes rollout

\`\`\`bash
kubectl rollout restart deploy -n pagatu
kubectl rollout status  deploy -n pagatu
\`\`\`

### 🧪 Local stack

\`\`\`bash
bash script/start-docker.sh          # build + run everything
bash script/start-docker.sh logs    # tail all service logs
\`\`\`

| Endpoint | URL |
|:--|:--|
| Gateway | http://localhost:8080 |
| Eureka dashboard | http://localhost:8761 |
| Mailpit inbox | http://localhost:8025 |
| PostgreSQL | localhost:5433 |
| NATS monitoring | http://localhost:8222 |

EOF

# ------------------------------------------------------------------- Quality
cat <<EOF
---

## 🛡️ Quality & Verification

| Gate | Tool | Result |
|:--|:--|:--|
| Compilation | Maven (\`mvnw clean compile\`) | 5/5 modules |
| Static analysis | Qodana (jvm-community) | ${QODANA_ICON} ${QODANA_TEXT} |
| Unit tests | JUnit 5 + Mockito | run in CI |
| Integration tests | Testcontainers (PostgreSQL, NATS) | run in CI |
| Contract tests | Pact (Gateway ↔ services) | run in CI |
| Container scan | Trivy | run in CI |
| Dependency audit | OWASP dependency-check | run in CI |

API documentation is exposed per service via **OpenAPI / Swagger UI**
(\`/swagger-ui.html\`) and aggregated at the gateway.

EOF

# -------------------------------------------------------------------- Badges
cat <<EOF
---

## 🏷️ Badges

### Release

EOF
badge_row "https://img.shields.io/github/v/release/${REPO_OWNER}/${REPO_NAME}?display_name=tag&label=Release&style=flat-square" \
         "https://img.shields.io/github/release-date/${REPO_OWNER}/${REPO_NAME}?display_name=released&label=Date&style=flat-square"
badge_row "https://img.shields.io/github/downloads/${REPO_OWNER}/${REPO_NAME}/total?style=flat-square&label=Downloads" \
         "https://img.shields.io/github/tag/${REPO_OWNER}/${REPO_NAME}?style=flat-square&label=Tag"

cat <<EOF

### CI/CD

EOF
badge_row "https://img.shields.io/github/actions/workflow/status/${REPO_OWNER}/${REPO_NAME}/ci.yml?branch=main&label=Build&style=flat-square" \
         "https://img.shields.io/github/actions/workflow/status/${REPO_OWNER}/${REPO_NAME}/ci.yml?branch=main&label=Tests&style=flat-square"
badge_row "${QODANA_BADGE}" \
         "https://img.shields.io/badge/CI-passing-brightgreen?style=flat-square&label=CI"

cat <<EOF

### Code

EOF
badge_row "https://img.shields.io/codecov/c/github/${REPO_OWNER}/${REPO_NAME}?style=flat-square&label=Coverage" \
         "https://img.shields.io/tokei/lines/github/${REPO_OWNER}/${REPO_NAME}?style=flat-square&label=Lines"
badge_row "https://img.shields.io/github/languages/code-size/${REPO_OWNER}/${REPO_NAME}?style=flat-square&label=Size" \
         "https://img.shields.io/github/languages/top/${REPO_OWNER}/${REPO_NAME}?style=flat-square&label=Top%20Language"

cat <<EOF

### Repository

EOF
badge_row "https://img.shields.io/github/license/${REPO_OWNER}/${REPO_NAME}?style=flat-square&label=License" \
         "https://img.shields.io/github/stars/${REPO_OWNER}/${REPO_NAME}?style=flat-square&label=Stars"
badge_row "https://img.shields.io/github/forks/${REPO_OWNER}/${REPO_NAME}?style=flat-square&label=Forks" \
         "https://img.shields.io/github/watchers/${REPO_OWNER}/${REPO_NAME}?style=flat-square&label=Watchers"
badge_row "https://img.shields.io/github/issues/${REPO_OWNER}/${REPO_NAME}?style=flat-square&label=Issues" \
         "https://img.shields.io/github/issues-pr/${REPO_OWNER}/${REPO_NAME}?style=flat-square&label=PRs"

cat <<EOF

### Tech Stack

EOF
badge_row "https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white" \
         "https://img.shields.io/badge/Spring%20Boot-3.4-6DB33F?style=flat-square&logo=springboot&logoColor=white"
badge_row "https://img.shields.io/badge/Spring%20Cloud-2024.0-6DB33F?style=flat-square&logo=spring&logoColor=white" \
         "https://img.shields.io/badge/PostgreSQL-15-4169E1?style=flat-square&logo=postgresql&logoColor=white"
badge_row "https://img.shields.io/badge/NATS-FF6C37?style=flat-square&logo=nats&logoColor=white" \
         "https://img.shields.io/badge/Flyway-CC0B2F?style=flat-square&logo=flywaydb&logoColor=white"
badge_row "https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white" \
         "https://img.shields.io/badge/Kubernetes-326CE5?style=flat-square&logo=kubernetes&logoColor=white"
badge_row "https://img.shields.io/badge/Apache%20Maven-C71A36?style=flat-square&logo=apache-maven&logoColor=white" \
         "https://img.shields.io/badge/JUnit5-25A162?style=flat-square&logo=junit5&logoColor=white"
badge_row "https://img.shields.io/badge/React-61DAFB?style=flat-square&logo=react&logoColor=black" \
         "https://img.shields.io/badge/Thymeleaf-005C0F?style=flat-square&logo=thymeleaf&logoColor=white"

# --------------------------------------------------------------- Contributors
cat <<EOF

---

## 👥 Contributors

EOF

if [ "$COMMIT_COUNT" -gt 0 ]; then
    git shortlog -sn --no-merges "$RANGE" 2>/dev/null \
        | sed -E 's/^([0-9]+)[[:space:]]+(.*)$/- **\2** — \1 commit(s)/' || true
    printf '\n[View all contributors →](%s)\n' "$CONTRIBUTORS_URL"
else
    printf '_Automated release._\n'
fi

# ------------------------------------------------------------------- Upgrade
cat <<EOF

---

## 🔐 Upgrade Notes

1. Pull the release branch and rebuild all modules:
   \`\`\`bash
   ./mvnw clean install
   \`\`\`
2. Apply database migrations — Flyway runs automatically in the \`prod\` profile
   (\`validate-on-migrate: true\`); check the migration list above before promoting.
3. Rebuild and push multi-arch images, then roll the deployments:
   \`\`\`bash
   bash script/buildone.sh all
   \`\`\`
4. No configuration changes are required unless listed under
   **Breaking Changes**.

> Environment variables consumed by the services are unchanged unless a
> breaking change is documented above.

---

## 🔗 Useful Links

| Resource | Link |
|:--|:--|
| Release notes | [${TAG}](${RELEASE_URL}) |
| Release branch | [\`${BRANCH_NAME}\`](${BRANCH_URL}) |
| Full diff | [${TAG}...main](${COMPARE_URL}) |
| Pull requests | [Merged PRs](${PR_URL}) |
| Closed issues | [Closed issues](${ISSUES_URL}) |
| CI runs | [Actions](${ACTIONS_URL}) |
| All releases | [Tags](${GITHUB_URL}/tags) |

---

<div align="center">

**Full changelog:** [${PREV_TAG:-start} → ${TAG}](${GITHUB_URL}/compare/${PREV_TAG:-HEAD}...${TAG})

_Generated on ${RELEASE_DATETIME} by PagaTu release automation._

</div>
EOF
} > "$OUTPUT_FILE"

echo -e "${GREEN}✅ Release README generated: ${OUTPUT_FILE}${NC}"
echo -e "${YELLOW}📋 Head:${NC}"
head -30 "$OUTPUT_FILE"
echo -e "${YELLOW}   … (${COMMIT_COUNT} commits rendered, ${FILES_CHANGED} files changed)${NC}"
