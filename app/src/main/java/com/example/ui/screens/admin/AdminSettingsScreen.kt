package com.example.ui.screens.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * AdminScreen für WLAN-Konfiguration.
 *
 * In diesem Screen kann der Admin:
 * - erlaubte WLAN SSIDs anzeigen
 * - neue WLANs hinzufügen
 * - bestehende WLANs löschen
 *
 * Diese WLAN-Liste wird später verwendet,
 * um zu prüfen ob sich das Gerät im richtigen Netzwerk befindet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSettingsScreen() {

    // Zugriff auf Firestore Datenbank
    val db = FirebaseFirestore.getInstance()

    // CoroutineScope für Firestore Operationen
    val scope = rememberCoroutineScope()

    // Snackbar zum Anzeigen von Meldungen
    val snackbarHostState = remember { SnackbarHostState() }

    // ===== Data =====

    // Liste der erlaubten WLAN SSIDs
    var ssids by remember { mutableStateOf<List<String>>(emptyList()) }

    // ===== UI State =====

    // Ladezustand beim Start
    var isLoading by remember { mutableStateOf(true) }

    // Zustand während Speichern/Löschen
    var isSaving by remember { mutableStateOf(false) }

    // Steuert ob das Formular zum Hinzufügen sichtbar ist
    var showAddForm by remember { mutableStateOf(false) }

    // ===== Form =====

    // Eingabefeld für neue SSID
    var newSsid by remember { mutableStateOf("") }

    // ===== Delete dialog =====

    // Steuert ob der Löschdialog angezeigt wird
    var showDeleteDialog by remember { mutableStateOf(false) }

    // SSID die gelöscht werden soll
    var ssidToDelete by remember { mutableStateOf<String?>(null) }

    /**
     * Speichert die Liste der SSIDs in Firestore.
     *
     * Die Liste wird im Dokument:
     * config / wifi
     * im Feld allowedSsids gespeichert.
     */
    suspend fun persistSsids(list: List<String>) {
        db.collection("config")
            .document("wifi")
            .set(mapOf("allowedSsids" to list), SetOptions.merge())
            .await()
    }

    /**
     * Setzt das Formular zurück.
     */
    fun resetForm() {
        newSsid = ""
    }

    /**
     * Initialer Ladevorgang beim Öffnen des Screens.
     *
     * Lädt alle erlaubten WLAN SSIDs aus Firestore.
     */
    LaunchedEffect(Unit) {
        isLoading = true
        try {

            val doc = db.collection("config").document("wifi").get().await()

            ssids = (doc.get("allowedSsids") as? List<*>)?.filterIsInstance<String>() ?: emptyList()

        } catch (e: Exception) {

            ssids = emptyList()

            snackbarHostState.showSnackbar(e.message ?: "Fehler beim Laden der WLANs")

        } finally {

            isLoading = false
        }
    }

    /**
     * Validierung für neue SSID.
     *
     * Prüft:
     * - ob Eingabe leer ist
     * - ob SSID bereits existiert
     * - ob gerade geladen oder gespeichert wird
     */
    val trimmed = newSsid.trim()

    val isDuplicate = trimmed.isNotEmpty() && ssids.any { it == trimmed }

    val canAdd = trimmed.isNotEmpty() && !isDuplicate && !isLoading && !isSaving

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        // Hauptliste (scrollbar)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),

            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 4.dp,
                bottom = 16.dp
            ),

            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ===== Titel =====
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(Icons.Default.Router, null)

                    Spacer(Modifier.width(10.dp))

                    Text("WLAN Konfiguration", style = MaterialTheme.typography.titleLarge)
                }
            }

            // ===== Hauptkarte =====
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

                        Text("Aktuelle WLANs", style = MaterialTheme.typography.titleMedium)

                        // Liste der SSIDs
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {

                            // Ladeanzeige
                            if (isLoading) {

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),

                                    verticalAlignment = Alignment.CenterVertically,

                                    horizontalArrangement = Arrangement.Center
                                ) {

                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )

                                    Spacer(Modifier.width(10.dp))

                                    Text("Laden…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                            }

                            // Wenn keine WLANs existieren
                            else if (ssids.isEmpty()) {

                                Text(
                                    "Noch keine WLANs vorhanden.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                            }

                            // Liste der vorhandenen WLANs
                            else {

                                ssids.forEach { ssid ->

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

                                            Icon(Icons.Default.Wifi, null)

                                            Spacer(Modifier.width(10.dp))

                                            Text(
                                                text = ssid,
                                                style = MaterialTheme.typography.bodyLarge,
                                                modifier = Modifier.weight(1f)
                                            )

                                            // Button zum Löschen eines WLANs
                                            IconButton(
                                                enabled = !isSaving,
                                                onClick = {
                                                    ssidToDelete = ssid
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

                            /**
                             * Großer Button zum Öffnen/Schließen
                             * des Formulars zum Hinzufügen.
                             */
                            Surface(
                                onClick = {
                                    showAddForm = !showAddForm
                                    if (!showAddForm) resetForm()
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
                                        text = if (showAddForm) "Hinzufügen schließen" else "WLAN hinzufügen",
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

                        // ===== Formular zum Hinzufügen =====
                        AnimatedVisibility(visible = showAddForm) {

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {

                                HorizontalDivider()

                                Text("Neue WLAN SSID", style = MaterialTheme.typography.titleSmall)

                                // Eingabefeld
                                OutlinedTextField(
                                    value = newSsid,
                                    onValueChange = { newSsid = it },
                                    label = { Text("SSID (z. B. HSBO_Gast)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    isError = isDuplicate
                                )

                                // Fehlermeldung bei Duplikat
                                if (isDuplicate && trimmed.isNotEmpty()) {
                                    Text(
                                        "SSID existiert bereits",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                /**
                                 * Button zum Speichern der neuen SSID.
                                 */
                                Button(
                                    enabled = canAdd,
                                    onClick = {

                                        if (trimmed.isBlank()) return@Button

                                        if (ssids.any { it == trimmed }) return@Button

                                        // Formular sofort schließen
                                        showAddForm = false

                                        newSsid = ""

                                        scope.launch {

                                            isSaving = true

                                            val old = ssids

                                            val updated = old + trimmed

                                            ssids = updated

                                            try {

                                                persistSsids(updated)

                                                snackbarHostState.showSnackbar("WLAN wurde gespeichert.")

                                            } catch (e: Exception) {

                                                ssids = old

                                                snackbarHostState.showSnackbar(e.message ?: "Fehler beim Speichern")

                                            } finally {

                                                isSaving = false
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {

                                    Icon(Icons.Default.Add, null)

                                    Spacer(Modifier.width(8.dp))

                                    Text("Hinzufügen")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ===== Delete Dialog =====
    if (showDeleteDialog) {

        val target = ssidToDelete

        AlertDialog(
            onDismissRequest = {
                if (!isSaving) {
                    showDeleteDialog = false
                    ssidToDelete = null
                }
            },

            containerColor = MaterialTheme.colorScheme.surface,

            title = { Text("WLAN löschen") },

            text = {
                Text(
                    if (target != null)
                        "Möchten Sie „$target“ wirklich löschen?"
                    else
                        "Möchten Sie dieses WLAN wirklich löschen?"
                )
            },

            confirmButton = {
                TextButton(
                    enabled = !isSaving && target != null,

                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),

                    onClick = {

                        val t = target ?: return@TextButton

                        showDeleteDialog = false

                        ssidToDelete = null

                        scope.launch {

                            isSaving = true

                            val old = ssids

                            val updated = old.filterNot { it == t }

                            ssids = updated

                            try {

                                persistSsids(updated)

                                snackbarHostState.showSnackbar("WLAN wurde gelöscht.")

                            } catch (e: Exception) {

                                ssids = old

                                snackbarHostState.showSnackbar(e.message ?: "Fehler beim Löschen")

                            } finally {

                                isSaving = false
                            }
                        }
                    }
                ) { Text("Löschen") }
            },

            dismissButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = {
                        showDeleteDialog = false
                        ssidToDelete = null
                    }
                ) { Text("Abbrechen") }
            }
        )
    }
}