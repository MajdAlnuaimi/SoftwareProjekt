package com.example.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Zentrale Registry für alle Icons der App.
 *
 * Diese Klasse organisiert:
 * - alle verfügbaren Icons
 * - ihre Zuordnung zu Kategorien
 * - Zugriff über Schlüssel (String Keys)
 *
 * Dadurch können Kategorien, Unterkategorien oder andere UI-Elemente
 * dynamisch Icons anzeigen, ohne direkt Material-Icons zu referenzieren.
 */
object IconRegistry {

    /**
     * Icons nach Kategorien gruppiert.
     *
     * Struktur:
     * IconCategory → Liste von (Key, Icon)
     *
     * Beispiel:
     * "pc" → Computer Icon
     * "drucker" → Printer Icon
     */
    val categorizedIcons: Map<IconCategory, List<Pair<String, ImageVector>>> = mapOf(

        // Hardware-bezogene Icons
        IconCategory.Hardware to listOf(
            "pc" to Icons.Default.Computer,
            "monitor" to Icons.Default.Tv,
            "maus" to Icons.Default.Mouse,
            "tastatur" to Icons.Default.Keyboard,
            "drucker" to Icons.Default.Print,
            "verbindung" to Icons.Default.Link,
            "strom" to Icons.Default.ElectricalServices,
            "usb" to Icons.Default.Usb,
            "scanner" to Icons.Default.Scanner,
            "beamer" to Icons.Default.Videocam,
            "headset" to Icons.Default.Headset,
            "speaker" to Icons.Default.Speaker,
            "kabel" to Icons.Default.Cable,
            "akku" to Icons.Default.BatteryFull,
            "ladegeraet" to Icons.Default.Power,
            "papier" to Icons.Default.Description,
            "toner" to Icons.Default.Edit,
            "memory" to Icons.Default.Memory,
            "storage" to Icons.Default.Storage,
            "gehaeuse" to Icons.Default.Dns,
            "halterung" to Icons.Default.Build,
            "fernbedienung" to Icons.Default.SettingsRemote,
            "stumm" to Icons.AutoMirrored.Filled.VolumeMute,
            "leise" to Icons.AutoMirrored.Filled.VolumeDown,
            "laut" to Icons.AutoMirrored.Filled.VolumeUp,
            "ton_aus" to Icons.AutoMirrored.Filled.VolumeOff,
            "sonst" to Icons.Default.MoreHoriz
        ),

        // Gebäude / Infrastruktur
        IconCategory.Gebaeude to listOf(
            "licht" to Icons.Default.Lightbulb,
            "strom" to Icons.Default.ElectricalServices,
            "steckdose" to Icons.Default.PowerInput,
            "stecker" to Icons.Default.Power,
            "heizung" to Icons.Default.Thermostat,
            "wasser" to Icons.Default.Plumbing,
            "tuer" to Icons.Default.DoorFront,
            "fenster" to Icons.Default.Window,
            "schloss" to Icons.Default.Lock,
            "klemmt" to Icons.Default.Warning,
            "griff" to Icons.Default.PanTool,
            "scharnier" to Icons.Default.Settings,
            "dichtung" to Icons.Default.LinearScale,
            "glas" to Icons.Default.CropSquare,
            "rahmen" to Icons.Default.ViewAgenda,
            "schalter" to Icons.Default.ToggleOn,
            "funktion" to Icons.Default.CheckCircle,
            "sonst" to Icons.Default.MoreHoriz
        ),

        // Netzwerk / Internet
        IconCategory.Internet to listOf(
            "wifi" to Icons.Default.Wifi,
            "abbrueche" to Icons.Default.SignalWifiOff,
            "reichweite" to Icons.Default.NetworkWifi,
            "verbindung" to Icons.Default.Link,
            "lan" to Icons.Default.SettingsEthernet,
            "router" to Icons.Default.Router,
            "switch" to Icons.Default.Hub,
            "speed" to Icons.Default.Speed,
            "ip" to Icons.Default.Language,
            "antenne" to Icons.Default.SettingsInputAntenna,
            "treiber" to Icons.Default.Settings,
            "drucker" to Icons.Default.Print,
            "sonst" to Icons.Default.MoreHoriz
        ),

        // Allgemeine / sonstige Icons
        IconCategory.Sonstiges to listOf(
            "warnung" to Icons.Default.Warning,
            "report" to Icons.Default.Report,
            "allgemein" to Icons.Default.Info,
            "settings" to Icons.Default.Settings,
            "add_photo" to Icons.Default.AddPhotoAlternate,
            "thema" to Icons.AutoMirrored.Filled.Label,
            "sonst" to Icons.Default.MoreHoriz
        )
    )

    /**
     * Flache Map aller Icons.
     *
     * Struktur:
     * Key → Icon
     *
     * Wird verwendet, um schnell ein Icon über seinen Key zu finden.
     */
    private val flat: Map<String, ImageVector> =
        categorizedIcons.values.flatten().associate { it.first to it.second }

    /**
     * Gibt ein Icon anhand seines Keys zurück.
     *
     * Falls kein Icon existiert → Standard-Icon (MoreHoriz).
     */
    fun iconFor(key: String): ImageVector =
        flat[key] ?: Icons.Default.MoreHoriz

    /**
     * Icons für Hauptkategorien (Header-Icons).
     *
     * Diese werden z.B. für große Kategorien im UI verwendet.
     */
    private val headerIconMap: Map<String, ImageVector> = mapOf(
        "computer" to Icons.Default.Computer,
        "business" to Icons.Default.Business,
        "wifi" to Icons.Default.Wifi,
        "build" to Icons.Default.Build,
        "language" to Icons.Default.Language,
        "info" to Icons.Default.Info,
        "warning" to Icons.Default.Warning,
        "settings" to Icons.Default.Settings,
        "report" to Icons.Default.Report,
        "print" to Icons.Default.Print,
        "phone" to Icons.Default.Phone,
        "more" to Icons.Default.MoreHoriz
    )

    /**
     * Gibt ein Header-Icon zurück.
     *
     * Falls kein spezielles Header-Icon existiert,
     * wird das normale Icon aus der Registry verwendet.
     */
    fun headerIconFor(key: String): ImageVector =
        headerIconMap[key] ?: iconFor(key)
}