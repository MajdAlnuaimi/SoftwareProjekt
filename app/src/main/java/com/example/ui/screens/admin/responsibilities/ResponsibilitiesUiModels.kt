package com.example.ui.screens.admin.responsibilities

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * UI-Datenmodell für eine Hauptkategorie im Adminbereich.
 *
 * Dieses Modell wird verwendet, um Hauptkategorien
 * im Screen darzustellen (z.B. als Kachel im Grid).
 */
data class HauptKategorieUi(

    // Eindeutige ID der Kategorie (Firestore Dokument-ID)
    val id: String,

    // Sichtbarer Name der Hauptkategorie
    val label: String,

    // Icon der Kategorie (Compose ImageVector)
    val icon: ImageVector,

    // Hintergrundfarbe der Kachel im UI
    val tileColor: Color
)

/**
 * UI-Datenmodell für Kategorien und Unterkategorien
 * im Wizard bzw. im Adminbereich.
 *
 * Dieses Modell wird für Ebene 2 und Ebene 3 verwendet.
 */
data class WizardLevelItemUi(

    // Eindeutige ID der Kategorie (Firestore Dokument-ID)
    val id: String,

    // Name der Kategorie / Unterkategorie
    val label: String,

    // Schlüssel für das Icon
    // Dieser wird später über die IconRegistry
    // in ein echtes Compose-Icon umgewandelt
    val iconKey: String,

    // Reihenfolge der Kategorie in der Liste
    // Wird verwendet für die Sortierung im UI
    val order: Int
)