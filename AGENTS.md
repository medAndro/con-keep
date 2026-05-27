## 프로젝트 구조
- MVVM + App Architecture
- presentation / domain / data 레이어 분리
- Hilt 의존성 주입
- Jetpack Compose UI (XML 절대 금지)
- StateFlow / SharedFlow 상태 관리

## 코딩 규칙
- Kotlin(KtLint) 코딩 컨벤션 준수
- 코루틴 + Flow 비동기 처리
- ViewModel에 Android Context 직접 참조 금지

## 인증/세션 관련 
- 토큰 갱신 로직은 AuthRepository에만 존재해야 함
- 자동 로그인 상태는 DataStore에 저장, SharedPreferences 혼용 금지
- FCM 토큰은 로그인 완료 후 반드시 재등록

## 테스트
- 버그 수정 시 재현 단위 테스트 작성 필수
- MockK Junit5 Robolectric 사용

## 작업 완료 절차
- 코드 변경 후 `./gradlew ktlintMainSourceSetFormat` 실행하여 KtLint 검증 통과 필수
- 검증 통과 후 기존 커밋 메시지 양식(`feat: ...`, `fix: ...`, `chore: ...`)에 맞춰 커밋 메시지 작성
- 커밋 전 `git status`로 의도하지 않은 변경 포함 여부 확인 후 커밋
