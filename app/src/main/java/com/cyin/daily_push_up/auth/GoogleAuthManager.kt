package com.cyin.daily_push_up.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

object GoogleAuthManager {

    // Web Client ID from Google Cloud Console
    private const val WEB_CLIENT_ID = "267131866578-m6rua7ccatqno7lp0t0jscsrvsf69u4f.apps.googleusercontent.com"

    suspend fun signIn(context: Context): GoogleIdTokenCredential {
        val credentialManager = CredentialManager.create(context)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(WEB_CLIENT_ID)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(context, request)
        return GoogleIdTokenCredential.createFrom(result.credential.data)
    }

    suspend fun signOut(context: Context) {
        val credentialManager = CredentialManager.create(context)
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
    }
}
