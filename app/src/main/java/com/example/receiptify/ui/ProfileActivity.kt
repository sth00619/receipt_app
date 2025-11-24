package com.example.receiptify.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.receiptify.R
import com.example.receiptify.api.RetrofitClient
import com.example.receiptify.api.models.ChangePasswordRequest
import com.example.receiptify.api.models.UpdateSettingRequest
import com.example.receiptify.databinding.ActivityProfileBinding
import com.example.receiptify.utils.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var preferenceManager: PreferenceManager

    private val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)
    private val dateFormat = SimpleDateFormat("yyyy.MM", Locale.KOREA)

    companion object {
        private const val TAG = "ProfileActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 다크모드 설정 적용
        preferenceManager = PreferenceManager(this)
        applyDarkMode()

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        loadProfile()
        setupClickListeners()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        // 하단 네비게이션
        binding.bottomNavigation.selectedItemId = R.id.nav_profile
    }

    /**
     * 프로필 데이터 로드
     */
    private fun loadProfile() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "📥 프로필 로드 시작")

                val response = RetrofitClient.api.getProfile()

                if (response.isSuccessful && response.body()?.success == true) {
                    val profileData = response.body()?.data!!

                    Log.d(TAG, "✅ 프로필 로드 성공: ${profileData.user.email}")

                    // 사용자 정보 표시
                    displayUserInfo(profileData)

                    // 통계 정보 표시
                    displayStats(profileData)

                    // Provider에 따라 비밀번호 변경 메뉴 표시/숨김
                    if (profileData.user.provider == "email") {
                        binding.layoutChangePassword.visibility = View.VISIBLE
                        binding.dividerPassword.visibility = View.VISIBLE
                    } else {
                        binding.layoutChangePassword.visibility = View.GONE
                        binding.dividerPassword.visibility = View.GONE
                    }

                    // 설정 로드
                    loadSettings()

                } else {
                    val errorMsg = response.body()?.message ?: "알 수 없는 오류"
                    Log.e(TAG, "❌ 프로필 로드 실패: $errorMsg")
                    Toast.makeText(this@ProfileActivity, "프로필 로드 실패: $errorMsg", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ 프로필 로드 오류", e)
                Toast.makeText(this@ProfileActivity, "오류: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 사용자 정보 표시
     */
    private fun displayUserInfo(profileData: com.example.receiptify.api.models.ProfileResponse) {
        // 프로필 이미지
        if (!profileData.user.photoUrl.isNullOrBlank()) {
            Glide.with(this)
                .load(profileData.user.photoUrl)
                .placeholder(R.drawable.ic_receipt)
                .circleCrop()
                .into(binding.ivProfile)
        } else {
            binding.ivProfile.setImageResource(R.drawable.ic_receipt)
        }

        // 사용자 이름
        binding.tvUserName.text = profileData.user.displayName ?: "사용자"

        // 이메일
        binding.tvEmail.text = profileData.user.email

        // 가입일
        try {
            val createdAtDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                .parse(profileData.user.createdAt)

            if (createdAtDate != null) {
                binding.tvJoinDate.text = "가입일: ${dateFormat.format(createdAtDate)}"
            }
        } catch (e: Exception) {
            Log.w(TAG, "가입일 파싱 실패", e)
            binding.tvJoinDate.text = "가입일: -"
        }
    }

    /**
     * 통계 정보 표시
     */
    private fun displayStats(profileData: com.example.receiptify.api.models.ProfileResponse) {
        val stats = profileData.stats

        // 이번 달 지출
        binding.tvMonthlySpending.text = "₩ ${numberFormat.format(stats.monthlySpending.toLong())}"

        // 총 영수증 개수
        binding.tvReceiptCount.text = "${stats.totalReceipts}개"

        // 정산 완료 (월별 영수증 개수로 표시)
        binding.tvNotificationCount.text = "${stats.monthlyReceiptCount}회"

        // 알림 배지는 추후 구현 (현재는 숨김)
        binding.tvNotificationBadge.visibility = View.GONE
    }

    /**
     * 설정 로드
     */
    private fun loadSettings() {
        // 다크모드 설정
        val isDarkMode = preferenceManager.isDarkMode()
        binding.switchDarkMode.isChecked = isDarkMode

        // 알림 설정
        val isNotificationEnabled = preferenceManager.isNotificationEnabled()
        binding.switchNotification.isChecked = isNotificationEnabled
    }

    /**
     * 클릭 리스너 설정
     */
    private fun setupClickListeners() {
        // 알림 설정 토글
        binding.switchNotification.setOnCheckedChangeListener { _, isChecked ->
            updateNotificationSetting(isChecked)
        }

        // 다크모드 토글
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            updateDarkModeSetting(isChecked)
        }

        // 비밀번호 변경
        binding.layoutChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        // 정산 완료 (알림)
        binding.cvNotifications.setOnClickListener {
            showNotificationsDialog()
        }

        // 로그아웃
        binding.layoutLogout.setOnClickListener {
            showLogoutDialog()
        }

        // 하단 네비게이션
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_categories -> {
                    startActivity(Intent(this, CategoriesActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_receipts -> {
                    startActivity(Intent(this, ReceiptScanActivity::class.java))
                    false
                }
                R.id.nav_profile -> true
                else -> false
            }
        }
    }

    /**
     * 알림 설정 업데이트
     */
    private fun updateNotificationSetting(enabled: Boolean) {
        lifecycleScope.launch {
            try {
                val request = UpdateSettingRequest(enabled)
                val response = RetrofitClient.api.updateNotificationSetting(request)

                if (response.isSuccessful && response.body()?.success == true) {
                    preferenceManager.setNotificationEnabled(enabled)
                    Log.d(TAG, "✅ 알림 설정 업데이트: $enabled")
                    Toast.makeText(
                        this@ProfileActivity,
                        if (enabled) "알림이 활성화되었습니다" else "알림이 비활성화되었습니다",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Log.e(TAG, "❌ 알림 설정 업데이트 실패")
                    binding.switchNotification.isChecked = !enabled
                    Toast.makeText(this@ProfileActivity, "설정 업데이트 실패", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 알림 설정 오류", e)
                binding.switchNotification.isChecked = !enabled
                Toast.makeText(this@ProfileActivity, "오류: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 다크모드 설정 업데이트
     */
    private fun updateDarkModeSetting(enabled: Boolean) {
        lifecycleScope.launch {
            try {
                val request = UpdateSettingRequest(enabled)
                val response = RetrofitClient.api.updateDarkModeSetting(request)

                if (response.isSuccessful && response.body()?.success == true) {
                    preferenceManager.setDarkMode(enabled)
                    Log.d(TAG, "✅ 다크모드 설정 업데이트: $enabled")

                    // 다크모드 즉시 적용
                    applyDarkMode()

                    Toast.makeText(
                        this@ProfileActivity,
                        if (enabled) "다크모드가 활성화되었습니다" else "라이트모드가 활성화되었습니다",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Log.e(TAG, "❌ 다크모드 설정 업데이트 실패")
                    binding.switchDarkMode.isChecked = !enabled
                    Toast.makeText(this@ProfileActivity, "설정 업데이트 실패", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 다크모드 설정 오류", e)
                binding.switchDarkMode.isChecked = !enabled
                Toast.makeText(this@ProfileActivity, "오류: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 다크모드 적용
     */
    private fun applyDarkMode() {
        val isDarkMode = preferenceManager.isDarkMode()

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    /**
     * 비밀번호 변경 다이얼로그
     */
    private fun showChangePasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)

        val etCurrentPassword = dialogView.findViewById<TextInputEditText>(R.id.etCurrentPassword)
        val etNewPassword = dialogView.findViewById<TextInputEditText>(R.id.etNewPassword)
        val etConfirmPassword = dialogView.findViewById<TextInputEditText>(R.id.etConfirmPassword)

        MaterialAlertDialogBuilder(this)
            .setTitle("비밀번호 변경")
            .setView(dialogView)
            .setPositiveButton("변경") { dialog, _ ->
                val currentPassword = etCurrentPassword.text.toString()
                val newPassword = etNewPassword.text.toString()
                val confirmPassword = etConfirmPassword.text.toString()

                if (currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
                    Toast.makeText(this, "모든 필드를 입력해주세요", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPassword != confirmPassword) {
                    Toast.makeText(this, "새 비밀번호가 일치하지 않습니다", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPassword.length < 6) {
                    Toast.makeText(this, "비밀번호는 6자 이상이어야 합니다", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                changePassword(currentPassword, newPassword)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    /**
     * 비밀번호 변경
     */
    private fun changePassword(currentPassword: String, newPassword: String) {
        lifecycleScope.launch {
            try {
                val request = ChangePasswordRequest(currentPassword, newPassword)
                val response = RetrofitClient.api.changePassword(request)

                if (response.isSuccessful && response.body()?.success == true) {
                    Log.d(TAG, "✅ 비밀번호 변경 성공")
                    Toast.makeText(this@ProfileActivity, "비밀번호가 변경되었습니다", Toast.LENGTH_SHORT).show()
                } else {
                    val errorMsg = response.body()?.message ?: "알 수 없는 오류"
                    Log.e(TAG, "❌ 비밀번호 변경 실패: $errorMsg")
                    Toast.makeText(this@ProfileActivity, "변경 실패: $errorMsg", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 비밀번호 변경 오류", e)
                Toast.makeText(this@ProfileActivity, "오류: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 알림 리스트 다이얼로그
     */
    private fun showNotificationsDialog() {
        val notifications = arrayOf(
            "11월 식비 지출이 예산을 초과했습니다",
            "이번 주 교통비가 지난 주보다 30% 증가했습니다",
            "쇼핑 카테고리 지출이 평소보다 높습니다"
        )

        MaterialAlertDialogBuilder(this)
            .setTitle("정산 완료 알림")
            .setItems(notifications) { _, which ->
                Toast.makeText(this, notifications[which], Toast.LENGTH_LONG).show()
            }
            .setPositiveButton("확인", null)
            .show()
    }

    /**
     * 로그아웃 다이얼로그
     */
    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("로그아웃")
            .setMessage("정말 로그아웃하시겠습니까?")
            .setPositiveButton("로그아웃") { _, _ ->
                performLogout()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    /**
     * 로그아웃 실행
     */
    private fun performLogout() {
        // Firebase 로그아웃
        FirebaseAuth.getInstance().signOut()

        // SharedPreferences 초기화
        preferenceManager.clearLoginInfo()

        Log.d(TAG, "✅ 로그아웃 완료")

        // 로그인 화면으로 이동
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}