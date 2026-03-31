# 🌽 콘킾(ConKeep) - 모바일 쿠폰 관리 서비스
<img width="2134" height="1042" alt="스크린샷2_홈" src="https://github.com/user-attachments/assets/cf2eb8cd-bb19-4217-abae-6c383cc4a818" />

PlayStore : https://download.conkeep.com

## 📍 서비스 주제

콘킾(ConKeep)은 AI 기반의 모바일 모바일 쿠폰 관리 및 공유 서비스입니다. 
쿠폰 이미지를 업로드하면 자동으로 바코드를 인식하고 브랜드, 상품명, 유효기간, 금액을 추출하여 체계적으로 관리할 수 있습니다.

프로토타입: https://github.com/medAndro/con-keep-prototype

## 🎨 Design & Wireframe
Lovable 프로토타입  + Banani를 참고하여 Figma로 직접 제작하였습니다.
- [🔗 Figma에서 와이어프레임 보기](https://www.figma.com/design/m9CfDFeDHGlPfl509ZIcvM/ConKeep-%EC%BD%98%ED%82%BE?node-id=706-2)

## 🛠 Tech Stack

### Frontend (Android)
- **UI & Framework**: Jetpack Compose (Material 3) 기반 선언형 UI
- **Architecture**: MVVM + Google App Architecture (Domain/Data/UI 레이어 분리)
- **Navigation**: 차세대 탐색 엔진 **Navigation 3 (Compose-based)** 도입
- **DI**: Hilt를 통한 의존성 주입 및 모듈 간 결합도 해제
- **Networking**: **Ktor 3** (ByteReadChannel을 활용한 메모리 효율적 통신)
- **Persistence**: **Room** (Offline-first 전략 및 로컬 캐싱 구현)
- **Image Loading**: **Coil 3** (Ktor3 엔진 연동 및 이미지 파이프라인 최적화)
- **On-Device AI**: Google ML Kit (On-device Barcode Scanning)

### Backend & Cloud (Serverless)
- **BaaS**: **Supabase** (Auth, Postgrest, Realtime)
- **Compute**: Cloudflare Workers (Serverless Backend Logic)
- **Storage**: Cloudflare R2 (S3 Compatible) + **Presigned URL** 보안 업로드
- **Online AI**: OpenRouter & Gemini (이미지 내 쿠폰 정보 인식 및 Json 파싱)


### 💡 Architectural Insights
- **Navigation 3**: Compose와의 더 깊은 통합과 선언적 상태 관리를 경험하고, 안드로이드 탐색 엔진의 차세대 표준을 미리 학습하기 위해 채택했습니다.
- **Ktor**: Retrofit 대비 높은 커스텀 자유도와 향후 Kotlin Multiplatform (KMP)으로의 확장 가능성을 고려하여 Ktor 엔진을 선택했습니다.
- **OpenRouter Gemini**: 비용 효율성과 응답 속도 사이의 균형을 고려하였고, 백업 모델 전환 및 신뢰성을 이유로 오픈라우터를 사용했습니다.
---

