package com.example.ui.screens.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen für verantwortliche Personen.
 *
 * Dieser Screen zeigt alle Meldungen,
 * für die der aktuell eingeloggte Benutzer zuständig ist.
 *
 * Die Meldungen werden aus der Firestore Collection
 * "wizard_reports" geladen und nach targetEmail gefiltert.
 */
@Composable
fun ResponsibleReportsScreen() {

    // Email des aktuell eingeloggten Benutzers
    val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email

    // Context (wird hier aktuell nicht aktiv genutzt)
    LocalContext.current

    // Firestore Instanz
    val db = FirebaseFirestore.getInstance()

    // Liste aller Meldungen
    var reports by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    // aktuell ausgewählte Meldung (für Details Dialog)
    var selectedReport by remember { mutableStateOf<Map<String, Any>?>(null) }

    // Snackbar für Fehlermeldungen oder Statusmeldungen
    val snackbarHostState = remember { SnackbarHostState() }

    // CoroutineScope für UI Aktionen
    val scope = rememberCoroutineScope()

    // Ladezustand
    var isLoading by remember { mutableStateOf(true) }

    /**
     * Firestore Listener
     *
     * Lädt alle Reports für die aktuelle verantwortliche Person.
     */
    DisposableEffect(currentUserEmail) {

        if (currentUserEmail == null) {
            isLoading = false
            return@DisposableEffect onDispose { }
        }

        isLoading = true

        val reg = db.collection("wizard_reports")

            // nur Reports für diesen Verantwortlichen
            .whereEqualTo("targetEmail", currentUserEmail)

            // neueste Meldungen zuerst
            .orderBy("timestamp", Query.Direction.DESCENDING)

            .addSnapshotListener { snapshot, e ->

                if (e != null) {
                    isLoading = false
                    scope.launch {
                        snackbarHostState.showSnackbar("Fehler: ${e.message}")
                    }
                    return@addSnapshotListener
                }

                if (snapshot != null) {

                    reports = snapshot.documents.map { doc ->
                        (doc.data ?: emptyMap()) + mapOf("id" to doc.id)
                    }
                }

                isLoading = false
            }

        // Listener entfernen wenn Screen geschlossen wird
        onDispose { reg.remove() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {

            /**
             * Header des Screens
             */
            Row(
                modifier = Modifier.padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground
                )

                Spacer(Modifier.width(10.dp))

                Text(
                    text = "Zuständige Meldungen",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            /**
             * Anzeige je nach Zustand:
             * - Loading
             * - Keine Meldungen
             * - Liste der Meldungen
             */
            when {

                // Ladeanzeige
                isLoading -> {
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

                // Keine Meldungen vorhanden
                reports.isEmpty() -> {
                    ResponsibleEmptyState()
                }

                // Liste der Meldungen
                else -> {

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {

                        items(reports) { report ->

                            ResponsibleReportCard(
                                report = report,

                                // öffnet Details Dialog
                                onClick = { selectedReport = report },

                                snackbarHostState = snackbarHostState
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Dialog mit Details der Meldung
     */
    selectedReport?.let { report ->

        ReportDetailsDialog(
            report = report,
            onDismiss = { selectedReport = null }
        )
    }
}

/**
 * Anzeige wenn keine Meldungen vorhanden sind.
 */
@Composable
private fun ResponsibleEmptyState() {

    val primary = Color(0xFF0F8B7A)
    val soft = Color(0xFFE7F6F3)

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

                Box(
                    modifier = Modifier
                        .size(74.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(soft),

                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "Keine zuständigen Meldungen",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Color(0xFF1A1C1E)
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Sobald dir Meldungen zugewiesen werden, erscheinen sie hier.",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Karte für eine einzelne Meldung.
 *
 * Zeigt:
 * - Bild
 * - Kategorie
 * - Raum
 * - Datum
 * - Zustand
 * - Statusauswahl
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResponsibleReportCard(
    report: Map<String, Any>,
    onClick: () -> Unit,
    snackbarHostState: SnackbarHostState
) {

    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    // ID der Meldung
    val id = report["id"] as? String ?: return

    // Bild URL
    val photoUrl = report["photoUrl"] as? String ?: ""

    // Raum
    val room = report["room"] as? String ?: "-"

    // Kategorien
    val kategorie = report["kategorieLabel"] as? String ?: "-"
    val unter = report["unterkategorieLabel"] as? String ?: "-"
    val haupt = report["hauptKategorieLabel"] as? String ?: "-"

    // Zustand der Meldung
    val zustand = (report["zustandLabel"] as? String)?.trim().orEmpty()
    val hasZustand = zustand.isNotEmpty()

    // Geräte Nummer
    val geraeteNummer = (report["geraeteNummer"] as? String)?.trim().orEmpty()

    /**
     * Lokaler Status
     */
    var localStatus by remember {
        mutableStateOf((report["reportStatus"] as? String ?: "EINGEGANGEN").uppercase())
    }

    var saving by remember { mutableStateOf(false) }

    /**
     * Zeitstempel
     */
    val timestampMillis = when (val ts = report["timestamp"]) {
        is Long -> ts
        is Timestamp -> ts.toDate().time
        else -> 0L
    }

    val dateLabel =
        if (timestampMillis > 0L)
            SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault()).format(Date(timestampMillis))
        else "-"

    /**
     * Status ändern
     */
    fun setStatus(newStatus: String) {

        if (saving || newStatus == localStatus) return

        saving = true

        db.collection("wizard_reports")
            .document(id)
            .update(
                mapOf(
                    "reportStatus" to newStatus,
                    "statusUpdatedAt" to System.currentTimeMillis()
                )
            )
            .addOnSuccessListener {

                localStatus = newStatus
                saving = false

                scope.launch {
                    snackbarHostState.showSnackbar("Status wurde geändert.")
                }
            }
            .addOnFailureListener { e ->

                saving = false

                scope.launch {
                    snackbarHostState.showSnackbar("Fehler: ${e.message}")
                }
            }
    }

    /**
     * Karte für eine einzelne Meldung
     */
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

            /**
             * Hauptinhalt der Karte
             */
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp),

                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                /**
                 * Bildvorschau
                 */
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(130.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFDCEDC8)),

                    contentAlignment = Alignment.Center
                ) {

                    if (photoUrl.isNotBlank()) {

                        AsyncImage(
                            model = photoUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                    } else {

                        Icon(
                            Icons.Default.Image,
                            null,
                            modifier = Modifier.size(30.dp),
                            tint = Color(0xFF689F38)
                        )
                    }
                }

                /**
                 * Textbereich
                 */
                Column(modifier = Modifier.weight(1f)) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {

                        Text(
                            text = "$kategorie - $unter",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1A1C1E),
                                fontSize = 19.sp
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = dateLabel,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    /**
                     * Raum + Gerätenummer
                     */
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),

                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

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

                        val hasNummer = geraeteNummer.isNotBlank()

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

                    /**
                     * Kategorie Pfad
                     */
                    Text(
                        text = "$haupt  ›  $kategorie  ›  $unter",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    /**
                     * Zustand der Meldung
                     */
                    if (hasZustand) {

                        Surface(
                            color = if (zustand == "Komplett defekt")
                                Color(0xFFFFEBEE)
                            else
                                Color(0xFFFFECB3),

                            shape = RoundedCornerShape(50.dp)
                        ) {

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {

                                Icon(
                                    imageVector =
                                        if (zustand == "Komplett defekt")
                                            Icons.Default.Cancel
                                        else
                                            Icons.Default.NotificationsActive,

                                    contentDescription = null,

                                    tint =
                                        if (zustand == "Komplett defekt")
                                            Color(0xFFD32F2F)
                                        else
                                            Color(0xFFFB8C00),

                                    modifier = Modifier.size(14.dp)
                                )

                                Spacer(Modifier.width(6.dp))

                                Text(
                                    text = zustand,

                                    color =
                                        if (zustand == "Komplett defekt")
                                            Color(0xFFD32F2F)
                                        else
                                            Color(0xFFFB8C00),

                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.8.dp,
                color = Color(0xFFF0F0F0)
            )

            /**
             * Status Auswahl (Neu / In Bearbeitung / Erledigt)
             */
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 12.dp)
            ) {

                val containerColor = when (localStatus) {

                    "EINGEGANGEN" -> Color(0xFFE3F2FD)

                    "IN_BEARBEITUNG" -> Color(0xFFFFE0B2)

                    "ERLEDIGT" -> Color(0xFFE8F5E9)

                    else -> Color(0xFFF0F2F5)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(containerColor),

                    verticalAlignment = Alignment.CenterVertically
                ) {

                    StatusActionChip(
                        rawStatus = "EINGEGANGEN",
                        selected = localStatus == "EINGEGANGEN",
                        modifier = Modifier.weight(1f),
                        onClick = { setStatus("EINGEGANGEN") }
                    )

                    StatusActionChip(
                        rawStatus = "IN_BEARBEITUNG",
                        selected = localStatus == "IN_BEARBEITUNG",
                        modifier = Modifier.weight(1f),
                        onClick = { setStatus("IN_BEARBEITUNG") }
                    )

                    StatusActionChip(
                        rawStatus = "ERLEDIGT",
                        selected = localStatus == "ERLEDIGT",
                        modifier = Modifier.weight(1f),
                        onClick = { setStatus("ERLEDIGT") }
                    )
                }

                /**
                 * Ladebalken während Statusupdate
                 */
                if (saving) {

                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 20.dp),

                        trackColor = Color.Transparent
                    )
                }
            }
        }
    }
}

/**
 * Chip für Statusauswahl
 */
@Composable
private fun StatusActionChip(
    rawStatus: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {

    val ui = statusUi(rawStatus)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) ui.fg.copy(alpha = 0.15f)
                else Color.Transparent
            )
            .clickable { onClick() },

        contentAlignment = Alignment.Center
    ) {

        Text(
            text = ui.label,

            color =
                if (selected) ui.fg
                else Color.Black.copy(alpha = 0.50f),

            fontWeight =
                if (selected) FontWeight.Bold
                else FontWeight.Medium,

            fontSize = 12.sp,

            maxLines = 1,

            overflow = TextOverflow.Ellipsis
        )
    }
}