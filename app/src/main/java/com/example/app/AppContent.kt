package com.example.app

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.app.appComponents.AppDrawer
import com.example.app.appComponents.AppTopBar
import com.example.app.screensNavigation.MainScreenContent
import com.example.data.FirebaseRepository
import com.example.data.ResponsibilityRepository
import com.example.google.GoogleAuthService
import com.example.homeNavigation.AppNavigator
import com.example.scan.RoomDetection
import com.example.state.AdminNavigationState
import com.example.state.UserAccessState
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.wizard.WizardState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

/**
 * Haupt-UI der gesamten App.
 *
 * Diese Composable ist der zentrale Einstiegspunkt für die Benutzeroberfläche.
 *
 * Sie kümmert sich um:
 * - Login / Logout
 * - Benutzerrechte (Admin / Superadmin)
 * - Navigation Drawer
 * - TopBar
 * - Bildschirmnavigation
 * - Wizard-Zustand
 * - Raum-Erkennung
 * - Absenden von Meldungen
 */
@Composable
fun AppContent(
    activity: MainActivity,
    navigator: AppNavigator
) {

    // Zustand des seitlichen Navigationsmenüs
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    // CoroutineScope für asynchrone Aktionen
    val scope = rememberCoroutineScope()

    // Repository für Firebase-Zugriffe
    val repo = remember { FirebaseRepository() }

    // Google Login Service
    val googleAuthClient = remember { GoogleAuthService(activity) }

    /**
     * Benutzer-Zugriffsstatus:
     * - eingeloggt
     * - Admin
     * - Superadmin
     */
    var accessState by remember {
        mutableStateOf(UserAccessState(isSignedIn = googleAuthClient.isSignedIn()))
    }

    // aktuell erkannter Raum
    var selectedRoom by rememberSaveable { mutableStateOf<String?>(null) }

    // Zustand des Schadens-Wizards
    var wizardState by rememberSaveable { mutableStateOf(WizardState()) }

    // Nachricht bei Raum-Erkennung
    var roomDetectMsg by remember { mutableStateOf<String?>(null) }

    // Flag ob gerade ein Raum gescannt wird
    var isDetectingRoom by remember { mutableStateOf(false) }

    // Flag ob ein Wizard-Bericht gesendet wird
    var isSubmitting by remember { mutableStateOf(false) }

    // Fehler beim Absenden
    var submitError by remember { mutableStateOf<String?>(null) }

    // Navigation innerhalb des Admin-Bereichs
    var adminNav by remember { mutableStateOf(AdminNavigationState()) }

    // aktuell eingeloggter Firebase User
    val currentUser = FirebaseAuth.getInstance().currentUser

    /**
     * Prüft ob der Benutzer Verantwortlichkeiten hat.
     * Falls ja, werden zusätzliche Menüpunkte angezeigt.
     */
    LaunchedEffect(currentUser?.email) {

        val email = currentUser?.email

        val isResponsible =
            if (email.isNullOrBlank()) {
                false
            } else {
                try {
                    ResponsibilityRepository.hasAnyResponsibility(email)
                } catch (_: Exception) {
                    false
                }
            }

        accessState = accessState.copy(isResponsibleUser = isResponsible)
    }

    /**
     * Listener für Adminrechte aus Firestore.
     */
    DisposableEffect(currentUser?.uid) {

        if (currentUser == null) {
            accessState = accessState.copy(isAdmin = false, isSuperAdmin = false)
            return@DisposableEffect onDispose { }
        }

        val reg = FirebaseFirestore.getInstance()
            .collection("admins")
            .document(currentUser.uid)
            .addSnapshotListener { doc, error ->

                if (error != null) {
                    accessState = accessState.copy(isAdmin = false, isSuperAdmin = false)
                    return@addSnapshotListener
                }

                val isAdmin = doc != null && doc.exists()
                val isSuperAdmin = doc?.getString("role") == "superadmin"

                accessState = accessState.copy(
                    isAdmin = isAdmin,
                    isSuperAdmin = isSuperAdmin
                )
            }

        onDispose { reg.remove() }
    }

    /**
     * Android Back Button Handling.
     */
    BackHandler(enabled = navigator.selectedMenuItem != "Home") {

        if (navigator.selectedMenuItem == "AdminLevel3") {

            val backId = adminNav.level2ParentId

            if (backId != null) {

                adminNav = adminNav.copy(
                    parentId = backId,
                    parentLabel = adminNav.level2ParentLabel,
                    parentIconKey = adminNav.level2ParentIconKey
                )

                if (navigator.backStack.isNotEmpty()) {
                    navigator.backStack.removeAt(navigator.backStack.lastIndex)
                }

                navigator.selectedMenuItem = "AdminLevel2"
                return@BackHandler
            }

            navigator.resetTo("AdminCategories")
            return@BackHandler
        }

        val ok = navigator.navigateBack()

        if (!ok) navigator.resetTo("Home")
    }

    /**
     * WLAN-Raumerkennung.
     */
    @SuppressLint("MissingPermission")
    suspend fun detectRoomNow() {

        isDetectingRoom = true
        roomDetectMsg = null

        try {

            val best = RoomDetection.detectMulti(activity).firstOrNull()

            if (best != null) {
                selectedRoom = best.roomName
            } else {
                selectedRoom = null
                roomDetectMsg = "Kein Raum erkannt. Bitte erneut versuchen oder Raum ändern."
            }

        } catch (_: Throwable) {

            selectedRoom = null
            roomDetectMsg = "Fehler beim Scannen. Bitte erneut versuchen."

        } finally {

            isDetectingRoom = false
        }
    }

    /**
     * Falls Benutzer nicht eingeloggt ist → LoginScreen anzeigen.
     */
    if (!accessState.isSignedIn) {

        LoginScreen(
            onLogin = {

                scope.launch {

                    val ok = googleAuthClient.signIn()

                    if (ok) {

                        accessState = accessState.copy(isSignedIn = true)

                        val user = FirebaseAuth.getInstance().currentUser

                        if (user != null) {

                            FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(user.uid)
                                .set(
                                    mapOf(
                                        "email" to user.email,
                                        "createdAt" to System.currentTimeMillis()
                                    )
                                )
                        }
                    }
                }
            }
        )

        return
    }

    /**
     * Fehler beim Senden einer Meldung anzeigen.
     */
    submitError?.let { msg ->
        LaunchedEffect(msg) {
            Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()
            submitError = null
        }
    }

    /**
     * Navigation Drawer (seitliches Menü).
     */
    ModalNavigationDrawer(

        drawerState = drawerState,

        drawerContent = {

            AppDrawer(

                selectedMenuItem = navigator.selectedMenuItem,

                isAdmin = accessState.isAdmin,
                isSuperAdmin = accessState.isSuperAdmin,
                isResponsibleUser = accessState.isResponsibleUser,

                onHomeClick = {
                    navigator.resetTo("Home")
                    scope.launch { drawerState.close() }
                },

                onWizardReportsClick = {
                    navigator.resetTo("WizardReports")
                    scope.launch { drawerState.close() }
                },

                onResponsibleReportsClick = {
                    navigator.resetTo("ResponsibleReports")
                    scope.launch { drawerState.close() }
                },

                onAdminManagementClick = {
                    navigator.resetTo("AdminManagement")
                    scope.launch { drawerState.close() }
                },

                onAdminSettingsClick = {
                    navigator.resetTo("AdminSettings")
                    scope.launch { drawerState.close() }
                },

                onAdminCategoriesClick = {
                    navigator.resetTo("AdminCategories")
                    scope.launch { drawerState.close() }
                },

                onAdminRoomsClick = {
                    navigator.resetTo("AdminRooms")
                    scope.launch { drawerState.close() }
                },

                onAdminResponsibilitiesClick = {
                    navigator.resetTo("AdminResponsibilities")
                    scope.launch { drawerState.close() }
                },

                onLogoutClick = {

                    scope.launch {

                        googleAuthClient.signOut()

                        accessState = accessState.copy(
                            isSignedIn = false,
                            isAdmin = false,
                            isSuperAdmin = false,
                            isResponsibleUser = false
                        )

                        navigator.resetTo("Home")

                        drawerState.close()
                    }
                }
            )
        }
    ) {

        /**
         * Hauptlayout der App
         */
        Scaffold(

            topBar = {
                AppTopBar(
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }

        ) { paddingValues ->

            Column(Modifier.padding(paddingValues)) {

                /**
                 * Sicherheitscheck:
                 * Wenn ein Nicht-Admin versucht Admin-Seiten zu öffnen → zurück zu Home
                 */
                if (!accessState.isAdmin && navigator.selectedMenuItem.startsWith("Admin")) {
                    navigator.resetTo("Home")
                }

                /**
                 * Hauptscreen-Navigation der App
                 */
                MainScreenContent(

                    selectedMenuItem = navigator.selectedMenuItem,
                    selectedRoom = selectedRoom,
                    roomDetectMsg = roomDetectMsg,
                    isDetectingRoom = isDetectingRoom,

                    wizardState = wizardState,

                    navigator = navigator,
                    adminNav = adminNav,

                    onWizardStateChange = { wizardState = it },

                    onSelectedRoomChange = { selectedRoom = it },

                    onRoomDetectMsgChange = { roomDetectMsg = it },

                    onAdminNavChange = { adminNav = it },

                    onDetectRoom = {
                        scope.launch { detectRoomNow() }
                    },

                    /**
                     * Meldung aus Wizard an Firebase senden
                     */
                    onSubmitWizard = { email ->

                        if (isSubmitting) return@MainScreenContent

                        isSubmitting = true
                        submitError = null

                        scope.launch {

                            try {

                                repo.submitWizardReport(
                                    wizardState = wizardState,
                                    room = selectedRoom ?: "UNKNOWN",
                                    targetEmail = email
                                )

                                wizardState = WizardState()

                                navigator.navigateTo("WizardSuccess")

                            } catch (e: Exception) {

                                submitError = e.message ?: "Fehler beim Senden"

                            } finally {

                                isSubmitting = false
                            }
                        }
                    }
                )
            }
        }
    }
}