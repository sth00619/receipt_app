package com.example.receiptify.api

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL = "http://10.0.2.2:3000/api/"

    // 🔑 Firebase ID Token을 자동으로 헤더에 추가하는 인터셉터
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()

        // Firebase에서 현재 사용자의 ID Token 가져오기
        val currentUser = FirebaseAuth.getInstance().currentUser

        val newRequest = if (currentUser != null) {
            try {
                // 동기적으로 토큰 가져오기 (주의: 네트워크 요청시에만 작동)
                val token = currentUser.getIdToken(false).result?.token

                if (token != null) {
                    originalRequest.newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                } else {
                    originalRequest
                }
            } catch (e: Exception) {
                android.util.Log.e("RetrofitClient", "Failed to get ID token", e)
                originalRequest
            }
        } else {
            originalRequest
        }

        chain.proceed(newRequest)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)  // 🔑 인증 인터셉터 추가
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: ReceiptApiService = retrofit.create(ReceiptApiService::class.java)

    /**
     * Firebase ID Token을 명시적으로 갱신할 때 사용
     */
    suspend fun refreshToken(): String? {
        return try {
            FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.await()?.token
        } catch (e: Exception) {
            android.util.Log.e("RetrofitClient", "Failed to refresh token", e)
            null
        }
    }
}