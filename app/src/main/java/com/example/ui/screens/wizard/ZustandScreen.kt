package com.example.ui.screens.wizard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * ZustandScreen
 *
 * Dieser Screen gehört zum Wizard (Schaden melden).
 *
 * Hier wählt der Benutzer den Zustand des Problems aus,
 * z.B.:
 * - Komplett defekt
 * - Teilweise defekt
 *
 * Der Screen erhält den aktuellen WizardState und zeigt
 * oben eine Breadcrumb-Navigation der bisherigen Auswahl.
 */
@Composable
fun ZustandScreen(
    /**
     * Aktueller Zustand des Wizards.
     * Enthält bereits ausgewählte Kategorien.
     */
    wizardState: WizardState,
    /**
     * Callback für den Zurück Button.
     */
    onBack: () -> Unit,
    /**
     * Callback wenn ein Zustand ausgewählt wird.
     *
     * id = technischer Wert (z.B. "defekt")
     * label = sichtbarer Text
     */
    onSelect: (id: String, label: String) -> Unit
) {

    // Hintergrundfarbe des Screens
    val pageBg = Color(0xFFF4F6FA)
    // Farbe des Titels
    val titleColor = Color(0xFF0F172A)
    // Farbe der Breadcrumb Texte
    val subColor = Color(0xFF64748B)

    /**
     * Breadcrumb Navigation erstellen.
     *
     * Zeigt:
     * Hauptkategorie → Kategorie → Unterkategorie
     */
    val breadcrumb = listOfNotNull(
        wizardState.hauptKategorieLabel,
        wizardState.kategorieLabel,
        wizardState.unterkategorieLabel
    )
        // leere Werte entfernen
        .filter { it.isNotBlank() }

    /**
     * Hauptlayout des Screens
     */
    Box(
        modifier = Modifier
            .fillMaxSize()
            // Hintergrundfarbe setzen
            .background(pageBg)
            // Abstand zum Rand
            .padding(18.dp),
        // Inhalt zentrieren
        contentAlignment = Alignment.Center
    ) {

        /**
         * Hauptkarte (weißes Panel)
         */
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(18.dp),
            shadowElevation = 10.dp,
            color = Color.White
        ) {

            /**
             * Inhalt der Karte
             */
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                /**
                 * Titel des Screens
                 */
                Text(
                    text = "In welchem Zustand ist das Problem?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = titleColor
                )

                Spacer(Modifier.height(6.dp))

                /**
                 * Breadcrumb Anzeige
                 *
                 * Beispiel:
                 * Möbel → Tisch → Tischplatte
                 */
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    breadcrumb.forEachIndexed { index, item ->

                        /**
                         * Name des aktuellen Elements
                         */
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodySmall,
                            color = subColor
                        )

                        /**
                         * Pfeil zwischen Breadcrumb Elementen
                         */
                        if (index < breadcrumb.lastIndex) {

                            Spacer(modifier = Modifier.width(4.dp))

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = subColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))

                /**
                 * Auswahl der Zustände
                 *
                 * Zwei Karten nebeneinander
                 */
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    /**
                     * Zustand: Komplett defekt
                     */
                    ZustandCard(
                        modifier = Modifier.weight(1f),
                        bg = Color(0xFFEF4444),
                        icon = Icons.Default.Cancel,
                        title = "Komplett defekt",
                        onClick = {
                            onSelect(
                                "defekt",
                                "Komplett defekt"
                            )
                        }
                    )

                    /**
                     * Zustand: Teilweise defekt
                     */
                    ZustandCard(
                        modifier = Modifier.weight(1f),
                        bg = Color(0xFFF59E0B),
                        icon = Icons.Default.Error,
                        title = "Teilweise defekt",
                        onClick = {
                            onSelect(
                                "teilweise_defekt",
                                "Teilweise defekt"
                            )
                        }
                    )
                }

                Spacer(Modifier.height(18.dp))

                /**
                 * Zurück Button
                 */
                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE9EEF6),
                        contentColor = titleColor
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {

                    Text(
                        "Zurück",
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * ZustandCard
 *
 * Wiederverwendbare UI-Komponente
 * für eine Zustandskarte.
 *
 * Wird im ZustandScreen verwendet.
 */
@Composable
private fun ZustandCard(
    // Modifier für Layout (z.B. Gewicht im Row)
    modifier: Modifier = Modifier,
    // Hintergrundfarbe der Karte
    bg: Color,
    // Icon des Zustands
    icon: ImageVector,
    // Titel des Zustands
    title: String,
    // Klickaktion
    onClick: () -> Unit
) {

    /**
     * Karte für Zustandsauswahl
     */
    Surface(
        modifier = modifier
            .height(120.dp)
            // Klick auf Karte auswählen
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = bg,
        shadowElevation = 2.dp
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            /**
             * Icon des Zustands
             */
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )

            Spacer(Modifier.height(10.dp))

            /**
             * Titel des Zustands
             */
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}