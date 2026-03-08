package com.example.google

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import java.util.concurrent.CancellationException

/**
 * Service-Klasse für Google Login in der App.
 *
 * Diese Klasse übernimmt:
 * - Google Sign-In über CredentialManager
 * - Authentifizierung bei Firebase
 * - Abmelden des Benutzers
 *
 * Sie wird im LoginScreen und in AppContent verwendet.
 */
class GoogleAuthService(private val context: Context) {

    // CredentialManager verwaltet Login-Credentials (Google / Passkeys etc.)
    private val credentialManager = CredentialManager.create(context)

    // Firebase Authentication Instanz
    private val firebaseAuth = FirebaseAuth.getInstance()

    /**
     * Prüft, ob bereits ein Benutzer eingeloggt ist.
     */
    fun isSignedIn(): Boolean =
        firebaseAuth.currentUser != null

    /**
     * Startet den Google Login.
     *
     * Ablauf:
     * 1. Credential Request erstellen
     * 2. Google Konto auswählen
     * 3. ID Token erhalten
     * 4. Mit Firebase authentifizieren
     */
    suspend fun signIn(): Boolean {

        if (isSignedIn()) return true

        try {

            val result = buildCredentialRequest()

            return handleSingIn(result)

        } catch (e: Exception) {

            if (e is CancellationException) throw e

            e.printStackTrace()

            return false
        }
    }

    /**
     * Verarbeitet das Login-Ergebnis.
     *
     * Hier wird:
     * - das Google ID Token gelesen
     * - ein Firebase Credential erstellt
     * - der Benutzer bei Firebase angemeldet
     */
    private suspend fun handleSingIn(
        result: GetCredentialResponse
    ): Boolean {

        val credential = result.credential

        return if (
            credential is CustomCredential &&
            credential.type ==
            GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {

            try {

                val tokenCredential =
                    GoogleIdTokenCredential.createFrom(credential.data)

                val authCredential =
                    GoogleAuthProvider.getCredential(
                        tokenCredential.idToken,
                        null
                    )

                val authResult =
                    firebaseAuth
                        .signInWithCredential(authCredential)
                        .await()

                authResult.user != null

            } catch (e: GoogleIdTokenParsingException) {

                e.printStackTrace()

                false
            }

        } else {

            false
        }
    }

    /**
     * Erstellt eine Anfrage für Google Sign-In.
     *
     * Der Benutzer kann hier sein Google Konto auswählen.
     */
    private suspend fun buildCredentialRequest(): GetCredentialResponse {

        val request = GetCredentialRequest.Builder()

            .addCredentialOption(

                GetGoogleIdOption.Builder()

                    // zeigt alle Google Konten
                    .setFilterByAuthorizedAccounts(false)

                    // Google OAuth Client ID
                    .setServerClientId(
                        "140720041627-77ji31ktdlr2j4l55950tg35uiftlno5.apps.googleusercontent.com"
                    )

                    // automatische Auswahl deaktiviert
                    .setAutoSelectEnabled(false)

                    .build()
            )

            .build()

        return credentialManager.getCredential(
            request = request,
            context = context
        )
    }

    /**
     * Meldet den Benutzer ab.
     *
     * Ablauf:
     * - Credential Zustand löschen
     * - Firebase Logout durchführen
     */
    suspend fun signOut() {

        credentialManager.clearCredentialState(
            ClearCredentialStateRequest()
        )

        FirebaseAuth.getInstance().signOut()
    }
}