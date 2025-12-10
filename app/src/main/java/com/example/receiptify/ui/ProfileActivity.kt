package com.example.receiptify.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.example.receiptify.R
import com.example.receiptify.databinding.ActivityProfileBinding
import com.example.receiptify.repository.AuthRepository
import com.example.receiptify.repository.NotificationRepository
import com.example.receiptify.repository.ProfileImageRepository
import com.example.receiptify.utils.PreferenceManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.navercorp.nid.NaverIdLoginSDK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var authRepository: AuthRepository
    private lateinit var prefs: SharedPreferences
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var profileImageRepository: ProfileImageRepository
    private var googleSignInClient: GoogleSignInClient? = null

    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleImageSelected(it) }
    }

    companion object {
        private const val TAG = "ProfileActivity"
        private const val PREFS_NAME = "receiptify_auth"
        private const val KEY_NAVER_LOGGED_IN = "naver_logged_in"
        private const val GOOGLE_WEB_CLIENT_ID = "763595991477-k7es3foiml6lknn646mqk7fnehhqd0d8.apps.googleusercontent.com"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authRepository = AuthRepository(this)
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        preferenceManager = PreferenceManager(this)
        notificationRepository = NotificationRepository()
        profileImageRepository = ProfileImageRepository(this)

        setupGoogleSignIn()
        setupToolbar()
        loadUserProfile()
        loadProfileImage()  // ✅ Load profile image
        loadSettings()  // ✅ 모든 설정 로드
        setupClickListeners()
        setupBottomNavigation()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(GOOGLE_WEB_CLIENT_ID)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Notification icon click
        binding.layoutNotificationIcon.setOnClickListener {
            val intent = Intent(this, NotificationsActivity::class.java)
            startActivity(intent)
        }

        // Chatbot icon click
        binding.btnChatbot.setOnClickListener {
            val intent = Intent(this, ChatListActivity::class.java)
            startActivity(intent)
        }

        // Load notification count
        loadNotificationCount()
    }

    private fun loadNotificationCount() {
        lifecycleScope.launch {
            try {
                val result = notificationRepository.getNotifications(unreadOnly = false)
                result.onSuccess { response ->
                    val unreadCount = response.unreadCount
                    withContext(Dispatchers.Main) {
                        if (unreadCount > 0) {
                            binding.tvNotificationBadge.visibility = android.view.View.VISIBLE
                            binding.tvNotificationBadge.text = if (unreadCount > 99) "99+" else unreadCount.toString()
                        } else {
                            binding.tvNotificationBadge.visibility = android.view.View.GONE
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load notification count", e)
            }
        }
    }

    private fun loadUserProfile() {
        val email = prefs.getString("user_email", "사용자") ?: "사용자"

        binding.tvUserName.text = email.split("@").getOrNull(0) ?: "사용자"
        binding.tvEmail.text = email
    }

    /**
     * Load profile image from Room database
     */
    private fun loadProfileImage() {
        lifecycleScope.launch {
            try {
                // Use email as user identifier
                val userEmail = prefs.getString("user_email", null)
                if (userEmail == null) {
                    Log.w(TAG, "No user email found")
                    binding.ivProfile.setImageResource(R.drawable.ic_person)
                    return@launch
                }

                val bitmap = profileImageRepository.getProfileImage(userEmail)

                bitmap?.let {
                    binding.ivProfile.setImageBitmap(it)
                    Log.d(TAG, "✅ Profile image loaded for: $userEmail")
                } ?: run {
                    // Set default profile image
                    binding.ivProfile.setImageResource(R.drawable.ic_person)
                    Log.d(TAG, "No profile image found, using default")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to load profile image", e)
                binding.ivProfile.setImageResource(R.drawable.ic_person)
            }
        }
    }

    /**
     * ✅ 현재 설정 상태를 UI에 반영
     */
    private fun loadSettings() {
        // 다크모드 상태
        val isDarkMode = preferenceManager.isDarkMode()
        binding.switchDarkMode.isChecked = isDarkMode
        Log.d(TAG, "현재 다크모드 상태: $isDarkMode")

        // 알림 상태
        val isNotificationEnabled = preferenceManager.isNotificationEnabled()
        binding.switchNotification.isChecked = isNotificationEnabled
        Log.d(TAG, "현재 알림 상태: $isNotificationEnabled")
    }

    private fun setupClickListeners() {
        // Profile image click - open image picker
        binding.layoutProfileImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        // 로그아웃 버튼
        binding.layoutLogout.setOnClickListener {
            showLogoutConfirmDialog()
        }

        // ✅ 알림 설정
        binding.switchNotification.setOnCheckedChangeListener { _, isChecked ->
            preferenceManager.setNotificationEnabled(isChecked)
            Toast.makeText(
                this,
                "알림: ${if (isChecked) "켜짐" else "꺼짐"}",
                Toast.LENGTH_SHORT
            ).show()
            Log.d(TAG, "알림 설정 변경: $isChecked")
        }

        // ✅ 다크모드 토글
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "다크모드 스위치 변경: $isChecked")

            // PreferenceManager에 저장
            preferenceManager.setDarkMode(isChecked)

            // 즉시 다크모드 적용
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }

            Toast.makeText(
                this,
                "다크모드: ${if (isChecked) "켜짐" else "꺼짐"}",
                Toast.LENGTH_SHORT
            ).show()

            // 액티비티 재시작하여 테마 적용
            recreate()
        }

        // 비밀번호 변경
        binding.layoutChangePassword.setOnClickListener {
            Toast.makeText(this, "비밀번호 변경 (준비중)", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Handle image selection from gallery
     */
    private fun handleImageSelected(uri: Uri) {
        lifecycleScope.launch {
            try {
                val bitmap = loadBitmapFromUri(uri)
                if (bitmap == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ProfileActivity,
                            "이미지를 불러올 수 없습니다",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                // Use email as user identifier
                val userEmail = prefs.getString("user_email", null)
                if (userEmail == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ProfileActivity,
                            "사용자 정보를 찾을 수 없습니다",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                Log.d(TAG, "Saving profile image for: $userEmail")
                val result = profileImageRepository.saveProfileImage(userEmail, bitmap)

                result.onSuccess {
                    withContext(Dispatchers.Main) {
                        binding.ivProfile.setImageBitmap(bitmap)
                        Toast.makeText(
                            this@ProfileActivity,
                            "프로필 사진이 변경되었습니다",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.d(TAG, "✅ Profile image saved and displayed")
                    }
                }.onFailure { error ->
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ProfileActivity,
                            "프로필 사진 저장 실패: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.e(TAG, "❌ Failed to save profile image", error)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception in handleImageSelected", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ProfileActivity,
                        "이미지 처리 중 오류가 발생했습니다",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    /**
     * Load bitmap from URI
     */
    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bitmap from URI", e)
            null
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.nav_profile

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this@ProfileActivity, HomeActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_categories -> {
                    val intent = Intent(this@ProfileActivity, CategoriesActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_profile -> true
                else -> false
            }
        }
    }

    private fun showLogoutConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("로그아웃")
            .setMessage("정말 로그아웃 하시겠습니까?")
            .setPositiveButton("로그아웃") { _, _ ->
                performLogout()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun performLogout() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "🚨 로그아웃 시작")

                // ✅✅✅ 1. 먼저 JWT 토큰을 삭제 (가장 먼저!)
                authRepository.logout()
                Log.d(TAG, "✅ JWT 토큰 삭제 완료")

                // 토큰 삭제 확인
                val remainingToken = authRepository.getToken()
                if (remainingToken != null) {
                    Log.e(TAG, "❌❌❌ 토큰이 아직 남아있음: ${remainingToken.take(50)}...")
                } else {
                    Log.d(TAG, "✅✅✅ 토큰 삭제 확인됨")
                }

                // 2. Firebase 로그아웃
                if (FirebaseAuth.getInstance().currentUser != null) {
                    FirebaseAuth.getInstance().signOut()
                    Log.d(TAG, "✅ Firebase 로그아웃")
                }

                // 3. Google 로그아웃
                if (googleSignInClient != null) {
                    googleSignInClient?.signOut()?.await()
                    Log.d(TAG, "✅ Google 로그아웃")
                }

                // 4. Naver 로그아웃
                if (NaverIdLoginSDK.getAccessToken() != null) {
                    NaverIdLoginSDK.logout()
                    prefs.edit().putBoolean(KEY_NAVER_LOGGED_IN, false).apply()
                    Log.d(TAG, "✅ Naver 로그아웃")
                }

                Log.d(TAG, "✅ 로그아웃 완료")

                // 5. LoginActivity로 이동 (토큰이 이미 삭제되었으므로 플래그 불필요)
                withContext(Dispatchers.Main) {
                    val intent = Intent(this@ProfileActivity, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ 로그아웃 중 오류", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ProfileActivity,
                        "로그아웃 중 오류가 발생했습니다",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }





    override fun onResume() {
        super.onResume()
        // Reload profile image when returning to activity
        loadProfileImage()
    }
}