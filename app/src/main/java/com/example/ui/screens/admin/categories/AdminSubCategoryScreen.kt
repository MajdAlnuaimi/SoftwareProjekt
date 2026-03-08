package com.example.ui.screens.admin.categories

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.icons.IconCategory
import com.example.icons.IconRegistry
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Admin-Screen zum Verwalten von Kategorien und Unterkategorien.
 *
 * Diese Composable wird für zwei Ebenen verwendet:
 * - Ebene 2: Kategorien
 * - Ebene 3: Unterkategorien
 *
 * Funktionen dieses Screens:
 * - vorhandene Elemente laden und anzeigen
 * - neue Elemente hinzufügen
 * - bestehende Elemente bearbeiten
 * - Elemente löschen
 * - Reihenfolge ändern
 * - bei Ebene 2 in die nächste Hierarchie-Ebene wechseln
 *
 * @param parentId ID des übergeordneten Elements
 * @param parentLabel Anzeigename des übergeordneten Elements
 * @param parentIconKey Icon-Key des übergeordneten Elements
 * @param onOpenChildren Callback zum Öffnen der nächsten Ebene
 * @param isLastLevel true = letzte Ebene, false = noch eine weitere Ebene vorhanden
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSubCategoryScreen(
    parentId: String,
    parentLabel: String,
    parentIconKey: String,
    onOpenChildren: (childId: String, childLabel: String, childIconKey: String) -> Unit,
    isLastLevel: Boolean = false // gleiche Seite für Ebene 2 & 3
) {
    // Firestore-Datenbankinstanz
    val db = FirebaseFirestore.getInstance()

    // Coroutine-Scope für asynchrone Datenbankoperationen
    val scope = rememberCoroutineScope()

    // Snackbar zum Anzeigen von Erfolgs-/Fehlermeldungen
    val snackbarHostState = remember { SnackbarHostState() }

    // Aktuelle Liste der Unterelemente (Kategorien / Unterkategorien)
    var subItems by remember { mutableStateOf<List<SubUiItem>>(emptyList()) }

    // -----------------------------
    // State für "Neu hinzufügen"
    // -----------------------------

    // Steuert, ob das Hinzufügen-Formular sichtbar ist
    var showAddForm by remember { mutableStateOf(false) }

    // Neuer Name für die anzulegende Kategorie
    var newLabel by remember { mutableStateOf("") }

    // Ausgewähltes Icon für das neue Element
    var selectedIconKey by remember { mutableStateOf("pc") }

    // Nur für letzte Ebene:
    // Soll nach der Auswahl noch ein Zustands-Screen gezeigt werden?
    var newHasZustand by remember { mutableStateOf(true) }

    // Nur für Ebene 2:
    // Muss später eine Gerätenummer eingegeben werden?
    var newHasGeraeteNummer by remember { mutableStateOf(false) }

    // Aktuell gewählte Icon-Kategorie im Tab-Bereich
    var selectedIconCategory by remember { mutableStateOf(IconCategory.Hardware) }

    // -----------------------------
    // Allgemeiner UI-Status
    // -----------------------------

    // true während Add / Edit / Delete läuft
    var isBusy by remember { mutableStateOf(false) }

    // true während Reihenfolge geändert wird
    var isReordering by remember { mutableStateOf(false) }

    // -----------------------------
    // State für Bearbeiten-Dialog
    // -----------------------------

    // Sichtbarkeit des Bearbeiten-Dialogs
    var showEditDialog by remember { mutableStateOf(false) }

    // Das aktuell zu bearbeitende Element
    var itemToEdit by remember { mutableStateOf<SubUiItem?>(null) }

    // Bearbeiteter Name
    var editLabel by remember { mutableStateOf("") }

    // Bearbeiteter Icon-Key
    var editIconKey by remember { mutableStateOf("pc") }

    // Nur für letzte Ebene:
    // Bearbeitete Einstellung für Zustandsseite
    var editHasZustand by remember { mutableStateOf(true) }

    // Nur für Ebene 2:
    // Bearbeitete Einstellung für Gerätenummer
    var editHasGeraeteNummer by remember { mutableStateOf(false) }

    // Gewählte Icon-Kategorie im Bearbeiten-Dialog
    var editIconCategory by remember { mutableStateOf(IconCategory.Hardware) }

    // -----------------------------
    // State für Löschen-Dialog
    // -----------------------------

    // Sichtbarkeit des Löschen-Dialogs
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Das aktuell zu löschende Element
    var itemToDelete by remember { mutableStateOf<SubUiItem?>(null) }

    // Alle kategorisierten Icons aus der zentralen IconRegistry
    val categorizedIcons = remember { IconRegistry.categorizedIcons }

    /**
     * Liefert das passende normale Icon zu einem Icon-Key.
     */
    fun iconFor(key: String) = IconRegistry.iconFor(key)

    /**
     * Liefert das passende Header-Icon zu einem Icon-Key.
     */
    fun headerIconFor(key: String) = IconRegistry.headerIconFor(key)

    /**
     * Wandelt die Enum-Kategorie in einen sichtbaren Titel für Tabs um.
     */
    fun tabTitle(cat: IconCategory) = when (cat) {
        IconCategory.Hardware -> "Hardware"
        IconCategory.Gebaeude -> "Gebäude"
        IconCategory.Internet -> "Internet"
        IconCategory.Sonstiges -> "Sonstiges"
    }

    /**
     * Speichert die aktuelle Reihenfolge der Elemente in Firestore.
     *
     * Jedes Element erhält einen neuen "order"-Wert entsprechend
     * seiner Position in der Liste.
     */
    suspend fun persistOrderToDb(itemsInUiOrder: List<SubUiItem>) {
        val batch = db.batch()

        itemsInUiOrder.forEachIndexed { index, item ->
            val orderValue = index + 1
            val ref = db.collection("categories").document(item.id)
            batch.update(ref, mapOf("order" to orderValue))
        }

        batch.commit().await()
    }

    /**
     * Löscht mehrere Dokumente in Firestore über Batch-Operationen.
     *
     * In Blöcken von maximal 450, damit Firestore-Limits sicher eingehalten werden.
     */
    suspend fun deleteIds(ids: List<String>) {
        val col = db.collection("categories")

        ids.chunked(450).forEach { part ->
            val batch = db.batch()
            part.forEach { id -> batch.delete(col.document(id)) }
            batch.commit().await()
        }
    }

    /**
     * Löscht ein Element und – falls vorhanden – alle direkten Kindelemente.
     *
     * Verhalten:
     * - Kinder mit parentId = itemId laden
     * - Kinder löschen
     * - anschließend das eigentliche Element löschen
     */
    suspend fun cascadeDeleteOneLevel(itemId: String) {
        val col = db.collection("categories")
        val childrenSnap = col.whereEqualTo("parentId", itemId).get().await()
        val childIds = childrenSnap.documents.map { it.id }

        deleteIds(childIds)
        col.document(itemId).delete().await()
    }

    // -----------------------------
    // Live-Listener für Firestore
    // -----------------------------

    /**
     * Beobachtet live alle Kategorien mit dem aktuellen parentId.
     *
     * Immer wenn sich Firestore ändert, wird die Liste automatisch aktualisiert.
     */
    DisposableEffect(parentId) {
        val reg: ListenerRegistration =
            db.collection("categories")
                .whereEqualTo("parentId", parentId)
                .addSnapshotListener { snap, _ ->
                    if (snap == null) return@addSnapshotListener

                    // Während Reordering keine Live-Daten übernehmen,
                    // damit die lokale Sortierung nicht kurz überschrieben wird
                    if (isReordering) return@addSnapshotListener

                    subItems = snap.documents.map { doc ->
                        SubUiItem(
                            id = doc.id,
                            label = doc.getString("label") ?: "",
                            iconKey = doc.getString("icon") ?: "sonst",
                            order = (doc.getLong("order") ?: 0L).toInt(),
                            hasZustand = doc.getBoolean("hasZustand") ?: true,
                            hasGeraeteNummer = doc.getBoolean("hasGeraeteNummer") ?: false
                        )
                    }.sortedBy { it.order }
                }

        onDispose { reg.remove() }
    }

    // =========================
    // Dialog: LÖSCHEN
    // =========================
    if (showDeleteDialog) {
        val c = itemToDelete

        AlertDialog(
            // Dialog nur schließen, wenn gerade keine Operation läuft
            onDismissRequest = {
                if (!isBusy && !isReordering) {
                    showDeleteDialog = false
                    itemToDelete = null
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Kategorie löschen") },
            text = {
                Text(
                    if (c != null) {
                        if (isLastLevel) {
                            // Letzte Ebene: nur das Element selbst löschen
                            "Möchten Sie „${c.label}“ wirklich löschen?"
                        } else {
                            // Nicht letzte Ebene: Kind-Elemente werden ebenfalls gelöscht
                            "Möchten Sie „${c.label}“ wirklich löschen? Alle Unterkategorien werden ebenfalls gelöscht."
                        }
                    } else {
                        "Möchten Sie diese Kategorie wirklich löschen?"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isBusy && !isReordering && c != null,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    onClick = {
                        val target = c ?: return@TextButton

                        // Dialog sofort schließen
                        showDeleteDialog = false
                        itemToDelete = null

                        scope.launch {
                            try {
                                isBusy = true

                                // Löschen inkl. direkter Kinder
                                cascadeDeleteOneLevel(target.id)

                                snackbarHostState.showSnackbar("Kategorie wurde gelöscht.")
                            } catch (e: Exception) {
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
                    enabled = !isBusy && !isReordering,
                    onClick = {
                        showDeleteDialog = false
                        itemToDelete = null
                    }
                ) { Text("Abbrechen") }
            }
        )
    }

    // =========================
    // Dialog: BEARBEITEN
    // =========================
    if (showEditDialog) {
        val current = itemToEdit

        // Bearbeiteter Name ohne Leerzeichen außen
        val trimmedEdit = editLabel.trim()

        // Prüft, ob der bearbeitete Name bereits bei einem anderen Eintrag existiert
        val duplicateExists = current != null && subItems.any {
            it.id != current.id && it.label.equals(trimmedEdit, ignoreCase = true)
        }

        AlertDialog(
            onDismissRequest = {
                if (!isBusy && !isReordering) {
                    showEditDialog = false
                    itemToEdit = null
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
                    // Eingabe für den neuen Namen
                    OutlinedTextField(
                        value = editLabel,
                        onValueChange = { editLabel = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Icon auswählen", style = MaterialTheme.typography.titleSmall)

                    // Tabs für Icon-Kategorien
                    ScrollableTabRow(
                        selectedTabIndex = editIconCategory.ordinal,
                        edgePadding = 0.dp
                    ) {
                        IconCategory.entries.forEach { cat ->
                            Tab(
                                selected = editIconCategory == cat,
                                onClick = { editIconCategory = cat },
                                text = { Text(text = tabTitle(cat), maxLines = 1) }
                            )
                        }
                    }

                    // Icons der aktuell gewählten Kategorie
                    val currentIcons = categorizedIcons[editIconCategory] ?: emptyList()

                    // Grid mit auswählbaren Icons
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 56.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(currentIcons) { (key, icon) ->
                            val selected = key == editIconKey

                            Surface(
                                modifier = Modifier.size(46.dp),
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

                    // Nur für Ebene 2:
                    // Checkbox, ob Gerätenummer erforderlich ist
                    if (!isLastLevel) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = editHasGeraeteNummer,
                                onCheckedChange = { editHasGeraeteNummer = it }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Gerätenummer erforderlich")
                        }
                    }

                    // Nur für letzte Ebene:
                    // Checkbox, ob Zustandsseite hinzugefügt werden soll
                    if (isLastLevel) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = editHasZustand,
                                onCheckedChange = { editHasZustand = it }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Zustandsseite hinzufügen")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isBusy && !isReordering && current != null && trimmedEdit.isNotEmpty() && !duplicateExists,
                    onClick = {
                        val target = current ?: return@TextButton

                        // Sicherheitscheck gegen doppelte Namen
                        val dup = subItems.any {
                            it.id != target.id && it.label.equals(trimmedEdit, ignoreCase = true)
                        }

                        if (dup) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Name existiert bereits. Bitte wählen Sie einen anderen.")
                            }
                            return@TextButton
                        }

                        // Dialog schließen
                        showEditDialog = false
                        itemToEdit = null

                        scope.launch {
                            try {
                                isBusy = true

                                // Firestore-Dokument aktualisieren
                                db.collection("categories").document(target.id)
                                    .update(
                                        buildMap<String, Any> {
                                            put("label", trimmedEdit)
                                            put("icon", editIconKey)

                                            // Je nach Ebene unterschiedliche Zusatzfelder speichern
                                            if (isLastLevel) {
                                                put("hasZustand", editHasZustand)
                                            } else {
                                                put("hasGeraeteNummer", editHasGeraeteNummer)
                                            }
                                        }
                                    )
                                    .await()

                                snackbarHostState.showSnackbar("Kategorie wurde gespeichert.")
                            } catch (e: Exception) {
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
                    enabled = !isBusy && !isReordering,
                    onClick = {
                        showEditDialog = false
                        itemToEdit = null
                    }
                ) { Text("Abbrechen") }
            }
        )
    }

    /**
     * Wiederverwendbarer Listeninhalt für eine einzelne Zeile.
     *
     * Zeigt:
     * - das Icon
     * - den Namen
     * - Pfeile für Reihenfolge
     * - Bearbeiten
     * - Löschen
     */
    @Composable
    fun ItemRowContent(index: Int, item: SubUiItem) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Kategorie-Icon
            Icon(iconFor(item.iconKey), null)
            Spacer(Modifier.width(10.dp))

            // Kategoriename
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )

            // ▲ Nach oben verschieben
            IconButton(
                enabled = !isReordering && index > 0,
                onClick = {
                    // Lokale Liste neu anordnen
                    val newList = subItems.toMutableList().apply {
                        add(index - 1, removeAt(index))
                    }.mapIndexed { i, it -> it.copy(order = i + 1) }

                    subItems = newList

                    scope.launch {
                        try {
                            isReordering = true
                            persistOrderToDb(newList)
                        } finally {
                            isReordering = false
                        }
                    }
                }
            ) { Icon(Icons.Default.KeyboardArrowUp, null) }

            // ▼ Nach unten verschieben
            IconButton(
                enabled = !isReordering && index < subItems.lastIndex,
                onClick = {
                    // Lokale Liste neu anordnen
                    val newList = subItems.toMutableList().apply {
                        add(index + 1, removeAt(index))
                    }.mapIndexed { i, it -> it.copy(order = i + 1) }

                    subItems = newList

                    scope.launch {
                        try {
                            isReordering = true
                            persistOrderToDb(newList)
                        } finally {
                            isReordering = false
                        }
                    }
                }
            ) { Icon(Icons.Default.KeyboardArrowDown, null) }

            // ✎ Bearbeiten
            IconButton(
                enabled = !isBusy && !isReordering,
                onClick = {
                    // Daten des ausgewählten Elements in den Edit-State übernehmen
                    itemToEdit = item
                    editLabel = item.label
                    editIconKey = item.iconKey
                    editHasZustand = item.hasZustand
                    editHasGeraeteNummer = item.hasGeraeteNummer

                    // Passende Tab-Kategorie für das aktuelle Icon finden
                    editIconCategory = IconCategory.entries.firstOrNull { cat ->
                        (categorizedIcons[cat] ?: emptyList()).any { it.first == item.iconKey }
                    } ?: IconCategory.Hardware

                    showEditDialog = true
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // 🗑 Löschen
            IconButton(
                enabled = !isBusy && !isReordering,
                onClick = {
                    itemToDelete = item
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

    // =========================
    // Haupt-UI
    // =========================
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

                // ===== Titelbereich =====
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.List, null)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Unterkategorien verwalten",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }

                // ===== Hinweistext =====
                item {
                    Column {
                        val editIconId = "editIconHint"

                        // Text mit eingebettetem Edit-Icon
                        val annotatedText = buildAnnotatedString {
                            append("Hinweis: Sie können die Reihenfolge mit den Pfeilen ändern. ")
                            if (!isLastLevel) {
                                append("Tippen Sie auf eine Kategorie, um die nächste Ebene zu verwalten. Über ")
                            } else {
                                append("Dies ist die letzte Ebene. Über ")
                            }
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

                            // Kopf der Karte mit Parent-Icon + Parent-Label
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(headerIconFor(parentIconKey), null)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "Kategorien von $parentLabel",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // ===== Scrollbarer Listenbereich =====
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 200.dp, max = 375.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (subItems.isEmpty()) {
                                    Text(
                                        "Noch keine Kategorien vorhanden.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    subItems.forEachIndexed { index, item ->
                                        if (!isLastLevel) {
                                            // In Ebene 2 ist ein Klick auf ein Element möglich,
                                            // um die nächste Unterebene zu öffnen
                                            Surface(
                                                onClick = { onOpenChildren(item.id, item.label, item.iconKey) },
                                                shape = RoundedCornerShape(14.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant
                                            ) { ItemRowContent(index, item) }
                                        } else {
                                            // In letzter Ebene keine weitere Navigation
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant
                                            ) { ItemRowContent(index, item) }
                                        }
                                    }
                                }
                            }

                            // ===== Button zum Öffnen/Schließen des Add-Formulars =====
                            Surface(
                                onClick = { showAddForm = !showAddForm },
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
                                    Icon(Icons.Default.AddCircle, null, tint = MaterialTheme.colorScheme.onPrimary)
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = if (showAddForm) "Hinzufügen schließen" else "Kategorie hinzufügen",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onPrimary)
                                }
                            }

                            // ===== Formular zum Hinzufügen =====
                            AnimatedVisibility(visible = showAddForm) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {

                                    HorizontalDivider()

                                    Text("Neue Kategorie", style = MaterialTheme.typography.titleSmall)

                                    // Namenseingabe
                                    OutlinedTextField(
                                        value = newLabel,
                                        onValueChange = { newLabel = it },
                                        label = { Text("Name") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    Text("Icon auswählen", style = MaterialTheme.typography.titleSmall)

                                    // Tabs für Icon-Kategorien
                                    ScrollableTabRow(
                                        selectedTabIndex = selectedIconCategory.ordinal,
                                        edgePadding = 0.dp
                                    ) {
                                        IconCategory.entries.forEach { cat ->
                                            Tab(
                                                selected = selectedIconCategory == cat,
                                                onClick = { selectedIconCategory = cat },
                                                text = { Text(text = tabTitle(cat), maxLines = 1) }
                                            )
                                        }
                                    }

                                    val currentIcons = categorizedIcons[selectedIconCategory] ?: emptyList()
                                    val columns = 6
                                    val rows = currentIcons.chunked(columns)

                                    // Scrollbarer Bereich mit Icon-Auswahl
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 220.dp)
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        rows.forEach { rowIcons ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                rowIcons.forEach { (key, icon) ->
                                                    val selected = key == selectedIconKey

                                                    Surface(
                                                        modifier = Modifier.size(46.dp),
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
                                                    Spacer(Modifier.width(10.dp))
                                                }

                                                // Leere Platzhalter für unvollständige Zeilen
                                                repeat(columns - rowIcons.size) {
                                                    Spacer(Modifier.size(46.dp))
                                                    Spacer(Modifier.width(10.dp))
                                                }
                                            }
                                        }
                                    }

                                    // Nur letzte Ebene:
                                    // Checkbox für Zustandsseite
                                    if (isLastLevel) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = newHasZustand,
                                                onCheckedChange = { newHasZustand = it }
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text("Zustandsseite hinzufügen")
                                        }
                                    }

                                    // Nur Ebene 2:
                                    // Checkbox für Gerätenummer
                                    if (!isLastLevel) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = newHasGeraeteNummer,
                                                onCheckedChange = { newHasGeraeteNummer = it }
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text("Gerätenummer erforderlich")
                                        }
                                    }

                                    // Validierung für neuen Namen
                                    val trimmed = newLabel.trim()
                                    val alreadyExists = subItems.any { it.label.equals(trimmed, ignoreCase = true) }
                                    val canAdd = trimmed.isNotEmpty() && !alreadyExists && !isBusy && !isReordering

                                    // Button zum Hinzufügen
                                    Button(
                                        enabled = canAdd,
                                        onClick = {
                                            if (trimmed.isBlank()) return@Button

                                            scope.launch {
                                                try {
                                                    isBusy = true

                                                    // Nächste freie Reihenfolge bestimmen
                                                    val nextOrder = (subItems.maxOfOrNull { it.order } ?: 0) + 1

                                                    // Neues Dokument in Firestore anlegen
                                                    db.collection("categories")
                                                        .document()
                                                        .set(
                                                            buildMap<String, Any> {
                                                                put("label", trimmed)
                                                                put("icon", selectedIconKey)
                                                                put("parentId", parentId)
                                                                put("order", nextOrder)

                                                                // Je nach Ebene unterschiedliche Zusatzfelder speichern
                                                                if (isLastLevel) {
                                                                    put("hasZustand", newHasZustand)
                                                                } else {
                                                                    put("hasGeraeteNummer", newHasGeraeteNummer)
                                                                }
                                                            }
                                                        )
                                                        .await()

                                                    // Formular zurücksetzen
                                                    showAddForm = false
                                                    newLabel = ""
                                                    selectedIconKey = "pc"
                                                    selectedIconCategory = IconCategory.Hardware
                                                    newHasZustand = true
                                                    newHasGeraeteNummer = false

                                                    snackbarHostState.showSnackbar("Kategorie wurde hinzugefügt.")
                                                } catch (e: Exception) {
                                                    snackbarHostState.showSnackbar("Hinzufügen fehlgeschlagen: ${e.localizedMessage}")
                                                } finally {
                                                    isBusy = false
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (alreadyExists) {
                                            Text("Kategorie schon vorhanden")
                                        } else {
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
        }
    }
}