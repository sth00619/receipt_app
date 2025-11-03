# Keytool 설정 가이드 (Keytool Setup Guide)

## 문제 설명 (Problem Description)

`keytool` 명령어를 실행할 때 다음과 같은 오류가 발생하는 경우:

```
keytool : 'keytool' 용어가 cmdlet, 함수, 스크립트 파일 또는 실행할 수 있는 프로그램 이름으로 인식되지 않습니다.
```

이는 Java JDK의 bin 디렉토리가 시스템 PATH에 추가되지 않았기 때문입니다.

---

## 해결 방법 (Solutions)

### Windows 환경

#### 방법 1: Android Studio를 통한 keytool 사용

Android Studio에 포함된 JDK의 keytool을 직접 경로로 사용:

```powershell
# Android Studio의 JDK 경로 (버전에 따라 다를 수 있음)
& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

또는:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\jdk\bin\keytool.exe" -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

#### 방법 2: JDK 경로 찾기 및 환경 변수 설정

1. **JDK 설치 경로 찾기:**

   ```powershell
   # Android Studio JDK 경로 확인
   dir "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe"

   # 또는 일반 JDK 설치 경로
   dir "C:\Program Files\Java\jdk-*\bin\keytool.exe"
   ```

2. **환경 변수에 영구적으로 추가:**

   - `Win + R` 키를 누르고 `sysdm.cpl` 실행
   - "고급" 탭 → "환경 변수" 클릭
   - "시스템 변수"에서 "Path" 선택 → "편집" 클릭
   - "새로 만들기" 클릭하고 JDK bin 경로 추가:
     ```
     C:\Program Files\Android\Android Studio\jbr\bin
     ```
   - 확인을 눌러 모든 창 닫기
   - **PowerShell을 재시작**하여 변경사항 적용

3. **현재 세션에만 임시로 추가 (재시작 불필요):**

   ```powershell
   $env:Path += ";C:\Program Files\Android\Android Studio\jbr\bin"
   ```

#### 방법 3: 스크립트로 자동화

프로젝트 루트에 제공된 `get-keytool.ps1` 스크립트 사용:

```powershell
# 스크립트 실행 권한 허용 (관리자 권한 필요)
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser

# keytool 경로 찾기
.\get-keytool.ps1
```

---

### macOS/Linux 환경

#### 방법 1: Android Studio의 JDK 사용

```bash
# macOS
~/Library/Application\ Support/Google/AndroidStudio*/jbr/Contents/Home/bin/keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android

# Linux
~/android-studio/jbr/bin/keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

#### 방법 2: JAVA_HOME 설정

```bash
# ~/.bashrc 또는 ~/.zshrc 파일에 추가

# macOS
export JAVA_HOME=$(/usr/libexec/java_home)

# Linux (경로는 설치 환경에 따라 다름)
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

# PATH에 추가
export PATH=$JAVA_HOME/bin:$PATH
```

변경 후 터미널 재시작 또는:

```bash
source ~/.bashrc  # 또는 source ~/.zshrc
```

---

## 일반적인 keytool 사용 예제

### Debug Keystore 정보 확인

```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

### SHA1 및 SHA256 지문 확인 (Firebase, Google Maps API 등에 필요)

```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep -E "SHA1|SHA256"
```

Windows PowerShell:
```powershell
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | Select-String "SHA1|SHA256"
```

### Release Keystore 생성

```bash
keytool -genkey -v -keystore release.keystore -alias release -keyalg RSA -keysize 2048 -validity 10000
```

---

## 문제 해결 (Troubleshooting)

### "경로를 찾을 수 없습니다" 오류

경로에 공백이 있는 경우 따옴표로 감싸야 합니다:

```powershell
& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" [options]
```

### JDK를 찾을 수 없는 경우

1. Android Studio가 설치되어 있는지 확인
2. JDK가 별도로 설치되어 있는지 확인:
   - [Oracle JDK](https://www.oracle.com/java/technologies/downloads/)
   - [OpenJDK](https://adoptium.net/)

### 환경 변수 변경이 적용되지 않는 경우

- PowerShell/터미널을 완전히 종료하고 다시 시작
- Windows의 경우 로그아웃 후 다시 로그인
- 시스템 재시작

---

## 참고 자료 (References)

- [Android Developer - Sign your app](https://developer.android.com/studio/publish/app-signing)
- [Oracle - keytool Documentation](https://docs.oracle.com/javase/8/docs/technotes/tools/unix/keytool.html)
