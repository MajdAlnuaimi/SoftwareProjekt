package com.example.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shre2fix.R


/**
 * LoginScreen
 *
 * Dieser Screen ist der Einstiegspunkt der App.
 * Hier meldet sich der Benutzer über Google an.
 *
 * Parameter:
 * onLogin -> Callback-Funktion, die aufgerufen wird,
 * wenn der Benutzer auf den Login-Button klickt.
 * Die eigentliche Google-Authentifizierung passiert
 * außerhalb dieses Screens (z.B. in MainActivity).
 */
@Composable
fun LoginScreen(onLogin: () -> Unit) {

    /**
     * Box wird als Hauptcontainer verwendet.
     * Dadurch können wir Hintergrundbild, Overlay
     * und Inhalt übereinander legen.
     */
    Box(modifier = Modifier.fillMaxSize()) {

        /**
         * Hintergrundbild des Login-Screens.
         *
         * Wird über die gesamte Bildschirmfläche
         * angezeigt.
         */
        Image(
            painter = painterResource(id = R.drawable.login_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        /**
         * Dunkles Overlay über dem Hintergrundbild.
         *
         * Dadurch wird der Text besser lesbar.
         */
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
        )

        /**
         * Hauptinhalt des Screens.
         *
         * Enthält:
         * - Logo
         * - Titel
         * - Beschreibung
         * - Login Button
         */
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),

            horizontalAlignment = Alignment.CenterHorizontally,

            verticalArrangement = Arrangement.Center
        ) {

            /**
             * App Logo (HS Bochum Logo)
             */
            Image(
                painter = painterResource(id = R.drawable.hslogo),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(120.dp)
                    .padding(bottom = 16.dp),

                contentScale = ContentScale.Fit
            )

            /**
             * App Titel
             */
            Text(
                "Share2Fix",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            /**
             * Kurze Beschreibung der App.
             */
            Text(
                "Melde Defekte – schnell & einfach",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f),

                modifier = Modifier.padding(bottom = 48.dp)
            )

            /**
             * Login Button.
             *
             * Startet die Google-Anmeldung.
             */
            Button(
                onClick = onLogin,

                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),

                shape = RoundedCornerShape(50),

                elevation = ButtonDefaults.buttonElevation(8.dp),

                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp)
            ) {

                /**
                 * Google Icon im Button.
                 */
                Icon(
                    painter = painterResource(id = R.drawable.ic_google),
                    contentDescription = null,
                    tint = Color.Unspecified,

                    modifier = Modifier.size(24.dp)
                )

                Spacer(Modifier.width(12.dp))

                /**
                 * Text im Login Button.
                 */
                Text(
                    "Mit Google anmelden",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}