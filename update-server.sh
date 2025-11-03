#!/bin/bash
# Mac Mini 서버 빠른 업데이트 스크립트
# 변경사항이 적을 때 사용 (빌드 캐시 활용)
# 사용법: ./update-server.sh

set -e

echo "🔄 Quick Update - Pulling & Restarting..."
echo ""

git pull origin main

echo ""
echo "🔄 Restarting backend service..."
docker-compose up -d --no-deps --build backend

echo ""
echo "✅ Update complete!"
echo "📝 View logs: docker-compose logs -f backend"
