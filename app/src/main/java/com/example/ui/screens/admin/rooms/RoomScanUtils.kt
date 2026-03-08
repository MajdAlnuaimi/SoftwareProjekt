package com.example.ui.screens.admin.rooms

import com.example.fingerprinting.WifiFingerprint

/**
 * Regulärer Ausdruck zur Prüfung,
 * ob eine BSSID im gültigen MAC-Adress-Format vorliegt.
 *
 * Beispiel für gültiges Format:
 * aa:bb:cc:dd:ee:ff
 */
val BSSID_REGEX = Regex("^([0-9a-f]{2}:){5}[0-9a-f]{2}$")

/**
 * Normalisiert einen eingegebenen Raumnamen.
 *
 * Ablauf:
 * 1. äußere Leerzeichen entfernen
 * 2. falls der String leer ist → direkt zurückgeben
 * 3. innere Leerzeichen, Tabs und Zeilenumbrüche entfernen
 * 4. ersten Buchstaben groß schreiben
 *
 * Beispiel:
 * " c0_08 " -> "C0_08"
 */
fun normalizeRoomName(input: String): String {

    // äußere Leerzeichen entfernen
    val t = input.trim()

    // falls leer → direkt zurückgeben
    if (t.isEmpty()) return t

    // Leerzeichen, Tabs und Zeilenumbrüche entfernen
    val cleaned = t.replace(" ", "").replace("\t", "").replace("\n", "")

    // ersten Buchstaben groß schreiben
    return cleaned.replaceFirstChar { it.uppercase() }
}

/**
 * Liefert die stärksten BSSIDs aus einem WLAN-Fingerprint.
 *
 * Vorgehen:
 * 1. Fingerprint-Map in Liste umwandeln
 * 2. nach Signalstärke absteigend sortieren
 * 3. nur die BSSID-Werte übernehmen
 * 4. alles in lowercase umwandeln
 * 5. auf die gewünschte Anzahl begrenzen
 *
 * Standard:
 * Es werden die Top 3 BSSIDs zurückgegeben.
 */
fun pickTopBssids(fp: WifiFingerprint, limit: Int = 3): List<String> {

    return fp.bssids
        .toList()                          // Map -> List<Pair<BSSID, RSSI>>
        .sortedByDescending { it.second }  // stärkstes Signal zuerst
        .map { it.first.lowercase() }      // nur BSSID, in lowercase
        .take(limit)                       // auf gewünschte Anzahl begrenzen
}