package com.example.ui.screens.admin.responsibilities

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.data.ResponsibilityRepository
import com.example.icons.IconRegistry
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Admin-Screen zum Verwalten von Zuständigkeiten.
 *
 * Hier kann festgelegt werden, welche E-Mail-Adresse
 * für einen bestimmten Raum und eine bestimmte Kategorie-Ebene zuständig ist.
 *
 * Die Zuständigkeit kann auf drei Ebenen gespeichert werden:
 *
 * 1. Hauptkategorie
 * 2. Kategorie
 * 3. Unterkategorie
 *
 * Je höher die Ebene ist, desto allgemeiner gilt die Zuständigkeit.
 *
 * Beispiel:
 * - Zuständigkeit auf Hauptkategorie-Ebene → gilt für alle darunterliegenden Kategorien
 * - Zuständigkeit auf Kategorie-Ebene → gilt für alle Unterkategorien dieser Kategorie
 * - Zuständigkeit auf Unterkategorie-Ebene → gilt nur für diese konkrete Unterkategorie
 */
@Composable
fun AdminResponsibilitiesScreen(
    /**
     * Callback zur Navigation in die Kategorienverwaltung.
     *
     * Wird verwendet, wenn noch keine Kategorien vorhanden sind
     * und der Admin zuerst Kategorien anlegen muss.
     */
    onOpenAdminCategories: () -> Unit
) {

    // Firestore-Datenbankinstanz
    val db = FirebaseFirestore.getInstance()

    // Coroutine-Scope für asynchrone Firestore-Aufrufe
    val scope = rememberCoroutineScope()

    // Snackbar-State für Erfolg-/Fehlermeldungen
    val snackbarHostState = remember { SnackbarHostState() }

    // Liste aller Räume: Pair<roomId, roomName>
    var rooms by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    // Aktuell ausgewählter Raum
    var selectedRoom by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Liste aller Hauptkategorien
    var hauptCategories by remember { mutableStateOf<List<HauptKategorieUi>>(emptyList()) }

    // Aktuell ausgewählte Hauptkategorie
    var selectedHaupt by remember { mutableStateOf<HauptKategorieUi?>(null) }

    // Liste aller Kategorien unter der gewählten Hauptkategorie
    var categories by remember { mutableStateOf<List<WizardLevelItemUi>>(emptyList()) }

    // Aktuell ausgewählte Kategorie
    var selectedCategory by remember { mutableStateOf<WizardLevelItemUi?>(null) }

    // Liste aller Unterkategorien unter der gewählten Kategorie
    var unterCategories by remember { mutableStateOf<List<WizardLevelItemUi>>(emptyList()) }

    // Aktuell ausgewählte Unterkategorie
    var selectedUnter by remember { mutableStateOf<WizardLevelItemUi?>(null) }

    // Checkbox: Zuständigkeit für komplette Hauptkategorie aktiv?
    var hauptCheck by remember { mutableStateOf(false) }

    // Checkbox: Zuständigkeit für komplette Kategorie aktiv?
    var categoryCheck by remember { mutableStateOf(false) }

    // E-Mail-Adresse der zuständigen Person
    var email by remember { mutableStateOf("") }

    // Farbpalette für Hauptkategorien
    // Diese Farben werden für die Kacheln der Hauptkategorien verwendet.
    val fullPalette = remember {
        listOf(
            Color(0xFF3B82F6),
            Color(0xFFF59E0B),
            Color(0xFF8B5CF6),
            Color(0xFF10B981),
            Color(0xFF06B6D4),
            Color(0xFFEF4444),
            Color(0xFF6366F1),
            Color(0xFFEC4899),
            Color(0xFF84CC16)
        )
    }

    // Farbe für "Sonstiges"
    val sonstigesColor = Color(0xFF64748B)

    /**
     * Setzt alle abhängigen Zustände zurück,
     * wenn ein neuer Raum ausgewählt wird.
     */
    fun resetFromRoom() {
        selectedHaupt = null
        selectedCategory = null
        selectedUnter = null
        hauptCheck = false
        categoryCheck = false
        email = ""
        categories = emptyList()
        unterCategories = emptyList()
    }

    /**
     * Setzt alle abhängigen Zustände zurück,
     * wenn eine neue Hauptkategorie ausgewählt wird.
     */
    fun resetFromHaupt() {
        selectedCategory = null
        selectedUnter = null
        categoryCheck = false
        email = ""
        categories = emptyList()
        unterCategories = emptyList()
    }

    /**
     * Setzt alle abhängigen Zustände zurück,
     * wenn eine neue Kategorie ausgewählt wird.
     */
    fun resetFromCategory() {
        selectedUnter = null
        email = ""
        unterCategories = emptyList()
    }

    /**
     * Setzt den kompletten Screen-Zustand zurück.
     */
    fun resetAll() {
        selectedRoom = null
        resetFromRoom()
    }

    // ---------------- LOAD ROOMS ----------------

    /**
     * Lädt beim ersten Rendern alle Räume aus Firestore.
     *
     * Sortierung erfolgt über das Feld "order".
     */
    LaunchedEffect(Unit) {
        val snap = db.collection("rooms")
            .orderBy("order", Query.Direction.ASCENDING)
            .get()
            .await()

        // Speichert jeden Raum als Pair(id, name)
        rooms = snap.documents.map { it.id to (it.getString("name") ?: it.id) }
    }

    // ---------------- LOAD HAUPTKATEGORIEN ----------------

    /**
     * Lädt beim ersten Rendern alle Hauptkategorien.
     *
     * Hauptkategorien sind alle Kategorien mit parentId = null.
     */
    LaunchedEffect(Unit) {
        val snap = db.collection("categories")
            .whereEqualTo("parentId", null)
            .get()
            .await()

        // Rohdaten aus Firestore lesen
        val raw = snap.documents.mapNotNull { doc ->
            val label = doc.getString("label") ?: return@mapNotNull null
            val iconKey = doc.getString("icon") ?: "more"
            val order = (doc.getLong("order") ?: 0L).toInt()
            Triple(order, doc.id, Pair(label, iconKey))
        }.sortedBy { it.first }

        // In UI-Modell umwandeln
        val mapped = raw.map { (_, id, pair) ->
            val (label, iconKey) = pair
            HauptKategorieUi(
                id = id,
                label = label,
                icon = IconRegistry.headerIconFor(iconKey),
                tileColor = Color.Unspecified
            )
        }

        // "Sonstiges" immer ans Ende sortieren
        val sorted = mapped.sortedWith(
            compareBy { it.label.trim().equals("Sonstiges", ignoreCase = true) }
        )

        // Farben aus Palette zuweisen
        var idx = 0
        hauptCategories = sorted.map { item ->
            val isSonst = item.label.trim().equals("Sonstiges", ignoreCase = true)
            val color = if (isSonst) sonstigesColor else fullPalette[idx++ % fullPalette.size]
            item.copy(tileColor = color)
        }
    }

    /**
     * Hilfsfunktion:
     * Lädt eine bereits gespeicherte Zuständigkeits-E-Mail
     * für Raum + Kategorie.
     */
    suspend fun loadExistingEmail(roomId: String, categoryId: String) {
        email = ResponsibilityRepository.getResponsibleEmail(roomId, categoryId) ?: ""
    }

    // ---------------- LOAD KATEGORIEN ----------------

    /**
     * Sobald eine Hauptkategorie gewählt wird,
     * werden die zugehörigen Kategorien geladen.
     */
    LaunchedEffect(selectedHaupt?.id) {
        val hid = selectedHaupt?.id ?: return@LaunchedEffect

        // Beim Wechsel der Hauptkategorie tiefere Ebenen zurücksetzen
        resetFromHaupt()

        val snap = db.collection("categories")
            .whereEqualTo("parentId", hid)
            .get()
            .await()

        val raw = snap.documents.mapNotNull { doc ->
            val label = doc.getString("label") ?: return@mapNotNull null
            val iconKey = doc.getString("icon") ?: "sonst"
            val order = (doc.getLong("order") ?: 0L).toInt()
            WizardLevelItemUi(doc.id, label, iconKey, order)
        }.sortedBy { it.order }

        // "Sonstiges" an Ende sortieren
        categories = raw.sortedWith(
            compareBy { it.label.trim().equals("Sonstiges", ignoreCase = true) }
        )
    }

    // ---------------- LOAD UNTERKATEGORIEN ----------------

    /**
     * Sobald eine Kategorie gewählt wird,
     * werden die zugehörigen Unterkategorien geladen.
     */
    LaunchedEffect(selectedCategory?.id) {
        val cid = selectedCategory?.id ?: return@LaunchedEffect

        // Beim Wechsel der Kategorie tiefere Ebene zurücksetzen
        resetFromCategory()

        val snap = db.collection("categories")
            .whereEqualTo("parentId", cid)
            .get()
            .await()

        val raw = snap.documents.mapNotNull { doc ->
            val label = doc.getString("label") ?: return@mapNotNull null
            val iconKey = doc.getString("icon") ?: "sonst"
            val order = (doc.getLong("order") ?: 0L).toInt()
            WizardLevelItemUi(doc.id, label, iconKey, order)
        }

        // Sortierung: zuerst order, dann "Sonstiges" zuletzt, dann alphabetisch
        unterCategories = raw.sortedWith(
            compareBy<WizardLevelItemUi> { it.order }
                .thenBy { it.label.trim().equals("Sonstiges", ignoreCase = true) }
                .thenBy { it.label.lowercase() }
        )
    }

    // ---------------- UI ----------------

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ---------------- TITEL ----------------
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Zuständigkeiten verwalten",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            // ---------------- ROOM CARD ----------------
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {

                        // Überschrift im Raum-Block
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Router, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Für welchen Raum soll eine Zuständigkeit festgelegt werden?")
                        }

                        Spacer(Modifier.height(12.dp))

                        // Alle Räume als auswählbare Elemente anzeigen
                        rooms.forEach { room ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (selectedRoom == room)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clickable {
                                        // Gewählten Raum setzen
                                        selectedRoom = room

                                        // Danach abhängige Auswahl zurücksetzen
                                        resetFromRoom()
                                    }
                            ) {
                                Text(room.second, modifier = Modifier.padding(14.dp))
                            }
                        }
                    }
                }
            }

            // ---------------- HAUPTKATEGORIEN ----------------
            if (selectedRoom != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {

                            Text("Wählen Sie die Hauptkategorie")
                            Spacer(Modifier.height(12.dp))

                            // Wenn keine Hauptkategorien vorhanden sind
                            if (hauptCategories.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Keine Hauptkategorien vorhanden.",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(Modifier.height(6.dp))

                                    Text(
                                        text = "Bitte zuerst Hauptkategorie hinzufügen.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(Modifier.height(12.dp))

                                    Button(
                                        onClick = onOpenAdminCategories,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary,
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Text("Zu Kategorien verwalten")
                                    }
                                }
                            } else {
                                // Hauptkategorien als Kacheln darstellen
                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    hauptCategories.chunked(2).forEach { row ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            row.forEach { cat ->
                                                HauptTile(
                                                    modifier = Modifier.weight(1f),
                                                    label = cat.label,
                                                    icon = cat.icon,
                                                    tileColor = cat.tileColor,
                                                    selected = (selectedHaupt?.id == cat.id),
                                                    onClick = {
                                                        // Gewählte Hauptkategorie setzen
                                                        selectedHaupt = cat

                                                        // Hierarchie-Checkboxen und Mail zurücksetzen
                                                        hauptCheck = false
                                                        categoryCheck = false
                                                        email = ""

                                                        // Vorhandene Zuständigkeit dieser Ebene laden
                                                        scope.launch {
                                                            loadExistingEmail(selectedRoom!!.first, cat.id)
                                                        }
                                                    }
                                                )
                                            }

                                            // Falls ungerade Anzahl → leeren Platz auffüllen
                                            if (row.size == 1) Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ---------------- HAUPTKATEGORIE-CHECK + EMAIL ----------------
            if (selectedHaupt != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = hauptCheck,
                                    onCheckedChange = { checked ->
                                        hauptCheck = checked

                                        if (checked) {
                                            // Wenn Hauptkategorie-Zuständigkeit aktiv ist,
                                            // tiefere Ebenen deaktivieren
                                            selectedCategory = null
                                            selectedUnter = null
                                            categoryCheck = false

                                            // Vorhandene Mail laden
                                            scope.launch {
                                                loadExistingEmail(selectedRoom!!.first, selectedHaupt!!.id)
                                            }
                                        } else {
                                            // Beim Deaktivieren Eingabe leeren
                                            email = ""
                                        }
                                    }
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Eine zuständige Person für die gesamte Hauptkategorie inklusive aller Unterkategorien")
                            }

                            // Nur wenn Checkbox aktiv ist, Eingabefeld + Speichern anzeigen
                            if (hauptCheck) {
                                Spacer(Modifier.height(16.dp))
                                Text("Zuständige Person")
                                Spacer(Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = { Text("E-Mail der zuständigen Person eingeben") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(Modifier.height(16.dp))

                                Button(
                                    enabled = email.isNotBlank(),
                                    onClick = {
                                        scope.launch {
                                            ResponsibilityRepository.saveResponsibility(
                                                roomId = selectedRoom!!.first,
                                                categoryId = selectedHaupt!!.id,
                                                email = email.trim()
                                            )
                                            snackbarHostState.showSnackbar("Gespeichert ✅")
                                            resetAll()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Speichern")
                                }
                            }
                        }
                    }
                }
            }

            // ---------------- KATEGORIEN ----------------
            if (selectedRoom != null && selectedHaupt != null && !hauptCheck) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Welche Kategorie ist betroffen?")
                            Spacer(Modifier.height(12.dp))

                            val tileColor = Color(0xFF14B8A6)

                            // Wenn keine Kategorien vorhanden sind
                            if (categories.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Keine kategorien vorhanden.",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(Modifier.height(6.dp))

                                    Text(
                                        text = "Bitte zuerst Kategorie hinzufügen.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(Modifier.height(12.dp))

                                    Button(
                                        onClick = onOpenAdminCategories,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary,
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Text("Zu Kategorien verwalten")
                                    }
                                }
                            } else {
                                // Kategorien als Kacheln
                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    categories.chunked(2).forEach { row ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            row.forEach { item ->
                                                val selected = selectedCategory?.id == item.id

                                                Surface(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(110.dp)
                                                        .clickable {
                                                            // Kategorie auswählen
                                                            selectedCategory = item

                                                            // Kategorie-Checkbox und Unterkategorie zurücksetzen
                                                            categoryCheck = false
                                                            selectedUnter = null
                                                            email = ""
                                                        }
                                                        .graphicsLayer {
                                                            // kleine Vergrößerung wenn ausgewählt
                                                            scaleX = if (selected) 1.03f else 1f
                                                            scaleY = if (selected) 1.03f else 1f
                                                        },
                                                    shape = RoundedCornerShape(14.dp),
                                                    color = tileColor,
                                                    shadowElevation = if (selected) 8.dp else 2.dp,
                                                    border = if (selected) BorderStroke(3.dp, Color(0xFF16A34A)) else null
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(vertical = 14.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = IconRegistry.iconFor(item.iconKey),
                                                            contentDescription = item.label,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(36.dp)
                                                        )
                                                        Spacer(Modifier.height(10.dp))
                                                        Text(
                                                            text = item.label,
                                                            color = Color.White,
                                                            style = MaterialTheme.typography.titleMedium
                                                        )
                                                    }
                                                }
                                            }

                                            if (row.size == 1) Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ---------------- KATEGORIE-CHECK + EMAIL ----------------
            if (selectedRoom != null && selectedCategory != null && !hauptCheck) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = categoryCheck,
                                    onCheckedChange = { checked ->
                                        categoryCheck = checked

                                        if (checked) {
                                            // Wenn Kategorie-Zuständigkeit aktiv,
                                            // Unterkategorie deaktivieren
                                            selectedUnter = null

                                            // Vorhandene Mail laden
                                            scope.launch {
                                                loadExistingEmail(selectedRoom!!.first, selectedCategory!!.id)
                                            }
                                        } else {
                                            email = ""
                                        }
                                    }
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Eine zuständige Person für die gesamte Kategorie inklusive aller Unterkategorien")
                            }

                            if (categoryCheck) {
                                Spacer(Modifier.height(16.dp))
                                Text("Zuständige Person")
                                Spacer(Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = { Text("E-Mail der zuständigen Person eingeben") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(Modifier.height(16.dp))

                                Button(
                                    enabled = email.isNotBlank(),
                                    onClick = {
                                        scope.launch {
                                            ResponsibilityRepository.saveResponsibility(
                                                roomId = selectedRoom!!.first,
                                                categoryId = selectedCategory!!.id,
                                                email = email.trim()
                                            )
                                            snackbarHostState.showSnackbar("Gespeichert ✅")
                                            resetAll()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Speichern")
                                }
                            }
                        }
                    }
                }
            }

            // ---------------- UNTERKATEGORIEN ----------------
            if (selectedRoom != null && selectedCategory != null && !hauptCheck && !categoryCheck) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {

                            Text("Wählen Sie die Kategorie")
                            Spacer(Modifier.height(6.dp))

                            val iconTint = Color(0xFF14B8A6)

                            // Wenn keine Unterkategorien vorhanden sind
                            if (unterCategories.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Keine Unterkategorien vorhanden.",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(Modifier.height(6.dp))

                                    Text(
                                        text = "Bitte zuerst Unterkategorie hinzufügen.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(Modifier.height(12.dp))

                                    Button(
                                        onClick = onOpenAdminCategories,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary,
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Text("Zu Kategorien verwalten")
                                    }
                                }
                            } else {
                                // Unterkategorien als Grid mit 3 Spalten
                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    unterCategories.chunked(3).forEach { row ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            row.forEach { item ->
                                                val selected = selectedUnter?.id == item.id

                                                Surface(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(100.dp)
                                                        .clickable {
                                                            // Unterkategorie auswählen
                                                            selectedUnter = item

                                                            // Bereits gespeicherte Mail laden
                                                            scope.launch {
                                                                loadExistingEmail(selectedRoom!!.first, item.id)
                                                            }
                                                        }
                                                        .graphicsLayer {
                                                            scaleX = if (selected) 1.03f else 1f
                                                            scaleY = if (selected) 1.03f else 1f
                                                        },
                                                    shape = RoundedCornerShape(14.dp),
                                                    color = Color.White,
                                                    shadowElevation = if (selected) 8.dp else 2.dp,
                                                    border = if (selected)
                                                        BorderStroke(3.dp, Color(0xFF16A34A))
                                                    else
                                                        ButtonDefaults.outlinedButtonBorder(enabled = true)
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(12.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = IconRegistry.iconFor(item.iconKey),
                                                            contentDescription = item.label,
                                                            tint = iconTint,
                                                            modifier = Modifier.size(26.dp)
                                                        )
                                                        Spacer(Modifier.height(8.dp))
                                                        Text(
                                                            text = item.label,
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                    }
                                                }
                                            }

                                            // Leere Zellen auffüllen, wenn Zeile nicht voll ist
                                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // Nur wenn eine Unterkategorie gewählt ist,
                            // E-Mail-Eingabe + Speichern anzeigen
                            if (selectedUnter != null) {
                                Text("Zuständige Person")
                                Spacer(Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = { Text("E-Mail der zuständigen Person eingeben") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(Modifier.height(16.dp))

                                Button(
                                    enabled = email.isNotBlank(),
                                    onClick = {
                                        scope.launch {
                                            ResponsibilityRepository.saveResponsibility(
                                                roomId = selectedRoom!!.first,
                                                categoryId = selectedUnter!!.id,
                                                email = email.trim()
                                            )
                                            snackbarHostState.showSnackbar("Gespeichert ✅")
                                            resetAll()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Speichern")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}