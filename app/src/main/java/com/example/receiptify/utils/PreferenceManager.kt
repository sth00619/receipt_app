package com.example.receiptify.utils

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "receiptify_prefs"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_EMAIL = "email"
        private const val KEY_TOKEN = "token"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    fun saveLoginInfo(userId: String, email: String, token: String, displayName: String? = null) {
        prefs.edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_EMAIL, email)
            putString(KEY_TOKEN, token)
            putString(KEY_DISPLAY_NAME, displayName)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun getDisplayName(): String? = prefs.getString(KEY_DISPLAY_NAME, null)

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun clearLoginInfo() {
        prefs.edit().clear().apply()
    }

    fun isDarkMode(): Boolean = prefs.getBoolean("dark_mode", false)

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean("dark_mode", enabled).apply()
    }

    fun isNotificationEnabled(): Boolean = prefs.getBoolean("notification_enabled", true)
    fun setNotificationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("notification_enabled", enabled).apply()
    }
}