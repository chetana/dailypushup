package com.cyin.daily_push_up.auth

import android.content.Context
import android.util.Base64
import org.json.JSONObject

object TokenStore {

    private const val PREFS_NAME = "auth_prefs"
    private const val KEY_ID_TOKEN = "google_id_token"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_TOKEN_EXP = "token_exp" // unix seconds

    private const val REFRESH_MARGIN_SEC = 5 * 60L // refresh 5 min before expiry

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveToken(context: Context, idToken: String, email: String?, name: String?) {
        val exp = parseJwtExp(idToken)
        prefs(context).edit()
            .putString(KEY_ID_TOKEN, idToken)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_USER_NAME, name)
            .putLong(KEY_TOKEN_EXP, exp)
            .apply()
    }

    fun getToken(context: Context): String? =
        prefs(context).getString(KEY_ID_TOKEN, null)

    fun isLoggedIn(context: Context): Boolean =
        getToken(context) != null

    /** Returns true if the token is expired or expires within 5 minutes */
    fun isExpiringSoon(context: Context): Boolean {
        val exp = prefs(context).getLong(KEY_TOKEN_EXP, 0L)
        if (exp == 0L) return false
        val nowSec = System.currentTimeMillis() / 1000
        return nowSec >= exp - REFRESH_MARGIN_SEC
    }

    fun getUserEmail(context: Context): String? =
        prefs(context).getString(KEY_USER_EMAIL, null)

    fun getUserName(context: Context): String? =
        prefs(context).getString(KEY_USER_NAME, null)

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    private fun parseJwtExp(token: String): Long {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return 0L
            val payload = Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING)
            val json = JSONObject(String(payload, Charsets.UTF_8))
            json.optLong("exp", 0L)
        } catch (e: Exception) {
            0L
        }
    }
}
