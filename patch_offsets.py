import re

with open("/app/applet/app/src/main/java/com/yash/shortw/DashboardTabs.kt", "r") as f:
    content = f.read()

# Add Offset X and Offset Y sliders to TriggerTab

offset_sliders = """
            Column {
                Text("Offset Y: ${AppState.settings.triggerOffsetY.toInt()}dp", color = GhostWhite)
                Slider(
                    value = AppState.settings.triggerOffsetY,
                    onValueChange = { AppState.settings = AppState.settings.copy(triggerOffsetY = it); AppState.saveSettings(context) },
                    valueRange = -500f..500f,
                    colors = SliderDefaults.colors(thumbColor = NeonPurple, activeTrackColor = NeonPurple)
                )
            }
"""

if "Offset Y" not in content:
    content = content.replace('Text("Trigger Action"', offset_sliders + 'Text("Trigger Action"')

with open("/app/applet/app/src/main/java/com/yash/shortw/DashboardTabs.kt", "w") as f:
    f.write(content)

