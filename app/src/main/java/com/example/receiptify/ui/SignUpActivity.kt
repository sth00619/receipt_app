package com.example.receiptify.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.receiptify.R
import com.example.receiptify.auth.FirebaseAuthManager
import com.example.receiptify.databinding.ActivitySignupBinding
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var authManager: FirebaseAuthManager

    companion object {
        private const val TAG = "SignUpActivity"
    }

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
            Log.d(TAG, "Back button clicked")
            finish()
        }

        // 회원가입
        binding.btnSignUp.setOnClickListener {
            Log.d(TAG, "Sign up button clicked")

            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            Log.d(TAG, "Input - email: $email, password length: ${password.length}")

            if (validateInput(email, password, confirmPassword)) {
                signUpWithEmail(email, password)
            } else {
                Log.d(TAG, "Input validation failed")
            }
        }
    }

    private fun validateInput(email: String, password: String, confirmPassword: String): Boolean {
        when {
            email.isEmpty() -> {
                binding.emailInputLayout.error = getString(R.string.error_empty_email)
                Log.d(TAG, "Email is empty")
                return false
            }
            !authManager.isValidEmail(email) -> {
                binding.emailInputLayout.error = getString(R.string.error_invalid_email)
                Log.d(TAG, "Email format is invalid")
                return false
            }
            password.isEmpty() -> {
                binding.passwordInputLayout.error = getString(R.string.error_empty_password)
                Log.d(TAG, "Password is empty")
                return false
            }
            !authManager.isValidPassword(password) -> {
                binding.passwordInputLayout.error = getString(R.string.error_short_password)
                Log.d(TAG, "Password is too short")
                return false
            }
            confirmPassword.isEmpty() -> {
                binding.confirmPasswordInputLayout.error = getString(R.string.error_empty_password)
                Log.d(TAG, "Confirm password is empty")
                return false
            }
            password != confirmPassword -> {
                binding.confirmPasswordInputLayout.error = getString(R.string.error_password_mismatch)
                Log.d(TAG, "Passwords do not match")
                return false
            }
            else -> {
                binding.emailInputLayout.error = null
                binding.passwordInputLayout.error = null
                binding.confirmPasswordInputLayout.error = null
                Log.d(TAG, "Input validation successful")
                return true
            }
        }
    }

    private fun signUpWithEmail(email: String, password: String) {
        Log.d(TAG, "Sign up attempt started: email=$email")

        lifecycleScope.launch {
            try {
                binding.btnSignUp.isEnabled = false
                Log.d(TAG, "Button disabled")

                Log.d(TAG, "Calling Firebase signUpWithEmail")
                val result = authManager.signUpWithEmail(email, password)

                result.onSuccess { user ->
                    Log.d(TAG, "Sign up successful: ${user.email}, uid: ${user.uid}")
                    Toast.makeText(
                        this@SignUpActivity,
                        getString(R.string.signup_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    navigateToMain()
                }.onFailure { exception ->
                    Log.e(TAG, "Sign up failed: ${exception.javaClass.simpleName}", exception)
                    Log.e(TAG, "Error message: ${exception.message}")
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
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.btnSignUp.isEnabled = true
                Log.d(TAG, "Button re-enabled")
            }
        }
    }

    private fun navigateToMain() {
        Log.d(TAG, "Navigating to HomeActivity")
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}