package com.example.scan

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Klasse zum Durchführen von WLAN-Scans.
 *
 * Aufgaben:
 * - einzelne WLAN-Scans durchführen
 * - mehrere Scans hintereinander durchführen
 * - Ergebnisse stabil sammeln
 *
 * Wird verwendet von:
 * - RoomDetection
 * - WifiFingerprintRepository
 */
object WifiScanner {

    /**
     * Führt einen einzelnen WLAN-Scan durch.
     *
     * Je nach Android-Version wird eine unterschiedliche Methode verwendet.
     *
     * Parameter:
     * timeoutMs → maximale Wartezeit für neue Scan-Ergebnisse (API 30+)
     * settleDelayMs → Wartezeit bis Ergebnisse stabil sind (API 29)
     */
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    suspend fun oneShotSnapshot(
        context: Context,
        timeoutMs: Long = 12_000L,
        settleDelayMs: Long = 1_200L
    ): List<ScanResult> {

        // Prüfen ob Standort-Berechtigung vorhanden ist
        ensureFineLocation(context)

        // Prüfen ob Standortdienste aktiviert sind
        ensureLocationServices(context)

        val wm = context.applicationContext.getSystemService(WifiManager::class.java)
            ?: return emptyList()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            // Android 11+
            getSnapshotApi30Plus(wm, timeoutMs)

        } else {

            // Android 10
            getSnapshotApi29(wm, settleDelayMs)
        }
    }

    /**
     * Führt mehrere WLAN-Scans nacheinander durch.
     *
     * Wird verwendet um stabilere Ergebnisse zu erhalten.
     */
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    suspend fun multiScan(
        context: Context,
        nTimes: Int = 5,
        delayBetweenMs: Long = 1_000L,
        timeoutMs: Long = 12_000L,
        settleDelayMs: Long = 1_200L
    ): List<List<ScanResult>> {

        val out = ArrayList<List<ScanResult>>(nTimes)

        repeat(nTimes) { i ->

            // einzelner Scan
            out += oneShotSnapshot(context, timeoutMs, settleDelayMs)

            // Pause zwischen den Scans
            if (i < nTimes - 1) delay(delayBetweenMs)
        }

        return out
    }

    /**
     * WLAN-Scan für Android 11+ (API 30+).
     *
     * Nutzt ScanResultsCallback um neue Scan-Ergebnisse zu erhalten.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("MissingPermission")
    private suspend fun getSnapshotApi30Plus(
        wm: WifiManager,
        timeoutMs: Long
    ): List<ScanResult> = withContext(Dispatchers.IO) {

        // vorhandene Ergebnisse sofort verwenden
        scanResultsSafe(wm).takeIf { it.isNotEmpty() }?.let {
            return@withContext it
        }

        suspendCancellableCoroutine { cont ->

            val executor = java.util.concurrent.Executors.newSingleThreadExecutor()

            val cb = object : WifiManager.ScanResultsCallback() {

                override fun onScanResultsAvailable() {

                    val results = scanResultsSafe(wm)

                    if (!cont.isCompleted) cont.resume(results)
                }
            }

            try {

                wm.registerScanResultsCallback(executor, cb)

            } catch (_: SecurityException) {

                if (!cont.isCompleted) cont.resume(scanResultsSafe(wm))
            }

            // Timeout falls kein Callback kommt
            val timeoutThread = Thread {

                try {
                    Thread.sleep(timeoutMs)
                } catch (_: InterruptedException) {}

                if (!cont.isCompleted) cont.resume(scanResultsSafe(wm))
            }.apply { start() }

            cont.invokeOnCancellation {

                try {
                    wm.unregisterScanResultsCallback(cb)
                } catch (_: Exception) {}

                executor.shutdown()

                timeoutThread.interrupt()
            }
        }
    }

    /**
     * WLAN-Scan für Android 10 (API 29).
     */
    @SuppressLint("MissingPermission", "Deprecation")
    private suspend fun getSnapshotApi29(
        wm: WifiManager,
        settleDelayMs: Long
    ): List<ScanResult> = withContext(Dispatchers.IO) {

        try {
            wm.startScan()
        } catch (_: SecurityException) {}

        // warten bis Ergebnisse bereit sind
        delay(settleDelayMs)

        scanResultsSafe(wm)
    }

    /**
     * Sichere Abfrage der Scan-Ergebnisse.
     */
    @SuppressLint("MissingPermission")
    private fun scanResultsSafe(wm: WifiManager): List<ScanResult> = try {

        wm.scanResults ?: emptyList()

    } catch (_: SecurityException) {

        emptyList()
    }

    /**
     * Prüft ob die Standort-Berechtigung vorhanden ist.
     */
    private fun hasFineLocation(context: Context) =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * Erzwingt Standort-Berechtigung.
     */
    private fun ensureFineLocation(context: Context) {

        if (!hasFineLocation(context)) {

            throw IllegalStateException(
                "Berechtigung ACCESS_FINE_LOCATION fehlt."
            )
        }
    }

    /**
     * Prüft ob Standortdienste aktiviert sind.
     */
    private fun isLocationEnabled(context: Context): Boolean {

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false

        return try {

            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        } catch (_: Exception) {
            false
        }
    }

    /**
     * Erzwingt aktive Standortdienste.
     */
    private fun ensureLocationServices(context: Context) {

        if (!isLocationEnabled(context)) {

            throw IllegalStateException(
                "Standortdienste sind deaktiviert."
            )
        }
    }
}