package com.example.ui.screens.admin.responsibilities

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * UI-Komponente für eine Hauptkategorie-Kachel.
 *
 * Diese Kachel wird im Adminbereich verwendet, um
 * eine Hauptkategorie visuell darzustellen.
 *
 * Die Kachel zeigt:
 * - Icon der Kategorie
 * - Name der Kategorie
 * - Hintergrundfarbe
 *
 * Wenn die Kachel ausgewählt ist:
 * - wird sie leicht vergrößert
 * - bekommt einen grünen Rahmen
 * - erhält stärkeren Schatten
 */
@Composable
fun HauptTile(

    // Modifier wird von außen übergeben (z.B. Layout, Gewicht oder Padding)
    modifier: Modifier,

    // Name der Hauptkategorie
    label: String,

    // Icon der Hauptkategorie
    icon: ImageVector,

    // Hintergrundfarbe der Kachel
    tileColor: Color,

    // Gibt an, ob die Kachel aktuell ausgewählt ist
    selected: Boolean,

    // Funktion die ausgeführt wird, wenn die Kachel angeklickt wird
    onClick: () -> Unit
) {

    // Surface ist ein Material-Container (ähnlich wie eine Card)
    Surface(

        modifier = modifier

            // feste Höhe der Kachel
            .height(110.dp)

            // macht die Kachel klickbar
            .clickable { onClick() }

            // kleine Vergrößerung wenn ausgewählt
            .graphicsLayer {
                scaleX = if (selected) 1.05f else 1f
                scaleY = if (selected) 1.05f else 1f
            },

        // abgerundete Ecken der Kachel
        shape = RoundedCornerShape(14.dp),

        // Hintergrundfarbe
        color = tileColor,

        // Schattenhöhe (stärker wenn ausgewählt)
        shadowElevation = if (selected) 8.dp else 2.dp,

        // grüner Rahmen wenn ausgewählt
        border = if (selected)
            BorderStroke(3.dp, Color(0xFF16A34A))
        else
            null
    ) {

        // Inhalt der Kachel wird vertikal angeordnet
        Column(

            modifier = Modifier
                .fillMaxSize()      // komplette Fläche nutzen
                .padding(vertical = 14.dp),

            // horizontale Zentrierung
            horizontalAlignment = Alignment.CenterHorizontally,

            // vertikale Zentrierung
            verticalArrangement = Arrangement.Center
        ) {

            // Icon der Kategorie
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )

            // Abstand zwischen Icon und Text
            Spacer(Modifier.height(10.dp))

            // Kategoriename
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}