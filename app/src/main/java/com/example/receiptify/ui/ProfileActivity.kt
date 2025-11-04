package com.example.receiptify.ui

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.receiptify.R
import com.example.receiptify.auth.FirebaseAuthManager
import com.example.receiptify.databinding.ActivityProfileBinding
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.NidOAuthLogin // 이 import는 필요합니다.
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
            sharedPreferences.edit().putBoolean(KEY_DARK_MODE_ENABLED, isChecked).apply()

            // 다크 모드 적용
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }

            // 액티비티 재시작
            recreate()
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
        // Firebase 로그아웃
        authManager.signOut()

        // Naver 로그아웃
        val naverAccessToken = NaverIdLoginSDK.getAccessToken()
        if (naverAccessToken != null) {
            // 수정된 부분: Context 인수가 제거됨 (SDK 변경 사항)
            NidOAuthLogin().callDeleteTokenApi(object : com.navercorp.nid.oauth.OAuthLoginCallback {
                override fun onSuccess() {
                    // 성공
                }
                override fun onFailure(httpStatus: Int, message: String) {
                    // 실패해도 로그아웃 진행
                }
                override fun onError(errorCode: Int, message: String) {
                    // 에러 발생해도 로그아웃 진행
                }
            })
        }

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        navigateToLogin()
    }
    // --- 여기까지 수정된 부분 ---

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}