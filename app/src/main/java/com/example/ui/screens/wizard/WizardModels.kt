package com.example.ui.screens.wizard

/**
 * WizardLevelItemUi
 *
 * Dieses Datenmodell wird im Wizard verwendet,
 * um Kategorien, Unterkategorien oder andere Auswahlpunkte
 * im UI darzustellen.
 *
 * Die Daten kommen in der Regel aus der Firestore Collection "categories".
 */
data class WizardLevelItemUi(

    /**
     * Eindeutige ID des Elements
     *
     * Entspricht normalerweise der Firestore Dokument-ID.
     */
    val id: String,

    /**
     * Sichtbarer Name des Elements
     *
     * Wird im UI angezeigt (z.B. Kategorie-Name).
     */
    val label: String,

    /**
     * Schlüssel für das Icon.
     *
     * Dieser Key wird im IconRegistry verwendet,
     * um das passende Material Icon zu laden.
     *
     * Beispiel:
     * "pc"
     * "tisch"
     * "lampe"
     * "sonst"
     */
    val iconKey: String,

    /**
     * Reihenfolge des Elements.
     *
     * Wird verwendet um Kategorien
     * im UI sortiert darzustellen.
     *
     * Kleinere Werte erscheinen zuerst.
     */
    val order: Int,

    /**
     * Gibt an, ob nach der Auswahl dieses Elements
     * noch ein Zustand ausgewählt werden muss.
     *
     * Beispiel:
     * "kaputt"
     * "locker"
     * "fehlt"
     *
     * Standardwert = true
     */
    val hasZustand: Boolean = true,

    /**
     * Gibt an, ob eine Gerätenummer
     * abgefragt werden soll.
     *
     * Beispiel:
     * PC, Beamer, Drucker.
     *
     * Standardwert = false
     */
    val hasGeraeteNummer: Boolean = false
)