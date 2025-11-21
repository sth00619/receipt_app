package com.example.receiptify.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.receiptify.R
import com.example.receiptify.databinding.ActivitySignupBinding
import com.example.receiptify.repository.AuthRepository
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var authRepository: AuthRepository

    companion object {
        private const val TAG = "SignUpActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authRepository = AuthRepository(this)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // 뒤로가기
        binding.btnBack.setOnClickListener {
            Log.d(TAG, "Back button clicked")
            finish()
        }

        // 회원가입
        binding.btnSignUp.setOnClickListener {
            Log.d(TAG, "Sign up button clicked")

            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            if (validateInput(email, password, confirmPassword)) {
                signUpWithEmail(email, password)
            }
        }
    }

    private fun validateInput(email: String, password: String, confirmPassword: String): Boolean {
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
            password.length < 6 -> {
                binding.passwordInputLayout.error = getString(R.string.error_short_password)
                return false
            }
            confirmPassword.isEmpty() -> {
                binding.confirmPasswordInputLayout.error = getString(R.string.error_empty_password)
                return false
            }
            password != confirmPassword -> {
                binding.confirmPasswordInputLayout.error = getString(R.string.error_password_mismatch)
                return false
            }
            else -> {
                binding.emailInputLayout.error = null
                binding.passwordInputLayout.error = null
                binding.confirmPasswordInputLayout.error = null
                return true
            }
        }
    }

    private fun signUpWithEmail(email: String, password: String) {
        Log.d(TAG, "Sign up attempt: email=$email")

        lifecycleScope.launch {
            try {
                binding.btnSignUp.isEnabled = false

                val displayName = email.split("@")[0]  // 이메일에서 사용자 이름 추출
                val result = authRepository.register(email, password, displayName)

                result.onSuccess { userData ->
                    Log.d(TAG, "✅ 회원가입 성공: ${userData.email}")

                    Toast.makeText(
                        this@SignUpActivity,
                        "회원가입 성공! 환영합니다, ${userData.displayName}님",
                        Toast.LENGTH_SHORT
                    ).show()

                    navigateToMain()

                }.onFailure { exception ->
                    Log.e(TAG, "❌ 회원가입 실패", exception)

                    Toast.makeText(
                        this@SignUpActivity,
                        exception.message ?: getString(R.string.error_signup_failed),
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Exception occurred", e)
                Toast.makeText(
                    this@SignUpActivity,
                    "오류: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.btnSignUp.isEnabled = true
            }
        }
    }

    private fun navigateToMain() {
        Log.d(TAG, "Navigating to HomeActivity")
        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}