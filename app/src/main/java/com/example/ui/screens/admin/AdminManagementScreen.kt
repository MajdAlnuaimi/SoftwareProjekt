package com.example.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

/**
 * Datenmodell für einen Admin-Benutzer.
 *
 * uid   = Dokument-ID in Firestore
 * email = E-Mail-Adresse des Admins
 * role  = Rolle des Admins (z.B. admin oder superadmin)
 */
data class AdminUser(
    val uid: String,
    val email: String,
    val role: String
)

/**
 * Datenmodell für normale App-Benutzer.
 *
 * Diese Benutzer können später zu Admins
 * befördert werden.
 */
data class AppUser(
    val uid: String,
    val email: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminManagementScreen() {

    // Zugriff auf Firestore-Datenbank
    val db = FirebaseFirestore.getInstance()

    // Liste aller Admins
    var admins by remember { mutableStateOf<List<AdminUser>>(emptyList()) }

    // Liste aller Benutzer
    var users by remember { mutableStateOf<List<AppUser>>(emptyList()) }

    // Suchfeld für Benutzer
    var search by remember { mutableStateOf("") }

    // ===== Snackbar =====
    // Snackbar zum Anzeigen von Erfolg / Fehler Meldungen
    val snackbarHostState = remember { SnackbarHostState() }

    // CoroutineScope für Snackbar und Firestore Aktionen
    val scope = rememberCoroutineScope()

    // ===== Delete confirm dialog state =====
    // Steuert ob der Löschdialog angezeigt wird
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Der Admin der gelöscht werden soll
    var adminToDelete by remember { mutableStateOf<AdminUser?>(null) }

    /**
     * Firestore Listener
     *
     * Wird aktiviert wenn der Screen angezeigt wird
     * und lädt automatisch:
     * - alle Admins
     * - alle Benutzer
     */
    DisposableEffect(Unit) {

        // Listener für Admins
        val adminListener: ListenerRegistration =
            db.collection("admins")
                .addSnapshotListener { snap, _ ->
                    if (snap != null) {
                        admins = snap.documents.mapNotNull { doc ->

                            // Email aus Firestore lesen
                            val email = doc.getString("email") ?: return@mapNotNull null

                            // Rolle lesen (default = admin)
                            val role = doc.getString("role") ?: "admin"

                            AdminUser(doc.id, email, role)
                        }
                    }
                }

        // Listener für normale Benutzer
        val userListener: ListenerRegistration =
            db.collection("users")
                .addSnapshotListener { snap, _ ->
                    if (snap != null) {
                        users = snap.documents.mapNotNull { doc ->

                            val email = doc.getString("email") ?: return@mapNotNull null

                            AppUser(uid = doc.id, email = email)
                        }
                    }
                }

        // Listener entfernen wenn Screen geschlossen wird
        onDispose {
            adminListener.remove()
            userListener.remove()
        }
    }

    // Benutzer nach Suchbegriff filtern
    val filteredUsers = users.filter { it.email.contains(search, ignoreCase = true) }

    /**
     * Superadmins filtern
     */
    val superAdmins = remember(admins) {
        admins.filter { it.role == "superadmin" }.sortedBy { it.email.lowercase() }
    }

    /**
     * Normale Admins filtern
     */
    val normalAdmins = remember(admins) {
        admins.filter { it.role != "superadmin" }.sortedBy { it.email.lowercase() }
    }

    // Form der Cards im UI
    val cardShape = RoundedCornerShape(16.dp)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { padding ->

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),

                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 30.dp
                ),

                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                /**
                 * Header des Screens
                 */
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Admins verwalten",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                // ===== Aktuelle Admins (Grouped) =====
                item {

                    /**
                     * Card die alle aktuellen Admins anzeigt
                     */
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {

                            Text(
                                "Aktuelle Admins",
                                style = MaterialTheme.typography.titleMedium
                            )

                            // ===== SUPERADMIN CARD =====
                            if (superAdmins.isNotEmpty()) {

                                /**
                                 * Anzeige aller Superadmins
                                 */
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFEFF4FF)
                                    ),
                                    elevation = CardDefaults.cardElevation(2.dp)
                                ) {

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {

                                        Text(
                                            text = "Superadmin",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.secondary
                                        )

                                        superAdmins.forEach { admin ->

                                            // Anzeige der Email
                                            Text(
                                                text = admin.email,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }

                            // ===== ADMIN CARD =====
                            if (normalAdmins.isNotEmpty()) {

                                /**
                                 * Anzeige aller normalen Admins
                                 */
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFF1FAF5)
                                    ),
                                    elevation = CardDefaults.cardElevation(2.dp)
                                ) {

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {

                                        Text(
                                            text = "Admin",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        normalAdmins.forEach { admin ->

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {

                                                // Admin Email anzeigen
                                                Text(
                                                    text = admin.email,
                                                    modifier = Modifier.weight(1f),
                                                    style = MaterialTheme.typography.bodyLarge
                                                )

                                                // Button zum Entfernen eines Admins
                                                IconButton(
                                                    onClick = {
                                                        adminToDelete = admin
                                                        showDeleteDialog = true
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Admin entfernen",
                                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
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

                /**
                 * Trennlinie
                 */
                item {
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // ===== Search =====
                item {

                    /**
                     * Card zum Hinzufügen neuer Admins
                     */
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = cardShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {

                            Text("Admin hinzufügen", style = MaterialTheme.typography.titleMedium)

                            /**
                             * Suchfeld für Benutzer
                             */
                            OutlinedTextField(
                                value = search,
                                onValueChange = { search = it },
                                label = { Text("Benutzer suchen (E-Mail)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(Modifier.height(12.dp))

                            Text("Benutzer", style = MaterialTheme.typography.titleMedium)

                            val query = search.trim()

                            val baseList = if (query.isEmpty()) users else filteredUsers

                            // Benutzer anzeigen die noch kein Admin sind
                            val listToShow = baseList.filter { user ->
                                admins.none { it.uid == user.uid }
                            }

                            if (listToShow.isEmpty()) {

                                Text(
                                    "Keine Benutzer gefunden.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                            } else {

                                listToShow.forEachIndexed { index, user ->

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {

                                        // Benutzer Email
                                        Text(
                                            text = user.email,
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodyLarge
                                        )

                                        /**
                                         * Button um Benutzer zum Admin zu machen
                                         */
                                        IconButton(
                                            onClick = {

                                                db.collection("admins")
                                                    .document(user.uid)
                                                    .set(
                                                        mapOf(
                                                            "email" to user.email,
                                                            "role" to "admin",
                                                            "createdAt" to System.currentTimeMillis()
                                                        )
                                                    )
                                                    .addOnSuccessListener {
                                                        scope.launch {
                                                            snackbarHostState.showSnackbar("Admin wurde hinzugefügt.")
                                                        }
                                                    }
                                                    .addOnFailureListener {
                                                        scope.launch {
                                                            snackbarHostState.showSnackbar("Hinzufügen fehlgeschlagen: ${it.localizedMessage}")
                                                        }
                                                    }
                                            }
                                        ) {

                                            Icon(
                                                imageVector = Icons.Default.PersonAdd,
                                                contentDescription = "Als Admin hinzufügen",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    if (index != listToShow.lastIndex) {
                                        //HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ===== Delete confirmation dialog =====
            if (showDeleteDialog && adminToDelete != null) {

                val target = adminToDelete!!

                AlertDialog(
                    onDismissRequest = {
                        showDeleteDialog = false
                        adminToDelete = null
                    },
                    containerColor = MaterialTheme.colorScheme.surface,

                    title = { Text("Admin löschen") },

                    text = { Text("Sind Sie sicher, dass Sie diesen Admin löschen möchten?") },

                    confirmButton = {
                        TextButton(
                            onClick = {

                                db.collection("admins")
                                    .document(target.uid)
                                    .delete()
                                    .addOnSuccessListener {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Admin wurde gelöscht.")
                                        }
                                    }
                                    .addOnFailureListener {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Löschen fehlgeschlagen: ${it.localizedMessage}")
                                        }
                                    }

                                showDeleteDialog = false
                                adminToDelete = null
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color.Red
                            )
                        ) { Text("Löschen") }
                    },

                    dismissButton = {
                        TextButton(
                            onClick = {
                                showDeleteDialog = false
                                adminToDelete = null
                            }
                        ) { Text("Abbrechen") }
                    }
                )
            }
        }
    }
}