package com.example.receiptify.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.receiptify.R
import com.example.receiptify.auth.FirebaseAuthManager
import com.example.receiptify.databinding.ActivityLoginBinding
import com.example.receiptify.repository.UserRepository
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
    private lateinit var prefs: SharedPreferences
    private lateinit var userRepository: UserRepository

    private var isNavigating = false

    companion object {
        private const val TAG = "LoginActivity"
        private const val RC_GOOGLE_SIGN_IN = 9001
        private const val PREFS_NAME = "receiptify_auth"
        private const val KEY_NAVER_LOGGED_IN = "naver_logged_in"

        private const val GOOGLE_WEB_CLIENT_ID = "763595991477-k7es3foiml6lknn646mqk7fnehhqd0d8.apps.googleusercontent.com"
        private const val NAVER_CLIENT_ID = "4_hKHdQVR0VetSVY9IHn"
        private const val NAVER_CLIENT_SECRET = "ktALIseJP6"
        private const val NAVER_CLIENT_NAME = "Receiptify"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "=================================================")
        Log.d(TAG, "onCreate started")
        Log.d(TAG, "Intent: ${intent}")
        Log.d(TAG, "Intent Data: ${intent?.data}")
        Log.d(TAG, "=================================================")

        authManager = FirebaseAuthManager.getInstance()
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        userRepository = UserRepository()

        setupGoogleSignIn()
        setupNaverSignIn()

        // 🔍 인텐트 데이터 확인 (네이버 콜백)
        handleNaverOAuthCallback()

        checkLoginStatusAndProceed()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG, "=================================================")
        Log.d(TAG, "onNewIntent called")
        Log.d(TAG, "Intent: ${intent}")
        Log.d(TAG, "Intent Data: ${intent.data}")
        Log.d(TAG, "=================================================")
        setIntent(intent)
        handleNaverOAuthCallback()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "=================================================")
        Log.d(TAG, "onResume called")
        Log.d(TAG, "Intent: ${intent}")
        Log.d(TAG, "Intent Data: ${intent?.data}")
        Log.d(TAG, "=================================================")

        // OAuth 콜백 처리
        handleNaverOAuthCallback()
    }

    private fun handleNaverOAuthCallback() {
        val uri = intent?.data
        Log.d(TAG, "=================================================")
        Log.d(TAG, "handleNaverOAuthCallback called")
        Log.d(TAG, "Intent Data URI: $uri")

        if (uri != null) {
            Log.d(TAG, "URI Details:")
            Log.d(TAG, "  Scheme: ${uri.scheme}")
            Log.d(TAG, "  Host: ${uri.host}")
            Log.d(TAG, "  Path: ${uri.path}")
            Log.d(TAG, "  Query: ${uri.query}")

            // 패키지명 기반 또는 naverlogin 둘 다 처리
            val isNaverCallback = (uri.scheme == "com.example.receiptify" || uri.scheme == "naverlogin")
                    && uri.host == "oauth"

            Log.d(TAG, "Is Naver Callback: $isNaverCallback")

            if (isNaverCallback) {
                Log.d(TAG, "✅✅✅ Naver OAuth callback detected! ✅✅✅")

                // 토큰 확인
                val token = NaverIdLoginSDK.getAccessToken()
                Log.d(TAG, "Access Token: ${if (token != null) "EXISTS (${token.take(20)}...)" else "NULL"}")

                if (token != null) {
                    Log.d(TAG, "✅ Token exists, calling getNaverUserProfile()")
                    getNaverUserProfile()
                } else {
                    Log.e(TAG, "❌ Token is null after OAuth callback")

                    // 약간의 딜레이 후 재시도
                    Handler(Looper.getMainLooper()).postDelayed({
                        val retryToken = NaverIdLoginSDK.getAccessToken()
                        Log.d(TAG, "Retry Token: ${if (retryToken != null) "EXISTS" else "NULL"}")

                        if (retryToken != null) {
                            Log.d(TAG, "✅ Token available on retry")
                            getNaverUserProfile()
                        } else {
                            Toast.makeText(
                                this,
                                "로그인 처리 중 오류가 발생했습니다",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }, 500)
                }
            } else {
                Log.d(TAG, "❌ Not a Naver OAuth callback")
            }
        } else {
            Log.d(TAG, "Intent data is null - no callback URI")
        }
        Log.d(TAG, "=================================================")
    }

    private fun checkLoginStatusAndProceed() {
        if (isNavigating) {
            Log.d(TAG, "Already navigating, skip checkLoginStatus")
            return
        }

        val firebaseUser = authManager.currentUser
        val naverLoggedIn = prefs.getBoolean(KEY_NAVER_LOGGED_IN, false)
        val naverToken = NaverIdLoginSDK.getAccessToken()

        Log.d(TAG, "checkLoginStatus - Firebase: ${firebaseUser != null}, Naver Pref: $naverLoggedIn, Naver Token: ${naverToken != null}")

        // 🔥 중요: 이미 LoginActivity UI가 표시되었다면 자동 이동하지 않음
        if (::binding.isInitialized) {
            Log.d(TAG, "Login UI already initialized, staying on LoginActivity")
            return
        }

        if (firebaseUser != null || (naverLoggedIn && naverToken != null)) {
            Log.d(TAG, "User already logged in, navigating to HomeActivity directly")
            navigateToMain()
        } else {
            Log.d(TAG, "User not logged in, showing login screen")
            binding = ActivityLoginBinding.inflate(layoutInflater)
            setContentView(binding.root)
            setupClickListeners()
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
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (validateInput(email, password)) {
                loginWithEmail(email, password)
            }
        }

        binding.btnGoogleLogin.setOnClickListener {
            signInWithGoogle()
        }

        binding.btnNaverLogin.setOnClickListener {
            signInWithNaver()
        }

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
                    // ✨ MongoDB와 사용자 동기화
                    syncUserWithMongoDB()
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
        Log.d(TAG, "==================================================")
        Log.d(TAG, "🔵 Naver login button clicked")

        val naverLoggedIn = prefs.getBoolean(KEY_NAVER_LOGGED_IN, false)
        Log.d(TAG, "Naver logged in flag: $naverLoggedIn")

        if (naverLoggedIn) {
            Log.d(TAG, "✅ Already logged in (from pref) - Skipping authentication")
            Log.d(TAG, "==================================================")
            Toast.makeText(this, "이미 로그인되어 있습니다", Toast.LENGTH_SHORT).show()
            navigateToMain()
            return
        }

        Log.d(TAG, "❌ Not logged in - Starting Naver authentication")
        Log.d(TAG, "==================================================")

        val oauthLoginCallback = object : OAuthLoginCallback {
            override fun onSuccess() {
                Log.d(TAG, "=================================================")
                Log.d(TAG, "✅✅✅ Naver OAuth SUCCESS (Callback) ✅✅✅")
                val token = NaverIdLoginSDK.getAccessToken()
                Log.d(TAG, "Access Token: ${if (token != null) "EXISTS (${token.take(20)}...)" else "NULL"}")
                Log.d(TAG, "=================================================")

                if (token != null) {
                    getNaverUserProfile()
                } else {
                    Log.e(TAG, "❌ Token is null in onSuccess callback!")
                    runOnUiThread {
                        Toast.makeText(
                            this@LoginActivity,
                            "토큰을 받지 못했습니다",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            override fun onError(errorCode: Int, message: String) {
                Log.e(TAG, "=================================================")
                Log.e(TAG, "❌❌❌ Naver OAuth ERROR ❌❌❌")
                Log.e(TAG, "Error Code: $errorCode")
                Log.e(TAG, "Message: $message")
                Log.e(TAG, "=================================================")

                runOnUiThread {
                    Toast.makeText(
                        this@LoginActivity,
                        "네이버 로그인 실패: $message",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(httpStatus: Int, message: String) {
                Log.e(TAG, "=================================================")
                Log.e(TAG, "❌❌❌ Naver OAuth FAILURE ❌❌❌")
                Log.e(TAG, "HTTP Status: $httpStatus")
                Log.e(TAG, "Message: $message")
                Log.e(TAG, "=================================================")

                runOnUiThread {
                    Toast.makeText(
                        this@LoginActivity,
                        "네이버 로그인 실패",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

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
                    prefs.edit().putBoolean(KEY_NAVER_LOGGED_IN, true).apply()
                    Log.d(TAG, "✅ Naver login flag saved")

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
                // ✨ MongoDB와 사용자 동기화
                syncUserWithMongoDB()
            }.onFailure { exception ->
                Toast.makeText(
                    this@LoginActivity,
                    exception.message ?: getString(R.string.error_google_signin),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // ✨ 새로운 함수: MongoDB 사용자 동기화
    private fun syncUserWithMongoDB() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "🔄 MongoDB 사용자 동기화 시작...")

                val syncResult = userRepository.syncUser()

                syncResult.onSuccess { user ->
                    Log.d(TAG, "✅ MongoDB 동기화 완료: ${user.email}")

                    Toast.makeText(
                        this@LoginActivity,
                        "환영합니다, ${user.displayName ?: user.email}님!",
                        Toast.LENGTH_SHORT
                    ).show()

                    navigateToMain()

                }.onFailure { error ->
                    Log.e(TAG, "❌ MongoDB 동기화 실패", error)

                    Toast.makeText(
                        this@LoginActivity,
                        "로그인 성공! (동기화는 나중에 자동으로 됩니다)",
                        Toast.LENGTH_SHORT
                    ).show()

                    navigateToMain()
                }

            } catch (e: Exception) {
                Log.e(TAG, "동기화 중 오류", e)
                Toast.makeText(
                    this@LoginActivity,
                    getString(R.string.login_success),
                    Toast.LENGTH_SHORT
                ).show()
                navigateToMain()
            }
        }
    }

    private fun navigateToMain() {
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
            isNavigating = false

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