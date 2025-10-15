#!/bin/bash

# Simple PagaTu Version Manager
# Creates version branches instead of tags

set -e

# Configuration
VERSIONS_FILE="versions.json"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

# Check dependencies
check_dependencies() {
    # Try to locate jq across platforms
    JQ_BIN=$(command -v jq 2>/dev/null || true)

    if [[ -z "$JQ_BIN" || ! -x "$JQ_BIN" ]]; then
        # Try common Windows WinGet path
        if [[ -f "/c/Users/$USERNAME/AppData/Local/Microsoft/WinGet/Packages/jqlang.jq_Microsoft.Winget.Source_8wekyb3d8bbwe/jq.exe" ]]; then
            JQ_BIN="/c/Users/$USERNAME/AppData/Local/Microsoft/WinGet/Packages/jqlang.jq_Microsoft.Winget.Source_8wekyb3d8bbwe/jq.exe"
        # Try common Scoop path
        elif [[ -f "/c/Users/$USERNAME/scoop/shims/jq.exe" ]]; then
            JQ_BIN="/c/Users/$USERNAME/scoop/shims/jq.exe"
        fi
    fi

    if [[ -z "$JQ_BIN" || ! -x "$JQ_BIN" ]]; then
        echo -e "${RED}❌ Error: jq is required but not installed or not in PATH${NC}"
        echo -e "${YELLOW}Install jq: https://jqlang.org/download/${NC}"
        exit 1
    else
        alias jq="$JQ_BIN"
        echo -e "${GREEN}✅ jq detected at: $JQ_BIN${NC}"
    fi

    # Check git
    if ! command -v git &> /dev/null; then
        echo -e "${RED}❌ Error: git is required but not installed${NC}"
        exit 1
    fi
}

# Add common Windows jq paths to PATH if needed
if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "win32" ]]; then
  export PATH="$PATH:/c/Users/$USER/AppData/Local/Microsoft/WinGet/Packages/jqlang.jq_Microsoft.Winget.Source_8wekyb3d8bbwe"
fi


# Get current versions
get_versions() {
    local versions_path="$PROJECT_ROOT/$VERSIONS_FILE"
    if [ ! -f "$versions_path" ]; then
        echo -e "${RED}❌ Error: Versions file not found: $versions_path${NC}"
        exit 1
    fi
    cat "$versions_path"
}

# Save versions
save_versions() {
    local versions_data="$1"
    local versions_path="$PROJECT_ROOT/$VERSIONS_FILE"
    local timestamp=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
    
    echo "$versions_data" | jq ".metadata.lastUpdated = \"$timestamp\"" > "$versions_path"
    echo -e "${GREEN}✅ Updated versions file${NC}"
}

# List project version and services
list_versions() {
    local versions=$(get_versions)
    
    local project_name=$(echo "$versions" | jq -r '.project.name')
    local project_version=$(echo "$versions" | jq -r '.project.version')
    local branch_prefix=$(echo "$versions" | jq -r '.project.branchPrefix')
    
    echo -e "${BLUE}📦 $project_name Project${NC}"
    echo "==================="
    echo -e "${YELLOW}Version: $project_version${NC}"
    echo -e "${YELLOW}Branch: ${branch_prefix}${project_version}${NC}"
    echo ""
    echo -e "${BLUE}Services (all use project version):${NC}"
    
    echo "$versions" | jq -r '.services | to_entries[] | "  • \(.key) - \(.value.description)"' | while read -r line; do
        echo -e "${YELLOW}$line${NC}"
    done
}

# Bump version
bump_version() {
    local current_version="$1"
    local bump_type="$2"
    
    if [[ ! "$current_version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
        echo -e "${RED}❌ Error: Invalid version format: $current_version${NC}"
        exit 1
    fi
    
    IFS='.' read -ra VERSION_PARTS <<< "$current_version"
    local major=${VERSION_PARTS[0]}
    local minor=${VERSION_PARTS[1]}
    local patch=${VERSION_PARTS[2]}
    
    case "$bump_type" in
        "patch")
            ((patch++))
            ;;
        "minor")
            ((minor++))
            patch=0
            ;;
        "major")
            ((major++))
            minor=0
            patch=0
            ;;
        *)
            echo -e "${RED}❌ Error: Invalid bump type: $bump_type${NC}"
            exit 1
            ;;
    esac
    
    echo "$major.$minor.$patch"
}

# Update project version (affects all services)
update_project_version() {
    local bump_type="$1"
    
    local versions=$(get_versions)
    
    # Get current project version
    local current_version=$(echo "$versions" | jq -r '.project.version')
    local new_version=$(bump_version "$current_version" "$bump_type")
    local branch_prefix=$(echo "$versions" | jq -r '.project.branchPrefix')
    local new_branch="${branch_prefix}${new_version}"
    
    echo -e "${BLUE}🔄 Updating PagaTu project version${NC}"
    echo -e "   Current: $current_version"
    echo -e "   New: $new_version"
    echo -e "   Branch: $new_branch"
    echo ""
    
    # Update project version in JSON
    local updated_versions=$(echo "$versions" | jq ".project.version = \"$new_version\"")
    save_versions "$updated_versions"
    
    # Update pom.xml files for all services
    echo -e "${BLUE}📦 Updating all service pom.xml files...${NC}"
    echo "$versions" | jq -r '.services | to_entries[] | "\(.key) \(.value.path)"' | while read -r service_name service_path; do
        # Clean up the path - remove ./ prefix if present
        clean_path=$(echo "$service_path" | sed 's|^\./||')
        local pom_path="$PROJECT_ROOT/$clean_path/pom.xml"

        if [ -f "$pom_path" ]; then
            if sed -i.bak '/<groupId>com\.pagatu<\/groupId>/,/<name>.*<\/name>/{
                                     /<version>/ s|<version>[^<]*</version>|<version>'"$new_version"'</version>|
                                   }' "$pom_path" 2>/dev/null; then
                rm -f "$pom_path.bak"
                echo -e "${GREEN}✅ Updated $service_name pom.xml to $new_version${NC}"
            fi
        fi
    done
    
    echo -e "${GREEN}✅ All services updated to version $new_version${NC}"
}

# Show help
show_help() {
    cat << EOF
Simple PagaTu Version Manager
============================

Usage:
  ./simple-version-manager.sh <command> [args]

Commands:
  list                    Show project version and all services
  bump <type>             Bump project version (patch/minor/major)
  help                    Show this help

Examples:
  ./simple-version-manager.sh list
  ./simple-version-manager.sh bump patch
  ./simple-version-manager.sh bump minor

Note: Uses unified versioning - all services share the project version
      Creates version branches like: release/pagatu-v1.0.1
EOF
}

# Main function
main() {
    check_dependencies
    
    case "${1:-help}" in
        "list")
            list_versions
            ;;
        "bump")
            if [ -z "$2" ]; then
                echo -e "${RED}❌ Error: Bump type required (patch/minor/major)${NC}"
                show_help
                exit 1
            fi
            update_project_version "$2"
            ;;
        "help"|*)
            show_help
            ;;
    esac
}

main "$@"