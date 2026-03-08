package com.example.app.screensNavigation

import androidx.compose.runtime.Composable
import com.example.homeNavigation.AppNavigator
import com.example.state.AdminNavigationState
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.reports.ResponsibleReportsScreen
import com.example.ui.screens.wifi.RoomDetectionScreen
import com.example.ui.screens.reports.WizardReportsListScreen
import com.example.ui.screens.admin.AdminCategoryScreen
import com.example.ui.screens.admin.AdminManagementScreen
import com.example.ui.screens.admin.responsibilities.AdminResponsibilitiesScreen
import com.example.ui.screens.admin.rooms.AdminRoomsScreen
import com.example.ui.screens.admin.AdminSettingsScreen
import com.example.ui.screens.admin.categories.AdminSubCategoryScreen
import com.example.ui.screens.wizard.HauptkategorieScreen
import com.example.ui.screens.wizard.KategorieScreen
import com.example.ui.screens.wizard.SuccessScreen
import com.example.ui.screens.wizard.SummaryScreen
import com.example.ui.screens.wizard.UnterkategorieScreen
import com.example.ui.screens.wizard.WizardState
import com.example.ui.screens.wizard.ZustandScreen

/**
 * Zentrale Steuerung des Inhaltsbereichs der App.
 *
 * Diese Composable entscheidet anhand des aktuell ausgewählten Menüpunktes
 * (selectedMenuItem), welcher Screen angezeigt wird.
 *
 * Sie ist damit der zentrale "Router" der Anwendung.
 *
 * Verantwortlich für:
 * - Navigation zwischen Screens
 * - Übergabe von Zuständen (State)
 * - Steuerung des Wizard-Flows
 * - Navigation im Adminbereich
 */
