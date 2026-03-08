package com.example.state

/**
 * Zustand für die WLAN-Sicherheitsprüfung der App.
 *
 * Diese Klasse speichert Informationen darüber:
 * - welche WLAN-Netzwerke erlaubt sind
 * - mit welchem WLAN das Gerät aktuell verbunden ist
 * - ob der Zugriff erlaubt ist
 */
data class WifiGuardState(

    /** Liste der erlaubten WLAN-SSIDs */
    val allowedSet: Set<String> = emptySet(),

    /** Aktuell verbundenes WLAN */
    val currentSsid: String? = null,

    /** Ergebnis der WLAN-Prüfung (true = erlaubt, false = blockiert) */
    val wifiAllowed: Boolean? = null,

    /** Gibt an, ob gerade ein erneuter Verbindungsversuch läuft */
    val isRetrying: Boolean = false,

    /** Zeigt an, ob die WLAN-Konfiguration bereits geladen wurde */
    val configLoaded: Boolean = false
)

/**
 * Zustand für Benutzerrechte innerhalb der App.
 *
 * Wird verwendet um zu bestimmen:
 * - welche Menüpunkte sichtbar sind
 * - welche Funktionen erlaubt sind
 */
data class UserAccessState(

    /** Benutzer ist eingeloggt */
    val isSignedIn: Boolean = false,

    /** Benutzer ist Administrator */
    val isAdmin: Boolean = false,

    /** Benutzer hat SuperAdmin-Rechte */
    val isSuperAdmin: Boolean = false,

    /** Benutzer ist für bestimmte Meldungen zuständig */
    val isResponsibleUser: Boolean = false
)

/**
 * Zustand für die Navigation im Admin-Bereich.
 *
 * Diese Struktur speichert, in welcher Hierarchie
 * sich der Benutzer im Admin-Menü befindet.
 */
data class AdminNavigationState(

    /** ID der aktuell ausgewählten Hauptkategorie */
    val parentId: String? = null,

    /** Name der Hauptkategorie */
    val parentLabel: String = "",

    /** Icon-Schlüssel der Hauptkategorie */
    val parentIconKey: String = "more",

    /** ID der ausgewählten Unterkategorie (Ebene 2) */
    val level2ParentId: String? = null,

    /** Name der Unterkategorie */
    val level2ParentLabel: String = "",

    /** Icon-Schlüssel der Unterkategorie */
    val level2ParentIconKey: String = "more"
)