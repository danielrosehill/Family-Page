package com.danielrosehill.familypager.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SettingsRepository(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "family_pager_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var yourName: String
        get() = prefs.getString(KEY_YOUR_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_YOUR_NAME, value).apply()

    var spouseName: String
        get() = prefs.getString(KEY_SPOUSE_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SPOUSE_NAME, value).apply()

    var appApiToken: String
        get() = prefs.getString(KEY_APP_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_APP_TOKEN, value).apply()

    var spouseUserKey: String
        get() = prefs.getString(KEY_SPOUSE_USER_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SPOUSE_USER_KEY, value).apply()

    var yourUserKey: String
        get() = prefs.getString(KEY_YOUR_USER_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_YOUR_USER_KEY, value).apply()

    var pushoverEmail: String
        get() = prefs.getString(KEY_PUSHOVER_EMAIL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PUSHOVER_EMAIL, value).apply()

    var pushoverPassword: String
        get() = prefs.getString(KEY_PUSHOVER_PASSWORD, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PUSHOVER_PASSWORD, value).apply()

    var receivePages: Boolean
        get() = prefs.getBoolean(KEY_RECEIVE_PAGES, false)
        set(value) = prefs.edit().putBoolean(KEY_RECEIVE_PAGES, value).apply()

    var sessionSecret: String
        get() = prefs.getString(KEY_SESSION_SECRET, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SESSION_SECRET, value).apply()

    var deviceId: String
        get() = prefs.getString(KEY_DEVICE_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DEVICE_ID, value).apply()

    val isConfigured: Boolean
        get() = yourName.isNotBlank() && spouseName.isNotBlank() &&
                appApiToken.isNotBlank() && spouseUserKey.isNotBlank()

    val isReceiveConfigured: Boolean
        get() = receivePages && sessionSecret.isNotBlank() && deviceId.isNotBlank()

    companion object {
        private const val KEY_YOUR_NAME = "your_name"
        private const val KEY_SPOUSE_NAME = "spouse_name"
        private const val KEY_APP_TOKEN = "app_api_token"
        private const val KEY_SPOUSE_USER_KEY = "spouse_user_key"
        private const val KEY_YOUR_USER_KEY = "your_user_key"
        private const val KEY_PUSHOVER_EMAIL = "pushover_email"
        private const val KEY_PUSHOVER_PASSWORD = "pushover_password"
        private const val KEY_RECEIVE_PAGES = "receive_pages"
        private const val KEY_SESSION_SECRET = "session_secret"
        private const val KEY_DEVICE_ID = "device_id"
    }
}
