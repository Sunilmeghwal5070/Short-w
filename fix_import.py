import re

def fix(filepath):
    with open(filepath, "r") as f:
        content = f.read()

    if "import androidx.compose.material.icons.Icons" not in content:
        content = content.replace("import android.view.WindowManager", "import android.view.WindowManager\nimport androidx.compose.material.icons.Icons\nimport androidx.compose.material.icons.rounded.Close")
    
    content = content.replace("androidx.compose.material.icons.Icons.Rounded.Clear", "Icons.Rounded.Close")

    with open(filepath, "w") as f:
        f.write(content)

fix("/app/applet/app/src/main/java/com/yash/shortw/OverlayService.kt")
fix("/app/applet/app/src/main/java/com/yash/shortw/OverlayAccessibilityService.kt")
