package com.example.receiptify.api

import android.content.Context
import android.util.Log
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
    private const val EXCHANGE_RATE_BASE_URL = "https://api.frankfurter.app/"
    private const val TAG = "RetrofitClient"

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        Log.d(TAG, "✅ RetrofitClient 초기화 완료")
    }

    // 인증 토큰을 자동으로 헤더에 추가하는 인터셉터
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()

        // 1. SharedPreferences에서 JWT 토큰 가져오기
        val jwtToken = appContext?.getSharedPreferences("receiptify_auth", Context.MODE_PRIVATE)
            ?.getString("auth_token", null)

        if (jwtToken != null) {
            Log.d(TAG, "🔑 JWT 토큰 발견: ${jwtToken.take(30)}...")
        } else {
            Log.w(TAG, "⚠️ JWT 토큰 없음")
        }

        val newRequest = if (jwtToken != null) {
            // JWT 토큰을 Authorization 헤더에 추가
            val requestWithAuth = originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer $jwtToken")
                .build()

            Log.d(TAG, "📤 요청: ${originalRequest.method} ${originalRequest.url}")
            Log.d(TAG, "📤 Authorization 헤더 추가됨")

            requestWithAuth
        } else {
            // 2. JWT 토큰이 없으면 Firebase ID 토큰 시도
            val currentUser = FirebaseAuth.getInstance().currentUser

            if (currentUser != null) {
                try {
                    val firebaseToken = currentUser.getIdToken(false).result?.token

                    if (firebaseToken != null) {
                        Log.d(TAG, "🔥 Firebase 토큰 사용: ${firebaseToken.take(30)}...")
                        originalRequest.newBuilder()
                            .addHeader("Authorization", "Bearer $firebaseToken")
                            .build()
                    } else {
                        Log.w(TAG, "⚠️ Firebase 토큰도 없음")
                        originalRequest
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Firebase 토큰 가져오기 실패", e)
                    originalRequest
                }
            } else {
                Log.w(TAG, "⚠️ 인증 정보 없음 - 원본 요청 전송")
                originalRequest
            }
        }

        // 요청 전송
        val response = chain.proceed(newRequest)

        // 응답 로깅
        Log.d(TAG, "📥 응답: ${response.code} ${response.message}")

        response
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
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

    private val exchangeRateClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val exchangeRateRetrofit = Retrofit.Builder()
        .baseUrl(EXCHANGE_RATE_BASE_URL)
        .client(exchangeRateClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: ReceiptApiService = retrofit.create(ReceiptApiService::class.java)
    val receiptApi: ReceiptApiService = api
    val exchangeRateApi: ExchangeRateApiService = exchangeRateRetrofit.create(ExchangeRateApiService::class.java)

    suspend fun refreshToken(): String? {
        return try {
            FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.await()?.token
        } catch (e: Exception) {
            Log.e(TAG, "❌ Firebase 토큰 갱신 실패", e)
            null
        }
    }
}