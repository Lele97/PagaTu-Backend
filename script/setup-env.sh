#!/bin/bash
# PagaTu Environment Setup Script
# Run this script to set up your .env file securely

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
NC='\033[0m' # No Color

echo -e "${CYAN}🔐 PagaTu Security Setup${NC}"
echo -e "${CYAN}=========================${NC}"

# Check if .env already exists
if [[ -f ".env" ]]; then
    echo -e "${YELLOW}⚠️  .env file already exists!${NC}"
    read -p "Do you want to overwrite it? (y/N): " -r overwrite
    if [[ ! $overwrite =~ ^[Yy]$ ]]; then
        echo -e "${RED}Setup cancelled.${NC}"
        exit 0
    fi
fi

# Check if .env.template exists
if [[ ! -f ".env.template" ]]; then
    echo -e "${RED}❌ .env.template not found! Please ensure you're in the project root directory.${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}📋 We'll help you create a secure .env file.${NC}"
echo -e "${YELLOW}⚠️  NEVER commit the .env file to version control!${NC}"
echo ""

# Generate JWT Secret
echo -e "${WHITE}🔑 Generating secure JWT secret...${NC}"
if command -v openssl &> /dev/null; then
    JWT_SECRET=$(openssl rand -base64 32)
    echo -e "${GREEN}✅ JWT secret generated using OpenSSL (256-bit)${NC}"
elif command -v python3 &> /dev/null; then
    JWT_SECRET=$(python3 -c "import secrets, base64; print(base64.b64encode(secrets.token_bytes(32)).decode())")
    echo -e "${GREEN}✅ JWT secret generated using Python (256-bit)${NC}"
else
    echo -e "${YELLOW}⚠️  Neither OpenSSL nor Python3 found. Using fallback method.${NC}"
    JWT_SECRET=$(head /dev/urandom | tr -dc A-Za-z0-9 | head -c 32)
    echo -e "${GREEN}✅ JWT secret generated (256-bit)${NC}"
fi

# Get user inputs
echo ""
echo -e "${WHITE}📝 Please provide the following information:${NC}"
echo ""

read -s -p "Database Password (for user 'pagatu'): " DB_PASSWORD
echo ""

read -s -p "PostgreSQL Root Password: " POSTGRES_PASSWORD
echo ""

read -p "Domain URL (default: http://localhost:8888): " DOMAIN_URL
DOMAIN_URL=${DOMAIN_URL:-http://localhost:8888}

# Email: di default Mailpit, nessuna credenziale.
# L'SMTP reale e' facoltativo e va richiesto esplicitamente.
echo ""
read -p "Use a real SMTP server instead of local Mailpit? (y/N): " USE_REAL_SMTP
MAIL_HOST=mailpit
MAIL_PORT=1025
MAIL_USERNAME=""
MAIL_PASSWORD=""
if [[ $USE_REAL_SMTP =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}A real SMTP will be used. Credentials stay in .env only.${NC}"
    read -p "  SMTP host (e.g. smtp.gmail.com): " MAIL_HOST
    read -p "  SMTP port (587 for Gmail): " MAIL_PORT
    read -p "  Username: " MAIL_USERNAME
    if [[ $MAIL_HOST == *gmail.com* ]]; then
        read -s -p "  Gmail app password (NOT your account password): " MAIL_PASSWORD
        echo ""
        echo -e "${YELLOW}  Create one at https://myaccount.google.com/apppasswords${NC}"
    else
        read -s -p "  Password: " MAIL_PASSWORD
        echo ""
    fi
else
    echo -e "${GREEN}Local Mailpit selected. Emails stay on this machine.${NC}"
fi

echo ""
echo -e "${WHITE}🔄 Creating .env file...${NC}"

# Create .env file from template
cp ".env.template" ".env"

# Replace placeholders with actual values
if command -v sed &> /dev/null; then
    # Use sed for replacements (works on Linux/macOS/Git Bash)
    sed -i.bak "s/CHANGE_ME_TO_SECURE_256_BIT_KEY/$JWT_SECRET/g" ".env"
    sed -i.bak "s/CHANGE_ME_TO_SECURE_DB_PASSWORD/$DB_PASSWORD/g" ".env"
    sed -i.bak "s/CHANGE_ME_TO_SECURE_ROOT_PASSWORD/$POSTGRES_PASSWORD/g" ".env"
    sed -i.bak "s|^MAIL_HOST=.*|MAIL_HOST=$MAIL_HOST|" ".env"
    sed -i.bak "s|^MAIL_PORT=.*|MAIL_PORT=$MAIL_PORT|" ".env"
    sed -i.bak "s|^MAIL_USERNAME=.*|MAIL_USERNAME=$MAIL_USERNAME|" ".env"
    sed -i.bak "s|^MAIL_PASSWORD=.*|MAIL_PASSWORD=$MAIL_PASSWORD|" ".env"
    sed -i.bak "s|http://localhost:8888|$DOMAIN_URL|g" ".env"
    rm ".env.bak" 2>/dev/null || true
else
    # Fallback: use a simple replacement method
    echo -e "${YELLOW}⚠️  sed not available. You'll need to manually edit .env file.${NC}"
fi

echo ""
echo -e "${GREEN}✅ .env file created successfully!${NC}"
echo ""
echo -e "${CYAN}🔍 Security Verification:${NC}"
echo -e "${GREEN}- JWT secret: 256-bit generated ✅${NC}"
echo -e "${GREEN}- Database passwords: Set ✅${NC}"
if [[ $USE_REAL_SMTP =~ ^[Yy]$ ]]; then
    echo -e "${GREEN}- SMTP: $MAIL_HOST ✅${NC}"
else
    echo -e "${GREEN}- SMTP: local Mailpit, no credentials ✅${NC}"
fi
echo -e "${GREEN}- .env file created: ✅${NC}"

# Check if .env is actually ignored by git
if git check-ignore -q ".env" 2>/dev/null; then
    echo -e "${GREEN}- .env ignored by git: ✅${NC}"
else
    echo -e "${YELLOW}- .env ignored by git: ⚠️  Missing!${NC}"
    echo -e "${YELLOW}  Add '*.env' to your .gitignore file!${NC}"
fi

echo ""
echo -e "${CYAN}🚀 Next Steps:${NC}"
echo -e "${WHITE}1. Review the .env file (but don't commit it!)${NC}"
echo -e "${WHITE}2. Start your services: docker-compose up -d${NC}"
echo -e "${WHITE}3. Check logs: docker-compose logs${NC}"
echo ""
echo -e "${CYAN}📖 For the full explanation, see the Getting Started section of README.md${NC}"
echo ""
echo -e "${YELLOW}⚠️  IMPORTANT REMINDERS:${NC}"
echo -e "${RED}- Revoke any Gmail app password you no longer use, and create a new one${NC}"
echo -e "${RED}-  https://myaccount.google.com/apppasswords${NC}"
echo -e "${RED}- Change database passwords if using an existing DB${NC}"
echo -e "${RED}- NEVER commit the .env file to version control${NC}"

# Clear sensitive variables
unset DB_PASSWORD
unset POSTGRES_PASSWORD
unset MAIL_PASSWORD
unset JWT_SECRET

echo ""
echo -e "${GREEN}🔒 Setup complete! Your secrets are secure.${NC}"
echo ""
echo -e "${CYAN}💡 Tip: Make this script executable with: chmod +x setup-env.sh${NC}"