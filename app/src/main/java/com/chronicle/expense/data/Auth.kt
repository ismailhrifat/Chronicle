package com.chronicle.expense.data

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

object Auth {
    private val auth get() = FirebaseAuth.getInstance()

    /** Shows the Google account sheet. Null when the response is not a Google ID token; throws on cancel. */
    suspend fun googleCredential(activity: Activity): GoogleIdTokenCredential? {
        val resId = activity.resources.getIdentifier("default_web_client_id", "string", activity.packageName)
        if (resId == 0) throw IllegalStateException("Google sign-in is not enabled for this Firebase project yet.")
        val option = GetSignInWithGoogleOption.Builder(activity.getString(resId)).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = CredentialManager.create(activity).getCredential(activity, request).credential
        return if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            GoogleIdTokenCredential.createFrom(credential.data)
        } else null
    }

    /** Returns null on success or user cancel, otherwise a message to show. */
    suspend fun signInWithGoogle(activity: Activity): String? {
        return try {
            val google = googleCredential(activity) ?: return "Unexpected sign-in response. Please try again."
            auth.signInWithCredential(GoogleAuthProvider.getCredential(google.idToken, null)).await()
            null
        } catch (e: GetCredentialCancellationException) {
            null
        } catch (e: Exception) {
            e.localizedMessage ?: "Sign-in failed. Please try again."
        }
    }

    suspend fun signOut(context: Context) {
        auth.signOut()
        runCatching { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
    }
}
