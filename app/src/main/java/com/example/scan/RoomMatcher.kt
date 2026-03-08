package com.example.scan

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Datenklasse für ein Matching-Ergebnis.
 *
 * Beschreibt, wie gut ein Raum zum aktuellen WLAN-Scan passt.
 */
data class RoomMatch(

    /** Name des Raumes */
    val roomName: String,

    /** Anzahl übereinstimmender Access Points */
    val matchCount: Int,

    /** Anzahl aller Access Points im Raum-Fingerprint */
    val roomSize: Int,

    /** Ähnlichkeitsscore (0.0 – 1.0) */
    val score: Double
)

/**
 * Klasse zum Vergleichen von WLAN-Fingerprints mit gespeicherten Räumen.
 *
 * Aufgaben:
 * - Raumdaten aus Firestore laden
 * - BSSID normalisieren
 * - aktuelle WLAN-Scans mit bekannten Räumen vergleichen
 */
object RoomMatcher {

    private const val ROOMS_COLLECTION = "rooms"
    private const val ACCESS_POINTS_COLLECTION = "access_points"

    /**
     * Lädt alle Räume und deren Access Points aus Firestore.
     *
     * Ergebnisstruktur:
     *
     * roomName → Set<BSSID>
     *
     * Beispiel:
     *
     * Raum A → {ap1, ap2, ap3}
     * Raum B → {ap4, ap5}
     */
    suspend fun loadRoomDb(): Map<String, Set<String>> {

        val db = FirebaseFirestore.getInstance()

        // 1️ Räume laden
        val roomsSnapshot = db.collection(ROOMS_COLLECTION).get().await()

        val roomDb = mutableMapOf<String, MutableSet<String>>()

        for (doc in roomsSnapshot.documents) {

            roomDb[doc.id] = mutableSetOf()
        }

        // 2 Access Points laden
        val apSnapshot = db.collection(ACCESS_POINTS_COLLECTION).get().await()

        for (doc in apSnapshot.documents) {

            val roomId = doc.getString("roomId") ?: continue

            val bssid = doc.getString("bssid") ?: continue

            roomDb[roomId]?.add(normalizeBssid(bssid))
        }

        return roomDb
    }

    /**
     * Normalisiert eine BSSID.
     *
     * Entfernt:
     * - Leerzeichen
     * - Groß/Kleinschreibung
     */
    fun normalizeBssid(raw: String?): String =
        raw?.trim()?.lowercase() ?: ""

    /**
     * Vergleicht aktuelle Access Points mit allen gespeicherten Räumen.
     *
     * Parameter:
     *
     * current → aktuelle BSSIDs
     * roomDb → Raumdatenbank
     * minMatches → minimale Anzahl an Treffern
     * requireScoreAtLeast → minimaler Score
     */
    fun matchRooms(
        current: Set<String>,
        roomDb: Map<String, Set<String>>,
        minMatches: Int = 1,
        requireScoreAtLeast: Double = 0.0
    ): List<RoomMatch> {

        // aktuelle BSSIDs normalisieren
        val normalizedCurrent = current.map {
            normalizeBssid(it)
        }.toSet()

        val out = mutableListOf<RoomMatch>()

        // jeden Raum prüfen
        for ((room, apSet) in roomDb) {

            if (apSet.isEmpty()) continue

            // Anzahl der übereinstimmenden Access Points
            val matches = apSet.count { normalizedCurrent.contains(it) }

            // Ähnlichkeit berechnen
            val score = matches.toDouble() / apSet.size.toDouble()

            // nur gültige Treffer behalten
            if (matches >= minMatches && score >= requireScoreAtLeast) {

                out += RoomMatch(
                    room,
                    matches,
                    apSet.size,
                    score
                )
            }
        }

        // beste Treffer zuerst
        return out.sortedByDescending { it.score }
    }
}