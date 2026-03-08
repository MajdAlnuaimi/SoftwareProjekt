package com.example.ui.screens.admin.rooms

/**
 * UI-Datenmodell für einen Raum im Adminbereich.
 *
 * Dieses Modell wird verwendet, um Räume im
 * AdminRoomsScreen darzustellen und zu bearbeiten.
 */
data class RoomUi(

    /**
     * Eindeutige ID des Raums.
     *
     * In Firestore ist dies die Dokument-ID.
     * In der App entspricht sie direkt dem Raumnamen
     * (z.B. "C0_08").
     */
    val id: String,

    /**
     * Sichtbarer Name des Raums.
     *
     * Wird im UI angezeigt.
     */
    val name: String,

    /**
     * Sortierreihenfolge des Raums.
     *
     * Dieser Wert bestimmt,
     * an welcher Position der Raum in der Liste erscheint.
     *
     * Beispiel:
     * 1 = erster Raum
     * 2 = zweiter Raum
     * 3 = dritter Raum
     */
    val order: Int
)

/**
 * UI-Datenmodell für einen Access Point.
 *
 * Wird im Adminbereich verwendet,
 * um die WLAN Access Points eines Raums darzustellen.
 */
data class ApUi(

    /**
     * Eindeutige ID des Access Points.
     *
     * Dies ist die Dokument-ID in der Firestore Collection
     * "access_points".
     */
    val id: String,

    /**
     * BSSID des WLAN Access Points.
     *
     * Das ist die MAC-Adresse des Routers,
     * z.B.:
     * aa:bb:cc:dd:ee:ff
     */
    val bssid: String
)