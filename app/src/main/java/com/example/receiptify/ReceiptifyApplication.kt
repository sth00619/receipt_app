package com.example.receiptify

import android.app.Application
import android.util.Log
import com.example.receiptify.api.RetrofitClient

class ReceiptifyApplication : Application() {

    companion object {
        private const val TAG = "ReceiptifyApplication"
    }

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "🚀 Application onCreate - Initializing RetrofitClient")

        // RetrofitClient 초기화 (매우 중요!)
        RetrofitClient.init(this)

        Log.d(TAG, "✅ RetrofitClient initialized successfully")
    }
}