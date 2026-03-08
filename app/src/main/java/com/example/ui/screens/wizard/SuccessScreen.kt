package com.example.ui.screens.wizard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * SuccessScreen
 *
 * Dieser Screen wird angezeigt, nachdem der Benutzer
 * eine Schadensmeldung erfolgreich abgeschickt hat.
 *
 * Der Screen zeigt:
 * - Erfolgs-Icon
 * - Bestätigungstext
 * - Button zurück zur Startseite
 */
@Composable
fun SuccessScreen(

    // Callback um zurück zur Startseite zu navigieren
    onBackToHome: () -> Unit
) {

    // Hintergrundfarbe der Seite
    val pageBg = Color(0xFFF4F6FA)

    // Farbe für den Titeltext
    val titleColor = Color(0xFF0F172A)

    // Farbe für den Untertitel
    val subColor = Color(0xFF64748B)

    // Hauptfarbe für den Button
    val accentGreen = Color(0xFF14B8A6)

    /**
     * Hauptcontainer des Screens
     */
    Box(
        modifier = Modifier
            .fillMaxSize()

            // Hintergrundfarbe
            .background(pageBg)

            // Abstand zum Rand
            .padding(18.dp),

        // Inhalt zentrieren
        contentAlignment = Alignment.Center
    ) {

        /**
         * Hauptkarte (weiße Box)
         */
        Surface(

            modifier = Modifier.fillMaxWidth(),

            // Kartenform
            shape = RoundedCornerShape(18.dp),

            // Schatten der Karte
            shadowElevation = 10.dp,

            // Hintergrundfarbe der Karte
            color = Color.White
        ) {

            /**
             * Inhalt der Karte
             */
            Column(

                modifier = Modifier.padding(22.dp),

                horizontalAlignment = Alignment.CenterHorizontally,

                // Abstand zwischen Elementen
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                /**
                 * Erfolgs-Emoji
                 */
                Text(
                    text = "✅",
                    style = MaterialTheme.typography.displaySmall
                )

                /**
                 * Erfolgstitel
                 */
                Text(
                    text = "Schaden wurde erfolgreich eingereicht",

                    style = MaterialTheme.typography.titleLarge,

                    fontWeight = FontWeight.SemiBold,

                    color = titleColor,

                    textAlign = TextAlign.Center
                )

                /**
                 * Erklärungstext
                 */
                Text(
                    text = "Vielen Dank! Wir kümmern uns so schnell wie möglich darum.",

                    style = MaterialTheme.typography.bodyMedium,

                    color = subColor,

                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(6.dp))

                /**
                 * Button zurück zur Startseite
                 */
                Button(

                    // Aktion beim Klick
                    onClick = onBackToHome,

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),

                    shape = RoundedCornerShape(12.dp),

                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentGreen,
                        contentColor = Color.White
                    )
                ) {

                    Text("Zur Startseite")
                }
            }
        }
    }
}