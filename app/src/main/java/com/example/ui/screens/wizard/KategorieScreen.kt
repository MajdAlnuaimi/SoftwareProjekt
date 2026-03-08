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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.icons.IconRegistry
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Screen zur Auswahl der Kategorie eines Problems.
 *
 * Dieser Screen wird nach der Auswahl der Hauptkategorie angezeigt
 * und lädt alle dazugehörigen Kategorien aus Firebase Firestore.
 */
@Composable
fun KategorieScreen(

    // ID der ausgewählten Hauptkategorie
    hauptKategorieId: String,

    // Name der Hauptkategorie (für Anzeige im UI)
    hauptKategorieLabel: String,

    // Callback für Zurück-Button
    onBack: () -> Unit,

    // Callback wenn eine Kategorie ausgewählt wird
    onNext: (id: String, label: String, hasGeraeteNummer: Boolean) -> Unit
) {

    // Hintergrundfarbe der Seite
    val pageBg = Color(0xFFF4F6FA)

    // Form der Hauptkarte
    val cardShape = RoundedCornerShape(18.dp)

    // Farbe der Kategorie-Tiles
    val tileColor = Color(0xFF14B8A6)

    // Liste der Kategorien
    var items by remember { mutableStateOf<List<WizardLevelItemUi>>(emptyList()) }

    // Ladezustand
    var loading by remember { mutableStateOf(true) }

    /**
     * Firestore Anfrage
     * lädt Kategorien die zu der Hauptkategorie gehören
     */
    LaunchedEffect(hauptKategorieId) {

        loading = true

        val snap = FirebaseFirestore.getInstance()

            // Kategorien Collection
            .collection("categories")

            // nur Kategorien dieser Hauptkategorie
            .whereEqualTo("parentId", hauptKategorieId)

            .get()

            .await()

        /**
         * Firestore Dokumente in UI Modelle umwandeln
         */
        val raw = snap.documents.mapNotNull { doc ->

            val label = doc.getString("label") ?: return@mapNotNull null
            val iconKey = doc.getString("icon") ?: "sonst"
            val order = (doc.getLong("order") ?: 0L).toInt()

            // bestimmt ob eine Gerätenummer abgefragt wird
            val hasGeraeteNummer = doc.getBoolean("hasGeraeteNummer") ?: false

            WizardLevelItemUi(
                id = doc.id,
                label = label,
                iconKey = iconKey,
                order = order,
                hasGeraeteNummer = hasGeraeteNummer
            )
        }

            // Kategorien nach Reihenfolge sortieren
            .sortedBy { it.order }

        /**
         * Kategorie "Sonstiges" ans Ende verschieben
         */
        items = raw.sortedWith(
            compareBy { it.label.trim().equals("Sonstiges", ignoreCase = true) }
        )

        loading = false
    }

    /**
     * Hauptlayout
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
                    text = "Was ist das Problem?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                Spacer(Modifier.height(6.dp))

                /**
                 * Anzeige der gewählten Hauptkategorie
                 */
                Text(
                    text = "Kategorie: $hauptKategorieLabel",
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
                     * Grid Layout der Kategorien
                     */
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {

                        // Kategorien werden in Reihen mit 2 Elementen angezeigt
                        items.chunked(2).forEach { row ->

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {

                                row.forEach { item ->

                                    /**
                                     * Kategorie Tile
                                     */
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(110.dp)

                                            // Kategorie auswählen
                                            .clickable {
                                                onNext(
                                                    item.id,
                                                    item.label,
                                                    item.hasGeraeteNummer
                                                )
                                            },

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
                                             * Kategorie Icon
                                             */
                                            Icon(
                                                imageVector = IconRegistry.iconFor(item.iconKey),
                                                contentDescription = item.label,
                                                tint = Color.White,
                                                modifier = Modifier.size(36.dp)
                                            )

                                            Spacer(Modifier.height(10.dp))

                                            /**
                                             * Kategorie Name
                                             */
                                            Text(
                                                text = item.label,
                                                color = Color.White,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                /**
                                 * Platzhalter wenn nur 1 Tile in der Reihe ist
                                 */
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