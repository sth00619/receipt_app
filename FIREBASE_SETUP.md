# Firebase 설정 가이드

## 문제 원인

현재 `google-services.json` 파일에 OAuth 클라이언트 정보가 없어서 계정 생성 시 "An internal error occurred" 에러가 발생하고 있습니다.

## 해결 방법

### 1. Firebase Console에서 Authentication 설정

1. [Firebase Console](https://console.firebase.google.com/)에 접속
2. 프로젝트 선택: **receiptify-18b9d**
3. 왼쪽 메뉴에서 **Authentication** 클릭
4. **Sign-in method** 탭 선택
5. **이메일/비밀번호** 인증 방식 활성화
   - "Email/Password" 클릭
   - "Enable" 토글 켜기
   - "Save" 클릭

### 2. Firestore Database 설정

1. Firebase Console에서 **Firestore Database** 클릭
2. 데이터베이스 생성 (아직 생성하지 않은 경우)
3. **Rules** 탭에서 다음 규칙으로 설정:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // users 컬렉션: 인증된 사용자만 자신의 문서 읽기/쓰기 가능
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }

    // 기본적으로 모든 접근 차단
    match /{document=**} {
      allow read, write: if false;
    }
  }
}
```

### 3. Android 앱 SHA-1 인증서 지문 등록

1. 터미널에서 다음 명령어 실행하여 SHA-1 지문 획득:

**Debug 키스토어 (개발용):**
```bash
cd /home/user/receipt_app
./gradlew signingReport
```

또는:
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

2. SHA-1과 SHA-256 지문 복사
3. Firebase Console → Project Settings → Your apps → Android 앱 선택
4. "Add fingerprint" 클릭하여 SHA-1과 SHA-256 추가

### 4. 새로운 google-services.json 다운로드

1. Firebase Console → Project Settings → Your apps
2. Android 앱의 **"Download google-services.json"** 버튼 클릭
3. 다운로드한 파일로 기존 파일 교체:
   ```bash
   # 다운로드한 파일을 다음 경로에 복사
   cp ~/Downloads/google-services.json /home/user/receipt_app/app/google-services.json
   ```

### 5. 앱 재빌드 및 테스트

```bash
cd /home/user/receipt_app
./gradlew clean
./gradlew assembleDebug
```

또는 Android Studio에서:
- Build → Clean Project
- Build → Rebuild Project

### 6. 앱 재설치

기존 앱을 완전히 삭제하고 새로 설치:
```bash
adb uninstall com.example.receiptify
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 추가 확인 사항

### 로그 확인

계정 생성 시 에러 로그 확인:
```bash
adb logcat | grep -E "(FirebaseAuth|FirebaseAuthManager|Firestore)"
```

### 네트워크 연결 확인

- 에뮬레이터/기기가 인터넷에 연결되어 있는지 확인
- Firebase Console에서 프로젝트가 활성 상태인지 확인

### Google Play Services 확인

기기/에뮬레이터에 Google Play Services가 설치되어 있는지 확인

## 예상되는 에러 메시지와 해결 방법

| 에러 메시지 | 원인 | 해결 방법 |
|------------|------|----------|
| "An internal error occurred" | OAuth 클라이언트 미설정 | SHA-1 등록 및 google-services.json 재다운로드 |
| "PERMISSION_DENIED" | Firestore 규칙 문제 | Firestore Rules 설정 확인 |
| "Network error" | 인터넷 연결 문제 | 네트워크 연결 확인 |
| "com.google.android.gms.common.api.ApiException" | Play Services 문제 | 에뮬레이터에 Play Store가 있는 이미지 사용 |

## 문의사항

위 단계를 모두 수행했는데도 문제가 지속되면 다음을 확인해주세요:
1. Android Studio Logcat의 전체 에러 로그
2. Firebase Console의 Authentication 및 Firestore 설정 스크린샷
3. 사용 중인 에뮬레이터/기기 정보
