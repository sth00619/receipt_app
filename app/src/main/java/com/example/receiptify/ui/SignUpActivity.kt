package com.example.receiptify.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.receiptify.MainActivity
import com.example.receiptify.R
import com.example.receiptify.auth.FirebaseAuthManager
import com.example.receiptify.databinding.ActivitySignupBinding
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var authManager: FirebaseAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authManager = FirebaseAuthManager.getInstance()

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // 뒤로가기
        binding.btnBack.setOnClickListener {
            finish()
        }

        // 회원가입
        binding.btnSignUp.setOnClickListener {
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
            !authManager.isValidEmail(email) -> {
                binding.emailInputLayout.error = getString(R.string.error_invalid_email)
                return false
            }
            password.isEmpty() -> {
                binding.passwordInputLayout.error = getString(R.string.error_empty_password)
                return false
            }
            !authManager.isValidPassword(password) -> {
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
        lifecycleScope.launch {
            try {
                binding.btnSignUp.isEnabled = false

                val result = authManager.signUpWithEmail(email, password)

                result.onSuccess {
                    Toast.makeText(
                        this@SignUpActivity,
                        getString(R.string.signup_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    navigateToMain()
                }.onFailure { exception ->
                    Toast.makeText(
                        this@SignUpActivity,
                        exception.message ?: getString(R.string.error_signup_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                binding.btnSignUp.isEnabled = true
            }
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}