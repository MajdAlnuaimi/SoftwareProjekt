package com.example.ui.screens.reports

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.outlined.Drafts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen: Meine Meldungen
 *
 * Dieser Screen zeigt alle Schadensmeldungen,
 * die der aktuell eingeloggte Benutzer erstellt hat.
 *
 * Die Daten werden live aus Firestore geladen.
 */
@Composable
fun WizardReportsListScreen(
    onCreateNewReport: () -> Unit
) {
    // E-Mail des aktuell angemeldeten Benutzers
    val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email

    // Android Context für Toast-Meldungen
    val context = LocalContext.current

    // Firestore Instanz
    val db = FirebaseFirestore.getInstance()

    // Liste aller Meldungen des Benutzers
    var reports by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    // aktuell ausgewählte Meldung für den Detaildialog
    var selectedReport by remember { mutableStateOf<Map<String, Any>?>(null) }

    // Ladezustand
    var isLoading by remember { mutableStateOf(true) }

    // Firestore Listener: Lädt Meldungen des eingeloggten Users live nach
    DisposableEffect(currentUserEmail) {
        // Wenn kein Benutzer eingeloggt ist, gibt es nichts zu laden
        if (currentUserEmail == null) {
            isLoading = false
            return@DisposableEffect onDispose { }
        }

        isLoading = true

        val reg = db.collection("wizard_reports")
            // Nur Reports dieses Benutzers
            .whereEqualTo("userEmail", currentUserEmail)
            // Neueste Meldungen zuerst
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                // Fehler beim Laden anzeigen
                if (e != null) {
                    Toast.makeText(context, "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
                    isLoading = false
                    return@addSnapshotListener
                }

                // Dokumente in Maps umwandeln und die Dokument-ID mitgeben
                if (snapshot != null) {
                    reports = snapshot.documents.map { doc ->
                        (doc.data ?: emptyMap()) + mapOf("id" to doc.id)
                    }
                }

                isLoading = false
            }

        // Listener entfernen, wenn der Screen verlassen wird
        onDispose { reg.remove() }
    }

    // Hauptlayout des Screens
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Kopfbereich
        Row(
            modifier = Modifier.padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Drafts, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Meine Meldungen",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Je nach Zustand: Loading, leerer Zustand oder Liste
        when {
            isLoading -> {
                // Ladeanzeige
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Lade Meldungen…",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            reports.isEmpty() -> {
                // Leerer Zustand wenn noch keine Meldungen vorhanden sind
                EmptyReportsState(onCreateNewReport = onCreateNewReport)
            }

            else -> {
                // Liste aller Meldungen
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(reports) { report ->
                        WizardReportCard(
                            report = report,
                            onClick = { selectedReport = report } // Öffnet Details-Dialog
                        )
                    }
                }
            }
        }
    }

    // Details-Dialog anzeigen, wenn ein Report ausgewählt wurde
    selectedReport?.let { report ->
        ReportDetailsDialog(
            report = report,
            onDismiss = { selectedReport = null }
        )
    }
}

/**
 * UI für den Fall, dass noch keine Meldungen vorhanden sind.
 */
