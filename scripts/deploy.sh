#!/bin/bash

# 배포 스크립트
# 사용법: ./scripts/deploy.sh

set -e  # 에러 발생 시 즉시 종료

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 프로젝트 디렉토리
PROJECT_DIR=$(cd "$(dirname "$0")/.." && pwd)
LOG_FILE="$PROJECT_DIR/deploy.log"

cd "$PROJECT_DIR"

echo "========================================" | tee -a "$LOG_FILE"
echo "🚀 Deployment started at $(date)" | tee -a "$LOG_FILE"
echo "========================================" | tee -a "$LOG_FILE"

# 1. Git pull
echo -e "${YELLOW}📥 Pulling latest code...${NC}" | tee -a "$LOG_FILE"
git fetch origin
git pull origin main | tee -a "$LOG_FILE"

# 2. 환경 변수 확인
if [ ! -f ".env" ]; then
  echo -e "${RED}❌ .env file not found!${NC}" | tee -a "$LOG_FILE"
  echo -e "${YELLOW}💡 Creating .env from .env.example...${NC}" | tee -a "$LOG_FILE"
  cp .env.example .env
  echo -e "${YELLOW}⚠️  Please edit .env file with your settings${NC}" | tee -a "$LOG_FILE"
  exit 1
fi

# 3. Docker Compose down
echo -e "${YELLOW}🛑 Stopping existing containers...${NC}" | tee -a "$LOG_FILE"
docker compose down | tee -a "$LOG_FILE"

# 4. Docker 이미지 정리 (선택 사항)
echo -e "${YELLOW}🧹 Cleaning up old images...${NC}" | tee -a "$LOG_FILE"
docker image prune -f | tee -a "$LOG_FILE"

# 5. Docker Compose up
echo -e "${YELLOW}🐳 Building and starting containers...${NC}" | tee -a "$LOG_FILE"
docker compose up --build -d | tee -a "$LOG_FILE"

# 6. Wait for services
echo -e "${YELLOW}⏳ Waiting for services to start...${NC}" | tee -a "$LOG_FILE"
sleep 15

# 7. Health check
echo -e "${YELLOW}✅ Checking service health...${NC}" | tee -a "$LOG_FILE"
MAX_RETRIES=10
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
  if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Health check passed!${NC}" | tee -a "$LOG_FILE"
    echo "" | tee -a "$LOG_FILE"
    echo -e "${GREEN}📊 Container Status:${NC}" | tee -a "$LOG_FILE"
    docker compose ps | tee -a "$LOG_FILE"
    echo "" | tee -a "$LOG_FILE"
    echo "========================================" | tee -a "$LOG_FILE"
    echo -e "${GREEN}🎉 Deployment successful at $(date)${NC}" | tee -a "$LOG_FILE"
    echo "========================================" | tee -a "$LOG_FILE"
    exit 0
  fi

  RETRY_COUNT=$((RETRY_COUNT + 1))
  echo -e "${YELLOW}⏳ Retry $RETRY_COUNT/$MAX_RETRIES...${NC}" | tee -a "$LOG_FILE"
  sleep 5
done

# Health check failed
echo -e "${RED}❌ Health check failed after $MAX_RETRIES retries${NC}" | tee -a "$LOG_FILE"
echo "" | tee -a "$LOG_FILE"
echo -e "${RED}📋 Container Status:${NC}" | tee -a "$LOG_FILE"
docker compose ps | tee -a "$LOG_FILE"
echo "" | tee -a "$LOG_FILE"
echo -e "${RED}📋 Backend Logs (last 50 lines):${NC}" | tee -a "$LOG_FILE"
docker compose logs backend | tail -n 50 | tee -a "$LOG_FILE"
echo "" | tee -a "$LOG_FILE"
echo -e "${RED}📋 MySQL Logs (last 20 lines):${NC}" | tee -a "$LOG_FILE"
docker compose logs mysql | tail -n 20 | tee -a "$LOG_FILE"

exit 1
