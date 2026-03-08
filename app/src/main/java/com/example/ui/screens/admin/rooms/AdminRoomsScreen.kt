package com.example.ui.screens.admin.rooms

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.fingerprinting.Mode
import com.example.fingerprinting.WifiFingerprint
import com.example.fingerprinting.WifiFingerprintRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Admin-Screen zum Verwalten von Räumen und deren Access Points.
 *
 * Aufgaben dieses Screens:
 * - vorhandene Räume anzeigen
 * - Reihenfolge der Räume ändern
 * - neue Räume anlegen
 * - vorhandene Räume bearbeiten
 * - Räume löschen
 * - WLAN-Fingerprints scannen
 * - die wichtigsten Access Points (Top 3) pro Raum speichern
 *
 * Firestore Collections:
 * - rooms
 * - access_points
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun AdminRoomsScreen() {

    // Firestore-Datenbankinstanz
    val db = FirebaseFirestore.getInstance()

    // Coroutine-Scope für asynchrone Operationen
    val scope = rememberCoroutineScope()

    // Android Context, wird für WLAN-Scan benötigt
    val ctx = LocalContext.current

    // Snackbar für Erfolg- und Fehlermeldungen
    val snackbarHostState = remember { SnackbarHostState() }

    // --------------------------------------------------
    // STATE: Räume
    // --------------------------------------------------

    // Liste aller Räume im UI
    var rooms by remember { mutableStateOf<List<RoomUi>>(emptyList()) }

    // --------------------------------------------------
    // STATE: Allgemeiner UI-Status
    // --------------------------------------------------

    // true wenn gerade Add/Edit/Delete läuft
    var isBusy by remember { mutableStateOf(false) }

    // true wenn gerade die Reihenfolge geändert wird
    var isReordering by remember { mutableStateOf(false) }

    // true wenn gerade gespeichert wird
    var isSaving by remember { mutableStateOf(false) }

    // --------------------------------------------------
    // STATE: Löschen
    // --------------------------------------------------

    // Sichtbarkeit des Löschen-Dialogs
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Der aktuell zu löschende Raum
    var roomToDelete by remember { mutableStateOf<RoomUi?>(null) }

    // --------------------------------------------------
    // STATE: Add/Edit Formular
    // --------------------------------------------------

    // Sichtbarkeit des Formulars
    var showForm by remember { mutableStateOf(false) }

    // Raum der gerade bearbeitet wird
    // null = neuer Raum
    // nicht null = bestehender Raum wird bearbeitet
    var editingRoom by remember { mutableStateOf<RoomUi?>(null) }

    // Eingabefeld für Raumname
    var roomNameInput by remember { mutableStateOf("") }

    // --------------------------------------------------
    // STATE: Scan-Einstellungen
    // --------------------------------------------------

    // Scan-Modus: SINGLE oder MULTI
    var mode by remember { mutableStateOf(Mode.SINGLE) }

    // Anzahl Samples als Texteingabe
    var samplesText by remember { mutableStateOf("5") }

    // Anzahl Samples als Integer
    var samples by remember { mutableIntStateOf(5) }

    // Delay zwischen mehreren Scans als Texteingabe
    var delayText by remember { mutableStateOf("1000") }

    // Delay zwischen mehreren Scans als Long
    var delayMs by remember { mutableLongStateOf(1000L) }

    // --------------------------------------------------
    // STATE: Scan-Ergebnis
    // --------------------------------------------------

    // true während ein WLAN-Scan läuft
    var isScanning by remember { mutableStateOf(false) }

    // Letzter erzeugter Fingerprint
    var lastFingerprint by remember { mutableStateOf<WifiFingerprint?>(null) }

    // Aus dem Scan ermittelte Top 3 BSSIDs
    var scannedTop3 by remember { mutableStateOf<List<String>>(emptyList()) }

    // --------------------------------------------------
    // STATE: Vorhandene Access Points beim Bearbeiten
    // --------------------------------------------------

    // Bereits gespeicherte Access Points des bearbeiteten Raums
    var existingAps by remember { mutableStateOf<List<ApUi>>(emptyList()) }

    // --------------------------------------------------
    // Firestore Listener: Räume
    // --------------------------------------------------

    /**
     * Beobachtet live alle Räume in Firestore.
     *
     * Immer wenn sich die Collection "rooms" ändert,
     * wird die Liste neu geladen.
     *
     * Während Reordering wird der Listener ignoriert,
     * damit lokale Sortier-Updates nicht sofort überschrieben werden.
     */
    DisposableEffect(Unit) {
        val reg: ListenerRegistration =
            db.collection("rooms")
                .addSnapshotListener { snap, _ ->
                    if (snap == null) return@addSnapshotListener
                    if (isReordering) return@addSnapshotListener

                    rooms = snap.documents.mapNotNull { doc ->
                        val name = doc.getString("name") ?: return@mapNotNull null
                        val order = (doc.getLong("order") ?: 0L).toInt()
                        RoomUi(id = doc.id, name = name, order = order)
                    }.sortedBy { it.order }
                }

        onDispose { reg.remove() }
    }

    // --------------------------------------------------
    // Firestore Listener: Access Points des bearbeiteten Raums
    // --------------------------------------------------

    /**
     * Wenn ein Raum bearbeitet wird, werden dessen Access Points live geladen.
     *
     * Dadurch können beim Bearbeiten auch ohne neuen Scan
     * die bereits gespeicherten Top 3 angezeigt werden.
     */
    DisposableEffect(editingRoom?.id) {
        val roomId = editingRoom?.id

        if (roomId == null) {
            existingAps = emptyList()
            return@DisposableEffect onDispose { }
        }

        val reg = db.collection("access_points")
            .whereEqualTo("roomId", roomId)
            .addSnapshotListener { snap, _ ->
                existingAps = snap?.documents?.map { d ->
                    ApUi(
                        id = d.id,
                        bssid = (d.getString("bssid") ?: "").lowercase()
                    )
                } ?: emptyList()
            }

        onDispose { reg.remove() }
    }

    /**
     * Speichert die aktuelle Reihenfolge der Räume in Firestore.
     *
     * Jeder Raum bekommt einen neuen "order"-Wert.
     */
    suspend fun persistOrderToDb(itemsInUiOrder: List<RoomUi>) {
        val batch = db.batch()

        itemsInUiOrder.forEachIndexed { index, item ->
            val orderValue = index + 1
            val ref = db.collection("rooms").document(item.id)

            batch.update(
                ref,
                mapOf(
                    "order" to orderValue,
                    "name" to item.name
                )
            )
        }

        batch.commit().await()
    }

    /**
     * Löscht einen Raum und alle zugehörigen Access Points.
     *
     * Verwendet Batch, damit alles in einem Schritt gelöscht wird.
     */
    suspend fun deleteRoomAndAps(roomId: String) {
        val apsSnap = db.collection("access_points")
            .whereEqualTo("roomId", roomId)
            .get()
            .await()

        val batch = db.batch()

        apsSnap.documents.forEach { batch.delete(it.reference) }
        batch.delete(db.collection("rooms").document(roomId))

        batch.commit().await()
    }

    /**
     * Setzt das Formular vollständig zurück.
     *
     * Wird nach Speichern oder beim Schließen des Formulars verwendet.
     */
    fun resetForm() {
        editingRoom = null
        roomNameInput = ""

        mode = Mode.SINGLE

        samplesText = "5"
        samples = 5

        delayText = "1000"
        delayMs = 1000L

        isScanning = false
        lastFingerprint = null
        scannedTop3 = emptyList()

        existingAps = emptyList()
    }

    /**
     * Prüft ob die App aktuell auf einem Emulator läuft.
     *
     * Falls ja, wird beim Scan ein Fake-Fingerprint erzeugt,
     * damit die Funktion auch im Emulator testbar bleibt.
     */
    fun isEmulatorNow(): Boolean {
        val fpPrint = android.os.Build.FINGERPRINT
        val model = android.os.Build.MODEL
        val brand = android.os.Build.BRAND
        val device = android.os.Build.DEVICE
        val product = android.os.Build.PRODUCT

        return fpPrint.contains("generic", true) ||
                fpPrint.contains("emulator", true) ||
                model.contains("Emulator", true) ||
                model.contains("Android SDK built for x86", true) ||
                brand.contains("generic", true) ||
                device.contains("generic", true) ||
                product.contains("sdk", true) ||
                product.contains("emulator", true) ||
                product.contains("simulator", true)
    }

    // --------------------------------------------------
    // Berechnete Zustände / Validierung
    // --------------------------------------------------

    // Normalisierter Raumname (z.B. Leerzeichen entfernen, Format vereinheitlichen)
    val normalizedName = normalizeRoomName(roomNameInput)

    /**
     * Prüft, ob der Raumname bereits existiert.
     *
     * Beim Bearbeiten ist derselbe Raumname erlaubt,
     * solange er zum aktuell bearbeiteten Raum gehört.
     */
    val isDuplicateName: Boolean = run {
        val current = editingRoom
        if (isSaving) return@run false

        rooms.any { r ->
            val sameRoom = current != null && r.id == current.id
            !sameRoom && r.name.equals(normalizedName, ignoreCase = true)
        }
    }

    /**
     * Die aktuell gespeicherten Top 3 Access Points des Raums.
     *
     * Wird beim Bearbeiten verwendet,
     * wenn noch kein neuer Scan durchgeführt wurde.
     */
    val existingTop3 = remember(existingAps) {
        existingAps.asSequence()
            .map { it.bssid }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
            .take(3)
            .toList()
    }

    /**
     * Die final zu speichernden Top 3 Access Points.
     *
     * Falls ein neuer Scan gemacht wurde → scannedTop3 verwenden.
     * Sonst → bestehende Access Points beibehalten.
     */
    val finalTop3ToSave = remember(scannedTop3, existingTop3) {
        scannedTop3.ifEmpty { existingTop3 }
    }

    /**
     * Scan ist erlaubt wenn:
     * - gerade nicht gescannt wird
     * - ein gültiger Raumname existiert
     * - kein Duplikat vorliegt
     * - nicht gespeichert wird
     */
    val canScan = !isScanning &&
            normalizedName.isNotBlank() &&
            !isDuplicateName &&
            !isSaving

    /**
     * Speichern eines neuen Raums ist erlaubt wenn:
     * - wir im Add-Modus sind
     * - Raumname gültig ist
     * - Name nicht doppelt ist
     * - exakt 3 gültige BSSIDs gescannt wurden
     * - nicht gerade gespeichert oder blockiert wird
     */
    val canSaveAdd = run {
        editingRoom == null &&
                normalizedName.isNotBlank() &&
                !isDuplicateName &&
                scannedTop3.size == 3 &&
                scannedTop3.all { BSSID_REGEX.matches(it) } &&
                !isSaving && !isBusy
    }

    /**
     * Speichern eines bestehenden Raums ist erlaubt wenn:
     * - wir im Edit-Modus sind
     * - Raumname gültig ist
     * - Name nicht doppelt ist
     * - finale Top 3 vorhanden und gültig sind
     * - nicht gespeichert oder blockiert wird
     */
    val canSaveEdit = run {
        editingRoom != null &&
                normalizedName.isNotBlank() &&
                !isDuplicateName &&
                finalTop3ToSave.isNotEmpty() &&
                finalTop3ToSave.all { BSSID_REGEX.matches(it) } &&
                !isSaving && !isBusy
    }

    // --------------------------------------------------
    // UI
    // --------------------------------------------------

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 4.dp,
                    bottom = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // --------------------------------------------------
                // Titel des Screens
                // --------------------------------------------------
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Router, null)
                        Spacer(Modifier.width(10.dp))
                        Text("Räume & Access Points", style = MaterialTheme.typography.titleLarge)
                    }
                }

                // --------------------------------------------------
                // Hauptkarte mit Raumliste und Formular
                // --------------------------------------------------
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {

                            Text("Aktuelle Räume", style = MaterialTheme.typography.titleMedium)

                            // --------------------------------------------------
                            // Liste der Räume
                            // --------------------------------------------------
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 420.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {

                                if (rooms.isEmpty()) {
                                    Text(
                                        "Noch keine Räume vorhanden.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    rooms.forEachIndexed { index, room ->
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Raumname anzeigen
                                                Text(
                                                    room.name,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    modifier = Modifier.weight(1f)
                                                )

                                                // ---------------- UP: Raum nach oben verschieben ----------------
                                                IconButton(
                                                    enabled = !isReordering && index > 0 && !isSaving,
                                                    onClick = {
                                                        val newList = rooms.toMutableList().apply {
                                                            add(index - 1, removeAt(index))
                                                        }.mapIndexed { i, it -> it.copy(order = i + 1) }

                                                        rooms = newList

                                                        scope.launch {
                                                            try {
                                                                isReordering = true
                                                                persistOrderToDb(newList)
                                                            } finally {
                                                                isReordering = false
                                                            }
                                                        }
                                                    }
                                                ) {
                                                    Icon(Icons.Default.KeyboardArrowUp, null)
                                                }

                                                // ---------------- DOWN: Raum nach unten verschieben ----------------
                                                IconButton(
                                                    enabled = !isReordering && index < rooms.lastIndex && !isSaving,
                                                    onClick = {
                                                        val newList = rooms.toMutableList().apply {
                                                            add(index + 1, removeAt(index))
                                                        }.mapIndexed { i, it -> it.copy(order = i + 1) }

                                                        rooms = newList

                                                        scope.launch {
                                                            try {
                                                                isReordering = true
                                                                persistOrderToDb(newList)
                                                            } finally {
                                                                isReordering = false
                                                            }
                                                        }
                                                    }
                                                ) {
                                                    Icon(Icons.Default.KeyboardArrowDown, null)
                                                }

                                                // ---------------- EDIT: Formular zum Bearbeiten öffnen ----------------
                                                IconButton(
                                                    enabled = !isBusy && !isReordering && !isSaving,
                                                    onClick = {
                                                        editingRoom = room
                                                        roomNameInput = room.name
                                                        lastFingerprint = null
                                                        scannedTop3 = emptyList()
                                                        showForm = true
                                                    }
                                                ) {
                                                    Icon(
                                                        Icons.Default.Edit,
                                                        null,
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }

                                                // ---------------- DELETE: Löschdialog öffnen ----------------
                                                IconButton(
                                                    enabled = !isBusy && !isReordering && !isSaving,
                                                    onClick = {
                                                        roomToDelete = room
                                                        showDeleteDialog = true
                                                    }
                                                ) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        null,
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // --------------------------------------------------
                                // Button zum Öffnen/Schließen des Formulars
                                // --------------------------------------------------
                                Surface(
                                    onClick = {
                                        showForm = !showForm
                                        if (!showForm) resetForm()
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.AddCircle,
                                            null,
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            text = when {
                                                !showForm -> "Raum hinzufügen"
                                                editingRoom == null -> "Hinzufügen schließen"
                                                else -> "Bearbeitung schließen"
                                            },
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            Icons.Default.ChevronRight,
                                            null,
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }

                            // --------------------------------------------------
                            // Formular für Add / Edit
                            // --------------------------------------------------
                            AnimatedVisibility(visible = showForm) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    HorizontalDivider()

                                    val isEdit = editingRoom != null

                                    Text(
                                        text = if (isEdit) "Raum bearbeiten" else "Neuer Raum",
                                        style = MaterialTheme.typography.titleSmall
                                    )

                                    // ---------------- Eingabe Raumname ----------------
                                    OutlinedTextField(
                                        value = roomNameInput,
                                        onValueChange = {
                                            roomNameInput = it

                                            // Bei Änderung des Namens bisherige Scan-Daten verwerfen
                                            lastFingerprint = null
                                            scannedTop3 = emptyList()
                                        },
                                        label = { Text("Raumname (z. B. C0_08)") },
                                        supportingText = {
                                            if (roomNameInput.isNotBlank()) {
                                                Text("Wird gespeichert als: $normalizedName")
                                            }
                                        },
                                        isError = roomNameInput.isNotBlank() &&
                                                (normalizedName.isBlank() || isDuplicateName),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    if (!isSaving && isDuplicateName && roomNameInput.isNotBlank()) {
                                        Text(
                                            "Raum existiert bereits",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }

                                    // --------------------------------------------------
                                    // WLAN Scan Bereich
                                    // --------------------------------------------------
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {

                                            // Titel des Scan-Bereichs
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.Wifi,
                                                    null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    "Access Points - Scan",
                                                    style = MaterialTheme.typography.titleSmall
                                                )
                                            }

                                            // ---------------- Wahl des Scanmodus ----------------
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                FilterChip(
                                                    selected = mode == Mode.SINGLE,
                                                    onClick = { mode = Mode.SINGLE },
                                                    label = { Text("Einmal") },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                                        selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                                                        containerColor = MaterialTheme.colorScheme.surface
                                                    )
                                                )

                                                Spacer(Modifier.width(8.dp))

                                                FilterChip(
                                                    selected = mode == Mode.MULTI,
                                                    onClick = { mode = Mode.MULTI },
                                                    label = { Text("Mehrfach") },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                                        selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                                                        containerColor = MaterialTheme.colorScheme.surface
                                                    )
                                                )
                                            }

                                            // ---------------- Zusätzliche Eingaben für MULTI ----------------
                                            if (mode == Mode.MULTI) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    OutlinedTextField(
                                                        value = samplesText,
                                                        onValueChange = {
                                                            samplesText = it
                                                            samples = it.toIntOrNull()?.coerceAtLeast(1) ?: samples
                                                        },
                                                        label = { Text("Samples") },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        modifier = Modifier.weight(1f)
                                                    )

                                                    OutlinedTextField(
                                                        value = delayText,
                                                        onValueChange = {
                                                            delayText = it
                                                            delayMs = it.toLongOrNull()?.coerceAtLeast(0L) ?: delayMs
                                                        },
                                                        label = { Text("Delay (ms)") },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }

                                            // ---------------- Scan starten ----------------
                                            OutlinedButton(
                                                enabled = canScan,
                                                onClick = {
                                                    if (normalizedName.isBlank()) return@OutlinedButton

                                                    isScanning = true
                                                    lastFingerprint = null
                                                    scannedTop3 = emptyList()

                                                    scope.launch {
                                                        try {
                                                            // Auf Emulator werden Demo-Daten verwendet
                                                            val fp = if (isEmulatorNow()) {
                                                                WifiFingerprint(
                                                                    roomName = normalizedName,
                                                                    bssids = mapOf(
                                                                        "aa:bb:cc:dd:ee:01" to -40,
                                                                        "aa:bb:cc:dd:ee:02" to -55,
                                                                        "aa:bb:cc:dd:ee:03" to -70
                                                                    )
                                                                )
                                                            } else {
                                                                when (mode) {
                                                                    Mode.SINGLE ->
                                                                        WifiFingerprintRepository.collectFingerprintSingle(
                                                                            ctx,
                                                                            normalizedName
                                                                        )

                                                                    Mode.MULTI ->
                                                                        WifiFingerprintRepository.collectFingerprintMulti(
                                                                            context = ctx,
                                                                            roomName = normalizedName,
                                                                            samples = samples,
                                                                            delayBetweenMs = delayMs
                                                                        )
                                                                }
                                                            }

                                                            lastFingerprint = fp
                                                            scannedTop3 = pickTopBssids(fp, 3)

                                                            snackbarHostState.showSnackbar(
                                                                "Scan abgeschlossen: ${fp.bssids.size} Access Points"
                                                            )
                                                        } catch (e: Exception) {
                                                            snackbarHostState.showSnackbar(
                                                                "Fehler beim Scannen: ${e.localizedMessage}"
                                                            )
                                                        } finally {
                                                            isScanning = false
                                                        }
                                                    }
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(46.dp),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                if (isScanning) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(18.dp),
                                                        strokeWidth = 2.dp
                                                    )
                                                    Spacer(Modifier.width(10.dp))
                                                    Text("Scanne…")
                                                } else {
                                                    Icon(Icons.Default.Search, null)
                                                    Spacer(Modifier.width(8.dp))
                                                    Text(
                                                        if (mode == Mode.SINGLE)
                                                            "Einmal scannen"
                                                        else
                                                            "Mehrfach scannen"
                                                    )
                                                }
                                            }

                                            // Hinweis falls Scan aktuell nicht erlaubt ist
                                            if (!canScan && !isScanning) {
                                                val hint = when {
                                                    normalizedName.isBlank() ->
                                                        "Bitte zuerst einen Raumnamen eingeben."

                                                    isDuplicateName ->
                                                        "Dieser Raum existiert bereits."

                                                    isSaving ->
                                                        "Speichern läuft…"

                                                    else -> ""
                                                }

                                                if (hint.isNotBlank()) {
                                                    Text(
                                                        hint,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            // ---------------- Anzeige der Top 3 APs ----------------
                                            val showList = finalTop3ToSave.isNotEmpty()

                                            if (showList) {
                                                Surface(
                                                    shape = RoundedCornerShape(14.dp),
                                                    color = MaterialTheme.colorScheme.surface
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(12.dp),
                                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            if (scannedTop3.isNotEmpty())
                                                                "Neue Access Points (Top 3)"
                                                            else
                                                                "Aktuelle Access Points",
                                                            style = MaterialTheme.typography.titleSmall
                                                        )

                                                        finalTop3ToSave.forEach { Text("• $it") }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // --------------------------------------------------
                                    // Speichern (Add oder Edit)
                                    // --------------------------------------------------
                                    val saveEnabled =
                                        if (editingRoom == null) canSaveAdd else canSaveEdit

                                    Button(
                                        enabled = saveEnabled,
                                        onClick = {
                                            if (normalizedName.isBlank()) return@Button

                                            scope.launch {
                                                try {
                                                    isBusy = true
                                                    isSaving = true

                                                    val isEdit = editingRoom != null

                                                    if (!isEdit) {
                                                        // ======================================
                                                        // ADD: Neuen Raum speichern
                                                        // ======================================

                                                        // Letzte Sortierposition holen
                                                        val lastSnap = db.collection("rooms")
                                                            .orderBy("order", Query.Direction.DESCENDING)
                                                            .limit(1)
                                                            .get()
                                                            .await()

                                                        val lastOrder = lastSnap.documents
                                                            .firstOrNull()
                                                            ?.getLong("order")
                                                            ?.toInt() ?: 0

                                                        val newOrder = lastOrder + 1

                                                        // Neuer Raum wird mit Raumname als Dokument-ID gespeichert
                                                        db.collection("rooms")
                                                            .document(normalizedName)
                                                            .set(
                                                                mapOf(
                                                                    "name" to normalizedName,
                                                                    "order" to newOrder
                                                                )
                                                            )
                                                            .await()

                                                        // Top 3 Access Points speichern
                                                        val batch = db.batch()
                                                        scannedTop3.forEach { bssid ->
                                                            val ref = db.collection("access_points").document()
                                                            batch.set(
                                                                ref,
                                                                mapOf(
                                                                    "roomId" to normalizedName,
                                                                    "bssid" to bssid
                                                                )
                                                            )
                                                        }
                                                        batch.commit().await()

                                                        showForm = false
                                                        resetForm()
                                                        snackbarHostState.showSnackbar("Room wurde gespeichert.")
                                                    } else {
                                                        // ======================================
                                                        // EDIT: Bestehenden Raum speichern
                                                        // ======================================

                                                        val old = editingRoom ?: return@launch
                                                        val oldId = old.id
                                                        val keepOrder = old.order

                                                        // Entweder neue Top 3 oder bestehende Top 3 speichern
                                                        val bssidsToSave = finalTop3ToSave

                                                        // Alte Access Points laden
                                                        val oldApsSnap = db.collection("access_points")
                                                            .whereEqualTo("roomId", oldId)
                                                            .get()
                                                            .await()

                                                        val batch = db.batch()

                                                        // Alte Access Points löschen
                                                        oldApsSnap.documents.forEach { batch.delete(it.reference) }

                                                        if (normalizedName == oldId) {
                                                            // Name unverändert → Dokument einfach updaten
                                                            batch.update(
                                                                db.collection("rooms").document(oldId),
                                                                mapOf(
                                                                    "name" to normalizedName,
                                                                    "order" to keepOrder
                                                                )
                                                            )
                                                        } else {
                                                            // Name geändert → neues Dokument anlegen, altes löschen
                                                            batch.set(
                                                                db.collection("rooms").document(normalizedName),
                                                                mapOf(
                                                                    "name" to normalizedName,
                                                                    "order" to keepOrder
                                                                )
                                                            )
                                                            batch.delete(db.collection("rooms").document(oldId))
                                                        }

                                                        // Neue Access Points hinzufügen
                                                        bssidsToSave.forEach { bssid ->
                                                            val ref = db.collection("access_points").document()
                                                            batch.set(
                                                                ref,
                                                                mapOf(
                                                                    "roomId" to normalizedName,
                                                                    "bssid" to bssid
                                                                )
                                                            )
                                                        }

                                                        batch.commit().await()

                                                        showForm = false
                                                        resetForm()
                                                        snackbarHostState.showSnackbar("Änderungen wurden gespeichert!")
                                                    }
                                                } catch (e: Exception) {
                                                    snackbarHostState.showSnackbar(
                                                        "Speichern fehlgeschlagen: ${e.localizedMessage}"
                                                    )
                                                } finally {
                                                    isSaving = false
                                                    isBusy = false
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(46.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        if (isSaving) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Text("Speichern…")
                                        } else {
                                            Icon(
                                                if (editingRoom == null)
                                                    Icons.Default.Add
                                                else
                                                    Icons.Default.Save,
                                                contentDescription = null
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                if (editingRoom == null)
                                                    "Hinzufügen"
                                                else
                                                    "Änderung speichern"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --------------------------------------------------
    // Löschen-Dialog
    // --------------------------------------------------
    if (showDeleteDialog) {
        val r = roomToDelete

        AlertDialog(
            onDismissRequest = {
                if (!isBusy && !isReordering && !isSaving) {
                    showDeleteDialog = false
                    roomToDelete = null
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Raum löschen") },
            text = {
                Text(
                    if (r != null)
                        "Raum „${r.name}“ und alle Access Points löschen?"
                    else
                        "Raum löschen?"
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isBusy && !isReordering && !isSaving && r != null,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    onClick = {
                        val target = r ?: return@TextButton

                        showDeleteDialog = false
                        roomToDelete = null

                        scope.launch {
                            try {
                                isBusy = true
                                deleteRoomAndAps(target.id)
                                snackbarHostState.showSnackbar("Raum wurde gelöscht.")
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar(
                                    "Löschen fehlgeschlagen: ${e.localizedMessage}"
                                )
                            } finally {
                                isBusy = false
                            }
                        }
                    }
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isBusy && !isReordering && !isSaving,
                    onClick = {
                        showDeleteDialog = false
                        roomToDelete = null
                    }
                ) {
                    Text("Abbrechen")
                }
            }
        )
    }
}