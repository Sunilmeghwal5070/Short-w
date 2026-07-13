package com.yash.shortw

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import java.io.DataOutputStream

object ControlManager {
    fun toggleSetting(context: Context, control: String) {
        when (control) {
            "Wi-Fi" -> toggleWifi(context)
            "Bluetooth" -> toggleBluetooth(context)
            "Flashlight" -> toggleFlashlight(context)
            "Do Not Disturb" -> toggleDnd(context)
            "Screen Rotation" -> toggleRotation(context)
            "Airplane Mode" -> toggleAirplaneMode(context)
            "Data" -> toggleData(context)
            "Location" -> toggleLocation(context)
            "Battery Saver" -> toggleBatterySaver(context)
            "Dark Mode" -> toggleDarkMode(context)
            "NFC" -> toggleNfc(context)
            "Hotspot" -> toggleHotspot(context)
            "Screen Record" -> Toast.makeText(context, "Screen record requires system API.", Toast.LENGTH_SHORT).show()
            else -> Toast.makeText(context, "$control toggled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun runRootCommand(command: String): Boolean {
        try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("$command\n")
            os.writeBytes("exit\n")
            os.flush()
            process.waitFor()
            return process.exitValue() == 0
        } catch (e: Exception) {
            return false
        }
    }

    private fun openSettings(context: Context, action: String) {
        try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open settings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleRotation(context: Context) {
        try {
            if (Settings.System.canWrite(context)) {
                val current = Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)
                Settings.System.putInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, if (current == 1) 0 else 1)
                Toast.makeText(context, "Auto-Rotate ${if (current == 1) "Disabled" else "Enabled"}", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Toast.makeText(context, "Grant Write Settings permission first", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to toggle rotation", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun toggleWifi(context: Context) {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val isEnabled = wifiManager.isWifiEnabled
            // Try standard API first
            wifiManager.isWifiEnabled = !isEnabled
            Toast.makeText(context, "Wi-Fi ${if (!isEnabled) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            // Fallback to root or intent
            if (!runRootCommand("svc wifi ${if (isWifiOn(context)) "disable" else "enable"}")) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val panelIntent = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    context.startActivity(panelIntent)
                } else {
                    openSettings(context, Settings.ACTION_WIFI_SETTINGS)
                }
            } else {
                Toast.makeText(context, "Wi-Fi toggled (Root)", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun isWifiOn(context: Context): Boolean {
        return try { (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).isWifiEnabled } catch(e: Exception) { false }
    }

    @SuppressLint("MissingPermission")
    private fun toggleBluetooth(context: Context) {
        try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = bluetoothManager.adapter
            if (adapter != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(context, "Bluetooth permission missing", Toast.LENGTH_SHORT).show()
                    return
                }
                val isEnabled = adapter.isEnabled
                if (isEnabled) adapter.disable() else adapter.enable()
                Toast.makeText(context, "Bluetooth ${if (!isEnabled) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            // Fallback
            if (!runRootCommand("svc bluetooth ${if (isBluetoothOn(context)) "disable" else "enable"}")) {
                 openSettings(context, Settings.ACTION_BLUETOOTH_SETTINGS)
            } else {
                 Toast.makeText(context, "Bluetooth toggled (Root)", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    @SuppressLint("MissingPermission")
    private fun isBluetoothOn(context: Context): Boolean {
        return try { (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter?.isEnabled == true } catch(e: Exception) { false }
    }

    private var isFlashlightOn = false
    private fun toggleFlashlight(context: Context) {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList[0]
            isFlashlightOn = !isFlashlightOn
            cameraManager.setTorchMode(cameraId, isFlashlightOn)
        } catch (e: Exception) {
            Toast.makeText(context, "Flashlight unavailable", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleDnd(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.isNotificationPolicyAccessGranted) {
            val currentFilter = notificationManager.currentInterruptionFilter
            if (currentFilter == NotificationManager.INTERRUPTION_FILTER_ALL) {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                Toast.makeText(context, "DND Enabled", Toast.LENGTH_SHORT).show()
            } else {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                Toast.makeText(context, "DND Disabled", Toast.LENGTH_SHORT).show()
            }
        } else {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    private fun toggleData(context: Context) {
        if (!runRootCommand("svc data toggle")) {
            openSettings(context, Settings.ACTION_DATA_ROAMING_SETTINGS)
        } else {
            Toast.makeText(context, "Data Toggled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleAirplaneMode(context: Context) {
        val isAirplaneMode = Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
        if (!runRootCommand("settings put global airplane_mode_on ${if (isAirplaneMode) 0 else 1}; am broadcast -a android.intent.action.AIRPLANE_MODE --ez state ${!isAirplaneMode}")) {
            openSettings(context, Settings.ACTION_AIRPLANE_MODE_SETTINGS)
        } else {
            Toast.makeText(context, "Airplane Mode Toggled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleLocation(context: Context) {
        val mode = Settings.Secure.getInt(context.contentResolver, Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF)
        val newMode = if (mode == Settings.Secure.LOCATION_MODE_OFF) Settings.Secure.LOCATION_MODE_HIGH_ACCURACY else Settings.Secure.LOCATION_MODE_OFF
        if (!runRootCommand("settings put secure location_mode $newMode")) {
            openSettings(context, Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        } else {
            Toast.makeText(context, "Location Toggled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleBatterySaver(context: Context) {
        if (!runRootCommand("settings put global low_power 1")) { // Simplified toggle
            openSettings(context, Settings.ACTION_BATTERY_SAVER_SETTINGS)
        }
    }

    private fun toggleDarkMode(context: Context) {
        val uiManager = context.getSystemService(Context.UI_MODE_SERVICE) as android.app.UiModeManager
        val currentMode = uiManager.nightMode
        uiManager.nightMode = if (currentMode == android.app.UiModeManager.MODE_NIGHT_YES) android.app.UiModeManager.MODE_NIGHT_NO else android.app.UiModeManager.MODE_NIGHT_YES
        Toast.makeText(context, "Dark Mode Toggled", Toast.LENGTH_SHORT).show()
    }

    private fun toggleNfc(context: Context) {
        if (!runRootCommand("svc nfc enable")) { // Requires root on most devices
            openSettings(context, Settings.ACTION_NFC_SETTINGS)
        }
    }

    private fun toggleHotspot(context: Context) {
         openSettings(context, Settings.ACTION_WIRELESS_SETTINGS) // Hotspot toggle via reflection is very complex and flaky across versions
    }
}
