#!/bin/bash

# Playwright E2E 테스트 환경 설정 스크립트

set -e

echo "🚀 Playwright E2E 테스트 환경을 설정합니다..."

# Node.js 버전 확인
if ! command -v node &> /dev/null; then
    echo "❌ Node.js가 설치되어 있지 않습니다."
    echo "Node.js 18 이상을 설치해주세요: https://nodejs.org/"
    exit 1
fi

NODE_VERSION=$(node -v | cut -d'v' -f2 | cut -d'.' -f1)
if [ "$NODE_VERSION" -lt 18 ]; then
    echo "❌ Node.js 버전이 18 미만입니다. (현재: $(node -v))"
    echo "Node.js 18 이상을 설치해주세요."
    exit 1
fi

echo "✅ Node.js 버전: $(node -v)"

# npm 확인
if ! command -v npm &> /dev/null; then
    echo "❌ npm이 설치되어 있지 않습니다."
    exit 1
fi

echo "✅ npm 버전: $(npm -v)"

# 의존성 설치
echo ""
echo "📦 npm 패키지를 설치합니다..."
npm install

# Playwright 브라우저 설치
echo ""
echo "🌐 Playwright 브라우저를 설치합니다..."
npx playwright install

# Linux의 경우 시스템 의존성도 설치
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    echo ""
    echo "🐧 Linux 시스템 의존성을 설치합니다..."
    npx playwright install-deps || {
        echo "⚠️  시스템 의존성 설치에 실패했습니다."
        echo "sudo 권한으로 다시 시도하거나 수동으로 설치해주세요:"
        echo "  sudo npx playwright install-deps"
    }
fi

echo ""
echo "✅ 설정이 완료되었습니다!"
echo ""
echo "다음 명령어로 테스트를 실행할 수 있습니다:"
echo "  npm test                # 모든 테스트 실행"
echo "  npm run test:ui         # UI 모드로 실행"
echo "  npm run test:headed     # 브라우저 창을 보며 실행"
echo "  npm run test:chat       # 채팅 테스트만 실행"
echo "  npm run test:permissions # 권한 테스트만 실행"
echo ""
echo "자세한 내용은 README.md를 참고하세요."
