package com.example.ui.screens.wifi

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.scan.RoomDetection
import com.example.scan.RoomMatch
import com.example.shre2fix.R
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

/**
 * Screen zur automatischen Raumerkennung.
 *
 * Dieser Screen versucht über WLAN-Fingerprints
 * den aktuellen Raum zu erkennen.
 *
 * Zusätzlich kann der Benutzer den Raum auch
 * manuell aus einer Liste auswählen.
 */
@Composable
fun RoomDetectionScreen(
    onRoomSelected: (String) -> Unit
) {

    // Zugriff auf Android Context
    val context = LocalContext.current

    // Coroutine Scope für asynchrone Aufgaben (z.B. WLAN Scan)
    val scope = rememberCoroutineScope()

    // Status: läuft gerade ein Scan
    var isScanning by remember { mutableStateOf(false) }

    // Ergebnisse der Raum-Erkennung
    var results by remember { mutableStateOf<List<RoomMatch>>(emptyList()) }

    // Fehlermeldung
    var error by remember { mutableStateOf<String?>(null) }

    // Prüfen ob ein Raum erkannt wurde
    val hasResult = results.isNotEmpty()

    // Buttontext abhängig vom Zustand
    val buttonLabel = if (hasResult) "Weiter" else "Jetzt\nScannen"

    // Farben für UI
    val pageBg = Color.White
    val cardBorder = Color(0xFFE3E8F0)
    val primaryGreen = Color(0xFF0F8B7A)
    val titleColor = Color(0xFF334155)
    val textDark = Color(0xFF0F172A)
    val outlineGray = Color(0xFF94A3B8)

    // =============================
    // Räume aus Firestore laden
    // =============================

    val db = remember { FirebaseFirestore.getInstance() }

    // Liste der Räume
    var rooms by remember { mutableStateOf<List<String>>(emptyList()) }

    // Ladezustand
    var roomsLoading by remember { mutableStateOf(true) }

    // Fehler beim Laden
    var roomsError by remember { mutableStateOf<String?>(null) }

    /**
     * Firestore Listener
     *
     * Lädt automatisch alle Räume aus der
     * Collection "rooms".
     */
    DisposableEffect(Unit) {

        roomsLoading = true
        roomsError = null

        val reg = db.collection("rooms")

            // Räume nach Reihenfolge sortieren
            .orderBy("order", Query.Direction.ASCENDING)

            .addSnapshotListener { snap, e ->

                // Fehler beim Laden
                if (e != null) {

                    roomsError = e.message ?: "Fehler beim Laden der Räume"
                    roomsLoading = false
                    rooms = emptyList()
                    return@addSnapshotListener
                }

                // Namen der Räume extrahieren
                rooms = snap?.documents
                    ?.mapNotNull { it.getString("name") }
                    ?: emptyList()

                roomsLoading = false
                roomsError = null
            }

        // Listener entfernen wenn Screen geschlossen wird
        onDispose { reg.remove() }
    }

    /**
     * Launcher zum Anfragen von Android Berechtigungen
     * (WLAN + Standort)
     */
    val requestPermsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->

        // Prüfen ob alle Berechtigungen erteilt wurden
        val allGranted = grants.values.all { it }

        if (allGranted) {

            // Scan starten
            scope.launch {
                performScan(context) { r, e, s ->
                    results = r
                    error = e
                    isScanning = s
                }
            }

        } else {

            error = "Berechtigungen fehlen (WLAN/Standort)."
        }
    }

    /**
     * Startet einen WLAN Scan
     */
    fun startScan() {

        error = null

        val needs = mutableListOf<String>()

        /**
         * Android 13+ benötigt NEARBY_WIFI_DEVICES
         */
        if (Build.VERSION.SDK_INT >= 33) {

            if (
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.NEARBY_WIFI_DEVICES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                needs += Manifest.permission.NEARBY_WIFI_DEVICES
            }
        }

        /**
         * Standortberechtigung prüfen
         */
        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            needs += Manifest.permission.ACCESS_FINE_LOCATION
        }

        // Wenn Berechtigungen fehlen → anfragen
        if (needs.isNotEmpty()) {

            requestPermsLauncher.launch(needs.toTypedArray())

        } else {

            // Scan direkt starten
            scope.launch {
                performScan(context) { r, e, s ->
                    results = r
                    error = e
                    isScanning = s
                }
            }
        }
    }

    /**
     * Hauptlayout des Screens
     */
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(pageBg),
        color = pageBg
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 18.dp),

            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(12.dp))

            /**
             * App Logo
             */
            Image(
                painter = painterResource(id = R.drawable.applogo),
                contentDescription = "ShareToFix Logo",
                modifier = Modifier.size(220.dp)
            )

            Spacer(Modifier.height(2.dp))

            /**
             * Titel
             */
            Text(
                text = "Aktueller Raum",
                style = MaterialTheme.typography.labelLarge,
                color = titleColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            /**
             * Text des erkannten Raums
             */
            val roomText =
                when {

                    isScanning -> "Wird erkannt…"

                    results.isNotEmpty() -> results.first().roomName

                    else -> "Kein Raum erkannt"
                }

            /**
             * Statuspunkt Farbe
             */
            val dotColor =
                if (results.isNotEmpty())
                    primaryGreen
                else
                    Color(0xFFEF4444)

            /**
             * Anzeige des aktuellen Raums
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

                    /**
                     * Statuspunkt
                     */
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(dotColor, RoundedCornerShape(2.dp))
                    )

                    Spacer(Modifier.width(10.dp))

                    /**
                     * Raumname
                     */
                    Text(
                        text = roomText,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                        color = textDark
                    )
                }
            }

            /**
             * Fehlermeldung anzeigen
             */
            if (error != null) {

                Spacer(Modifier.height(10.dp))

                Surface(
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Text(
                        text = "Fehler: $error",
                        color = Color(0xFFB71C1C),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            /**
             * Scan / Weiter Button
             */
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {

                OutlinedButton(

                    onClick = {

                        if (hasResult) {

                            // Wenn Raum erkannt → weiter
                            onRoomSelected(results.first().roomName)

                        } else {

                            // Sonst Scan starten
                            startScan()
                        }
                    },

                    enabled = !isScanning,

                    modifier = Modifier.size(140.dp),

                    shape = RoundedCornerShape(70.dp),

                    border = BorderStroke(2.dp, outlineGray),

                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White,
                        disabledContentColor = Color.White.copy(alpha = 0.6f)
                    )
                ) {

                    /**
                     * Ladeanimation während Scan
                     */
                    if (isScanning) {

                        CircularProgressIndicator(
                            strokeWidth = 3.dp,
                            color = primaryGreen,
                            modifier = Modifier.size(32.dp)
                        )

                    } else {

                        Text(
                            text = buttonLabel,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleMedium,
                            color = primaryGreen,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            /**
             * Hinweistext
             */
            Text(
                text = """
                    Hinweis:
                    Aktuell sind nur die unten aufgeführten Räume verfügbar.
                    Weitere Räume werden bald unterstützt.
                """.trimIndent(),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            /**
             * Titel für manuelle Auswahl
             */
            Text(
                text = "Wählen Sie, in welchem Raum sind Sie?",
                style = MaterialTheme.typography.bodyMedium,
                color = titleColor,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))

            /**
             * Anzeige der Raumliste
             */
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
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                else -> {

                    RoomButtonsGrid(
                        rooms = rooms,
                        primaryGreen = primaryGreen,
                        onRoomSelected = onRoomSelected
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

/**
 * Grid Layout für Raum Buttons
 */
@Composable
private fun RoomButtonsGrid(
    rooms: List<String>,
    primaryGreen: Color,
    onRoomSelected: (String) -> Unit
) {

    val rows = rooms.chunked(3)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

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
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryGreen)
                    ) {

                        Text(room, fontWeight = FontWeight.Medium)
                    }
                }

                repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * Führt den WLAN Scan durch und versucht
 * den Raum anhand von Fingerprints zu bestimmen.
 */
@RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
private suspend fun performScan(
    context: Context,
    onUpdate: (List<RoomMatch>, String?, Boolean) -> Unit
) {

    // Scan gestartet
    onUpdate(emptyList(), null, true)

    try {

        // Raumkandidaten berechnen
        val candidates = RoomDetection.detectMulti(context)

        onUpdate(candidates, null, false)

    } catch (t: Throwable) {

        onUpdate(emptyList(), t.message ?: "Scan fehlgeschlagen", false)
    }
}