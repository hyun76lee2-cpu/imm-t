# IMM 진도계 (안드로이드)

스마트폰 가속도 센서로 IMM 진도를 실시간 계산하는 앱.
화면과 계산은 `app/src/main/assets/imm-meter.html`(웹 앱과 같은 코드)에 있고,
`MainActivity.java`는 센서를 최고 속도(보통 200 Hz)로 읽어 100 ms마다 넘겨준다.

## 방법 1: GitHub에서 자동 빌드 (설치할 것 없음)

1. GitHub에 새 저장소를 만든다.
2. 이 폴더의 파일을 전부 올린다. 웹 업로드에서 `.github` 폴더가 빠지는 경우가 있으니,
   올린 뒤 `Add file > Create new file`에서 경로를 `.github/workflows/build.yml`로 입력하고
   이 폴더의 같은 파일 내용을 붙여넣어 만든다.
3. 저장소의 `Actions` 탭에서 `Build APK`가 끝나기를 기다린다 (몇 분).
   자동으로 안 돌면 `Build APK` 선택 후 `Run workflow`.
4. 완료된 실행을 열고 아래 `Artifacts`의 `imm-meter-apk`를 받아 압축을 풀면 APK가 있다.
5. 폰으로 APK를 옮겨 설치한다 (출처를 알 수 없는 앱 설치 허용 필요).

## 방법 2: Android Studio

폴더를 열고 동기화가 끝나면 `Build > Build App Bundle(s) / APK(s) > Build APK(s)`.

## 화면만 고칠 때

`assets/imm-meter.html`만 바꾸고 다시 빌드하면 된다.
같은 파일을 폰 브라우저에서 열면 브라우저 센서(낮은 샘플링)로도 동작한다.
