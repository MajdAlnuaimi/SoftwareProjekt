package com.example.ui.screens.wizard

import java.io.Serializable

/**
 * WizardState
 *
 * Dieses Datenmodell speichert den aktuellen Zustand
 * des gesamten Schaden-Melde-Wizards.
 *
 * Der Wizard besteht aus mehreren Screens:
 *
 * Hauptkategorie → Kategorie → Unterkategorie → Zustand → Summary
 *
 * Alle gewählten Werte werden hier gespeichert,
 * damit sie zwischen den Screens weitergegeben werden können.
 *
 * Das Objekt wird während des Wizards immer wieder aktualisiert.
 */
data class WizardState(

    /**
     * Level 1 – Hauptkategorie
     *
     * ID der ausgewählten Hauptkategorie
     * (Firestore Dokument-ID)
     */
    val hauptKategorieId: String? = null,

    /**
     * Name der Hauptkategorie
     *
     * Wird im UI angezeigt.
     */
    val hauptKategorieLabel: String? = null,

    /**
     * Level 2 – Kategorie
     *
     * ID der ausgewählten Kategorie.
     */
    val kategorieId: String? = null,

    /**
     * Name der Kategorie
     *
     * Wird im UI angezeigt.
     */
    val kategorieLabel: String? = null,

    /**
     * Gibt an ob eine Gerätenummer
     * abgefragt werden muss.
     *
     * Beispiel:
     * PC, Drucker oder Beamer.
     */
    val hasGeraeteNummer: Boolean = false,

    /**
     * Level 3 – Unterkategorie
     *
     * ID der Unterkategorie.
     */
    val unterkategorieId: String? = null,

    /**
     * Name der Unterkategorie
     *
     * Wird im UI angezeigt.
     */
    val unterkategorieLabel: String? = null,

    /**
     * Zustand des Problems
     *
     * Beispiel:
     * Defekt
     * Teilweise defekt
     */
    val zustandId: String? = null,

    /**
     * Sichtbarer Text des Zustands
     */
    val zustandLabel: String? = null,

    /**
     * Gerätenummer
     *
     * Wird nur benötigt wenn
     * hasGeraeteNummer = true
     */
    val geraeteNummer: String? = null,

    /**
     * Beschreibung des Problems
     *
     * Optionaler Text des Nutzers.
     */
    val beschreibung: String? = null,

    /**
     * URI des hochgeladenen Fotos.
     *
     * Kann aus Kamera oder Galerie kommen.
     */
    val photoUri: String? = null

) : Serializable


/**
 * Zustand Enum
 *
 * Definiert mögliche Zustände eines Problems.
 *
 * Diese Werte können im ZustandScreen angezeigt werden.
 */
enum class Zustand(

    /**
     * Anzeige-Text des Zustands
     */
    val label: String

) : Serializable {

    /**
     * Gerät ist komplett defekt
     */
    Defekt("Defekt"),

    /**
     * Gerät funktioniert teilweise
     */
    TeilweiseDefekt("Teilweise defekt"),
}