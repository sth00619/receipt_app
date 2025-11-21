package com.example.receiptify

import android.app.Application
import com.example.receiptify.api.RetrofitClient

class ReceiptifyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // RetrofitClient 초기화
        RetrofitClient.init(this)
    }
}