package com.example.ui.screens.wizard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
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
 * UnterkategorieScreen
 *
 * Dieser Screen zeigt alle Unterkategorien
 * der zuvor gewählten Kategorie.
 *
 * Ablauf im Wizard:
 *
 * Hauptkategorie → Kategorie → Unterkategorie → Zustand → Zusammenfassung
 *
 * Dieser Screen lädt die Unterkategorien aus Firestore
 * und zeigt sie als auswählbare Tiles an.
 */
@Composable
fun UnterkategorieScreen(

    // Name der gewählten Hauptkategorie (nur Anzeige im UI)
    hauptKategorieLabel: String,

    // ID der gewählten Kategorie (für Firestore Abfrage)
    kategorieId: String,

    // Name der Kategorie (Anzeige im UI)
    kategorieLabel: String,

    // Callback für Zurück Button
    onBack: () -> Unit,

    // Callback wenn eine Unterkategorie ausgewählt wird
    // id = Dokument ID
    // label = sichtbarer Name
    // hasZustand = ob ein Zustand-Screen danach angezeigt wird
    onNext: (id: String, label: String, hasZustand: Boolean) -> Unit
) {

    // Hintergrundfarbe des Screens
    val pageBg = Color(0xFFF4F6FA)

    // Form der Hauptkarte
    val cardShape = RoundedCornerShape(18.dp)

    // Farbe der Icons
    val iconTint = Color(0xFF14B8A6)

    /**
     * Liste der Unterkategorien
     */
    var items by remember { mutableStateOf<List<WizardLevelItemUi>>(emptyList()) }

    /**
     * Ladezustand
     */
    var loading by remember { mutableStateOf(true) }

    /**
     * Firestore Anfrage
     * Lädt Unterkategorien der aktuellen Kategorie
     */
    LaunchedEffect(kategorieId) {

        loading = true

        val snap = FirebaseFirestore.getInstance()

            // Kategorien Collection
            .collection("categories")

            // nur Unterkategorien dieser Kategorie
            .whereEqualTo("parentId", kategorieId)

            .get()

            .await()

        /**
         * Firestore Dokumente in UI Modelle umwandeln
         */
        val raw = snap.documents.mapNotNull { doc ->

            // Name der Unterkategorie
            val label = doc.getString("label") ?: return@mapNotNull null

            // Icon Schlüssel
            val iconKey = doc.getString("icon") ?: "sonst"

            // Sortierreihenfolge
            val order = (doc.getLong("order") ?: 0L).toInt()

            /**
             * hasZustand
             *
             * Bestimmt ob nach dieser Auswahl
             * noch ein Zustand ausgewählt werden muss
             *
             * Wenn Feld fehlt → default = true
             */
            val hasZustand = doc.getBoolean("hasZustand") ?: true

            WizardLevelItemUi(
                doc.id,
                label,
                iconKey,
                order,
                hasZustand
            )
        }

        /**
         * Sortierung der Unterkategorien
         *
         * 1. nach order
         * 2. "Sonstiges" immer ans Ende
         * 3. alphabetisch
         */
        items = raw.sortedWith(
            compareBy<WizardLevelItemUi> { it.order }
                .thenBy { it.label.trim().equals("Sonstiges", ignoreCase = true) }
                .thenBy { it.label.lowercase() }
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
                    text = "Welcher Teil ist betroffen?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                Spacer(Modifier.height(6.dp))

                /**
                 * Anzeige der aktuellen Navigation
                 *
                 * Hauptkategorie → Kategorie
                 */
                Row(verticalAlignment = Alignment.CenterVertically) {

                    Text(
                        text = hauptKategorieLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = kategorieLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF64748B)
                    )
                }

                Spacer(Modifier.height(18.dp))

                /**
                 * Ladeanzeige
                 */
                if (loading) {

                    CircularProgressIndicator()

                } else {

                    /**
                     * Grid mit Unterkategorien
                     */
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

                        /**
                         * Elemente werden in Reihen mit 3 Spalten angezeigt
                         */
                        items.chunked(3).forEach { row ->

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {

                                row.forEach { item ->

                                    val enabled = true

                                    /**
                                     * Tile einer Unterkategorie
                                     */
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(100.dp)

                                            // Auswahl der Unterkategorie
                                            .clickable {

                                                onNext(
                                                    item.id,
                                                    item.label,
                                                    item.hasZustand
                                                )
                                            },

                                        shape = RoundedCornerShape(14.dp),

                                        color = Color.White,

                                        shadowElevation = 2.dp,

                                        border = ButtonDefaults.outlinedButtonBorder(enabled = enabled)
                                    ) {

                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(12.dp),

                                            horizontalAlignment = Alignment.CenterHorizontally,

                                            verticalArrangement = Arrangement.Center
                                        ) {

                                            /**
                                             * Icon der Unterkategorie
                                             */
                                            Icon(
                                                imageVector = IconRegistry.iconFor(item.iconKey),
                                                contentDescription = item.label,
                                                tint = iconTint,
                                                modifier = Modifier.size(26.dp)
                                            )

                                            Spacer(Modifier.height(8.dp))

                                            /**
                                             * Name der Unterkategorie
                                             */
                                            Text(
                                                text = item.label,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                /**
                                 * Platzhalter wenn Reihe nicht voll ist
                                 */
                                repeat(3 - row.size) {

                                    Spacer(Modifier.weight(1f))
                                }
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