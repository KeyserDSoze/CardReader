package com.keyserdsoze.cardreader

import android.accounts.Account
import android.app.Activity
import android.content.Intent
import android.content.MutableContextWrapper
import androidx.activity.result.IntentSenderRequest
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.keyserdsoze.cardreader.data.GoogleAccountIdentity
import com.keyserdsoze.cardreader.data.cloud.DRIVE_APPDATA_SCOPE

sealed interface GoogleSignInResult {
    data class Success(val account: GoogleAccountIdentity) : GoogleSignInResult
    data object Canceled : GoogleSignInResult
    data class Failure(val message: String) : GoogleSignInResult
}

class GoogleAccountCoordinator(private val activity: Activity) {
    private val credentialManager = CredentialManager.create(activity)
    private val authorizationClient = Identity.getAuthorizationClient(activity)
    private val driveScope = Scope(DRIVE_APPDATA_SCOPE)

    suspend fun signIn(serverClientId: String): GoogleSignInResult {
        if (serverClientId.isBlank()) return GoogleSignInResult.Failure("Google sync is not configured in this build")
        val option = GetSignInWithGoogleOption.Builder(serverClientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        return try {
            val credential = credentialManager.getCredential(
                request = request,
                context = MutableContextWrapper(activity),
            ).credential
            val custom = credential as? CustomCredential ?: return GoogleSignInResult.Failure("Unsupported credential")
            if (custom.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                return GoogleSignInResult.Failure("Unsupported Google credential")
            }
            val google = GoogleIdTokenCredential.createFrom(custom.data)
            val email = google.email ?: return GoogleSignInResult.Failure("Google account has no email")
            GoogleSignInResult.Success(GoogleAccountIdentity(google.uniqueId, email, google.displayName))
        } catch (_: GetCredentialCancellationException) {
            GoogleSignInResult.Canceled
        } catch (error: GetCredentialException) {
            GoogleSignInResult.Failure(error.message ?: "Google sign-in failed")
        } catch (error: GoogleIdTokenParsingException) {
            GoogleSignInResult.Failure(error.message ?: "Google credential is invalid")
        } catch (error: IllegalArgumentException) {
            GoogleSignInResult.Failure(error.message ?: "Google sign-in configuration is invalid")
        }
    }

    fun requestDriveAuthorization(
        accountEmail: String,
        onAuthorized: () -> Unit,
        onResolution: (IntentSenderRequest) -> Unit,
        onFailure: () -> Unit,
    ) {
        val request = AuthorizationRequest.builder()
            .setAccount(Account(accountEmail, GOOGLE_ACCOUNT_TYPE))
            .setRequestedScopes(listOf(driveScope))
            .build()
        authorizationClient.authorize(request)
            .addOnSuccessListener { result ->
                if (result.hasResolution()) {
                    result.pendingIntent?.let { onResolution(IntentSenderRequest.Builder(it.intentSender).build()) } ?: onFailure()
                } else {
                    onAuthorized()
                }
            }
            .addOnFailureListener { onFailure() }
    }

    fun finishDriveAuthorization(data: Intent?): Boolean = data != null && runCatching {
        authorizationClient.getAuthorizationResultFromIntent(data)
        true
    }.getOrDefault(false)

    fun disconnect(accountEmail: String, onComplete: () -> Unit, onFailure: () -> Unit) {
        val request = RevokeAccessRequest.builder()
            .setAccount(Account(accountEmail, GOOGLE_ACCOUNT_TYPE))
            .setScopes(listOf(driveScope))
            .build()
        authorizationClient.revokeAccess(request)
            .addOnSuccessListener { onComplete() }
            .addOnFailureListener { onFailure() }
    }

    suspend fun clearCredentialSession() {
        runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
    }

    private companion object { const val GOOGLE_ACCOUNT_TYPE = "com.google" }
}
