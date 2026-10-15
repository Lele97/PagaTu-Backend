#!/bin/bash

# PagaTu Version Derivation Script
# Derives version from git tags and release branches - no versions.json needed

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

# Ensure tags and release branches are available from origin if possible
fetch_git_metadata() {
    if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
        git fetch --tags origin 2>/dev/null || true
        git fetch origin '+refs/heads/release/*:refs/remotes/origin/release/*' 2>/dev/null || true
    fi
}

# Get latest release tag or branch name
get_latest_release_ref() {
    # Combine tags and release branches (both remote and local)
    local refs
    refs=$( (
        git tag -l "pagatu-v*" 2>/dev/null || true
        git branch -r --list "origin/release/pagatu-v*" 2>/dev/null | sed -e 's|.*release/||' -e 's/^[ *]*//' || true
        git branch --list "release/pagatu-v*" 2>/dev/null | sed -e 's|.*release/||' -e 's/^[ *]*//' || true
    ) | sed -e 's/^[ *]*//' -e 's|remotes/origin/||' | grep -E '^pagatu-v[0-9]+\.[0-9]+\.[0-9]+' | sort -V -u )

    if [ -n "$refs" ]; then
        echo "$refs" | tail -1
    fi
}

# Resolve git commit ref for a release tag or branch
resolve_git_ref() {
    local target="$1"
    local candidates=(
        "refs/tags/$target"
        "$target"
        "origin/release/$target"
        "release/$target"
        "origin/$target"
    )
    for ref in "${candidates[@]}"; do
        if git rev-parse --verify "$ref" >/dev/null 2>&1; then
            echo "$ref"
            return 0
        fi
    done
    return 1
}

# Extract semver version (X.Y.Z) from tag/branch name
extract_version() {
    local ref="$1"
    echo "$ref" | sed -E 's/.*pagatu-v([0-9]+\.[0-9]+\.[0-9]+).*/\1/'
}

# Calculate next version based on commits since last release
# Informational messages go to stderr (>&2); only the version number goes to stdout
calculate_next_version() {
    local latest_ref="$1"
    local current_version=$(extract_version "$latest_ref")

    if [ -z "$current_version" ]; then
        current_version="1.0.0"
    fi

    IFS='.' read -ra VERSION_PARTS <<< "$current_version"
    local major=${VERSION_PARTS[0]:-1}
    local minor=${VERSION_PARTS[1]:-0}
    local patch=${VERSION_PARTS[2]:-0}

    # Find the git commit ref for latest_ref
    local resolved_ref
    resolved_ref=$(resolve_git_ref "$latest_ref" || echo "")

    local commit_count=0
    local commits=""

    if [ -n "$resolved_ref" ]; then
        commit_count=$(git rev-list --count "${resolved_ref}..HEAD" 2>/dev/null || echo 0)
        if [ "$commit_count" -gt 0 ]; then
            commits=$(git log --oneline "${resolved_ref}..HEAD" 2>/dev/null || echo "")
        fi
    fi

    echo -e "${BLUE}📊 Commits since $latest_ref: $commit_count${NC}" >&2

    # Determine bump type based on commit messages
    local has_breaking=false
    local has_feature=false
    local has_fix=false

    if [ "$commit_count" -gt 0 ] && [ -n "$commits" ]; then
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

    # Decide bump type safely without ((var++)) exit code traps under set -e
    if [ "$has_breaking" = true ]; then
        major=$((major + 1))
        minor=0
        patch=0
        echo -e "${YELLOW}🔴 Breaking changes detected -> MAJOR bump${NC}" >&2
    elif [ "$has_feature" = true ]; then
        minor=$((minor + 1))
        patch=0
        echo -e "${YELLOW}🟢 Features detected -> MINOR bump${NC}" >&2
    elif [ "$has_fix" = true ] || [ "$commit_count" -gt 0 ]; then
        patch=$((patch + 1))
        echo -e "${YELLOW}🟡 Fixes/changes detected -> PATCH bump${NC}" >&2
    else
        echo -e "${GREEN}✅ No changes since last release${NC}" >&2
    fi

    echo "$major.$minor.$patch"
}

# Main function
main() {
    local command="${1:-show}"

    fetch_git_metadata

    case "$command" in
        "show"|"current")
            local latest_ref=$(get_latest_release_ref)
            if [ -z "$latest_ref" ]; then
                echo -e "${RED}❌ No release tags found. Starting from v1.0.0${NC}" >&2
                echo "1.0.0"
            else
                local current_version=$(extract_version "$latest_ref")
                echo -e "${GREEN}✅ Latest release: $latest_ref${NC}" >&2
                echo -e "${BLUE}📦 Current version: $current_version${NC}" >&2
                echo "$current_version"
            fi
            ;;
        "next"|"bump")
            local latest_ref=$(get_latest_release_ref)
            if [ -z "$latest_ref" ]; then
                echo -e "${YELLOW}⚠️  No release tags found. Starting from v1.0.0${NC}" >&2
                echo "1.0.0"
            else
                local next_version
                next_version=$(calculate_next_version "$latest_ref")
                echo "$next_version"
            fi
            ;;
        "branch")
            local latest_ref=$(get_latest_release_ref)
            local version="1.0.0"
            if [ -n "$latest_ref" ]; then
                version=$(calculate_next_version "$latest_ref" 2>/dev/null)
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