package com.example.receiptify.api

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * RetrofitClient 객체는 API 통신을 위한 싱글톤 OkHttpClient와 Retrofit 인스턴스를 관리합니다.
 * JWT 토큰 또는 Firebase ID 토큰을 자동으로 요청 헤더에 추가하는 인터셉터를 포함합니다.
 */
object RetrofitClient {

    private const val BASE_URL = "http://10.0.2.2:3000/api/"

    // Context를 저장하여 SharedPreferences 접근에 사용
    private var appContext: Context? = null

    /**
     * Context 초기화 (Application 클래스에서 반드시 호출되어야 함)
     */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    // 🔑 인증 토큰을 자동으로 헤더에 추가하는 인터셉터
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()

        // 1. SharedPreferences에서 JWT 토큰 가져오기 (백엔드 인증 토큰)
        val jwtToken = appContext?.getSharedPreferences("receiptify_auth", Context.MODE_PRIVATE)
            ?.getString("auth_token", null)

        val newRequest = if (jwtToken != null) {
            // 1-1. JWT 토큰이 있을 경우, 이를 사용하여 요청 헤더를 빌드
            originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer $jwtToken")
                .build()
        } else {
            // 2. JWT 토큰이 없을 경우, Firebase ID 토큰 시도 (하위 호환성/Firebase 전용 인증)
            val currentUser = FirebaseAuth.getInstance().currentUser

            if (currentUser != null) {
                try {
                    // 주의: .result를 사용하면 메인 스레드 블로킹 경고가 발생할 수 있음.
                    // OkHttp Interceptor는 일반적으로 백그라운드 스레드에서 실행되므로 안전합니다.
                    val firebaseToken = currentUser.getIdToken(false).result?.token

                    if (firebaseToken != null) {
                        originalRequest.newBuilder()
                            .addHeader("Authorization", "Bearer $firebaseToken")
                            .build()
                    } else {
                        originalRequest
                    }
                } catch (e: Exception) {
                    android.util.Log.e("RetrofitClient", "Failed to get Firebase token", e)
                    originalRequest
                }
            } else {
                // 3. 토큰이 없는 경우, 오리지널 요청 그대로 진행
                originalRequest
            }
        }

        chain.proceed(newRequest)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // 네트워크 요청 및 응답 본문을 포함하여 로그 레벨 설정
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)  // 🔑 인증 인터셉터 추가
        .addInterceptor(loggingInterceptor) // 로깅 인터셉터 추가
        .connectTimeout(30, TimeUnit.SECONDS) // 연결 제한 시간
        .readTimeout(30, TimeUnit.SECONDS)    // 읽기 제한 시간
        .writeTimeout(30, TimeUnit.SECONDS)   // 쓰기 제한 시간
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // ReceiptApiService 인터페이스 구현체
    val api: ReceiptApiService = retrofit.create(ReceiptApiService::class.java)

    // receiptApi 추가 (api와 동일한 인스턴스)
    val receiptApi: ReceiptApiService = api

    /**
     * Firebase ID Token을 명시적으로 갱신할 때 사용 (JWT 사용 시에는 필요 없음)
     */
    suspend fun refreshToken(): String? {
        return try {
            // true를 전달하여 토큰 강제 갱신
            FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.await()?.token
        } catch (e: Exception) {
            android.util.Log.e("RetrofitClient", "Failed to refresh Firebase token", e)
            null
        }
    }
}