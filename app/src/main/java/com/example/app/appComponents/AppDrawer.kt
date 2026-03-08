package com.example.app.appComponents

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Drafts
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth

/**
 * Seitliches App-Menü (Navigation Drawer) der Anwendung.
 *
 * Diese Composable zeigt:
 * - die Benutzerinformationen oben
 * - allgemeine Navigationspunkte
 * - zusätzliche Admin-Bereiche abhängig von der Benutzerrolle
 * - einen Logout-Bereich
 *
 * Die Anzeige bestimmter Menüpunkte hängt davon ab, ob der Benutzer:
 * - Admin ist
 * - Superadmin ist
 * - als zuständige Person hinterlegt ist
 */
@Composable
fun AppDrawer(
    selectedMenuItem: String,
    isAdmin: Boolean,
    isSuperAdmin: Boolean,
    isResponsibleUser: Boolean,
    onHomeClick: () -> Unit,
    onWizardReportsClick: () -> Unit,
    onResponsibleReportsClick: () -> Unit,
    onAdminManagementClick: () -> Unit,
    onAdminSettingsClick: () -> Unit,
    onAdminCategoriesClick: () -> Unit,
    onAdminRoomsClick: () -> Unit,
    onAdminResponsibilitiesClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    // Farbschema aus dem aktuellen MaterialTheme
    val cs = MaterialTheme.colorScheme

    // Hintergrund-Verlauf des seitlichen Menüs
    val drawerGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0B1220),
            cs.secondary.copy(alpha = 0.90f),
            Color(0xFF0B1220)
        )
    )

    // Standardfarben für Überschriften, Text und Icons
    val sectionTitleColor = Color.White.copy(alpha = 0.55f)
    val itemTextColor = Color.White.copy(alpha = 0.90f)
    val itemIconColor = Color.White.copy(alpha = 0.75f)

    // Hintergrund für den aktuell ausgewählten Menüpunkt
    val selectedBg = cs.primary.copy(alpha = 0.22f)

    // Einheitliche Form für alle Menüpunkte
    val itemShape = RoundedCornerShape(16.dp)

    // Farblogik für die Drawer-Menüpunkte
    val drawerItemColors = NavigationDrawerItemDefaults.colors(
        selectedContainerColor = selectedBg,
        selectedTextColor = Color.White,
        selectedIconColor = Color.White,
        unselectedContainerColor = Color.Transparent,
        unselectedTextColor = itemTextColor,
        unselectedIconColor = itemIconColor
    )

    /**
     * Kleine Trennlinie zwischen verschiedenen Bereichen im Drawer.
     */
    @Composable
    fun Trennlinie() {
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            color = Color.White.copy(alpha = 0.10f)
        )
    }

    /**
     * Überschrift für einen Abschnitt innerhalb des Drawers.
     */
    @Composable
    fun Abschnittstitel(text: String) {
        Text(
            text = text,
            color = sectionTitleColor,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
        )
    }

    // Äußere Hülle des Navigation Drawers
    ModalDrawerSheet(drawerContainerColor = Color.Transparent) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(min = 280.dp, max = 340.dp)
                .background(drawerGradient)
                .padding(top = 8.dp)
        ) {
            // Scrollbarer Bereich für alle Inhalte des Drawers
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                contentPadding = PaddingValues(bottom = 10.dp)
            ) {

                // =========================
                // Benutzerkarte oben
                // =========================
                item {
                    Surface(
                        color = Color.White.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(20.dp),
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Aktuell eingeloggter Firebase-Benutzer
                            val user = FirebaseAuth.getInstance().currentUser
                            val displayName = user?.displayName ?: "User"
                            val email = user?.email ?: ""
                            val photoUrl = user?.photoUrl

                            // Bereich für Profilbild oder Standard-Icon
                            Surface(
                                color = Color.White.copy(alpha = 0.10f),
                                shape = CircleShape,
                                modifier = Modifier.size(44.dp)
                            ) {
                                if (photoUrl != null) {
                                    // Profilbild aus Firebase laden
                                    AsyncImage(
                                        model = photoUrl,
                                        contentDescription = "Profilbild",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                } else {
                                    // Fallback, falls kein Profilbild vorhanden ist
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Benutzer",
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier
                                            .padding(10.dp)
                                            .fillMaxSize()
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Anzeigename des Benutzers
                                    Text(
                                        text = displayName,
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1
                                    )

                                    // Rollenbezeichnung je nach Berechtigung
                                    val roleLabel = when {
                                        isSuperAdmin -> "SUPERADMIN"
                                        isAdmin -> "ADMIN"
                                        else -> "USER"
                                    }

                                    // Hintergrundfarbe des Rollen-Badges
                                    val roleBg = when {
                                        isSuperAdmin -> cs.secondary.copy(alpha = 0.20f)
                                        isAdmin -> cs.primary.copy(alpha = 0.22f)
                                        else -> Color.White.copy(alpha = 0.10f)
                                    }

                                    // Rahmenfarbe des Rollen-Badges
                                    val roleBorder = when {
                                        isSuperAdmin -> cs.secondary.copy(alpha = 0.55f)
                                        isAdmin -> cs.primary.copy(alpha = 0.55f)
                                        else -> Color.White.copy(alpha = 0.15f)
                                    }

                                    // Badge zur Anzeige der Rolle
                                    Surface(
                                        color = roleBg,
                                        contentColor = Color.White,
                                        shape = RoundedCornerShape(999.dp),
                                        border = BorderStroke(1.dp, roleBorder)
                                    ) {
                                        Text(
                                            text = roleLabel,
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.padding(
                                                horizontal = 14.dp,
                                                vertical = 7.dp
                                            ),
                                            color = Color.White.copy(alpha = 0.95f)
                                        )
                                    }
                                }

                                // E-Mail-Adresse des Benutzers
                                Text(
                                    text = email,
                                    color = Color.White.copy(alpha = 0.75f),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // =========================
                // Allgemeiner Bereich
                // =========================
                item { Abschnittstitel("Allgemein") }

                item {
                    NavigationDrawerItem(
                        label = { Text("Home") },
                        selected = selectedMenuItem == "Home",
                        onClick = onHomeClick,
                        icon = { Icon(Icons.Default.Home, null) },
                        colors = drawerItemColors,
                        shape = itemShape,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                item {
                    NavigationDrawerItem(
                        label = { Text("Meine Meldungen") },
                        selected = selectedMenuItem == "WizardReports",
                        onClick = onWizardReportsClick,
                        icon = { Icon(Icons.Outlined.Drafts, null) },
                        colors = drawerItemColors,
                        shape = itemShape,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Nur sichtbar, wenn der Benutzer als zuständige Person eingetragen ist
                if (isResponsibleUser) {
                    item {
                        NavigationDrawerItem(
                            label = { Text("Zuständige Meldungen") },
                            selected = selectedMenuItem == "ResponsibleReports",
                            onClick = onResponsibleReportsClick,
                            icon = { Icon(Icons.Default.NotificationsActive, null) },
                            colors = drawerItemColors,
                            shape = itemShape,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // =========================
                // Admin-Bereich
                // =========================
                if (isAdmin) {
                    item { Trennlinie() }
                    item { Abschnittstitel("Administration") }

                    // Nur für Superadmins sichtbar
                    if (isSuperAdmin) {
                        item {
                            NavigationDrawerItem(
                                label = { Text("Admins verwalten") },
                                selected = selectedMenuItem == "AdminManagement",
                                onClick = onAdminManagementClick,
                                icon = { Icon(Icons.Default.ManageAccounts, null) },
                                colors = drawerItemColors,
                                shape = itemShape,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    item {
                        NavigationDrawerItem(
                            label = { Text("Wlan Einstellungen") },
                            selected = selectedMenuItem == "AdminSettings",
                            onClick = onAdminSettingsClick,
                            icon = { Icon(Icons.Default.AdminPanelSettings, null) },
                            colors = drawerItemColors,
                            shape = itemShape,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    item {
                        NavigationDrawerItem(
                            label = { Text("Kategorien") },
                            selected = selectedMenuItem == "AdminCategories",
                            onClick = onAdminCategoriesClick,
                            icon = { Icon(Icons.Default.Category, null) },
                            colors = drawerItemColors,
                            shape = itemShape,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    item {
                        NavigationDrawerItem(
                            label = { Text("Räume & APs") },
                            selected = selectedMenuItem == "AdminRooms",
                            onClick = onAdminRoomsClick,
                            icon = { Icon(Icons.Default.Router, null) },
                            colors = drawerItemColors,
                            shape = itemShape,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    item {
                        NavigationDrawerItem(
                            label = { Text("Zuständigkeiten") },
                            selected = selectedMenuItem == "AdminResponsibilities",
                            onClick = onAdminResponsibilitiesClick,
                            icon = { Icon(Icons.Default.AdminPanelSettings, null) },
                            colors = drawerItemColors,
                            shape = itemShape,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // =========================
                // Logout-Bereich
                // =========================
                item { Spacer(Modifier.height(8.dp)) }
                item { Trennlinie() }
                item { Abschnittstitel("Logout") }

                item {
                    NavigationDrawerItem(
                        label = { Text("Abmelden") },
                        selected = false,
                        onClick = onLogoutClick,
                        icon = { Icon(Icons.AutoMirrored.Filled.Logout, null) },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent,
                            unselectedTextColor = MaterialTheme.colorScheme.error,
                            unselectedIconColor = MaterialTheme.colorScheme.error,
                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                            selectedTextColor = MaterialTheme.colorScheme.error,
                            selectedIconColor = MaterialTheme.colorScheme.error
                        ),
                        shape = itemShape,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }

                item { Spacer(Modifier.height(10.dp)) }
            }
        }
    }
}