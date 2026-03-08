package com.example.ui.screens.admin.categories

/**
 * UI-Datenmodell für Kategorien und Unterkategorien
 * im Admin-Bereich.
 *
 * Diese Klasse wird verwendet, um Firestore-Daten
 * in einer für die Oberfläche passenden Form darzustellen.
 *
 * Sie enthält die wichtigsten Informationen, die der
 * Admin-Screen für Anzeige, Bearbeitung und Sortierung braucht.
 */
data class SubUiItem(

    /**
     * Eindeutige Dokument-ID aus Firestore.
     *
     * Diese ID wird verwendet für:
     * - Bearbeiten
     * - Löschen
     * - Sortierung speichern
     * - Navigation in Unterebenen
     */
    val id: String,

    /**
     * Sichtbarer Name der Kategorie / Unterkategorie.
     */
    val label: String,

    /**
     * Schlüssel des zugehörigen Icons.
     *
     * Dieser Key wird über die IconRegistry
     * in ein echtes Compose-Icon umgewandelt.
     */
    val iconKey: String,

    /**
     * Reihenfolge des Elements in der Liste.
     *
     * Wird für die Sortierung im Adminbereich verwendet.
     * Kleinere Werte werden weiter oben angezeigt.
     */
    val order: Int,

    /**
     * Nur relevant für Ebene 3.
     *
     * Gibt an, ob nach der Auswahl dieser Unterkategorie
     * im Wizard noch eine Zustandsseite angezeigt werden soll.
     *
     * Standardwert:
     * true
     */
    val hasZustand: Boolean = true,

    /**
     * Nur relevant für Ebene 2.
     *
     * Gibt an, ob im Wizard für diese Kategorie
     * eine Gerätenummer eingegeben werden muss.
     *
     * Standardwert:
     * false
     */
    val hasGeraeteNummer: Boolean = false
)