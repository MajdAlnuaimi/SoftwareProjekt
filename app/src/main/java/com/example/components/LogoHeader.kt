package com.example.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.shre2fix.R

/**
 * Header-Komponente mit Logo und Titel.
 *
 * Diese Composable zeigt:
 * - das Logo der Hochschule Bochum
 * - den Namen der Hochschule
 *
 * Sie wird typischerweise im oberen Bereich eines Screens
 * (z.B. Login-Screen oder Startseite) verwendet.
 */
@Composable
fun LogoHeader() {

    /**
     * Surface dient als Hintergrundcontainer.
     * Hier wird ein leicht transparenter weißer Hintergrund verwendet.
     */
    Surface(
        color = Color.White.copy(alpha = 0.2f),
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {

        /**
         * Box wird verwendet um den Inhalt horizontal zu zentrieren.
         */
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {

            /**
             * Row enthält:
             * - Logo
             * - Text "Hochschule Bochum"
             */
            Row(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                /**
                 * Logo der Hochschule aus den Ressourcen.
                 */
                Image(
                    painter = painterResource(id = R.drawable.hslogo),
                    contentDescription = "Hochschule Bochum Logo",
                    modifier = Modifier.height(56.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(Modifier.width(12.dp))

                /**
                 * Text mit dem Namen der Hochschule.
                 */
                Text(
                    text = "Hochschule Bochum",
                    color = Color(0xFFD50000),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}