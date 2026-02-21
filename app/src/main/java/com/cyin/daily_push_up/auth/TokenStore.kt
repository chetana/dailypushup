package com.cyin.daily_push_up.auth

import android.content.Context

object TokenStore {

    private const val PREFS_NAME = "auth_prefs"
    private const val KEY_ID_TOKEN = "google_id_token"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_NAME = "user_name"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveToken(context: Context, idToken: String, email: String?, name: String?) {
        prefs(context).edit()
            .putString(KEY_ID_TOKEN, idToken)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_USER_NAME, name)
            .apply()
    }

    fun getToken(context: Context): String? =
        prefs(context).getString(KEY_ID_TOKEN, null)

    fun isLoggedIn(context: Context): Boolean =
        getToken(context) != null

    fun getUserEmail(context: Context): String? =
        prefs(context).getString(KEY_USER_EMAIL, null)

    fun getUserName(context: Context): String? =
        prefs(context).getString(KEY_USER_NAME, null)

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
