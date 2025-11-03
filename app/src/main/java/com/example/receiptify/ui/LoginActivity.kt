package com.example.receiptify.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.receiptify.MainActivity
import com.example.receiptify.R
import com.example.receiptify.auth.FirebaseAuthManager
import com.example.receiptify.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.NidOAuthLogin
import com.navercorp.nid.oauth.OAuthLoginCallback
import com.navercorp.nid.profile.NidProfileCallback
import com.navercorp.nid.profile.data.NidProfileResponse
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var authManager: FirebaseAuthManager
    private lateinit var googleSignInClient: GoogleSignInClient

    companion object {
        private const val TAG = "LoginActivity"
        private const val RC_GOOGLE_SIGN_IN = 9001

        /**
         * Google Sign-In 설정 가이드:
         *
         * 1. SHA-1 인증서 지문 추가:
         *    - 터미널에서: ./gradlew signingReport
         *    - SHA-1과 SHA-256 복사
         *
         * 2. Firebase Console 설정:
         *    - https://console.firebase.google.com/project/receiptify-18b9d
         *    - 프로젝트 설정 → Android 앱 → SHA 인증서 지문 추가
         *
         * 3. Google Sign-In 활성화:
         *    - Authentication → Sign-in method → Google 활성화
         *
         * 4. google-services.json 업데이트:
         *    - 프로젝트 설정에서 새 google-services.json 다운로드
         *    - app/ 폴더에 덮어쓰기
         *
         * 5. Web Client ID 추출:
         *    - google-services.json에서 "client_type": 3 인 client_id 찾기
         *    - 또는 Google Cloud Console → API 및 서비스 → 사용자 인증 정보
         *    - "웹 클라이언트" 또는 "Web client (auto created by Google Service)" ID 복사
         *    - 아래 GOOGLE_WEB_CLIENT_ID에 붙여넣기
         */
        // TODO: Firebase Console에서 발급받은 Web Client ID로 변경
        private const val GOOGLE_WEB_CLIENT_ID = "YOUR_GOOGLE_WEB_CLIENT_ID"

        // TODO: Naver Developers에서 발급받은 정보로 변경
        private const val NAVER_CLIENT_ID = "YOUR_NAVER_CLIENT_ID"
        private const val NAVER_CLIENT_SECRET = "YOUR_NAVER_CLIENT_SECRET"
        private const val NAVER_CLIENT_NAME = "Receiptify"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authManager = FirebaseAuthManager.getInstance()

        // 이미 로그인된 경우 MainActivity로 이동
        if (authManager.currentUser != null) {
            navigateToMain()
            return
        }

        setupGoogleSignIn()
        setupNaverSignIn()
        setupClickListeners()
    }

    private fun setupGoogleSignIn() {
        // Google Web Client ID 설정 확인
        if (GOOGLE_WEB_CLIENT_ID == "YOUR_GOOGLE_WEB_CLIENT_ID") {
            Log.e(TAG, "Google Web Client ID가 설정되지 않았습니다. Firebase Console에서 Web Client ID를 확인하세요.")
            // 사용자에게 알림을 표시하지 않고 로그만 남김 (개발자용)
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(GOOGLE_WEB_CLIENT_ID)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun setupNaverSignIn() {
        NaverIdLoginSDK.initialize(
            this,
            NAVER_CLIENT_ID,
            NAVER_CLIENT_SECRET,
            NAVER_CLIENT_NAME
        )
    }

    private fun setupClickListeners() {
        // 이메일 로그인
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (validateInput(email, password)) {
                loginWithEmail(email, password)
            }
        }

        // Google 로그인
        binding.btnGoogleLogin.setOnClickListener {
            signInWithGoogle()
        }

        // Naver 로그인
        binding.btnNaverLogin.setOnClickListener {
            signInWithNaver()
        }

        // 회원가입 화면으로 이동
        binding.tvSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        when {
            email.isEmpty() -> {
                binding.emailInputLayout.error = getString(R.string.error_empty_email)
                return false
            }
            !authManager.isValidEmail(email) -> {
                binding.emailInputLayout.error = getString(R.string.error_invalid_email)
                return false
            }
            password.isEmpty() -> {
                binding.passwordInputLayout.error = getString(R.string.error_empty_password)
                return false
            }
            else -> {
                binding.emailInputLayout.error = null
                binding.passwordInputLayout.error = null
                return true
            }
        }
    }

    private fun loginWithEmail(email: String, password: String) {
        lifecycleScope.launch {
            try {
                binding.btnLogin.isEnabled = false

                val result = authManager.signInWithEmail(email, password)

                result.onSuccess {
                    Toast.makeText(
                        this@LoginActivity,
                        getString(R.string.login_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    navigateToMain()
                }.onFailure { exception ->
                    Toast.makeText(
                        this@LoginActivity,
                        exception.message ?: getString(R.string.error_login_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                binding.btnLogin.isEnabled = true
            }
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN)
    }

    private fun signInWithNaver() {
        val oauthLoginCallback = object : OAuthLoginCallback {
            override fun onSuccess() {
                // 로그인 성공 후 사용자 프로필 가져오기
                NidOAuthLogin().callProfileApi(object : NidProfileCallback<NidProfileResponse> {
                    override fun onSuccess(result: NidProfileResponse) {
                        val email = result.profile?.email
                        val name = result.profile?.name

                        Log.d(TAG, "Naver 로그인 성공 - Email: $email, Name: $name")

                        // TODO: Naver 계정을 Firebase와 연동하는 로직 구현
                        // 현재는 단순히 MainActivity로 이동
                        Toast.makeText(
                            this@LoginActivity,
                            getString(R.string.login_success),
                            Toast.LENGTH_SHORT
                        ).show()
                        navigateToMain()
                    }

                    override fun onError(errorCode: Int, message: String) {
                        Log.e(TAG, "Naver 프로필 가져오기 실패: $message")
                        Toast.makeText(
                            this@LoginActivity,
                            getString(R.string.error_naver_signin),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    override fun onFailure(httpStatus: Int, message: String) {
                        Log.e(TAG, "Naver 프로필 가져오기 실패: $message")
                        Toast.makeText(
                            this@LoginActivity,
                            getString(R.string.error_naver_signin),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            }

            override fun onError(errorCode: Int, message: String) {
                Log.e(TAG, "Naver 로그인 실패: $message")
                Toast.makeText(
                    this@LoginActivity,
                    getString(R.string.error_naver_signin),
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onFailure(httpStatus: Int, message: String) {
                Log.e(TAG, "Naver 로그인 실패: $message")
                Toast.makeText(
                    this@LoginActivity,
                    getString(R.string.error_naver_signin),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        NaverIdLoginSDK.authenticate(this, oauthLoginCallback)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_GOOGLE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken

                if (idToken != null) {
                    firebaseAuthWithGoogle(idToken)
                } else {
                    Log.e(TAG, "Google Sign-In ID Token이 null입니다. Firebase 설정을 확인하세요.")
                    Toast.makeText(
                        this,
                        "Google 로그인 설정 오류: ID Token을 받을 수 없습니다.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: ApiException) {
                val errorMessage = when (e.statusCode) {
                    10 -> "Google Play Services가 업데이트되지 않았거나 설정이 잘못되었습니다. (Error: DEVELOPER_ERROR)\n\nFirebase Console에서:\n1. SHA-1 인증서 추가\n2. google-services.json 업데이트\n3. Google Sign-In 활성화"
                    12501 -> "Google 로그인이 취소되었습니다."
                    7 -> "네트워크 연결을 확인하세요."
                    else -> "Google 로그인 실패 (Code: ${e.statusCode})"
                }

                Log.e(TAG, "Google 로그인 실패 - StatusCode: ${e.statusCode}, Message: ${e.message}", e)
                Toast.makeText(
                    this,
                    errorMessage,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        lifecycleScope.launch {
            val result = authManager.signInWithGoogle(idToken)

            result.onSuccess {
                Toast.makeText(
                    this@LoginActivity,
                    getString(R.string.login_success),
                    Toast.LENGTH_SHORT
                ).show()
                navigateToMain()
            }.onFailure { exception ->
                Toast.makeText(
                    this@LoginActivity,
                    exception.message ?: getString(R.string.error_google_signin),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}