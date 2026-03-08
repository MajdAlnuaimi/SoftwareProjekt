package com.example.icons

/**
 * Enum zur Gruppierung von Icons innerhalb der App.
 *
 * Diese Kategorien helfen dabei,
 * Icons thematisch zu organisieren und z.B. im Adminbereich
 * übersichtlich anzuzeigen.
 *
 * Beispiel:
 * Beim Erstellen oder Bearbeiten einer Kategorie kann
 * ein passendes Icon aus einer bestimmten Gruppe ausgewählt werden.
 */
enum class IconCategory {

    /**
     * Icons für Hardware-Geräte
     * z.B. Drucker, Computer, Monitor
     */
    Hardware,

    /**
     * Icons für Gebäude oder Räume
     * z.B. Raum, Gebäude, Büro
     */
    Gebaeude,

    /**
     * Icons für Netzwerk oder Internet
     * z.B. WLAN, Router, Verbindung
     */
    Internet,

    /**
     * Sonstige Icons,
     * die nicht in die anderen Kategorien passen.
     */
    Sonstiges
}
