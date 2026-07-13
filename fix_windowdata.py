import re

with open("/app/applet/app/src/main/java/com/yash/shortw/MainActivity.kt", "r") as f:
    content = f.read()

content = content.replace(
    'FloatingWindowData(AppState.windowIdCounter++, "Macro & Auto-Clicker", Icons.Rounded.TouchApp, null, androidx.compose.ui.unit.IntOffset(100, 200), androidx.compose.ui.unit.IntSize(600, 400), NeonPurple, null)',
    'FloatingWindowData(id = AppState.windowIdCounter++, title = "Macro & Auto-Clicker", icon = Icons.Rounded.TouchApp, offset = androidx.compose.ui.unit.IntOffset(100, 200), size = androidx.compose.ui.unit.IntSize(600, 400), color = NeonPurple)'
)

content = content.replace(
    'FloatingWindowData(AppState.windowIdCounter++, "Live Screen OCR", Icons.Rounded.TextSnippet, null, androidx.compose.ui.unit.IntOffset(150, 250), androidx.compose.ui.unit.IntSize(700, 500), ElectricBlue, null)',
    'FloatingWindowData(id = AppState.windowIdCounter++, title = "Live Screen OCR", icon = Icons.Rounded.TextSnippet, offset = androidx.compose.ui.unit.IntOffset(150, 250), size = androidx.compose.ui.unit.IntSize(700, 500), color = ElectricBlue)'
)

content = content.replace(
    'FloatingWindowData(AppState.windowIdCounter++, "Universal Clipboard", Icons.Rounded.ContentPaste, null, androidx.compose.ui.unit.IntOffset(50, 100), androidx.compose.ui.unit.IntSize(800, 900), HyperPink, null)',
    'FloatingWindowData(id = AppState.windowIdCounter++, title = "Universal Clipboard", icon = Icons.Rounded.ContentPaste, offset = androidx.compose.ui.unit.IntOffset(50, 100), size = androidx.compose.ui.unit.IntSize(800, 900), color = HyperPink)'
)

with open("/app/applet/app/src/main/java/com/yash/shortw/MainActivity.kt", "w") as f:
    f.write(content)

