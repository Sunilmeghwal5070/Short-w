import re

with open("/app/applet/app/src/main/java/com/yash/shortw/MainActivity.kt", "r") as f:
    content = f.read()

content = content.replace("var hudVisible by mutableStateOf(false)", "var hudVisible by mutableStateOf(false)\n    var lastTriggeredSide by mutableStateOf(1)")

# Update TriggerComponent to set lastTriggeredSide
trigger_code = """                    if (dx > 5 && kotlin.math.abs(dx) > kotlin.math.abs(dy) && !AppState.sidebarVisible) {
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        AppState.sidebarVisible = true
                    }"""
                    
new_trigger_code = """                    if (dx > 5 && kotlin.math.abs(dx) > kotlin.math.abs(dy) && !AppState.sidebarVisible) {
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        AppState.lastTriggeredSide = if (isRight) 1 else 0
                        AppState.sidebarVisible = true
                    }"""

content = content.replace(trigger_code, new_trigger_code)

# Double tap / Single tap triggers
content = content.replace("onDoubleTap = { if (AppState.settings.triggerAction == 2) { haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress); AppState.sidebarVisible = true } },",
                          "onDoubleTap = { if (AppState.settings.triggerAction == 2) { haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress); AppState.lastTriggeredSide = if (isRight) 1 else 0; AppState.sidebarVisible = true } },")

content = content.replace("onTap = { if (AppState.settings.triggerAction == 1) { haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress); AppState.sidebarVisible = true } }",
                          "onTap = { if (AppState.settings.triggerAction == 1) { haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress); AppState.lastTriggeredSide = if (isRight) 1 else 0; AppState.sidebarVisible = true } }")

# Update SidebarComponent to use lastTriggeredSide
# But we need to define isRightEdge effectively: val isRightPanel = AppState.lastTriggeredSide == 1
# Actually `AppState.settings.edgePosition != 0` logic was used to mean "is right side".
content = content.replace("AppState.settings.edgePosition != 0", "AppState.lastTriggeredSide == 1")
content = content.replace("AppState.settings.edgePosition == 0", "AppState.lastTriggeredSide == 0")

with open("/app/applet/app/src/main/java/com/yash/shortw/MainActivity.kt", "w") as f:
    f.write(content)
