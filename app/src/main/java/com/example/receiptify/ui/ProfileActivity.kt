package com.example.receiptify.ui

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.receiptify.R
import com.example.receiptify.auth.FirebaseAuthManager
import com.example.receiptify.databinding.ActivityProfileBinding
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.NidOAuthLogin
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var authManager: FirebaseAuthManager
    private lateinit var sharedPreferences: SharedPreferences

    private val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)
    private val dateFormat = SimpleDateFormat("yyyy.MM", Locale.KOREA)

    companion object {
        private const val PREFS_NAME = "ReceiptifyPrefs"
        private const val KEY_NOTIFICATION_ENABLED = "notification_enabled"
        private const val KEY_DARK_MODE_ENABLED = "dark_mode_enabled"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authManager = FirebaseAuthManager.getInstance()
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        setupUserInfo()
        setupStatistics()
        setupSettings()
        setupClickListeners()
    }

    private fun setupUserInfo() {
        val currentUser = authManager.currentUser
        val naverAccessToken = NaverIdLoginSDK.getAccessToken()

        if (currentUser != null) {
            // Firebase 사용자
            binding.tvUserEmail.text = currentUser.email ?: "user@example.com"
            binding.tvUserName.text = currentUser.displayName ?: currentUser.email?.substringBefore("@") ?: "User"

            // 가입일 (Firebase에서 가져오기)
            currentUser.metadata?.creationTimestamp?.let { timestamp ->
                val date = Date(timestamp)
                binding.tvJoinDate.text = "Join date: ${dateFormat.format(date)}"
            }
        } else if (naverAccessToken != null) {
            // Naver 사용자
            binding.tvUserName.text = "Naver User"
            binding.tvUserEmail.text = "naver@example.com"
            binding.tvJoinDate.text = "Join date: ${dateFormat.format(Date())}"
        } else {
            // 로그인 안 됨
            navigateToLogin()
        }
    }

    private fun setupStatistics() {
        // 임시 데이터
        binding.tvTotalSpending.text = "₩ ${numberFormat.format(226000)}"
        binding.tvTotalReceipts.text = "47개"
        binding.tvTotalRewards.text = "12회"
    }

    private fun setupSettings() {
        // 알림 설정 불러오기
        val notificationEnabled = sharedPreferences.getBoolean(KEY_NOTIFICATION_ENABLED, true)
        binding.switchNotification.isChecked = notificationEnabled

        // 다크 모드 설정 불러오기
        val darkModeEnabled = sharedPreferences.getBoolean(KEY_DARK_MODE_ENABLED, false)
        binding.switchDarkMode.isChecked = darkModeEnabled
    }

    private fun setupClickListeners() {
        // 알림 설정
        binding.switchNotification.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(KEY_NOTIFICATION_ENABLED, isChecked).apply()
            val message = if (isChecked) "Notifications enabled" else "Notifications disabled"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        // 다크 모드 설정
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            // 먼저 설정 저장
            sharedPreferences.edit().putBoolean(KEY_DARK_MODE_ENABLED, isChecked).apply()

            // 다크 모드 적용
            val nightMode = if (isChecked) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }

            // Binder 트랜잭션 충돌 방지를 위해 지연 후 적용
            Handler(Looper.getMainLooper()).postDelayed({
                if (!isFinishing && !isDestroyed) {
                    try {
                        AppCompatDelegate.setDefaultNightMode(nightMode)
                        // recreate() 대신 새 Intent로 재시작하여 Binder 트랜잭션 충돌 방지
                        val intent = Intent(this, ProfileActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        finish()
                        // 부드러운 전환 효과
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    } catch (e: Exception) {
                        // 예외 발생 시 로깅만 하고 계속 진행
                        e.printStackTrace()
                    }
                }
            }, 100) // 100ms 지연으로 Binder 트랜잭션 완료 대기
        }

        // 언어 설정
        binding.layoutLanguage.setOnClickListener {
            Toast.makeText(this, "Language settings coming soon", Toast.LENGTH_SHORT).show()
        }

        // 연결된 카드
        binding.layoutLinkedCards.setOnClickListener {
            Toast.makeText(this, "Linked cards feature coming soon", Toast.LENGTH_SHORT).show()
        }

        // 비밀번호 변경
        binding.layoutSecurity.setOnClickListener {
            Toast.makeText(this, "Password change feature coming soon", Toast.LENGTH_SHORT).show()
        }

        // 앱 설정
        binding.layoutAppSettings.setOnClickListener {
            Toast.makeText(this, "App settings coming soon", Toast.LENGTH_SHORT).show()
        }

        // 도움말
        binding.layoutHelp.setOnClickListener {
            Toast.makeText(this, "Help feature coming soon", Toast.LENGTH_SHORT).show()
        }

        // 로그아웃
        binding.layoutLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // --- 여기부터 수정된 부분 ---
    private fun performLogout() {
        // lifecycle 확인: Activity가 종료 중이거나 파괴된 상태가 아닌지 확인
        if (isFinishing || isDestroyed) {
            return
        }

        try {
            // Firebase 로그아웃
            authManager.signOut()

            // Naver 로그아웃 - Binder 트랜잭션 오류 방지를 위해 비동기 처리
            val naverAccessToken = NaverIdLoginSDK.getAccessToken()
            if (naverAccessToken != null && !isFinishing && !isDestroyed) {
                // Handler를 사용하여 Binder 트랜잭션 충돌 방지
                Handler(Looper.getMainLooper()).post {
                    if (!isFinishing && !isDestroyed) {
                        try {
                            NidOAuthLogin().callDeleteTokenApi(object : com.navercorp.nid.oauth.OAuthLoginCallback {
                                override fun onSuccess() {
                                    // 성공 시 추가 작업 없음
                                }
                                override fun onFailure(httpStatus: Int, message: String) {
                                    // 실패해도 로그아웃 진행 (네트워크 오류 등)
                                }
                                override fun onError(errorCode: Int, message: String) {
                                    // 에러 발생해도 로그아웃 진행
                                }
                            })
                        } catch (e: Exception) {
                            // Binder 트랜잭션 오류 등 예외 처리
                            e.printStackTrace()
                        }
                    }
                }
            }

            // Toast와 화면 전환은 약간 지연하여 Binder 트랜잭션 완료 대기
            Handler(Looper.getMainLooper()).postDelayed({
                if (!isFinishing && !isDestroyed) {
                    Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
                    navigateToLogin()
                }
            }, 150) // 150ms 지연으로 안전한 처리 보장

        } catch (e: Exception) {
            // 전체 로그아웃 프로세스에서 예외 발생 시 처리
            e.printStackTrace()
            if (!isFinishing && !isDestroyed) {
                Toast.makeText(this, "Logout completed", Toast.LENGTH_SHORT).show()
                navigateToLogin()
            }
        }
    }
    // --- 여기까지 수정된 부분 ---

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}