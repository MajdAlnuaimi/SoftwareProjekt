package com.example.fingerprinting

/**
 * Enum zur Definition des Scan-Modus für die WLAN-Raumerkennung.
 *
 * Diese Modi bestimmen, wie viele WLAN-Scans durchgeführt werden,
 * um den Raum zu erkennen.
 *
 * SINGLE  → Es wird nur ein einzelner WLAN-Scan durchgeführt.
 * MULTI   → Es werden mehrere Scans durchgeführt, um ein stabileres
 *           und genaueres Ergebnis zu bekommen.
 */
enum class Mode {

    /**
     * Einzelner WLAN-Scan.
     * Schnell, aber eventuell weniger genau.
     */
    SINGLE,

    /**
     * Mehrere WLAN-Scans.
     * Dauert etwas länger, liefert aber stabilere Ergebnisse
     * bei der Raum-Erkennung.
     */
    MULTI
}