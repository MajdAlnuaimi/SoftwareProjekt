package com.example.app.appComponents

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.shre2fix.R

/**
 * TopBar (obere Navigationsleiste) der App.
 *
 * Diese Composable erstellt die obere Leiste der Anwendung und enthält:
 * - links ein Menü-Icon zum Öffnen des Navigation Drawers
 * - rechts das Logo der App
 *
 * Die Leiste wird in der Hauptnavigation der App verwendet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(onMenuClick: () -> Unit) {

    // TopAppBar ist die obere Navigationsleiste nach Material Design
    TopAppBar(

        // Titelbereich (hier leer, weil nur Logo angezeigt wird)
        title = { Text("") },

        // Navigations-Icon links (Hamburger-Menü)
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menü",
                    tint = Color.Black
                )
            }
        },

        // Aktionen rechts in der TopBar
        actions = {
            Image(
                // Logo der App aus den Ressourcen laden
                painter = painterResource(id = R.drawable.appo),
                contentDescription = "Share2Fix Logo",

                // Größe und Abstand des Logos
                modifier = Modifier
                    .height(25.dp)
                    .padding(end = 12.dp),

                // Bild wird proportional angepasst
                contentScale = ContentScale.Fit
            )
        },

        // Hintergrundfarbe der TopBar
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White
        )
    )
}