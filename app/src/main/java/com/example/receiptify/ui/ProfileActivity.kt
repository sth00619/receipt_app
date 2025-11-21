package com.example.receiptify.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.receiptify.R
import com.example.receiptify.databinding.ActivityProfileBinding
import com.example.receiptify.repository.AuthRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.navercorp.nid.NaverIdLoginSDK

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var authRepository: AuthRepository

    companion object {
        private const val TAG = "ProfileActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authRepository = AuthRepository(this)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // 알림 설정
        binding.switchNotification.setOnCheckedChangeListener { _, isChecked ->
            val message = if (isChecked) {
                getString(R.string.notifications_enabled)
            } else {
                getString(R.string.notifications_disabled)
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        // 다크 모드
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(
                this,
                if (isChecked) "다크 모드 활성화" else "다크 모드 비활성화",
                Toast.LENGTH_SHORT
            ).show()
        }

        // 언어 설정
        binding.layoutLanguage.setOnClickListener {
            Toast.makeText(this, "언어 설정", Toast.LENGTH_SHORT).show()
        }

        // 연결된 카드
        binding.layoutLinkedCards.setOnClickListener {
            Toast.makeText(this, "연결된 카드", Toast.LENGTH_SHORT).show()
        }

        // 비밀번호 변경
        binding.layoutSecurity.setOnClickListener {
            Toast.makeText(this, "비밀번호 변경", Toast.LENGTH_SHORT).show()
        }

        // 앱 설정
        binding.layoutAppSettings.setOnClickListener {
            Toast.makeText(this, "앱 설정", Toast.LENGTH_SHORT).show()
        }

        // 도움말
        binding.layoutHelp.setOnClickListener {
            Toast.makeText(this, "도움말", Toast.LENGTH_SHORT).show()
        }

        // 로그아웃
        binding.layoutLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.logout_confirm_title))
            .setMessage(getString(R.string.logout_confirm_message))
            .setPositiveButton(getString(R.string.logout_confirm_yes)) { _, _ ->
                performLogout()
            }
            .setNegativeButton(getString(R.string.logout_confirm_no), null)
            .show()
    }

    private fun performLogout() {
        Log.d(TAG, "🔓 로그아웃 시작")

        try {
            // 1. JWT 토큰 및 사용자 정보 삭제 (AuthRepository 사용)
            authRepository.logout()
            Log.d(TAG, "✅ JWT 토큰 삭제 완료")

            // 2. Naver 로그인 상태 제거
            val prefs = getSharedPreferences("receiptify_auth", Context.MODE_PRIVATE)
            prefs.edit().apply {
                remove("naver_logged_in")
                apply()
            }
            NaverIdLoginSDK.logout()
            Log.d(TAG, "✅ Naver 로그아웃 완료")

            // 3. Firebase 로그아웃
            FirebaseAuth.getInstance().signOut()
            Log.d(TAG, "✅ Firebase 로그아웃 완료")

            // 4. Google 로그아웃
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            val googleSignInClient = GoogleSignIn.getClient(this, gso)
            googleSignInClient.signOut().addOnCompleteListener {
                Log.d(TAG, "✅ Google 로그아웃 완료")
            }

            Toast.makeText(this, getString(R.string.logout_success), Toast.LENGTH_SHORT).show()

            // 5. LoginActivity로 이동
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()

            Log.d(TAG, "✅ LoginActivity로 이동 완료")

        } catch (e: Exception) {
            Log.e(TAG, "❌ 로그아웃 중 오류 발생", e)
            Toast.makeText(
                this,
                "로그아웃 중 오류가 발생했습니다: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}