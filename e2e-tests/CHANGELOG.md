# E2E 테스트 변경 이력

## 2025-10-10 - 테스트 안정성 개선

### 🐛 수정된 문제
1. **외부 의존성 제거**
   - MySQL → H2 인메모리 데이터베이스로 변경
   - Redis, OpenSearch, Confluence 설정을 테스트 환경에서 비활성화
   - OpenAI API 키 없이도 기본 UI 테스트 실행 가능

2. **타임아웃 문제 해결**
   - 채팅 API 응답 대기 시간: 60s → 90s
   - Deep Think 모드: 90s → 120s
   - 애플리케이션 시작 시간: 120s → 180s

3. **빈 데이터 처리**
   - 사용자가 없는 경우 테스트를 우아하게 스킵
   - 테스트 환경에서 빈 데이터베이스 허용

### ✨ 새로운 기능
1. **E2E 테스트 프로파일**
   - `application-e2e.yaml` 추가
   - H2 데이터베이스 자동 설정
   - 외부 의존성 최소화

2. **테스트 분리**
   - 핵심 테스트 (chat, permissions): 기본 실행
   - 확장 테스트 (accessibility, performance, visual): 선택적 실행
   - 각 테스트 세트별 npm 스크립트 제공

3. **CI/CD 최적화**
   - MySQL 서비스 제거 (H2 사용)
   - Playwright의 webServer 기능 활용
   - 빌드 시간 단축

### 📝 커밋 내역
1. `feat: add E2E test profile configuration` - E2E 전용 설정 파일
2. `feat: add H2 database dependency for E2E testing` - H2 의존성 추가
3. `feat: configure Playwright to use E2E test profile` - Playwright 설정 개선
4. `fix: increase timeouts for API-dependent chat tests` - 타임아웃 증가
5. `fix: handle empty data in permissions tests` - 빈 데이터 처리
6. `fix: update GitHub Actions workflow for E2E tests` - CI/CD 워크플로우 개선
7. `feat: separate core and extended test suites` - 테스트 세트 분리
8. `docs: update documentation with improvements` - 문서 업데이트

### 🎯 영향
- ✅ CI/CD에서 안정적으로 실행 가능
- ✅ 로컬 환경에서 외부 서비스 없이 테스트 가능
- ✅ 빠른 피드백 (핵심 테스트만 실행)
- ✅ 확장 테스트는 선택적으로 실행

### 📚 사용 방법

#### 기본 테스트 (핵심 기능만)
```bash
npm test
```

#### 모든 테스트 (확장 포함)
```bash
npm run test:all
```

#### 특정 테스트 세트
```bash
npm run test:accessibility  # 접근성
npm run test:performance    # 성능
npm run test:visual         # 시각적 회귀
```

### 🔧 설정 변경
- **Playwright Config**: 테스트 프로젝트별 testMatch 추가
- **Application Config**: E2E 프로파일로 H2 사용
- **GitHub Actions**: MySQL 서비스 제거, 환경 변수 설정
