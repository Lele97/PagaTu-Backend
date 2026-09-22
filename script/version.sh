#!/bin/bash

# PagaTu Version Derivation Script
# Derives version from git tags (release branches) - no versions.json needed

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

# Get latest release tag (format: release/pagatu-vX.Y.Z or pagatu-vX.Y.Z)
get_latest_release_tag() {
    # Try to get tags first, then release branches
    git tag -l "pagatu-v*" --sort=-v:refname 2>/dev/null | head -1 || \
    git branch -r -l "origin/release/pagatu-v*" --sort=-v:refname 2>/dev/null | head -1 | sed 's|origin/||' || \
    git branch -l "release/pagatu-v*" --sort=-v:refname 2>/dev/null | head -1
}

# Extract version from tag/branch name
extract_version() {
    local ref="$1"
    echo "$ref" | sed -E 's/.*pagatu-v([0-9]+\.[0-9]+\.[0-9]+).*/\1/'
}

# Calculate next version based on commits since last release (quiet - only outputs version)
calculate_next_version_quiet() {
    local latest_tag="$1"
    local current_version=$(extract_version "$latest_tag")

    IFS='.' read -ra VERSION_PARTS <<< "$current_version"
    local major=${VERSION_PARTS[0]}
    local minor=${VERSION_PARTS[1]}
    local patch=${VERSION_PARTS[2]}

    # Count commits since last release
    local commit_count=0
    if git rev-parse "$latest_tag" >/dev/null 2>&1; then
        commit_count=$(git rev-list --count "${latest_tag}..HEAD" 2>/dev/null || echo 0)
    else
        # If tag doesn't exist locally, try remote
        commit_count=$(git rev-list --count "origin/${latest_tag}..HEAD" 2>/dev/null || echo 0)
    fi

    # Determine bump type based on commit messages
    local has_breaking=false
    local has_feature=false
    local has_fix=false

    if [ "$commit_count" -gt 0 ]; then
        # Check commit messages since last release
        local commits
        if git rev-parse "$latest_tag" >/dev/null 2>&1; then
            commits=$(git log --oneline "${latest_tag}..HEAD" 2>/dev/null || echo "")
        else
            commits=$(git log --oneline "origin/${latest_tag}..HEAD" 2>/dev/null || echo "")
        fi

        if echo "$commits" | grep -qi "BREAKING CHANGE\|breaking:"; then
            has_breaking=true
        fi
        if echo "$commits" | grep -qi "^feat\|^feature:"; then
            has_feature=true
        fi
        if echo "$commits" | grep -qi "^fix\|^bug:"; then
            has_fix=true
        fi
    fi

    # Decide bump type
    if [ "$has_breaking" = true ]; then
        ((major++))
        minor=0
        patch=0
    elif [ "$has_feature" = true ]; then
        ((minor++))
        patch=0
    elif [ "$has_fix" = true ] || [ "$commit_count" -gt 0 ]; then
        ((patch++))
    fi

    echo "$major.$minor.$patch"
}

# Verbose version of calculate_next_version
calculate_next_version_verbose() {
    local latest_tag="$1"
    local current_version=$(extract_version "$latest_tag")

    IFS='.' read -ra VERSION_PARTS <<< "$current_version"
    local major=${VERSION_PARTS[0]}
    local minor=${VERSION_PARTS[1]}
    local patch=${VERSION_PARTS[2]}

    # Count commits since last release
    local commit_count=0
    if git rev-parse "$latest_tag" >/dev/null 2>&1; then
        commit_count=$(git rev-list --count "${latest_tag}..HEAD" 2>/dev/null || echo 0)
    else
        commit_count=$(git rev-list --count "origin/${latest_tag}..HEAD" 2>/dev/null || echo 0)
    fi

    echo -e "${BLUE}📊 Commits since $latest_tag: $commit_count${NC}"

    # Determine bump type based on commit messages
    local has_breaking=false
    local has_feature=false
    local has_fix=false

    if [ "$commit_count" -gt 0 ]; then
        local commits
        if git rev-parse "$latest_tag" >/dev/null 2>&1; then
            commits=$(git log --oneline "${latest_tag}..HEAD" 2>/dev/null || echo "")
        else
            commits=$(git log --oneline "origin/${latest_tag}..HEAD" 2>/dev/null || echo "")
        fi

        if echo "$commits" | grep -qi "BREAKING CHANGE\|breaking:"; then
            has_breaking=true
        fi
        if echo "$commits" | grep -qi "^feat\|^feature:"; then
            has_feature=true
        fi
        if echo "$commits" | grep -qi "^fix\|^bug:"; then
            has_fix=true
        fi
    fi

    # Decide bump type
    if [ "$has_breaking" = true ]; then
        ((major++))
        minor=0
        patch=0
        echo -e "${YELLOW}🔴 Breaking changes detected -> MAJOR bump${NC}"
    elif [ "$has_feature" = true ]; then
        ((minor++))
        patch=0
        echo -e "${YELLOW}🟢 Features detected -> MINOR bump${NC}"
    elif [ "$has_fix" = true ] || [ "$commit_count" -gt 0 ]; then
        ((patch++))
        echo -e "${YELLOW}🟡 Fixes/changes detected -> PATCH bump${NC}"
    else
        echo -e "${GREEN}✅ No changes since last release${NC}"
    fi

    echo "$major.$minor.$patch"
}

# Main function
main() {
    local command="${1:-show}"

    case "$command" in
        "show"|"current")
            local latest_tag=$(get_latest_release_tag)
            if [ -z "$latest_tag" ]; then
                echo -e "${RED}❌ No release tags found. Starting from v1.0.0${NC}"
                echo "1.0.0"
            else
                local current_version=$(extract_version "$latest_tag")
                echo -e "${GREEN}✅ Latest release: $latest_tag${NC}"
                echo -e "${BLUE}📦 Current version: $current_version${NC}"
                echo "$current_version"
            fi
            ;;
        "next"|"bump")
            local latest_tag=$(get_latest_release_tag)
            if [ -z "$latest_tag" ]; then
                echo -e "${YELLOW}⚠️  No release tags found. Starting from v1.0.0${NC}" >&2
                echo "1.0.0"
            else
                local next_version=$(calculate_next_version_verbose "$latest_tag")
                # next_version already contains the version as last line
                echo "$next_version"
            fi
            ;;
        "branch")
            local latest_tag=$(get_latest_release_tag)
            if [ -z "$latest_tag" ]; then
                local version="1.0.0"
            else
                local version=$(calculate_next_version_quiet "$latest_tag")
            fi
            local branch_name="release/pagatu-v$version"
            echo "$branch_name"
            ;;
        "help"|*)
            cat << EOF
PagaTu Version Manager (Git-based)
===================================

Usage:
  ./version.sh [command]

Commands:
  show / current     Show current version from latest release tag
  next / bump        Calculate next version based on commits since last release
  branch             Show next release branch name
  help               Show this help

Version Strategy:
  - Reads from git tags (pagatu-v*) or release branches (release/pagatu-v*)
  - Analyzes commit messages since last release:
      * BREAKING CHANGE / breaking: -> MAJOR
      * feat / feature:            -> MINOR
      * fix / bug: / any commits   -> PATCH
  - No versions.json file needed!

Examples:
  ./version.sh show
  ./version.sh next
  ./version.sh branch

Git Tag Format: pagatu-v1.0.4
Branch Format:  release/pagatu-v1.0.4
EOF
            ;;
    esac
}

main "$@"