@Composable
fun MainScreenContent(
    selectedMenuItem: String,
    selectedRoom: String?,
    roomDetectMsg: String?,
    isDetectingRoom: Boolean,
    wizardState: WizardState,
    navigator: AppNavigator,
    adminNav: AdminNavigationState,
    onWizardStateChange: (WizardState) -> Unit,
    onSelectedRoomChange: (String?) -> Unit,
    onRoomDetectMsgChange: (String?) -> Unit,
    onAdminNavChange: (AdminNavigationState) -> Unit,
    onDetectRoom: () -> Unit,
    onSubmitWizard: (String?) -> Unit
) {

    /**
     * Der zentrale Switch der App.
     *
     * Je nach selectedMenuItem wird ein anderer Screen geladen.
     */
    when (selectedMenuItem) {

        // =========================
        // HOME SCREEN
        // =========================
        "Home" -> HomeScreen(
            selectedRoom = selectedRoom,
            message = roomDetectMsg,
            isDetecting = isDetectingRoom,

            // Startet automatische Raum-Erkennung
            onAutoScan = onDetectRoom,

            skipAutoScan = navigator.skipAutoScanHomeOnce,
            onSkipAutoScanConsumed = { navigator.skipAutoScanHomeOnce = false },

            onRetryDetect = onDetectRoom,

            // Öffnet Raum-Auswahl
            onChangeRoom = { navigator.navigateTo("RoomDetection") },

            // Startet neuen Schadens-Wizard
            onSchadenMelden = {
                onWizardStateChange(WizardState())
                navigator.navigateTo("WizardCategory")
            },

            // Raum wurde ausgewählt
            onRoomSelected = { room ->
                onSelectedRoomChange(room)
                onRoomDetectMsgChange(null)
            }
        )

        // =========================
        // WIZARD – HAUPTKATEGORIE
        // =========================
        "WizardCategory" -> HauptkategorieScreen(
            onBack = { navigator.resetTo("Home") },

            onNext = { id, label ->

                // Wizard-State zurücksetzen und neue Hauptkategorie setzen
                onWizardStateChange(
                    wizardState.copy(
                        hauptKategorieId = id,
                        hauptKategorieLabel = label,
                        kategorieId = null,
                        kategorieLabel = null,
                        hasGeraeteNummer = false,
                        unterkategorieId = null,
                        unterkategorieLabel = null,
                        zustandId = null,
                        zustandLabel = null,
                        photoUri = null,
                        geraeteNummer = null,
                        beschreibung = null
                    )
                )

                navigator.navigateTo("WizardKategorie")
            }
        )

        // =========================
        // WIZARD – KATEGORIE
        // =========================
        "WizardKategorie" -> KategorieScreen(
            hauptKategorieId = wizardState.hauptKategorieId!!,
            hauptKategorieLabel = wizardState.hauptKategorieLabel!!,

            onBack = { if (!navigator.navigateBack()) navigator.resetTo("Home") },

            onNext = { id, label, hasGeraeteNummer ->

                // Kategorie im Wizard speichern
                onWizardStateChange(
                    wizardState.copy(
                        kategorieId = id,
                        kategorieLabel = label,
                        hasGeraeteNummer = hasGeraeteNummer,
                        unterkategorieId = null,
                        unterkategorieLabel = null,
                        zustandId = null,
                        zustandLabel = null,
                        photoUri = null,
                        geraeteNummer = null,
                        beschreibung = null
                    )
                )

                navigator.navigateTo("WizardUnterkategorie")
            }
        )

        // =========================
        // WIZARD – UNTERKATEGORIE
        // =========================
        "WizardUnterkategorie" -> UnterkategorieScreen(
            hauptKategorieLabel = wizardState.hauptKategorieLabel!!,
            kategorieId = wizardState.kategorieId!!,
            kategorieLabel = wizardState.kategorieLabel!!,

            onBack = { if (!navigator.navigateBack()) navigator.resetTo("Home") },

            onNext = { id, label, hasZustand ->

                onWizardStateChange(
                    wizardState.copy(
                        unterkategorieId = id,
                        unterkategorieLabel = label,
                        zustandId = null,
                        zustandLabel = null,
                        photoUri = null,
                        geraeteNummer = null,
                        beschreibung = null
                    )
                )

                // Falls Zustandsauswahl erforderlich ist
                if (hasZustand) {
                    navigator.navigateTo("WizardState")
                } else {
                    navigator.navigateTo("WizardSummary")
                }
            }
        )

        // =========================
        // WIZARD – ZUSTAND
        // =========================
        "WizardState" -> ZustandScreen(
            wizardState = wizardState,

            onBack = { if (!navigator.navigateBack()) navigator.resetTo("Home") },

            onSelect = { id, label ->

                onWizardStateChange(
                    wizardState.copy(
                        zustandId = id,
                        zustandLabel = label,
                        photoUri = null,
                        geraeteNummer = null,
                        beschreibung = null
                    )
                )

                navigator.navigateTo("WizardSummary")
            }
        )

        // =========================
        // WIZARD – ZUSAMMENFASSUNG
        // =========================
        "WizardSummary" -> SummaryScreen(
            roomId = selectedRoom ?: "UNKNOWN",
            wizardState = wizardState,

            onBack = { if (!navigator.navigateBack()) navigator.resetTo("Home") },

            onUpdate = onWizardStateChange,

            onSubmit = onSubmitWizard
        )

        // Erfolg nach Absenden
        "WizardSuccess" -> SuccessScreen(
            onBackToHome = { navigator.resetTo("Home") }
        )

        // Liste eigener Meldungen
        "WizardReports" -> WizardReportsListScreen(
            onCreateNewReport = { navigator.navigateTo("Home") }
        )

        // Meldungen für zuständige Personen
        "ResponsibleReports" -> ResponsibleReportsScreen()

        // =========================
        // ADMIN SCREENS
        // =========================
        "AdminSettings" -> AdminSettingsScreen()

        "AdminRooms" -> AdminRoomsScreen()

        // Hauptkategorien verwalten
        "AdminCategories" -> AdminCategoryScreen(
            onOpenChildren = { parentId, parentLabel, parentIconKey ->

                onAdminNavChange(
                    adminNav.copy(
                        parentId = parentId,
                        parentLabel = parentLabel,
                        parentIconKey = parentIconKey
                    )
                )

                navigator.navigateTo("AdminLevel2")
            }
        )

        // Kategorie Level 2
        "AdminLevel2" -> {

            val parentId = adminNav.parentId

            if (parentId == null) {
                navigator.resetTo("AdminCategories")
            } else {

                AdminSubCategoryScreen(
                    parentId = parentId,
                    parentLabel = adminNav.parentLabel,
                    parentIconKey = adminNav.parentIconKey,

                    onOpenChildren = { childId, childLabel, childIconKey ->

                        onAdminNavChange(
                            adminNav.copy(
                                level2ParentId = parentId,
                                level2ParentLabel = adminNav.parentLabel,
                                level2ParentIconKey = adminNav.parentIconKey,
                                parentId = childId,
                                parentLabel = childLabel,
                                parentIconKey = childIconKey
                            )
                        )

                        navigator.navigateTo("AdminLevel3")
                    },

                    isLastLevel = false
                )
            }
        }

        // Kategorie Level 3
        "AdminLevel3" -> {

            val parentId = adminNav.parentId

            if (parentId == null) {
                navigator.resetTo("AdminCategories")
            } else {

                AdminSubCategoryScreen(
                    parentId = parentId,
                    parentLabel = adminNav.parentLabel,
                    parentIconKey = adminNav.parentIconKey,
                    onOpenChildren = { _, _, _ -> },
                    isLastLevel = true
                )
            }
        }

        // Adminverwaltung
        "AdminManagement" -> AdminManagementScreen()

        // Zuständigkeiten
        "AdminResponsibilities" -> AdminResponsibilitiesScreen(
            onOpenAdminCategories = { navigator.resetTo("AdminCategories") }
        )

        // =========================
        // RAUM ERKENNUNG
        // =========================
        "RoomDetection" -> RoomDetectionScreen(
            onRoomSelected = { room ->

                onSelectedRoomChange(room)
                onRoomDetectMsgChange(null)

                navigator.skipAutoScanHomeOnce = true

                navigator.resetTo("Home")
            }
        )
    }
}