@Composable
private fun EmptyReportsState(
    onCreateNewReport: () -> Unit
) {
    // Primärfarbe der App
    val primaryGreen = Color(0xFF0F8B7A)

    // Heller Hintergrundton
    val softGreen = Color(0xFFE7F6F3)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Dekorative Icon-Box
                Box(
                    modifier = Modifier
                        .size(74.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(softGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Drafts,
                        contentDescription = null,
                        tint = primaryGreen,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Titel des Empty State
                Text(
                    text = "Noch keine Meldungen",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Color(0xFF1A1C1E)
                )

                Spacer(Modifier.height(6.dp))

                // Erklärungstext
                Text(
                    text = "Sobald du eine Meldung sendest, erscheint sie hier.",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(Modifier.height(16.dp))

                // Button zum Erstellen einer neuen Meldung
                Button(
                    onClick = onCreateNewReport,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Schaden melden", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

/* ============================================================
   UI-Karte: Layout bleibt wie gewünscht
   ============================================================ */

/**
 * Einzelne Kartenansicht einer Meldung in der Liste.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WizardReportCard(report: Map<String, Any>, onClick: () -> Unit) {
    // Daten aus dem Dokument lesen (mit sicheren Defaults)
    val photoUrl = report["photoUrl"] as? String ?: ""
    val room = report["room"] as? String ?: "-"
    val kategorie = report["kategorieLabel"] as? String ?: "-"
    val unter = report["unterkategorieLabel"] as? String ?: "-"
    val haupt = report["hauptKategorieLabel"] as? String ?: "-"
    val zustand = (report["zustandLabel"] as? String)?.trim().orEmpty()
    val hasZustand = zustand.isNotEmpty()
    val geraeteNummer = (report["geraeteNummer"] as? String)?.trim().orEmpty()

    // Status der Meldung lesen, Standard = EINGEGANGEN
    val reportStatus = (report["reportStatus"] as? String ?: "EINGEGANGEN").uppercase()

    // Status in UI-Farben/Text umwandeln
    val status = statusUi(reportStatus)

    // Zeitstempel robust lesen (Long oder Firebase Timestamp)
    val timestampMillis = when (val ts = report["timestamp"]) {
        is Long -> ts
        is Timestamp -> ts.toDate().time
        else -> 0L
    }

    // Datum formatieren
    val dateLabel = if (timestampMillis > 0L)
        SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault()).format(Date(timestampMillis))
    else "-"

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Vorschau-Bild (hochkant)
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(130.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFDCEDC8)),
                    contentAlignment = Alignment.Center
                ) {
                    // Wenn ein Bild vorhanden ist, anzeigen
                    if (photoUrl.isNotBlank()) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Sonst Platzhalter-Icon anzeigen
                        Icon(
                            Icons.Default.Image,
                            null,
                            modifier = Modifier.size(30.dp),
                            tint = Color(0xFF689F38)
                        )
                    }
                }

                // Textbereich
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        // Titel: Kategorie + Unterkategorie
                        Text(
                            text = "$kategorie - $unter",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1A1C1E),
                                fontSize = 19.sp
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        // Datum der Meldung
                        Text(text = dateLabel, fontSize = 11.sp, color = Color.Gray)
                    }

                    // Raum links, Gerät-Nr rechts
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Raum-Anzeige
                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Raum: $room",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = Color(0xFF2E7D32),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Prüfen ob Gerätenummer vorhanden ist
                        val hasNummer = geraeteNummer.isNotBlank()

                        // Gerätenummer-Anzeige
                        Surface(
                            color = if (hasNummer) Color(0xFFEDF1F7) else Color.Transparent,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (hasNummer) "Gerät-Nr.: $geraeteNummer" else "",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = if (hasNummer) Color(0xFF37474F) else Color.Transparent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Vollständiger Kategoriepfad
                    Text(
                        text = "$haupt  ›  $kategorie  ›  $unter",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    // Zustand nur anzeigen, wenn vorhanden
                    if (hasZustand) {
                        Surface(
                            color = if (zustand == "Komplett defekt") Color(0xFFFFEBEE) else Color(0xFFFFECB3),
                            shape = RoundedCornerShape(50.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                // Je nach Zustand anderes Icon/Farbe
                                Icon(
                                    imageVector = if (zustand == "Komplett defekt") Icons.Default.Cancel else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (zustand == "Komplett defekt") Color(0xFFD32F2F) else Color(0xFFFB8C00),
                                    modifier = Modifier.size(14.dp)
                                )

                                Spacer(Modifier.width(6.dp))

                                Text(
                                    text = zustand,
                                    color = if (zustand == "Komplett defekt") Color(0xFFD32F2F) else Color(0xFFFB8C00),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Trennlinie zwischen Inhalt und Status
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.8.dp,
                color = Color(0xFFF0F0F0)
            )

            // Status-Button (nur Anzeige, keine Änderung hier)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
                    .height(42.dp)
                    .clip(RoundedCornerShape(21.dp))
                    .background(status.bg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = status.label,
                    color = status.fg,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

/**
 * Kleines UI-Modell für die Darstellung des Status.
 */
data class StatusUi(val label: String, val fg: Color, val bg: Color)

/**
 * Ordnet einem Rohstatus passende UI-Werte zu.
 */
fun statusUi(raw: String): StatusUi {
    return when (raw) {
        "ERLEDIGT" -> StatusUi("ERLEDIGT", Color(0xFF1B5E20), Color(0xFFE8F5E9))
        "EINGEGANGEN" -> StatusUi("EINGEGANGEN", Color(0xFF1565C0), Color(0xFFE3F2FD))
        "IN_BEARBEITUNG" -> StatusUi("IN BEARBEITUNG", Color(0xFFEF6C00), Color(0xFFFFE0B2))
        else -> StatusUi("EINGEGANGEN", Color(0xFF1565C0), Color(0xFFE3F2FD))
    }
}

/* =========================
   Details-Dialog
   ========================= */

/**
 * Detaildialog für eine ausgewählte Meldung.
 *
 * Zeigt:
 * - Bild
 * - Raum
 * - Status
 * - Kategorien
 * - Gerätenummer
 * - Beschreibung
 * - Datum
 */
@Composable
fun ReportDetailsDialog(
    report: Map<String, Any>,
    onDismiss: () -> Unit
) {
    // Bild URL
    val photoUrl = report["photoUrl"] as? String ?: ""

    // Raum
    val room = report["room"] as? String ?: "-"

    // Kategorien
    val haupt = report["hauptKategorieLabel"] as? String ?: "-"
    val kategorie = report["kategorieLabel"] as? String ?: "-"
    val unter = report["unterkategorieLabel"] as? String ?: "-"

    // Zustand
    val zustand = (report["zustandLabel"] as? String)?.trim().orEmpty()
    val hasZustand = zustand.isNotEmpty()

    // Status
    val reportStatus = (report["reportStatus"] as? String ?: "EINGEGANGEN")
        .uppercase(Locale.getDefault())
    val status = statusUi(reportStatus)

    // Gerätenummer
    val geraeteNummer = (report["geraeteNummer"] as? String)?.trim().orEmpty()
    val hasNummer = geraeteNummer.isNotBlank()

    // Beschreibung
    val beschreibung = report["beschreibung"] as? String ?: "-"

    // Timestamp lesen
    val timestampMillis = when (val ts = report["timestamp"]) {
        is Long -> ts
        is Timestamp -> ts.toDate().time
        else -> 0L
    }

    // Datum formatieren
    val dateTime = if (timestampMillis > 0L)
        SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault()).format(Date(timestampMillis))
    else "-"

    // Scrollzustand für den Dialoginhalt
    val scrollState = rememberScrollState()

    // Zustand für Vollbild-Bildansicht
    var showFullImage by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        confirmButton = {
            // Schließen-Button unten rechts
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 8.dp, bottom = 2.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Schließen")
                }
            }
        },
        title = {
            // Titel im Dialog
            Text(
                text = "$kategorie - $unter",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Bild anzeigen, wenn vorhanden
                if (photoUrl.isNotBlank()) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showFullImage = true },
                        contentScale = ContentScale.Crop
                    )
                }

                // Raum und Status in einer Zeile
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Raum: $room", fontWeight = FontWeight.SemiBold)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(status.bg)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = status.label,
                            color = status.fg,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }

                // Gerätenummer nur anzeigen wenn vorhanden
                if (hasNummer) {
                    Text(text = "Gerätenummer: $geraeteNummer")
                }

                // Datum
                Text(text = "Datum: $dateTime", fontSize = 12.sp)

                // Kategoriepfad
                Text(text = "Kategorie: $haupt › $kategorie › $unter")

                // Zustand nur anzeigen wenn vorhanden
                if (hasZustand) {
                    Text(text = "Zustand: $zustand")
                }

                HorizontalDivider()

                // Abschnitt Beschreibung
                Text("Beschreibung", fontWeight = FontWeight.Bold)

                val hasDescription = beschreibung.isNotBlank() && beschreibung != "-"

                if (hasDescription) {
                    Text(
                        text = beschreibung,
                        lineHeight = 20.sp
                    )
                } else {
                    Text(
                        text = "Keine Beschreibung vorhanden.",
                        color = Color.Gray,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
        }
    )

    // Vollbildansicht des Bildes
    if (showFullImage && photoUrl.isNotBlank()) {
        FullscreenZoomImageDialog(
            photoUrl = photoUrl,
            onDismiss = { showFullImage = false }
        )
    }
}

/* =========================
   Fullscreen Image Dialog (Zoom + Pan + DoubleTap)
   ========================= */

/**
 * Vollbild-Dialog für ein Bild mit:
 * - Zoom
 * - Pan
 * - Doppeltipp zum Zoomen/Zurücksetzen
 */
@Composable
fun FullscreenZoomImageDialog(
    photoUrl: String,
    onDismiss: () -> Unit
) {
    // Aktueller Zoom-Faktor
    var scale by remember { mutableFloatStateOf(1f) }

    // Aktuelle Verschiebung in X-Richtung
    var offsetX by remember { mutableFloatStateOf(0f) }

    // Aktuelle Verschiebung in Y-Richtung
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Minimaler Zoom
    val minScale = 1f

    // Maximaler Zoom
    val maxScale = 5f

    // Zustand für Zoom- und Pan-Gesten
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        // Neuen Zoom berechnen und begrenzen
        val newScale = (scale * zoomChange).coerceIn(minScale, maxScale)
        scale = newScale

        // Verschieben nur wenn hineingezoomt ist
        if (scale > 1f) {
            offsetX += panChange.x
            offsetY += panChange.y
        } else {
            // Wenn nicht gezoomt, Bild wieder zentrieren
            offsetX = 0f
            offsetY = 0f
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Vollbildbild
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .transformable(transformState)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offsetX
                        translationY = offsetY
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                // Doppeltipp: reinzoomen oder zurücksetzen
                                if (scale > 1f) {
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                } else {
                                    scale = 2.5f
                                }
                            }
                        )
                    }
            )

            // Schließen-Button oben rechts
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(20.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(50)
                    )
                    .size(42.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }
    }
}