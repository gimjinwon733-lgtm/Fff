# 갤럭시 탭용 구글 지뢰찾기 자동 풀이 · Android 프로젝트

> **중요: 이 ZIP은 APK 설치 파일이 아니라 APK를 만들기 위한 Android 앱 소스코드입니다.**
> 현재 작업 환경에는 Android SDK / Gradle 빌드 도구와 실물 갤럭시 탭이 없어 APK 빌드 및 실기기 동작 검증을 마치지 못했습니다.

## 무엇이 들어 있나요?

- `app/src/main/java/com/example/minesautoplay/MainActivity.java` — 난이도 선택 및 처음 설정하는 앱 화면
- `AutoPlayService.java` — 접근성 화면 캡처, 터치 제어, 화면 위 조작창, 게임판 드래그 선택
- `BoardVision.java` — 구글 지뢰찾기 녹색/갈색 칸과 숫자 색상 인식
- `Solver.java` — 논리 추론, 부분집합 제약, 경계 영역의 가능한 지뢰 배치 열거, 확률 선택
- `.github/workflows/build-apk.yml` — GitHub Actions 자동 빌드 설정
- `SolverTest.java` — SDK 없이 검증 가능한 기초 논리 테스트

**테스트 완료**: `Solver.java`는 순수 Java 컴파일과 세 가지 논리 단위 테스트를 통과했습니다. 가상 8×10 게임판 40회에서 숫자 추론이 안전하다고 판정한 칸의 오류는 관측되지 않았습니다. 이 수치는 실제 구글 지뢰찾기 화면 인식이나 갤럭시 탭 자동 제어 성공을 의미하지 않습니다.

## APK 만들기

### A. 컴퓨터의 Android Studio가 있는 경우

1. Android Studio에서 이 폴더를 프로젝트로 엽니다.
2. Android SDK Platform 35 설치 및 Gradle Sync를 수행합니다 (JDK 17, Android Gradle Plugin 8.7.3, Gradle 8.9).
3. `Build > Build Bundle(s) / APK(s) > Build APK(s)`를 선택합니다.
4. `app/build/outputs/apk/debug/app-debug.apk` 를 갤럭시 탭에 전달합니다.

### B. 갤럭시 탭만 있는 경우 (GitHub Actions)

1. GitHub 계정으로 새 빈 저장소를 만들고, 압축을 푼 이 프로젝트 파일/폴더를 **경로 구조 그대로** 업로드합니다. `.github/workflows/build-apk.yml`도 포함해야 합니다. 모바일 브라우저에서는 파일/폴더 업로드가 불편할 수 있습니다.
2. 저장소의 **Actions** 탭에서 `APK 빌드`를 실행합니다. `main` 또는 `master` 브랜치로 업로드하면 자동 실행할 수도 있습니다.
3. 빌드가 성공하면 해당 실행의 **Artifacts** 항목에서 `minesweeper-debug-apk` 를 내려받습니다.
4. 내려받은 파일의 압축을 풀면 `app-debug.apk` 가 나옵니다.

이 방법은 APK를 얻기 위한 *빌드 과정*입니다. 테스트된 원클릭 설치 파일을 이미 제공하는 것은 아닙니다.

## APK 설치 후 사용

1. 앱 실행 → **접근성 서비스 켜기** → Android 설정에서 `지뢰찾기 자동 풀이 서비스`를 활성화합니다.
2. 앱에서 **초급 / 중급 / 고급** 난이도를 구글 지뢰찾기와 동일하게 설정합니다.
3. **구글 지뢰찾기 열기**를 선택하고 게임을 시작합니다.
4. 화면의 작은 조작창에서 **판 선택**을 누릅니다.
5. 초록색/갈색 **격자 전체의 바깥 모서리**를 시작점부터 끝점까지 드래그합니다. 위쪽 메뉴나 여백은 포함하지 마세요.
6. **시작**을 누르면 화면을 반복 인식하고 좌표를 터치합니다. **중지**를 누르면 멈춥니다.

**사용 후 Android 설정에서 접근성 서비스를 꺼 두는 것을 권장합니다.** 접근성 서비스는 앱 간 화면과 입력에 영향을 주는 민감한 기능입니다. 보안 정책으로 설정을 활성화할 수 없는 경우 이를 우회하지 마세요.

## 한계 / 주의

- 구글 검색 지뢰찾기의 기본 **녹색/갈색 테마**를 가정합니다. 게임이 업데이트되었거나 화면 배율/테마가 다르면 잘못 인식할 수 있습니다.
- Android 11 (API 30) 이상이 필요합니다.
- 소스에는 네트워크 권한이 없고 스크린샷을 저장하거나 외부에 업로드하는 코드는 없습니다.
- 확정적으로 풀 수 없는 장면에는 추측이 필요하며, 패배할 수 있습니다.
- 자동 인식이 의심스러우면 **중지** 버튼을 누르세요.
- 기기 및 Android 버전에 따라 외부 설치 앱의 접근성 권한 허용이 제한될 수 있습니다.
- 실제 APK 빌드·설치·갤럭시 탭 호환성은 아직 검증되지 않았습니다.

## 개발자 테스트

Android SDK 불필요:

```bash
javac -d output app/src/main/java/com/example/minesautoplay/Solver.java SolverTest.java
java -cp output SolverTest
```
