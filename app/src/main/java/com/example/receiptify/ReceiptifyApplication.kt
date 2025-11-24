package com.example.receiptify

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.example.receiptify.api.RetrofitClient
import com.example.receiptify.utils.PreferenceManager

class ReceiptifyApplication : Application() {

    companion object {
        private const val TAG = "ReceiptifyApplication"
    }

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "🚀 Application onCreate - Initializing")

        // RetrofitClient 초기화 (매우 중요!)
        RetrofitClient.init(this)
        Log.d(TAG, "✅ RetrofitClient initialized successfully")

        // 다크모드 설정 로드 및 적용
        initializeDarkMode()

        Log.d(TAG, "✅ Application initialization completed")
    }

    /**
     * 다크모드 설정 초기화
     */
    private fun initializeDarkMode() {
        try {
            val preferenceManager = PreferenceManager(this)
            val isDarkMode = preferenceManager.isDarkMode()

            if (isDarkMode) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                Log.d(TAG, "🌙 다크모드 활성화")
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                Log.d(TAG, "☀️ 라이트모드 활성화")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 다크모드 초기화 실패", e)
            // 실패 시 기본값(라이트모드)으로 설정
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}