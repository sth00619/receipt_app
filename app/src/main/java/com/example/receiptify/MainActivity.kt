package com.example.receiptify

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.receiptify.auth.FirebaseAuthManager
import com.example.receiptify.ui.LoginActivity  // ✅ 이게 맞습니다!

class MainActivity : AppCompatActivity() {

    private lateinit var authManager: FirebaseAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        authManager = FirebaseAuthManager.getInstance()

        // 로그인 확인
        val currentUser = authManager.currentUser
        if (currentUser == null) {
            navigateToLogin()
            return
        }

        // 사용자 정보 표시
        findViewById<TextView>(R.id.tvWelcome).text =
            "환영합니다, ${currentUser.email}님!"

        // 로그아웃 버튼
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            authManager.signOut()
            Toast.makeText(this, "로그아웃되었습니다", Toast.LENGTH_SHORT).show()
            navigateToLogin()
        }
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}