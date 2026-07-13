import re

with open("/app/applet/app/src/main/java/com/yash/shortw/MainActivity.kt", "r") as f:
    content = f.read()

# Replace the dummy sliders with actual ones
old_volume = """                                                var volume by remember { mutableFloatStateOf(0.5f) }
                                                androidx.compose.material3.Slider(
                                                    value = volume,
                                                    onValueChange = { volume = it },
                                                    modifier = Modifier.weight(1f),
                                                    colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = NeonPurple, activeTrackColor = NeonPurple)
                                                )"""

new_volume = """                                                val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
                                                val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).toFloat()
                                                var volume by remember { mutableFloatStateOf(audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) / maxVolume) }
                                                androidx.compose.material3.Slider(
                                                    value = volume,
                                                    onValueChange = { 
                                                        volume = it
                                                        audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, (it * maxVolume).toInt(), 0)
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = NeonPurple, activeTrackColor = NeonPurple)
                                                )"""

old_brightness = """                                                var brightness by remember { mutableFloatStateOf(0.5f) }
                                                androidx.compose.material3.Slider(
                                                    value = brightness,
                                                    onValueChange = { brightness = it },
                                                    modifier = Modifier.weight(1f),
                                                    colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = Color(0xFFFFC107), activeTrackColor = Color(0xFFFFC107))
                                                )"""

new_brightness = """                                                var brightness by remember { 
                                                    mutableFloatStateOf(
                                                        try {
                                                            android.provider.Settings.System.getInt(context.contentResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS) / 255f
                                                        } catch(e: Exception) { 0.5f }
                                                    ) 
                                                }
                                                androidx.compose.material3.Slider(
                                                    value = brightness,
                                                    onValueChange = { 
                                                        brightness = it
                                                        try {
                                                            if (android.provider.Settings.System.canWrite(context)) {
                                                                android.provider.Settings.System.putInt(context.contentResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE, android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                                                                android.provider.Settings.System.putInt(context.contentResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS, (it * 255).toInt())
                                                            } else {
                                                                val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
                                                                context.startActivity(intent)
                                                                android.widget.Toast.makeText(context, "Grant Write Settings permission", android.widget.Toast.LENGTH_SHORT).show()
                                                            }
                                                        } catch(e: Exception) {}
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = Color(0xFFFFC107), activeTrackColor = Color(0xFFFFC107))
                                                )"""

content = content.replace(old_volume, new_volume)
content = content.replace(old_brightness, new_brightness)

with open("/app/applet/app/src/main/java/com/yash/shortw/MainActivity.kt", "w") as f:
    f.write(content)

