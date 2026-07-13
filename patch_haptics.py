import re

with open("/app/applet/app/src/main/java/com/yash/shortw/MainActivity.kt", "r") as f:
    content = f.read()

content = content.replace("""            detectDragGestures(
                onDragStart = { 
                    isDragging = true 
                },""", """            detectDragGestures(
                onDragStart = { 
                    isDragging = true 
                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                },""")

content = content.replace("""                if (AppState.settings.triggerAction == 0) {
                    val dx = if (AppState.settings.isRightEdge) -with(density) { dragAmount.x.toDp().value } else with(density) { dragAmount.x.toDp().value }
                    val dy = with(density) { dragAmount.y.toDp().value }
                    if (dx > 5 && kotlin.math.abs(dx) > kotlin.math.abs(dy) && !AppState.sidebarVisible) {
                        AppState.sidebarVisible = true
                    }
                }""", """                if (AppState.settings.triggerAction == 0) {
                    val dx = if (AppState.settings.isRightEdge) -with(density) { dragAmount.x.toDp().value } else with(density) { dragAmount.x.toDp().value }
                    val dy = with(density) { dragAmount.y.toDp().value }
                    if (dx > 5 && kotlin.math.abs(dx) > kotlin.math.abs(dy) && !AppState.sidebarVisible) {
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        AppState.sidebarVisible = true
                    }
                }""")

with open("/app/applet/app/src/main/java/com/yash/shortw/MainActivity.kt", "w") as f:
    f.write(content)

