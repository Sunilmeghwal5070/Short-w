import re

def fix(filepath):
    with open(filepath, "r") as f:
        content = f.read()

    content = content.replace("androidx.compose.material.icons.Icons.Rounded.Close", "androidx.compose.material.icons.Icons.Rounded.Clear")

    with open(filepath, "w") as f:
        f.write(content)

fix("/app/applet/app/src/main/java/com/yash/shortw/OverlayService.kt")
fix("/app/applet/app/src/main/java/com/yash/shortw/OverlayAccessibilityService.kt")
