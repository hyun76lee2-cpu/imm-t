# IMM 진도계 (안드로이드)

스마트폰 가속도 센서로 IMM 진도를 실시간 계산하는 앱.
화면과 계산은 `src/main/assets/imm-meter.html`(웹 앱과 같은 코드)에 있고,
`MainActivity.java`는 센서를 최고 속도(보통 200 Hz)로 읽어 100 ms마다 넘겨준다.

## 저장소에 올라가 있어야 하는 구조

```
.github/workflows/build.yml
build.gradle.kts
settings.gradle.kts
gradle.properties
gradle/wrapper/gradle-wrapper.properties
src/main/AndroidManifest.xml
src/main/assets/imm-meter.html
src/main/java/com/imm/meter/MainActivity.java
```

GitHub 웹 업로드에서는 파일을 하나씩 고르지 말고, 압축을 푼 폴더 안의 내용을
폴더째 업로드 창에 끌어다 놓아야 폴더 구조가 유지된다.
`.github` 폴더는 숨김 폴더라 빠지기 쉬우니, 빠졌으면 `Add file > Create new file`에서
경로를 `.github/workflows/build.yml`로 입력하고 내용을 붙여넣는다.

## 빌드와 설치

1. 저장소의 `Actions` 탭에서 `Build APK` 실행이 끝나기를 기다린다.
   자동으로 안 돌면 `Build APK` 선택 후 `Run workflow`.
2. 실패하면 `Show project files` 단계의 출력이 위 구조와 같은지 먼저 확인한다.
3. 성공한 실행 화면 아래 `Artifacts`의 `imm-meter-apk`를 받아 압축을 풀고 폰에 설치한다
   (출처를 알 수 없는 앱 설치 허용 필요).

Android Studio가 있으면 폴더를 열고 `Build > Build App Bundle(s) / APK(s) > Build APK(s)`.
