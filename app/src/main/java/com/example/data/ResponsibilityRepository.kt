package com.example.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Repository für Zuständigkeiten in der App.
 *
 * Diese Klasse verwaltet:
 * - welche Person für eine bestimmte Kombination aus Raum + Kategorie zuständig ist
 * - Speicherung und Abruf dieser Zuständigkeiten aus Firebase Firestore
 *
 * Sie wird hauptsächlich im Adminbereich und beim Absenden
 * einer Schadensmeldung verwendet.
 */
object ResponsibilityRepository {

    /**
     * Zugriff auf die Firestore-Datenbank
     */
    private val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    /**
     * Erzeugt eine eindeutige Dokument-ID für Firestore.
     *
     * Kombination aus:
     * Raum-ID + Kategorie-ID
     *
     * Beispiel:
     * roomA__printer
     *
     * Sonderzeichen "/" werden ersetzt, damit Firestore keine Fehler erzeugt.
     */
    private fun docId(roomId: String, categoryId: String): String {

        fun safe(s: String) = s.trim().replace("/", "_")

        return "${safe(roomId)}__${safe(categoryId)}"
    }

    /**
     * Speichert oder aktualisiert eine Zuständigkeit.
     *
     * Wird im Adminbereich verwendet.
     *
     * Verhalten:
     * - wenn Eintrag existiert → Update
     * - wenn Eintrag nicht existiert → Neuer Eintrag
     */
    suspend fun saveResponsibility(
        roomId: String,
        categoryId: String,
        email: String
    ) {

        val cleanRoom = roomId.trim()
        val cleanCat = categoryId.trim()
        val cleanEmail = email.trim()

        val data = mapOf(
            "roomId" to cleanRoom,
            "categoryId" to cleanCat,
            "email" to cleanEmail
        )

        db.collection("responsibilities")
            .document(docId(cleanRoom, cleanCat))
            .set(data, SetOptions.merge())
            .await()
    }

    /**
     * Holt die zuständige Email für eine Raum + Kategorie Kombination.
     */
    suspend fun getResponsibleEmail(
        roomId: String,
        categoryId: String
    ): String? {

        val cleanRoom = roomId.trim()
        val cleanCat = categoryId.trim()

        val doc = db.collection("responsibilities")
            .document(docId(cleanRoom, cleanCat))
            .get()
            .await()

        return doc.getString("email")
    }

    /**
     * Hierarchische Suche nach einer zuständigen Person.
     *
     * Reihenfolge der Suche:
     *
     * 1. Unterkategorie
     * 2. Kategorie
     * 3. Hauptkategorie
     *
     * Dadurch kann eine Zuständigkeit auch auf höherer Ebene
     * definiert werden.
     */
    suspend fun getResponsibleEmailHierarchical(
        roomId: String,
        hauptKategorieId: String?,
        kategorieId: String?,
        unterKategorieId: String?
    ): String? {

        val candidates = listOfNotNull(
            unterKategorieId,
            kategorieId,
            hauptKategorieId
        )

        for (catId in candidates) {

            val email = getResponsibleEmail(roomId, catId)

            if (!email.isNullOrBlank()) return email
        }

        return null
    }

    /**
     * Prüft ob eine Person überhaupt eine Zuständigkeit besitzt.
     *
     * Wird verwendet um festzustellen,
     * ob ein Benutzer im Menü den Bereich
     * "Zuständige Meldungen" sehen darf.
     */
    suspend fun hasAnyResponsibility(email: String): Boolean {

        val snap = db.collection("responsibilities")
            .whereEqualTo("email", email.trim())
            .limit(1)
            .get()
            .await()

        return !snap.isEmpty
    }
}