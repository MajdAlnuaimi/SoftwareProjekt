package com.example.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.sp

/**
 * Admin-Screen zum Verwalten der Hauptkategorien.
 *
 * In diesem Screen können Hauptkategorien:
 * - geladen
 * - erstellt
 * - bearbeitet
 * - gelöscht
 * - sortiert
 * werden.
 *
 * Zusätzlich kann durch Klick auf eine Hauptkategorie
 * in die nächste Ebene (Unterkategorien) navigiert werden.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCategoryScreen(
    /**
     * Callback zum Öffnen der Unterkategorien einer Hauptkategorie.
     *
     * Übergibt:
     * - parentId: Dokument-ID der Kategorie
     * - parentLabel: sichtbarer Name
     * - parentIconKey: Key des gewählten Icons
     */
    onOpenChildren: (parentId: String, parentLabel: String, parentIconKey: String) -> Unit
) {
    // Firestore-Instanz + CoroutineScope (für await()/Batch-Operationen)
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    // Snackbar-State: zeigt kurze Meldungen (Erfolg/Fehler)
    val snackbarHostState = remember { SnackbarHostState() }

    // Kategorien-Liste (nur Hauptkategorien)
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }

    // State für "Neue Kategorie"
    var newCategory by remember { mutableStateOf("") }
    var selectedIconKey by remember { mutableStateOf("more") }

    // Busy-Flag: verhindert Mehrfachklicks während DB-Operationen
    var isBusy by remember { mutableStateOf(false) }

    // Dialog-State: Löschen
    var showDeleteDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }

    // Dialog-State: Bearbeiten
    var showEditDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }
    var editLabel by remember { mutableStateOf("") }
    var editIconKey by remember { mutableStateOf("more") }

    // Verfügbare Icons (Key + Icon)
    // Diese Liste wird sowohl zum Erstellen als auch Bearbeiten genutzt.
    val iconOptions = listOf(
        "computer" to Icons.Default.Computer,
        "business" to Icons.Default.Business,
        "wifi" to Icons.Default.Wifi,
        "build" to Icons.Default.Build,
        "language" to Icons.Default.Language,
        "info" to Icons.Default.Info,
        "warning" to Icons.Default.Warning,
        "settings" to Icons.Default.Settings,
        "report" to Icons.Default.Report,
        "print" to Icons.Default.Print,
        "phone" to Icons.Default.Phone,
        "more" to Icons.Default.MoreHoriz
    )

    /**
     * Speichert die aktuelle Reihenfolge aller Hauptkategorien in Firestore.
     *
     * Jede Kategorie erhält einen "order"-Wert entsprechend ihrer Position
     * in der Liste.
     */
    suspend fun persistOrderToDb(list: List<Category>) {
        val batch = db.batch()
        list.forEachIndexed { index, item ->
            val ref = db.collection("categories").document(item.id)
            batch.update(ref, mapOf("order" to (index + 1)))
        }
        batch.commit().await()
    }

    /**
     * Hilfsfunktion:
     * Teilt eine Liste in Blöcke zu je 10 Elementen.
     *
     * Grund:
     * Firestore erlaubt bei whereIn maximal 10 Werte.
     */
    fun <T> List<T>.chunked10(): List<List<T>> = this.chunked(10)

    /**
     * Löscht eine Hauptkategorie inklusive 2 Unterebenen:
     * - Kategorie
     * - UnterKategorie
     *
     * Ablauf:
     * 1. direkte Kategorie laden
     * 2. UnterKategorie über whereIn laden
     * 3. UnterKategorie löschen
     * 4. Kategorie löschen
     * 5. Hauptkategorie löschen
     */
    suspend fun cascadeDelete3Levels(rootId: String) {
        val col = db.collection("categories")

        // 1) Kategorie laden
        val childrenSnap = col.whereEqualTo("parentId", rootId).get().await()
        val childIds = childrenSnap.documents.map { it.id }

        // 2) UnterKategorie laden (whereIn max 10, daher chunked)
        val grandChildIds = mutableListOf<String>()
        childIds.chunked10().forEach { chunk ->
            val snap = col.whereIn("parentId", chunk).get().await()
            grandChildIds += snap.documents.map { it.id }
        }

        // 3) Batch-Löschung in Teile (Firestore Batch ~500 Ops, wir nutzen 450 zur Sicherheit)
        suspend fun deleteIds(ids: List<String>) {
            ids.chunked(450).forEach { part ->
                val batch = db.batch()
                part.forEach { id -> batch.delete(col.document(id)) }
                batch.commit().await()
            }
        }

        deleteIds(grandChildIds)
        deleteIds(childIds)
        col.document(rootId).delete().await()
    }

    /**
     * Lädt beim ersten Anzeigen des Screens
     * nur die Hauptkategorien (parentId == null)
     * und sortiert sie nach "order".
     */
    LaunchedEffect(Unit) {
        val snapshot = db.collection("categories")
            .whereEqualTo("parentId", null)
            .orderBy("order")
            .get()
            .await()

        categories = snapshot.documents.mapNotNull { doc ->
            val label = doc.getString("label") ?: return@mapNotNull null
            val order = (doc.getLong("order") ?: 0L).toInt()
            val iconKey = doc.getString("icon") ?: "more"

            Category(
                id = doc.id,
                label = label,
                order = order,
                iconKey = iconKey
            )
        }
    }

    // =========================
    // Dialog: DELETE
    // =========================
    if (showDeleteDialog) {
        val c = categoryToDelete

        AlertDialog(
            // Dialog nur schließen, wenn keine Operation läuft
            onDismissRequest = {
                if (!isBusy) {
                    showDeleteDialog = false
                    categoryToDelete = null
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "Kategorie löschen",
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    if (c != null)
                        "Möchten Sie „${c.label}“ wirklich löschen? Alle Unterkategorien werden ebenfalls gelöscht."
                    else
                        "Möchten Sie diese Kategorie wirklich löschen?"
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isBusy && c != null,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    onClick = {
                        val target = c ?: return@TextButton

                        // Dialog sofort schließen (besseres UX)
                        showDeleteDialog = false
                        categoryToDelete = null

                        scope.launch {
                            try {
                                isBusy = true

                                // Löscht HauptKategorie inkl. Unterebenen (Firestore)
                                cascadeDelete3Levels(target.id)

                                // Aktualisiert Liste lokal + normalisiert Reihenfolge
                                val remaining = categories
                                    .filterNot { it.id == target.id }
                                    .sortedBy { it.order }
                                    .mapIndexed { i, item -> item.copy(order = i + 1) }

                                categories = remaining
                                persistOrderToDb(remaining)

                                // Snackbar: Erfolg (Löschen)
                                snackbarHostState.showSnackbar("Kategorie wurde gelöscht.")
                            } catch (e: Exception) {
                                // Snackbar: Fehler (Löschen)
                                snackbarHostState.showSnackbar("Löschen fehlgeschlagen: ${e.localizedMessage}")
                            } finally {
                                isBusy = false
                            }
                        }
                    }
                ) { Text("Löschen") }
            },
            dismissButton = {
                TextButton(
                    enabled = !isBusy,
                    onClick = {
                        showDeleteDialog = false
                        categoryToDelete = null
                    }
                ) { Text("Abbrechen") }
            }
        )
    }

    // =========================
    // Dialog: EDIT (Sie-Form)
    // =========================
    if (showEditDialog) {
        val c = categoryToEdit

        // Prüft, ob der neue Name bereits bei einer anderen Kategorie existiert (Case-Insensitive)
        val editTrimmed = editLabel.trim()
        val editDuplicateExists = c != null && categories.any {
            it.id != c.id && it.label.equals(editTrimmed, ignoreCase = true)
        }

        AlertDialog(
            // Dialog nur schließen, wenn keine Operation läuft
            onDismissRequest = {
                if (!isBusy) {
                    showEditDialog = false
                    categoryToEdit = null
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            icon = {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Kategorie bearbeiten") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Eingabe für neuen Kategorienamen
                    OutlinedTextField(
                        value = editLabel,
                        onValueChange = { editLabel = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Auswahl des Kategorie-Icons
                    Text("Icon auswählen", style = MaterialTheme.typography.titleMedium)

                    // Icons in Zeilen zu 4 anzeigen
                    val columns = 4
                    val rows = iconOptions.chunked(columns)

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        rows.forEach { rowIcons ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(
                                    12.dp,
                                    Alignment.CenterHorizontally
                                )
                            ) {
                                rowIcons.forEach { (key, icon) ->
                                    val selected = key == editIconKey

                                    Surface(
                                        modifier = Modifier.size(50.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (selected)
                                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                        else
                                            MaterialTheme.colorScheme.surface,
                                        tonalElevation = if (selected) 2.dp else 0.dp,
                                        onClick = { editIconKey = key }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = key,
                                                tint = if (selected)
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                else
                                                    MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    // Button wird deaktiviert, wenn Name leer ist oder Duplikat existiert
                    enabled = !isBusy && c != null && editTrimmed.isNotEmpty() && !editDuplicateExists,
                    onClick = {
                        val target = c ?: return@TextButton
                        val newLabelTrim = editLabel.trim()

                        // Sicherheitscheck: verhindert Duplikate auch beim Klick
                        val duplicateExists = categories.any {
                            it.id != target.id && it.label.equals(newLabelTrim, ignoreCase = true)
                        }
                        if (duplicateExists) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Name existiert bereits. Bitte wählen Sie einen anderen.")
                            }
                            return@TextButton
                        }

                        // Dialog sofort schließen (besseres UX)
                        showEditDialog = false
                        categoryToEdit = null

                        scope.launch {
                            try {
                                isBusy = true

                                // Aktualisiert Name + Icon in Firestore
                                db.collection("categories").document(target.id)
                                    .update(
                                        mapOf(
                                            "label" to newLabelTrim,
                                            "icon" to editIconKey
                                        )
                                    )
                                    .await()

                                // Aktualisiert den Eintrag lokal in der Liste
                                categories = categories.map {
                                    if (it.id == target.id) it.copy(label = newLabelTrim, iconKey = editIconKey)
                                    else it
                                }

                                // Snackbar: Erfolg (Bearbeiten)
                                snackbarHostState.showSnackbar("Kategorie wurde gespeichert.")
                            } catch (e: Exception) {
                                // Snackbar: Fehler (Bearbeiten)
                                snackbarHostState.showSnackbar("Speichern fehlgeschlagen: ${e.localizedMessage}")
                            } finally {
                                isBusy = false
                            }
                        }
                    }
                ) { Text("Speichern") }
            },
            dismissButton = {
                TextButton(
                    enabled = !isBusy,
                    onClick = {
                        showEditDialog = false
                        categoryToEdit = null
                    }
                ) { Text("Abbrechen") }
            }
        )
    }

    // =========================
    // UI
    // =========================
    // Scaffold wird nur für Snackbars genutzt (Design bleibt unverändert)
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

                // Header: Titelbereich der Seite
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Category, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Hauptkategorien verwalten",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                }

                // Card: Neue Kategorie erstellen (Name + Icon + Button)
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

                            // Überschrift des Formulars
                            Text("Neue Hauptkategorie", style = MaterialTheme.typography.titleMedium)

                            // Validierung für neue Kategorie
                            val trimmed = newCategory.trim()
                            val alreadyExists = categories.any { it.label.equals(trimmed, ignoreCase = true) }
                            val canAdd = trimmed.isNotEmpty() && !alreadyExists && !isBusy

                            // Eingabefeld für Kategoriename
                            OutlinedTextField(
                                value = newCategory,
                                onValueChange = { newCategory = it },
                                label = { Text("Name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            // Bereich für Icon-Auswahl
                            Text("Icon auswählen", style = MaterialTheme.typography.titleMedium)

                            // Icons in Zeilen zu 6 anzeigen
                            val columns = 6
                            val rows = iconOptions.chunked(columns)

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                rows.forEach { rowIcons ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        rowIcons.forEach { (key, icon) ->
                                            val selected = key == selectedIconKey

                                            Surface(
                                                modifier = Modifier.size(50.dp),
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (selected)
                                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                                else
                                                    MaterialTheme.colorScheme.surface,
                                                tonalElevation = if (selected) 2.dp else 0.dp,
                                                onClick = { selectedIconKey = key }
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = icon,
                                                        contentDescription = key,
                                                        tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                                        else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                        }

                                        // Fehlende Plätze der letzten Zeile mit leeren Boxen auffüllen
                                        repeat(columns - rowIcons.size) { Spacer(Modifier.size(46.dp)) }
                                    }
                                }
                            }

                            // Button zum Anlegen einer neuen Hauptkategorie
                            Button(
                                enabled = canAdd,
                                onClick = {
                                    val nextOrder = (categories.maxOfOrNull { it.order } ?: 0) + 1
                                    scope.launch {
                                        try {
                                            isBusy = true

                                            // Neues Dokument mit Auto-ID erstellen
                                            val docRef = db.collection("categories").document()
                                            docRef.set(
                                                mapOf(
                                                    "label" to trimmed,
                                                    "order" to nextOrder,
                                                    "icon" to selectedIconKey,
                                                    "parentId" to null
                                                )
                                            ).await()

                                            // Lokal hinzufügen
                                            categories = categories + Category(
                                                id = docRef.id,
                                                label = trimmed,
                                                order = nextOrder,
                                                iconKey = selectedIconKey
                                            )

                                            // Formular zurücksetzen
                                            newCategory = ""
                                            selectedIconKey = "more"

                                            // Snackbar: Erfolg (Hinzufügen)
                                            snackbarHostState.showSnackbar("Kategorie wurde hinzugefügt.")
                                        } catch (e: Exception) {
                                            // Snackbar: Fehler (Hinzufügen)
                                            snackbarHostState.showSnackbar("Hinzufügen fehlgeschlagen: ${e.localizedMessage}")
                                        } finally {
                                            isBusy = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (alreadyExists) {
                                    Text("Kategorie ist bereits vorhanden")
                                } else {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(Modifier.width(5.dp))
                                    Text("Hinzufügen")
                                }
                            }
                        }
                    }
                }

                // Hinweis: Erklärung zu Sortierung und Bearbeiten
                item {
                    Column(modifier = Modifier.padding(top = 4.dp)) {
                        Text(
                            text = "Aktuelle Hauptkategorien",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(6.dp))
                        val editIconId = "editIcon"

                        // Hinweistext mit eingebettetem Edit-Icon
                        val annotatedText = buildAnnotatedString {
                            append("Hinweis: Sie können die Reihenfolge mit den Pfeilen ändern. ")
                            append("Tippen Sie auf eine Kategorie, um die Inhalte zu verwalten. Über ")
                            appendInlineContent(editIconId, "[edit]")
                            append(" können Sie Name und Icon bearbeiten.")
                        }

                        val inlineContent = mapOf(
                            editIconId to InlineTextContent(
                                Placeholder(
                                    width = 16.sp,
                                    height = 16.sp,
                                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        )

                        Text(
                            text = annotatedText,
                            inlineContent = inlineContent,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // List Items: Kategorien sortiert anzeigen
                itemsIndexed(
                    items = categories.sortedBy { it.order },
                    key = { _, item -> item.id }
                ) { index, category ->

                    // Passendes Icon zur Kategorie suchen
                    val icon = iconOptions.firstOrNull { it.first == category.iconKey }?.second
                        ?: Icons.Default.MoreHoriz

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            // Öffnet Unterkategorien der gewählten Kategorie
                            onOpenChildren(category.id, category.label, category.iconKey)
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            // Kategorie-Icon
                            Icon(icon, contentDescription = null)
                            Spacer(Modifier.width(10.dp))

                            // Kategoriename
                            Text(
                                text = category.label,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge
                            )

                            //  Nach oben verschieben und Reihenfolge speichern
                            IconButton(
                                enabled = !isBusy && index > 0,
                                onClick = {
                                    val sorted = categories.sortedBy { it.order }.toMutableList()
                                    sorted.add(index - 1, sorted.removeAt(index))

                                    val newList = sorted.mapIndexed { i, c -> c.copy(order = i + 1) }
                                    categories = newList

                                    scope.launch {
                                        try {
                                            isBusy = true
                                            persistOrderToDb(newList)
                                        } finally {
                                            isBusy = false
                                        }
                                    }
                                }
                            ) { Icon(Icons.Default.KeyboardArrowUp, null) }

                            //  Nach unten verschieben und Reihenfolge speichern
                            IconButton(
                                enabled = !isBusy && index < categories.lastIndex,
                                onClick = {
                                    val sorted = categories.sortedBy { it.order }.toMutableList()
                                    sorted.add(index + 1, sorted.removeAt(index))

                                    val newList = sorted.mapIndexed { i, c -> c.copy(order = i + 1) }
                                    categories = newList

                                    scope.launch {
                                        try {
                                            isBusy = true
                                            persistOrderToDb(newList)
                                        } finally {
                                            isBusy = false
                                        }
                                    }
                                }
                            ) { Icon(Icons.Default.KeyboardArrowDown, null) }

                            // Edit: Dialog öffnen (Name + Icon ändern)
                            IconButton(
                                enabled = !isBusy,
                                onClick = {
                                    categoryToEdit = category
                                    editLabel = category.label
                                    editIconKey = category.iconKey
                                    showEditDialog = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Kategorie bearbeiten",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            //  Delete: Dialog öffnen
                            IconButton(
                                enabled = !isBusy,
                                onClick = {
                                    categoryToDelete = category
                                    showDeleteDialog = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Kategorie löschen",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Datenmodell für eine Hauptkategorie.
 *
 * Wird lokal im UI verwendet.
 */
data class Category(
    // Firestore Dokument-ID
    val id: String,

    // Sichtbarer Kategoriename
    val label: String,

    // Reihenfolge in der Liste
    val order: Int,

    // Key für das ausgewählte Icon
    val iconKey: String
)