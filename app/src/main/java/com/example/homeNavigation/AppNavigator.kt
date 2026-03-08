package com.example.homeNavigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Einfache Navigationsklasse für die App.
 *
 * Diese Klasse verwaltet:
 * - den aktuell angezeigten Screen
 * - die Navigationshistorie (BackStack)
 * - spezielle Flags für bestimmte Navigationsfälle
 *
 * Sie ersetzt hier ein komplexeres Navigation-Framework
 * und ermöglicht eine leichte Steuerung der Screens.
 */
class AppNavigator(initialScreen: String = "Home") {

    /**
     * Aktuell ausgewählter Screen.
     * Wird von Compose beobachtet und löst UI-Updates aus.
     */
    var selectedMenuItem by mutableStateOf(initialScreen)

    /**
     * BackStack speichert die Navigationshistorie.
     * Wird genutzt für die Zurück-Navigation.
     */
    val backStack = mutableStateListOf<String>()

    /**
     * Flag um beim Zurückkehren zum Home-Screen
     * den automatischen Raum-Scan einmal zu überspringen.
     */
    var skipAutoScanHomeOnce by mutableStateOf(false)

    /**
     * Navigiert zu einem neuen Screen.
     *
     * Ablauf:
     * 1. aktueller Screen wird im BackStack gespeichert
     * 2. neuer Screen wird aktiviert
     */
    fun navigateTo(screen: String, canNavigate: Boolean = true) {

        if (!canNavigate || screen == selectedMenuItem) return

        backStack.add(selectedMenuItem)

        selectedMenuItem = screen
    }

    /**
     * Setzt die Navigation komplett zurück
     * und öffnet einen neuen Screen.
     *
     * Beispiel:
     * Nach Login oder Logout.
     */
    fun resetTo(screen: String, canNavigate: Boolean = true) {

        if (!canNavigate) return

        backStack.clear()

        selectedMenuItem = screen
    }

    /**
     * Navigiert einen Schritt zurück.
     *
     * Wenn kein Eintrag im BackStack existiert,
     * wird false zurückgegeben.
     */
    fun navigateBack(): Boolean {

        if (backStack.isEmpty()) return false

        val from = selectedMenuItem

        val to = backStack.removeAt(backStack.lastIndex)

        /**
         * Spezialfall:
         * Wenn der Benutzer von der Raum-Erkennung
         * zurück zum Home-Screen navigiert,
         * soll der automatische Raum-Scan einmal übersprungen werden.
         */
        if (from == "RoomDetection" && to == "Home") {
            skipAutoScanHomeOnce = true
        }

        selectedMenuItem = to

        return true
    }
}