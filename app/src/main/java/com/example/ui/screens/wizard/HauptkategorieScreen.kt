package com.example.ui.screens.wizard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.icons.IconRegistry
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Datenklasse für eine Hauptkategorie im UI.
 *
 * Diese Klasse enthält:
 * - id der Kategorie (Firestore Dokument ID)
 * - label (Anzeigetext)
 * - icon (Icon der Kategorie)
 * - tileColor (Farbe der Kategorie-Kachel)
 */
private data class HauptKategorieUi(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val tileColor: Color
)

/**
 * Screen zur Auswahl der Hauptkategorie eines Problems.
 *
 * Dieser Screen lädt alle Hauptkategorien aus Firestore
 * und zeigt sie als große farbige Tiles an.
 */
@Composable
fun HauptkategorieScreen(
    onBack: () -> Unit,                        // Callback für "Zurück"
    onNext: (id: String, label: String) -> Unit // Callback wenn Kategorie gewählt wird
) {

    // Hintergrundfarbe der Seite
    val pageBg = Color(0xFFF4F6FA)

    // Form der Karte
    val cardShape = RoundedCornerShape(18.dp)

    // Liste der Kategorien
    var categories by remember { mutableStateOf<List<HauptKategorieUi>>(emptyList()) }

    // Ladezustand
    var loading by remember { mutableStateOf(true) }

    /**
     * Farbpalette für Kategorie Tiles
     */
    val fullPalette = listOf(
        Color(0xFF3B82F6),
        Color(0xFFF59E0B),
        Color(0xFF8B5CF6),
        Color(0xFF10B981),
        Color(0xFF06B6D4),
        Color(0xFFEF4444),
        Color(0xFF6366F1),
        Color(0xFFEC4899),
        Color(0xFF84CC16)
    )

    // Farbe für Kategorie "Sonstiges"
    val sonstigesColor = Color(0xFF64748B)

    /**
     * Lädt Kategorien aus Firestore
     */
    LaunchedEffect(Unit) {

        loading = true

        val snap = FirebaseFirestore.getInstance()

            // Kategorien Collection
            .collection("categories")

            // nur Hauptkategorien (parentId = null)
            .whereEqualTo("parentId", null)

            .get()

            .await()

        /**
         * Firestore Dokumente in UI Objekte umwandeln
         */
        val raw = snap.documents.mapNotNull { doc ->

            val label = doc.getString("label") ?: return@mapNotNull null
            val iconKey = doc.getString("icon") ?: "more"
            val order = (doc.getLong("order") ?: 0L).toInt()

            Triple(order, doc.id, Pair(label, iconKey))
        }

            // nach order sortieren
            .sortedBy { it.first }

            .map { (_, id, pair) ->

                val (label, iconKey) = pair

                HauptKategorieUi(
                    id = id,
                    label = label,

                    // Icon aus IconRegistry laden
                    icon = IconRegistry.headerIconFor(iconKey),

                    tileColor = Color.Unspecified
                )
            }

        /**
         * Kategorie "Sonstiges" ans Ende sortieren
         */
        val sorted = raw.sortedWith(
            compareBy {
                it.label.trim().equals("Sonstiges", ignoreCase = true)
            }
        )

        /**
         * Farben auf Kategorien verteilen
         */
        var idx = 0

        categories = sorted.map { item ->

            val isSonst = item.label.trim().equals("Sonstiges", ignoreCase = true)

            val color =
                if (isSonst)
                    sonstigesColor
                else
                    fullPalette[idx++ % fullPalette.size]

            item.copy(tileColor = color)
        }

        loading = false
    }

    /**
     * Hauptlayout des Screens
     */
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg),
        contentAlignment = Alignment.Center
    ) {

        /**
         * Hauptkarte
         */
        Surface(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = cardShape,
            shadowElevation = 10.dp,
            color = Color.White
        ) {

            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                /**
                 * Titel
                 */
                Text(
                    text = "Worum geht es?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                Spacer(Modifier.height(6.dp))

                /**
                 * Untertitel
                 */
                Text(
                    text = "Wählen Sie die Kategorie des Problems",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B)
                )

                Spacer(Modifier.height(18.dp))

                /**
                 * Ladeanzeige
                 */
                if (loading) {

                    CircularProgressIndicator()

                } else {

                    /**
                     * Kategorien Grid
                     */
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

                        categories.chunked(2).forEach { row ->

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {

                                row.forEach { cat ->

                                    HauptTile(
                                        modifier = Modifier.weight(1f),
                                        label = cat.label,
                                        icon = cat.icon,
                                        tileColor = cat.tileColor,

                                        // Kategorie auswählen
                                        onClick = { onNext(cat.id, cat.label) }
                                    )
                                }

                                // wenn nur 1 Element in Reihe → Platzhalter
                                if (row.size == 1)
                                    Spacer(Modifier.weight(1f))
                            }
                        }
                    }
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
                        contentColor = Color(0xFF0F172A)
                    ),

                    elevation = ButtonDefaults.buttonElevation(0.dp)
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
 * Einzelne Kategorie-Kachel
 *
 * Wird im Grid angezeigt.
 */
@Composable
private fun HauptTile(
    modifier: Modifier,
    label: String,
    icon: ImageVector,
    tileColor: Color,
    onClick: () -> Unit
) {

    Surface(
        modifier = modifier
            .height(110.dp)
            .clickable { onClick() },

        shape = RoundedCornerShape(14.dp),

        color = tileColor,

        shadowElevation = 2.dp
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 14.dp),

            horizontalAlignment = Alignment.CenterHorizontally,

            verticalArrangement = Arrangement.Center
        ) {

            /**
             * Icon der Kategorie
             */
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )

            Spacer(Modifier.height(10.dp))

            /**
             * Kategorie Name
             */
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}