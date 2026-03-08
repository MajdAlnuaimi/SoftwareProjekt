package com.example.data

import androidx.core.net.toUri
import com.example.ui.screens.wizard.WizardState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Repository für alle Firebase-Operationen der App.
 *
 * Diese Klasse kümmert sich um:
 * - Hochladen von Fotos in Firebase Storage
 * - Speichern von Schadensmeldungen in Firestore
 * - Optionales Versenden von E-Mail-Benachrichtigungen
 *
 * Sie wird hauptsächlich vom Wizard-Prozess verwendet,
 * wenn eine neue Meldung abgesendet wird.
 */
class FirebaseRepository {

    // Firestore Datenbank
    private val db = FirebaseFirestore.getInstance()

    // Firebase Storage für Bild-Uploads
    private val storage = FirebaseStorage.getInstance()

    /**
     * Sendet eine neue Schadensmeldung an Firebase.
     *
     * Ablauf:
     * 1. Foto aus dem Wizard laden
     * 2. Foto in Firebase Storage hochladen
     * 3. Download-URL des Fotos holen
     * 4. Meldung in Firestore speichern
     * 5. Optional E-Mail-Benachrichtigung erstellen
     */
    suspend fun submitWizardReport(
        wizardState: WizardState,
        room: String,
        targetEmail: String?
    ) {

        /**
         * Foto aus dem Wizard-State holen.
         * Falls kein Foto vorhanden ist → Fehler.
         */
        val photoUriStr = wizardState.photoUri
            ?: throw IllegalStateException("Foto fehlt")

        val photoUri = photoUriStr.toUri()

        /**
         * Speicherort für das Bild in Firebase Storage.
         * UUID sorgt für einen eindeutigen Dateinamen.
         */
        val fileRef =
            storage.reference.child("wizard_reports/${UUID.randomUUID()}.jpg")

        /**
         * Foto hochladen
         */
        fileRef.putFile(photoUri).await()

        /**
         * Download-URL des hochgeladenen Bildes abrufen
         */
        val downloadUrl = fileRef.downloadUrl.await().toString()

        /**
         * Email des aktuell eingeloggten Benutzers
         */
        val userEmail = FirebaseAuth.getInstance().currentUser?.email

        /**
         * Daten der Schadensmeldung
         */
        val report = hashMapOf(

            "timestamp" to System.currentTimeMillis(),

            "room" to room,

            // Email des Meldung-Erstellers
            "userEmail" to userEmail,

            // Zielperson der Meldung
            "targetEmail" to (targetEmail ?: ""),

            // Bild-URL
            "photoUrl" to downloadUrl,

            // Hauptkategorie
            "hauptKategorieId" to wizardState.hauptKategorieId,
            "hauptKategorieLabel" to wizardState.hauptKategorieLabel,

            // Kategorie
            "kategorieId" to wizardState.kategorieId,
            "kategorieLabel" to wizardState.kategorieLabel,

            // Unterkategorie
            "unterkategorieId" to wizardState.unterkategorieId,
            "unterkategorieLabel" to wizardState.unterkategorieLabel,

            // Zustand
            "zustandId" to wizardState.zustandId,
            "zustandLabel" to wizardState.zustandLabel,

            // optionale Geräte-Nummer
            "geraeteNummer" to (wizardState.geraeteNummer ?: ""),

            // optionale Beschreibung
            "beschreibung" to (wizardState.beschreibung ?: ""),

            /**
             * Status der Meldung
             * Wird bei neuen Meldungen automatisch gesetzt
             */
            "reportStatus" to "EINGEGANGEN",

            "statusUpdatedAt" to System.currentTimeMillis()
        )

        /**
         * Meldung in Firestore speichern
         */
        db.collection("wizard_reports").add(report).await()

        /**
         * Optional: Email-Benachrichtigung erstellen,
         * wenn eine Zielperson existiert.
         */
        if (!targetEmail.isNullOrBlank()) {

            val zustandPart = wizardState.zustandLabel
                ?.takeIf { it.isNotBlank() }
                ?.let { "Zustand: $it\n" }
                ?: ""

            /**
             * Email-Daten (für Firebase Cloud Functions Mail-Service)
             */
            val mail = mapOf(
                "to" to targetEmail,
                "message" to mapOf(
                    "subject" to "Neue Schadensmeldung in Raum $room",
                    "text" to """
                Es wurde eine neue Meldung eingereicht.

                Raum: $room
                Kategorie: ${wizardState.kategorieLabel}
                Unterkategorie: ${wizardState.unterkategorieLabel}
                $zustandPart

                Beschreibung:
                ${wizardState.beschreibung ?: "-"}

                Bitte prüfen Sie die Meldung in der App.
            """.trimIndent()
                )
            )

            /**
             * Email-Auftrag in Firestore speichern
             * → wird später vom Mail-Service verarbeitet
             */
            db.collection("mail").add(mail).await()
        }
    }
}