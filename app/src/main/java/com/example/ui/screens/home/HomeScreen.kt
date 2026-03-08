package com.example.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.shre2fix.R
import com.example.components.LogoHeader
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

/**
 * HomeScreen
 *
 * Dieser Screen ist die Hauptseite der App.
 * Hier sieht der Benutzer:
 * - den aktuell erkannten Raum
 * - mögliche Fehlermeldungen
 * - die Möglichkeit den Raum neu zu erkennen
 * - eine manuelle Raumauswahl
 * - den Button zum Melden eines Schadens
 */
@Composable
fun HomeScreen(
    selectedRoom: String?,           // aktuell erkannter Raum
    message: String?,                // Fehlermeldung oder Hinweis
    isDetecting: Boolean,            // zeigt ob Raum gerade erkannt wird
    onAutoScan: () -> Unit,          // startet automatische Raumerkennung
    skipAutoScan: Boolean,           // verhindert automatischen Scan (wenn man von RoomDetection kommt)
    onSkipAutoScanConsumed: () -> Unit,
    onRetryDetect: () -> Unit,       // erneuter Scan
    onChangeRoom: () -> Unit,        // Raum manuell ändern
    onSchadenMelden: () -> Unit,     // Schaden melden starten
    onRoomSelected: (String) -> Unit // Raum aus Liste auswählen
) {

    // UI Farben
    val pageBg = Color.White
    val cardBorder = Color(0xFFE3E8F0)
    val primaryGreen = Color(0xFF0F8B7A)

    // Statuspunkt (grün wenn Raum erkannt)
    val dotColor = if (selectedRoom != null) primaryGreen else Color(0xFFEF4444)

    val logoBlue = Color(0xFF1E3A8A)

    // zählt wie oft Raum-Erkennung neu versucht wurde
    var retryCount by rememberSaveable { mutableIntStateOf(0) }

    /**
     * Automatischer Scan beim Öffnen des Screens
     *
     * Wenn skipAutoScan = true → kein Scan
     * sonst wird automatisch onAutoScan() gestartet
     */
    LaunchedEffect(Unit) {
        if (skipAutoScan) {
            onSkipAutoScanConsumed()
        } else {
            onAutoScan()
        }
    }

    // Firestore Zugriff
    val db = remember { FirebaseFirestore.getInstance() }

    // Liste aller Räume aus Firestore
    var rooms by remember { mutableStateOf<List<String>>(emptyList()) }

    // Ladezustände
    var roomsLoading by remember { mutableStateOf(false) }
    var roomsError by remember { mutableStateOf<String?>(null) }

    /**
     * Reset retry counter wenn ein Raum erkannt wurde
     */
    LaunchedEffect(selectedRoom) {
        if (selectedRoom != null) retryCount = 0
    }

    /**
     * Räume nur laden wenn der Benutzer
     * nach 3 Fehlversuchen manuell wählen muss
     */
    DisposableEffect(retryCount) {

        if (retryCount < 3) {
            onDispose { }
            return@DisposableEffect onDispose { }
        }

        roomsLoading = true
        roomsError = null

        val reg = db.collection("rooms")
            .orderBy("order", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, e ->

                if (e != null) {
                    roomsError = e.message ?: "Fehler beim Laden der Räume"
                    roomsLoading = false
                    return@addSnapshotListener
                }

                rooms = snap?.documents
                    ?.mapNotNull { it.getString("name") }
                    ?: emptyList()

                roomsLoading = false
                roomsError = null
            }

        // Listener entfernen wenn Screen verlassen wird
        onDispose { reg.remove() }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg),
        color = pageBg
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 18.dp),

            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(8.dp))

            // App Header
            LogoHeader()

            Spacer(Modifier.height(8.dp))

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = Color(0xFFE2E8F0)
            )

            Spacer(Modifier.height(12.dp))

            // App Logo
            Image(
                painter = painterResource(id = R.drawable.applogo),
                contentDescription = "ShareToFix Logo",
                modifier = Modifier.size(220.dp)
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Aktueller Raum",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF334155),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            /**
             * Card zeigt aktuellen Raumstatus
             */
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .border(1.dp, cardBorder, RoundedCornerShape(10.dp)),
                shape = RoundedCornerShape(10.dp),
                color = Color.White
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // Statuspunkt
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(dotColor, RoundedCornerShape(2.dp))
                    )

                    Spacer(Modifier.width(10.dp))

                    Text(
                        text = selectedRoom
                            ?: if (isDetecting) "Wird erkannt…" else "Kein Raum erkannt",

                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),

                        color = Color(0xFF0F172A)
                    )
                }
            }

            /**
             * Fehlermeldung anzeigen
             */
            if (message != null) {

                Spacer(Modifier.height(10.dp))

                Surface(
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Text(
                        text = message,
                        color = Color(0xFFB71C1C),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            /**
             * Wenn kein Raum erkannt wurde
             * → erneuter Versuch oder manuelle Auswahl
             */
            if (selectedRoom == null) {

                Spacer(Modifier.height(12.dp))

                if (retryCount < 3) {

                    val isRoomSelectButton = !isDetecting && retryCount == 2
                    val buttonColor = if (isRoomSelectButton) logoBlue else primaryGreen

                    Button(
                        onClick = {

                            if (retryCount == 2) {
                                retryCount = 3
                            } else {
                                retryCount += 1
                                onRetryDetect()
                            }
                        },

                        enabled = !isDetecting,

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),

                        shape = RoundedCornerShape(10.dp),

                        colors = ButtonDefaults.buttonColors(
                            containerColor = buttonColor,
                            contentColor = Color.White,
                            disabledContainerColor = primaryGreen.copy(alpha = 0.35f),
                            disabledContentColor = Color.White.copy(alpha = 0.7f)
                        )
                    ) {

                        Text(
                            if (isDetecting) {
                                "Bitte warten…"
                            } else if (retryCount == 2) {
                                "Raum auswählen"
                            } else {
                                "Erneut versuchen"
                            }
                        )
                    }

                } else {

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = """
                            Hinweis:
                            Aktuell sind nur die unten aufgeführten Räume verfügbar.
                            Weitere Räume werden bald unterstützt.
                        """.trimIndent(),

                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    when {

                        roomsLoading -> {
                            CircularProgressIndicator()
                        }

                        roomsError != null -> {
                            Surface(
                                color = Color(0xFFFFEBEE),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = roomsError ?: "Fehler beim Laden der Räume",
                                    color = Color(0xFFB71C1C),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        rooms.isEmpty() -> {
                            Text(
                                text = "Keine Räume verfügbar.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF64748B),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        else -> {
                            RoomSelectionGrid(
                                title = "Wählen Sie, in welchem Raum sind Sie?",
                                rooms = rooms,
                                primaryGreen = primaryGreen,
                                onRoomSelected = { room -> onRoomSelected(room) }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(2.dp))

            /**
             * Raum manuell ändern
             */
            if (selectedRoom != null) {

                Text(
                    text = "Falscher Raum? Hier ändern",
                    color = primaryGreen,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChangeRoom() }
                        .padding(top = 2.dp, bottom = 10.dp)
                )
            }

            Spacer(Modifier.height(6.dp))

            /**
             * Button zum Starten der Schadensmeldung
             */
            Button(
                onClick = onSchadenMelden,
                enabled = selectedRoom != null && !isDetecting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryGreen,
                    contentColor = Color.White,
                    disabledContainerColor = primaryGreen.copy(alpha = 0.35f),
                    disabledContentColor = Color.White.copy(alpha = 0.7f)
                )
            ) {

                Text(
                    text = "Schaden Melden",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

/**
 * Grid für manuelle Raumauswahl
 */
@Composable
private fun RoomSelectionGrid(
    title: String,
    rooms: List<String>,
    primaryGreen: Color,
    onRoomSelected: (String) -> Unit
) {

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF0F172A),
            fontWeight = FontWeight.SemiBold
        )

        val rows = rooms.chunked(3)

        rows.forEach { row ->

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                row.forEach { room ->

                    OutlinedButton(
                        onClick = { onRoomSelected(room) },

                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),

                        shape = RoundedCornerShape(10.dp),

                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = primaryGreen
                        )
                    ) {

                        Text(room, fontWeight = FontWeight.Medium)
                    }
                }

                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}