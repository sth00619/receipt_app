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
import com.example.receiptify.databinding.ActivityLoginBinding
import com.example.receiptify.repository.AuthRepository
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
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var prefs: SharedPreferences
    private lateinit var authRepository: AuthRepository
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

        Log.d(TAG, "onCreate started")

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        authRepository = AuthRepository(this)
        userRepository = UserRepository()

        setupGoogleSignIn()
        setupNaverSignIn()

        // 네이버 OAuth 콜백 처리
        handleNaverOAuthCallback()

        checkLoginStatusAndProceed()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent called")
        setIntent(intent)
        handleNaverOAuthCallback()
    }

    override fun onResume() {
        super.onResume()
        handleNaverOAuthCallback()
    }

    private fun handleNaverOAuthCallback() {
        val uri = intent?.data
        if (uri != null) {
            val isNaverCallback = (uri.scheme == "com.example.receiptify" || uri.scheme == "naverlogin")
                    && uri.host == "oauth"

            if (isNaverCallback) {
                Log.d(TAG, "✅ Naver OAuth callback detected!")

                val token = NaverIdLoginSDK.getAccessToken()
                if (token != null) {
                    Log.d(TAG, "✅ Token exists, calling getNaverUserProfile()")
                    getNaverUserProfile()
                } else {
                    Handler(Looper.getMainLooper()).postDelayed({
                        val retryToken = NaverIdLoginSDK.getAccessToken()
                        if (retryToken != null) {
                            getNaverUserProfile()
                        } else {
                            Toast.makeText(this, "로그인 처리 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
                        }
                    }, 500)
                }
            }
        }
    }

    private fun checkLoginStatusAndProceed() {
        if (isNavigating) {
            Log.d(TAG, "Already navigating, skip checkLoginStatus")
            return
        }

        // JWT 토큰이 있는지 확인
        val isLoggedIn = authRepository.isLoggedIn()
        val naverLoggedIn = prefs.getBoolean(KEY_NAVER_LOGGED_IN, false)
        val naverToken = NaverIdLoginSDK.getAccessToken()

        Log.d(TAG, "checkLoginStatus - Email Login: $isLoggedIn, Naver Pref: $naverLoggedIn, Naver Token: ${naverToken != null}")

        if (::binding.isInitialized) {
            Log.d(TAG, "Login UI already initialized, staying on LoginActivity")
            return
        }

        if (isLoggedIn || (naverLoggedIn && naverToken != null)) {
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
        // 일반 로그인
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

        // 회원가입 이동
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
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
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

    /**
     * 일반 이메일 로그인
     */
    private fun loginWithEmail(email: String, password: String) {
        lifecycleScope.launch {
            try {
                binding.btnLogin.isEnabled = false

                val result = authRepository.login(email, password)

                result.onSuccess { userData ->
                    Log.d(TAG, "✅ 로그인 성공: ${userData.email}")
                    Toast.makeText(
                        this@LoginActivity,
                        "환영합니다, ${userData.displayName ?: userData.email}님!",
                        Toast.LENGTH_SHORT
                    ).show()
                    navigateToMain()
                }.onFailure { exception ->
                    Log.e(TAG, "❌ 로그인 실패", exception)
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
        Log.d(TAG, "🔵 Naver login button clicked")

        val naverLoggedIn = prefs.getBoolean(KEY_NAVER_LOGGED_IN, false)
        if (naverLoggedIn) {
            Log.d(TAG, "✅ Already logged in (from pref) - Skipping authentication")
            Toast.makeText(this, "이미 로그인되어 있습니다", Toast.LENGTH_SHORT).show()
            navigateToMain()
            return
        }

        val oauthLoginCallback = object : OAuthLoginCallback {
            override fun onSuccess() {
                Log.d(TAG, "✅ Naver OAuth SUCCESS")
                val token = NaverIdLoginSDK.getAccessToken()
                if (token != null) {
                    getNaverUserProfile()
                } else {
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "토큰을 받지 못했습니다", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onError(errorCode: Int, message: String) {
                Log.e(TAG, "❌ Naver OAuth ERROR: $message")
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "네이버 로그인 실패: $message", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(httpStatus: Int, message: String) {
                Log.e(TAG, "❌ Naver OAuth FAILURE: $message")
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "네이버 로그인 실패", Toast.LENGTH_SHORT).show()
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

                Log.d(TAG, "Naver profile retrieved: $email")

                runOnUiThread {
                    prefs.edit().putBoolean(KEY_NAVER_LOGGED_IN, true).apply()

                    Toast.makeText(
                        this@LoginActivity,
                        "네이버 로그인 성공!\n환영합니다, ${name ?: "사용자"}님",
                        Toast.LENGTH_SHORT
                    ).show()

                    navigateToMain()
                }
            }

            override fun onError(errorCode: Int, message: String) {
                Log.e(TAG, "Naver profile error: $message")
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "프로필 정보를 가져오는데 실패했습니다", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(httpStatus: Int, message: String) {
                Log.e(TAG, "Naver profile failure: $message")
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "프로필 정보를 가져오는데 실패했습니다", Toast.LENGTH_SHORT).show()
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
                    // Google 로그인은 Firebase 방식 유지
                    // (Firebase Auth Manager 사용)
                    Toast.makeText(this, "Google 로그인 - Firebase 인증", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, getString(R.string.error_google_signin), Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                Log.e(TAG, "Google login failed", e)
                Toast.makeText(this, getString(R.string.error_google_signin), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToMain() {
        if (isNavigating) {
            Log.d(TAG, "Already navigating, skip duplicate call")
            return
        }

        isNavigating = true
        Log.d(TAG, "navigateToMain() CALLED")

        try {
            val intent = Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            startActivity(intent)
            finish()
            Log.d(TAG, "HomeActivity started and LoginActivity finished")

        } catch (e: Exception) {
            isNavigating = false
            Log.e(TAG, "ERROR starting HomeActivity", e)
            runOnUiThread {
                Toast.makeText(this, "화면 전환 오류: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}