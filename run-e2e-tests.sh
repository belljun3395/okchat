#!/bin/bash

# E2E 테스트 실행 스크립트
# 프로젝트 루트에서 실행하세요

set -e

echo "🧪 OKChat E2E 테스트를 시작합니다..."
echo ""

# e2e-tests 디렉토리 확인
if [ ! -d "e2e-tests" ]; then
    echo "❌ e2e-tests 디렉토리를 찾을 수 없습니다."
    echo "프로젝트 루트에서 이 스크립트를 실행해주세요."
    exit 1
fi

# Node.js 확인
if ! command -v node &> /dev/null; then
    echo "❌ Node.js가 설치되어 있지 않습니다."
    echo "https://nodejs.org/ 에서 Node.js를 설치해주세요."
    exit 1
fi

# 의존성 확인
if [ ! -d "e2e-tests/node_modules" ]; then
    echo "📦 의존성을 먼저 설치합니다..."
    cd e2e-tests
    npm install
    npx playwright install chromium
    cd ..
    echo "✅ 설치 완료!"
    echo ""
fi

# 애플리케이션 실행 확인
echo "🔍 애플리케이션이 실행 중인지 확인합니다..."
if curl -s http://localhost:8080 > /dev/null 2>&1; then
    echo "✅ 애플리케이션이 이미 실행 중입니다."
    APP_RUNNING=true
else
    echo "⚠️  애플리케이션이 실행 중이지 않습니다."
    echo "Playwright가 자동으로 애플리케이션을 시작합니다."
    echo "(또는 별도 터미널에서 './gradlew bootRun'을 실행하세요)"
    APP_RUNNING=false
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# 테스트 모드 선택
if [ "$1" == "" ]; then
    echo "테스트 모드를 선택하세요:"
    echo ""
    echo "  1) UI 모드 (시각적 디버깅) - 추천!"
    echo "  2) 헤드리스 모드 (모든 테스트)"
    echo "  3) 헤드 모드 (브라우저 보기)"
    echo "  4) 채팅 테스트만"
    echo "  5) 권한 테스트만"
    echo "  6) Chrome에서만"
    echo "  7) 종료"
    echo ""
    read -p "선택 (1-7): " choice
else
    choice=$1
fi

cd e2e-tests

case $choice in
    1|ui)
        echo "🎨 UI 모드로 실행합니다..."
        npm run test:ui
        ;;
    2|headless)
        echo "🤖 헤드리스 모드로 실행합니다..."
        npm test
        ;;
    3|headed)
        echo "👀 헤드 모드로 실행합니다..."
        npm run test:headed
        ;;
    4|chat)
        echo "💬 채팅 테스트만 실행합니다..."
        npm run test:chat
        ;;
    5|permissions)
        echo "🔐 권한 테스트만 실행합니다..."
        npm run test:permissions
        ;;
    6|chrome)
        echo "🌐 Chrome에서만 실행합니다..."
        npm run test:chrome
        ;;
    7|exit)
        echo "👋 종료합니다."
        exit 0
        ;;
    *)
        echo "❌ 잘못된 선택입니다."
        exit 1
        ;;
esac

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ 테스트 실행 완료!"
echo ""
echo "📊 리포트를 보려면: npm run report"
echo "📁 결과 위치: e2e-tests/playwright-report/"
echo ""
