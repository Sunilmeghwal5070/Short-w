import re

with open("/app/applet/app/src/main/java/com/yash/shortw/ControlManager.kt", "r") as f:
    content = f.read()

content = content.replace('"Screen Rotation" -> openSettings(context, Settings.ACTION_DISPLAY_SETTINGS)', '"Screen Rotation" -> toggleRotation(context)')

toggle_rotation = """    private fun toggleRotation(context: Context) {
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
}"""
content = content.replace("}", toggle_rotation, 1)

with open("/app/applet/app/src/main/java/com/yash/shortw/ControlManager.kt", "w") as f:
    f.write(content)

