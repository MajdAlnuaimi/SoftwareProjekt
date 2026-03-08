package com.example.ui.screens.wizard

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import coil.compose.rememberAsyncImagePainter
import com.example.data.ResponsibilityRepository
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * SummaryScreen
 *
 * Dieser Screen zeigt die finale Zusammenfassung der Meldung,
 * bevor der Schaden an das System gesendet wird.
 *
 * Funktionen:
 * - Anzeige der ausgewählten Kategorien
 * - Foto hinzufügen (Kamera oder Galerie)
 * - Gerätenummer eingeben (wenn erforderlich)
 * - Beschreibung hinzufügen
 * - Validierung der Pflichtfelder
 * - Senden der Meldung
 */
@Composable
fun SummaryScreen(
    roomId: String,                 // ID des aktuellen Raums
    wizardState: WizardState,       // aktueller Zustand des Wizards
    onBack: () -> Unit,             // zurück zum vorherigen Schritt
    onUpdate: (WizardState) -> Unit,// aktualisiert den WizardState
    onSubmit: (String?) -> Unit     // wird aufgerufen wenn Meldung gesendet wird
) {

    // Farben des Screens
    val pageBg = Color(0xFFF4F6FA)
    val titleColor = Color(0xFF0F172A)
    val subColor = Color(0xFF64748B)
    val borderColor = Color(0xFFE3E8F0)
    Color(0xFF14B8A6)
    val errorRed = Color(0xFFDC2626)

    // Zugriff auf Android Context
    val context = LocalContext.current

    /**
     * submitAttempted wird erst true wenn der Benutzer
     * versucht den Schaden abzuschicken.
     *
     * Dadurch erscheinen Validierungsfehler
     * erst nach dem ersten Klick auf "Einreichen".
     */
    var submitAttempted by remember { mutableStateOf(false) }

    /**
     * Dialog zum Auswählen zwischen:
     * - Kamera
     * - Galerie
     */
    var showPhotoPicker by remember { mutableStateOf(false) }

    // ----------------------------
    // Kamera / Galerie Logik
    // ----------------------------

    /**
     * Temporäre Bilddatei erstellen
     */
    val imageFile = remember {
        val timeStamp =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

        val storageDir =
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    /**
     * Uri für das Foto erstellen
     * (FileProvider erforderlich für Kamera)
     */
    val imageUri = remember(imageFile) {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    }

    /**
     * Launcher zum Fotografieren
     */
    val photoLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->

            // wenn Foto erfolgreich aufgenommen wurde
            if (success) {

                // URI im WizardState speichern
                onUpdate(
                    wizardState.copy(photoUri = imageUri.toString())
                )
            }
        }

    /**
     * Launcher zum Auswählen eines Bildes aus der Galerie
     */
    val galleryLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->

            if (uri != null) {

                onUpdate(
                    wizardState.copy(photoUri = uri.toString())
                )
            }
        }

    /**
     * Kamera Berechtigung anfragen
     */
    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {

                photoLauncher.launch(imageUri)

            } else {

                Toast.makeText(
                    context,
                    "Kameraberechtigung erforderlich",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    // ----------------------------
    // Validierungslogik
    // ----------------------------

    /**
     * Prüfen ob Gerätenummer benötigt wird
     */
    val needsGeraeteNummer = wizardState.hasGeraeteNummer

    /**
     * Prüfen ob Gerätenummer fehlt
     */
    val missingGeraeteNummer =
        needsGeraeteNummer &&
                wizardState.geraeteNummer.isNullOrBlank()

    /**
     * Prüfen ob Foto fehlt
     */
    val missingPhoto =
        wizardState.photoUri.isNullOrBlank()

    /**
     * Beschreibungstext
     */
    wizardState.beschreibung ?: ""

    // ----------------------------
    // Dialog Kamera / Galerie
    // ----------------------------

    if (showPhotoPicker) {

        AlertDialog(

            onDismissRequest = { showPhotoPicker = false },

            title = { Text("Foto hinzufügen") },

            text = {
                Text("Wie möchten Sie ein Foto hinzufügen?")
            },

            /**
             * Kamera Button
             */
            confirmButton = {

                TextButton(

                    onClick = {

                        showPhotoPicker = false

                        if (
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            )
                            != PackageManager.PERMISSION_GRANTED
                        ) {

                            cameraPermissionLauncher.launch(
                                Manifest.permission.CAMERA
                            )

                        } else {

                            photoLauncher.launch(imageUri)
                        }
                    }
                ) {
                    Text("Kamera")
                }
            },

            /**
             * Galerie Button
             */
            dismissButton = {

                TextButton(

                    onClick = {

                        showPhotoPicker = false

                        galleryLauncher.launch("image/*")
                    }

                ) {
                    Text("Galerie")
                }
            }
        )
    }

    // ----------------------------
    // Hauptlayout
    // ----------------------------

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
            .padding(18.dp),

        contentAlignment = Alignment.Center
    ) {

        /**
         * Hauptkarte
         */
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),

            shape = RoundedCornerShape(18.dp),

            shadowElevation = 10.dp,

            color = Color.White
        ) {

            /**
             * Scrollbarer Inhalt
             */
            Column(

                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),

                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                // ----------------------------
                // Titelbereich
                // ----------------------------

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Text(
                        text = "Zusammenfassung",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = titleColor
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "Überprüfen Sie Ihre Angaben und fügen Sie Details hinzu",
                        style = MaterialTheme.typography.bodySmall,
                        color = subColor,
                        textAlign = TextAlign.Center
                    )
                }

                // ----------------------------
                // Karte mit ausgewählten Kategorien
                // ----------------------------

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            borderColor,
                            RoundedCornerShape(12.dp)
                        ),

                    shape = RoundedCornerShape(12.dp),

                    color = Color(0xFFF8FAFC)
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {

                        Text(
                            text = "Ihre Auswahl",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = titleColor
                        )

                        SelectionRow(
                            "Hauptkategorie:",
                            wizardState.hauptKategorieLabel ?: "-"
                        )

                        SelectionRow(
                            "Kategorie:",
                            wizardState.kategorieLabel ?: "-"
                        )

                        SelectionRow(
                            "Unterkategorie:",
                            wizardState.unterkategorieLabel
                                ?: wizardState.unterkategorieId
                                ?: "-"
                        )

                        val zustandText =
                            wizardState.zustandLabel?.trim().orEmpty()

                        if (zustandText.isNotEmpty()) {

                            SelectionRow(
                                "Zustand:",
                                zustandText
                            )
                        }
                    }
                }

                // ----------------------------
                // FOTO UPLOAD
                // ----------------------------

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                    Text(
                        text = "Foto oder Screenshot hochladen *",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = titleColor
                    )

                    val photoBorderColor =
                        if (submitAttempted && missingPhoto)
                            errorRed
                        else
                            borderColor

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .border(
                                1.dp,
                                photoBorderColor,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                showPhotoPicker = true
                            },

                        shape = RoundedCornerShape(12.dp),

                        color = Color.White
                    ) {

                        val photo = wizardState.photoUri

                        if (photo.isNullOrBlank()) {

                            Column(
                                modifier = Modifier.fillMaxSize(),

                                verticalArrangement = Arrangement.Center,

                                horizontalAlignment =
                                    Alignment.CenterHorizontally
                            ) {

                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Foto",
                                    tint = subColor,
                                    modifier = Modifier.size(28.dp)
                                )

                                Spacer(Modifier.height(8.dp))

                                Text(
                                    text =
                                        "Klicken Sie, um ein Foto oder Screenshot hochzuladen",

                                    style =
                                        MaterialTheme.typography.bodySmall,

                                    color = titleColor
                                )

                                Spacer(Modifier.height(2.dp))

                                Text(
                                    text = "PNG, JPG bis zu 10MB",
                                    style =
                                        MaterialTheme.typography.bodySmall,

                                    color = subColor
                                )
                            }

                        } else {

                            Image(
                                painter =
                                    rememberAsyncImagePainter(
                                        photo.toUri()
                                    ),

                                contentDescription = "Foto",

                                modifier = Modifier.fillMaxSize(),

                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    if (!wizardState.photoUri.isNullOrBlank()) {

                        TextButton(
                            onClick = {
                                onUpdate(
                                    wizardState.copy(
                                        photoUri = null
                                    )
                                )
                            },

                            modifier =
                                Modifier.align(Alignment.End)
                        ) {

                            Text(
                                "Foto entfernen",
                                color = subColor
                            )
                        }
                    }
                }

                // ----------------------------
                // BUTTONS
                // ----------------------------

                Row(
                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    Button(
                        onClick = onBack,

                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),

                        shape = RoundedCornerShape(10.dp),

                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE9EEF6),
                            contentColor = titleColor
                        ),

                        elevation =
                            ButtonDefaults.buttonElevation(0.dp)
                    ) {

                        Text(
                            "Zurück",
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }

                    val scope = rememberCoroutineScope()

                    Button(

                        onClick = {

                            submitAttempted = true

                            if (!missingPhoto &&
                                !missingGeraeteNummer
                            ) {

                                scope.launch {

                                    val email =
                                        ResponsibilityRepository
                                            .getResponsibleEmailHierarchical(
                                                roomId = roomId,
                                                hauptKategorieId =
                                                    wizardState.hauptKategorieId,
                                                kategorieId =
                                                    wizardState.kategorieId,
                                                unterKategorieId =
                                                    wizardState.unterkategorieId
                                            )

                                    onSubmit(email)
                                }
                            }
                        },

                        modifier = Modifier
                            .weight(1.4f)
                            .height(42.dp),

                        shape = RoundedCornerShape(10.dp),

                        colors = ButtonDefaults.buttonColors(
                            containerColor =
                                MaterialTheme.colorScheme.primary,

                            contentColor = Color.White
                        )
                    ) {

                        Text("Schaden Einreichen")
                    }
                }

                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

/**
 * Zeile für Anzeige einer Auswahl
 * (Label + Wert)
 */
@Composable
private fun SelectionRow(label: String, value: String) {

    Row(
        modifier = Modifier.fillMaxWidth(),

        horizontalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF334155),
            modifier = Modifier.width(110.dp)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF0F172A)
        )
    }
}