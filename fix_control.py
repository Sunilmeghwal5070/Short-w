import re

with open("/app/applet/app/src/main/java/com/yash/shortw/ControlManager.kt", "r") as f:
    content = f.read()

# Let's just rewrite the file fully to avoid any mess.
new_content = """package com.yash.shortw

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast

object ControlManager {
    fun toggleSetting(context: Context, control: String) {
        when (control) {
            "Wi-Fi" -> toggleWifi(context)
            "Bluetooth" -> toggleBluetooth(context)
            "Flashlight" -> toggleFlashlight(context)
            "Do Not Disturb" -> toggleDnd(context)
            "Screen Rotation" -> toggleRotation(context)
            "Airplane Mode" -> openSettings(context, Settings.ACTION_AIRPLANE_MODE_SETTINGS)
            "Data" -> openSettings(context, Settings.ACTION_DATA_ROAMING_SETTINGS)
            "Brightness" -> openSettings(context, Settings.ACTION_DISPLAY_SETTINGS)
            "Volume" -> openSettings(context, Settings.ACTION_SOUND_SETTINGS)
            "Location" -> openSettings(context, Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            "Hotspot" -> openSettings(context, Settings.ACTION_WIRELESS_SETTINGS)
            "Screen Record" -> Toast.makeText(context, "Screen record requires root privilege.", Toast.LENGTH_SHORT).show()
            "Dark Mode" -> openSettings(context, Settings.ACTION_DISPLAY_SETTINGS)
            "Battery Saver" -> openSettings(context, Settings.ACTION_BATTERY_SAVER_SETTINGS)
            "NFC" -> openSettings(context, Settings.ACTION_NFC_SETTINGS)
            else -> Toast.makeText(context, "$control toggled", Toast.LENGTH_SHORT).show()
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val panelIntent = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(panelIntent)
        } else {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val isEnabled = wifiManager.isWifiEnabled
            wifiManager.isWifiEnabled = !isEnabled
            Toast.makeText(context, "Wi-Fi ${if (!isEnabled) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun toggleBluetooth(context: Context) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter
        if (adapter != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, "Bluetooth permission missing", Toast.LENGTH_SHORT).show()
                return
            }
            val isEnabled = adapter.isEnabled
            if (isEnabled) {
                adapter.disable()
            } else {
                adapter.enable()
            }
            Toast.makeText(context, "Bluetooth ${if (!isEnabled) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
        }
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
}
"""

with open("/app/applet/app/src/main/java/com/yash/shortw/ControlManager.kt", "w") as f:
    f.write(new_content)

