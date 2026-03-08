package com.example.ui.screens.wifi

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Screen der angezeigt wird,
 * wenn das aktuell verbundene WLAN
 * nicht erlaubt ist.
 *
 * Der Benutzer muss sich mit einem
 * erlaubten WLAN verbinden (z.B. eduroam).
 */
@Composable
fun WrongWifiScreen(
    detectedSsid: String?,              // aktuell erkanntes WLAN
    hasLocationPermission: Boolean,     // ob Standortberechtigung vorhanden ist
    isRetrying: Boolean,                // ob gerade erneut geprüft wird
    onRetry: () -> Unit,                // Aktion: erneut prüfen
    onOpenWifiSettings: () -> Unit,     // Aktion: WLAN Einstellungen öffnen
    onRequestLocationPermission: (() -> Unit)? = null, // Aktion: Standortberechtigung anfordern
) {

    // Hintergrundfarbe der Seite
    val pageBg = Color.White

    // Primärfarbe der App
    val primaryGreen = Color(0xFF0F8B7A)

    // Blauton für Buttons
    val logoBlue = Color(0xFF1E3A8A)

    // Textfarben
    val textDark = Color(0xFF0F172A)
    val textMid = Color(0xFF334155)
    val hintLight = Color(0xFF94A3B8)

    // Liste der erlaubten WLANs
    val requiredSsids = listOf("eduroam", "HSBO_Gast")

    // Aktuell verbundenes WLAN bereinigen (Anführungszeichen entfernen)
    val cleanSsid = detectedSsid?.replace("\"", "") ?: "unbekannt"

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = pageBg
    ) {

        /**
         * Scrollbare Seite
         * mit vertikal zentriertem Inhalt
         */
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {

            item { Spacer(Modifier.height(10.dp)) }

            /**
             * WLAN Aus Icon
             */
            item {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = textDark,
                    modifier = Modifier.size(56.dp)
                )
            }

            item { Spacer(Modifier.height(14.dp)) }

            /**
             * Titel
             */
            item {
                Text(
                    text = "WLAN nicht zugelassen",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = textDark
                )
            }

            item { Spacer(Modifier.height(18.dp)) }

            /**
             * Hauptkarte mit Informationen
             */
            item {

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(0.92f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        /**
                         * Erklärungstext
                         */
                        Text(
                            text = "Bitte verbinden Sie sich mit einem der folgenden WLANs:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textMid,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(14.dp))

                        /**
                         * Liste erlaubter WLANs
                         */
                        requiredSsids.forEach { ssid ->

                            WifiPill(ssid)

                            Spacer(Modifier.height(12.dp))
                        }

                        Spacer(Modifier.height(8.dp))

                        /**
                         * Anzeige des aktuell verbundenen WLANs
                         */
                        Surface(
                            color = Color(0xFFEAF2FF),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(0.75f)
                        ) {

                            Text(
                                text = buildAnnotatedString {

                                    append("Aktuell verbunden: ")

                                    withStyle(
                                        style = SpanStyle(
                                            fontStyle = FontStyle.Italic,
                                            fontWeight = FontWeight.Medium
                                        )
                                    ) {
                                        append(cleanSsid)
                                    }
                                },

                                color = Color(0xFF1E3A8A),

                                fontSize = 13.sp,

                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 6.dp),

                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        /**
                         * Hinweistext
                         */
                        Text(
                            text = "Hinweis:",
                            style = MaterialTheme.typography.bodySmall,
                            color = hintLight,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(Modifier.height(2.dp))

                        Text(
                            text = "Weitere WLANs können vom Administrator freigeschaltet werden.",
                            style = MaterialTheme.typography.bodySmall,
                            color = hintLight,
                            textAlign = TextAlign.Center
                        )

                        /**
                         * Anzeige wenn Standortberechtigung fehlt
                         */
                        if (!hasLocationPermission) {

                            Spacer(Modifier.height(14.dp))

                            /**
                             * Info Box
                             * erklärt warum Standortberechtigung benötigt wird
                             */
                            Surface(
                                color = Color(0xFFFFF7E6),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {

                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {

                                    Text(
                                        text = "Standortberechtigung erforderlich",
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF92400E),
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(Modifier.height(4.dp))

                                    Text(
                                        text =
                                            "Um das aktuell verbundene WLAN zu erkennen, benötigt die App Zugriff auf den Standort. " +
                                                    "Die Standortdaten werden ausschließlich zur WLAN-Überprüfung verwendet.",

                                        color = Color(0xFF92400E),
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            /**
                             * Button um Standortberechtigung anzufordern
                             */
                            AssistChip(
                                onClick = { onRequestLocationPermission?.invoke() },

                                label = {
                                    Text(
                                        "Standortberechtigung anfordern",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },

                                leadingIcon = {

                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },

                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    labelColor = MaterialTheme.colorScheme.error
                                ),

                                border = null
                            )

                            Spacer(Modifier.height(6.dp))
                        }

                        Spacer(Modifier.height(18.dp))

                        /**
                         * Button zum erneuten Prüfen des WLANs
                         */
                        Button(
                            onClick = onRetry,

                            enabled = !isRetrying,

                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),

                            shape = RoundedCornerShape(12.dp),

                            colors = ButtonDefaults.buttonColors(
                                containerColor = logoBlue,
                                contentColor = Color.White,
                                disabledContainerColor = logoBlue.copy(alpha = 0.35f),
                                disabledContentColor = Color.White.copy(alpha = 0.7f)
                            )
                        ) {

                            if (isRetrying) {

                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White
                                )

                                Spacer(Modifier.width(8.dp))

                                Text("Wird geprüft…", fontWeight = FontWeight.SemiBold)

                            } else {

                                Text("Nochmal prüfen", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        /**
                         * Button um WLAN Einstellungen zu öffnen
                         */
                        Button(
                            onClick = onOpenWifiSettings,

                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),

                            shape = RoundedCornerShape(12.dp),

                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryGreen,
                                contentColor = Color.White
                            )
                        ) {

                            Text("WLAN öffnen", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(10.dp)) }
        }
    }
}

/**
 * UI Element für einen WLAN Namen
 * (Pill / Chip Darstellung)
 */
@Composable
private fun WifiPill(ssid: String) {

    val pillBg = Color(0xFFF1F5F9)
    val pillText = Color(0xFF64748B)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = pillBg,
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),

            verticalAlignment = Alignment.CenterVertically,

            horizontalArrangement = Arrangement.Center
        ) {

            Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = null,
                tint = pillText,
                modifier = Modifier.size(18.dp)
            )

            Spacer(Modifier.width(10.dp))

            Text(
                text = ssid,
                color = pillText,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            )
        }
    }
}