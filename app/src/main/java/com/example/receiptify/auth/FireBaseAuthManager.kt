package com.example.receiptify.auth

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseAuthManager private constructor() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    companion object {
        @Volatile
        private var instance: FirebaseAuthManager? = null

        fun getInstance(): FirebaseAuthManager {
            return instance ?: synchronized(this) {
                instance ?: FirebaseAuthManager().also { instance = it }
            }
        }

        private const val TAG = "FirebaseAuthManager"
        private const val USERS_COLLECTION = "users"
    }

    // 현재 로그인된 사용자
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    // 이메일/비밀번호로 회원가입
    suspend fun signUpWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user

            if (user != null) {
                // Firestore에 사용자 정보 저장
                createUserDocument(user)
                Log.d(TAG, "회원가입 성공: ${user.email}")
                Result.success(user)
            } else {
                Result.failure(Exception("사용자 정보를 가져올 수 없습니다"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "회원가입 실패", e)
            Result.failure(e)
        }
    }

    // 이메일/비밀번호로 로그인
    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user

            if (user != null) {
                Log.d(TAG, "로그인 성공: ${user.email}")
                Result.success(user)
            } else {
                Result.failure(Exception("로그인에 실패했습니다"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "로그인 실패", e)
            Result.failure(e)
        }
    }

    // Google 로그인
    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user

            if (user != null) {
                // 신규 사용자인 경우 Firestore에 정보 저장
                if (result.additionalUserInfo?.isNewUser == true) {
                    createUserDocument(user)
                }
                Log.d(TAG, "Google 로그인 성공: ${user.email}")
                Result.success(user)
            } else {
                Result.failure(Exception("Google 로그인에 실패했습니다"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google 로그인 실패", e)
            Result.failure(e)
        }
    }

    // Firestore에 사용자 문서 생성
    private suspend fun createUserDocument(user: FirebaseUser) {
        try {
            val userMap = hashMapOf(
                "uid" to user.uid,
                "email" to (user.email ?: ""),
                "displayName" to (user.displayName ?: ""),
                "photoUrl" to (user.photoUrl?.toString() ?: ""),
                "createdAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis()
            )

            firestore.collection(USERS_COLLECTION)
                .document(user.uid)
                .set(userMap)
                .await()

            Log.d(TAG, "사용자 문서 생성 완료: ${user.uid}")
        } catch (e: Exception) {
            Log.e(TAG, "사용자 문서 생성 실패", e)
        }
    }

    // 로그아웃
    fun signOut() {
        auth.signOut()
        Log.d(TAG, "로그아웃 완료")
    }

    // 이메일 유효성 검사
    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // 비밀번호 유효성 검사 (최소 6자)
    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }
}