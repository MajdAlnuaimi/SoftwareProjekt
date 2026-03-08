package com.example.scan

import android.Manifest
import android.content.Context
import android.net.wifi.ScanResult
import androidx.annotation.RequiresPermission

/**
 * Kernklasse der Raumerkennung.
 *
 * Diese Klasse versucht anhand von WLAN-Scans
 * zu bestimmen, in welchem Raum sich das Gerät befindet.
 *
 * Vorgehensweise:
 * 1. Mehrere WLAN-Scans durchführen
 * 2. Access Points vergleichen
 * 3. Nur stabile Signale behalten
 * 4. Mit der Raum-Datenbank vergleichen
 */
object RoomDetection {

    /**
     * Führt mehrere WLAN-Scans durch und versucht
     * den passenden Raum zu bestimmen.
     *
     * Parameter:
     *
     * nTimes → Anzahl der Scans
     * delayBetweenMs → Pause zwischen Scans
     * minOccurrences → Wie oft ein Access Point vorkommen muss
     * minMatches → Mindestanzahl übereinstimmender Access Points
     * minScore → Mindestähnlichkeit für einen Treffer
     * rssiThresh → minimale Signalstärke
     */
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    suspend fun detectMulti(
        context: Context,
        nTimes: Int = 3,
        delayBetweenMs: Long = 1_000L,
        minOccurrences: Int = 2,
        minMatches: Int = 2,
        minScore: Double = 0.66,
        rssiThresh: Int = -85
    ): List<RoomMatch> {

        require(nTimes >= 1) { "nTimes muss >= 1 sein" }

        require(minOccurrences in 1..nTimes) {
            "minOccurrences muss zwischen 1 und nTimes liegen"
        }

        /**
         * Raumdatenbank laden
         * (Fingerprints der bekannten Räume)
         */
        val roomDb = RoomMatcher.loadRoomDb()

        /**
         * Mehrere WLAN-Scans durchführen
         */
        val scans: List<List<ScanResult>> = WifiScanner.multiScan(
            context = context,
            nTimes = nTimes,
            delayBetweenMs = delayBetweenMs
        )

        /**
         * Zählen wie oft jede BSSID vorkommt
         * (nur starke Signale werden berücksichtigt)
         */
        val counts = mutableMapOf<String, Int>()

        for (snap in scans) {

            val setInSnap = snap
                .filter {
                    it.BSSID != null &&
                            it.level >= rssiThresh
                }
                .map {
                    RoomMatcher.normalizeBssid(it.BSSID)
                }
                .toSet() // pro Scan nur einmal zählen

            for (b in setInSnap) {

                counts[b] = (counts[b] ?: 0) + 1
            }
        }

        /**
         * Nur Access Points behalten,
         * die häufig genug vorkommen.
         */
        val robustCurrent: Set<String> =
            counts.filter { it.value >= minOccurrences }
                .keys

        /**
         * Vergleich mit der Raum-Datenbank
         */
        return RoomMatcher.matchRooms(

            current = robustCurrent,

            roomDb = roomDb,

            minMatches = minMatches,

            requireScoreAtLeast = minScore
        )
    }
}