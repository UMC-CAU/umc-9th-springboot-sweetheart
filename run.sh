#!/bin/bash
# git bash 환경에서 실행해야 함
# 로컬 환경에서 실행할 때 사용하는 것
# 배포 환경에서 사용하는 것은 아님

# .env.local 파일 로드
if [ -f .env.local ]; then
    echo "Loading .env.local..."
    export $(cat .env.local | grep -v '^#' | xargs)
fi

# Spring Boot 실행
echo "Starting Spring Boot application..."
./gradlew bootRun
