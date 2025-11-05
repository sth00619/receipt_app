package com.example.receiptify.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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

    // 무한 루프 방지를 위한 플래그
    private var isNavigating = false

    companion object {
        private const val TAG = "LoginActivity"
        private const val RC_GOOGLE_SIGN_IN = 9001

        // Google Web Client ID
        private const val GOOGLE_WEB_CLIENT_ID = "763595991477-k7es3foiml6lknn646mqk7fnehhqd0d8.apps.googleusercontent.com"

        // Naver OAuth
        private const val NAVER_CLIENT_ID = "4_hKHdQVR0VetSVY9IHn"
        private const val NAVER_CLIENT_SECRET = "ktALIseJP6"
        private const val NAVER_CLIENT_NAME = "Receiptify"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authManager = FirebaseAuthManager.getInstance()

        setupGoogleSignIn()
        setupNaverSignIn()
        setupClickListeners()

        // onCreate에서 한 번만 로그인 상태 확인
        checkLoginStatus()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent called")
        setIntent(intent)

        // Naver OAuth 콜백 처리
        handleNaverOAuthCallback()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called")

        // onResume에서는 로그인 상태 확인하지 않음 (무한 루프 방지)
        // 대신 onCreate와 onNewIntent에서만 확인
    }

    private fun checkLoginStatus() {
        // 이미 화면 전환 중이면 중복 체크 방지
        if (isNavigating) {
            Log.d(TAG, "Already navigating, skip checkLoginStatus")
            return
        }

        // Firebase 로그인 확인
        val firebaseUser = authManager.currentUser

        // Naver 로그인 확인
        val naverAccessToken = NaverIdLoginSDK.getAccessToken()

        Log.d(TAG, "checkLoginStatus - Firebase user: ${firebaseUser?.email}, Naver token: ${naverAccessToken != null}")

        if (firebaseUser != null || naverAccessToken != null) {
            Log.d(TAG, "User already logged in, navigating to HomeActivity")
            navigateToMain()
        }
    }

    private fun handleNaverOAuthCallback() {
        val uri = intent?.data
        if (uri != null && uri.scheme == "naverlogin" && uri.host == "oauth") {
            Log.d(TAG, "Naver OAuth callback detected: $uri")

            // 콜백 후 프로필 정보 가져오기
            getNaverUserProfile()
        }
    }

    private fun setupGoogleSignIn() {
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
        Log.d(TAG, "Naver SDK initialized")
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
        Log.d(TAG, "Naver login button clicked")

        val oauthLoginCallback = object : OAuthLoginCallback {
            override fun onSuccess() {
                Log.d(TAG, "Naver OAuth success - Access token received")

                // 프로필 정보 가져오기
                getNaverUserProfile()
            }

            override fun onError(errorCode: Int, message: String) {
                Log.e(TAG, "Naver OAuth error - errorCode: $errorCode, message: $message")
                runOnUiThread {
                    Toast.makeText(
                        this@LoginActivity,
                        "네이버 로그인 실패: $message",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(httpStatus: Int, message: String) {
                Log.e(TAG, "Naver OAuth failure - httpStatus: $httpStatus, message: $message")
                runOnUiThread {
                    Toast.makeText(
                        this@LoginActivity,
                        "네이버 로그인 실패",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // Naver 로그인 시작
        NaverIdLoginSDK.authenticate(this, oauthLoginCallback)
    }

    private fun getNaverUserProfile() {
        Log.d(TAG, "Getting Naver user profile...")

        NidOAuthLogin().callProfileApi(object : NidProfileCallback<NidProfileResponse> {
            override fun onSuccess(result: NidProfileResponse) {
                val userId = result.profile?.id
                val email = result.profile?.email
                val name = result.profile?.name

                Log.d(TAG, "=".repeat(50))
                Log.d(TAG, "Naver profile retrieved successfully!")
                Log.d(TAG, "User ID: $userId")
                Log.d(TAG, "Email: $email")
                Log.d(TAG, "Name: $name")
                Log.d(TAG, "=".repeat(50))

                runOnUiThread {
                    Toast.makeText(
                        this@LoginActivity,
                        "네이버 로그인 성공!\n환영합니다, ${name ?: "사용자"}님",
                        Toast.LENGTH_SHORT
                    ).show()

                    Log.d(TAG, "Calling navigateToMain()...")
                    navigateToMain()
                }
            }

            override fun onError(errorCode: Int, message: String) {
                Log.e(TAG, "Naver profile error - errorCode: $errorCode, message: $message")
                runOnUiThread {
                    Toast.makeText(
                        this@LoginActivity,
                        "프로필 정보를 가져오는데 실패했습니다",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(httpStatus: Int, message: String) {
                Log.e(TAG, "Naver profile failure - httpStatus: $httpStatus, message: $message")
                runOnUiThread {
                    Toast.makeText(
                        this@LoginActivity,
                        "프로필 정보를 가져오는데 실패했습니다",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
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
                    Toast.makeText(
                        this,
                        getString(R.string.error_google_signin),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: ApiException) {
                Log.e(TAG, "Google login failed", e)
                Toast.makeText(
                    this,
                    getString(R.string.error_google_signin),
                    Toast.LENGTH_SHORT
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
        // 이미 화면 전환 중이면 중복 실행 방지
        if (isNavigating) {
            Log.d(TAG, "Already navigating, skip duplicate call")
            return
        }

        isNavigating = true

        Log.d(TAG, "=".repeat(50))
        Log.d(TAG, "navigateToMain() CALLED")
        Log.d(TAG, "=".repeat(50))

        try {
            val intent = Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            Log.d(TAG, "Starting HomeActivity...")
            startActivity(intent)
            finish()
            Log.d(TAG, "HomeActivity started and LoginActivity finished")

        } catch (e: Exception) {
            isNavigating = false  // 실패 시 플래그 리셋

            Log.e(TAG, "=".repeat(50))
            Log.e(TAG, "ERROR starting HomeActivity: ${e.message}", e)
            Log.e(TAG, "=".repeat(50))

            runOnUiThread {
                Toast.makeText(
                    this,
                    "화면 전환 오류: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
