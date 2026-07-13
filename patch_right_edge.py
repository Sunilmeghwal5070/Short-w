import re

with open("/app/applet/app/src/main/java/com/yash/shortw/MainActivity.kt", "r") as f:
    content = f.read()

# Replace if (AppState.settings.isRightEdge) with if (AppState.settings.edgePosition != 0) where appropriate
# or edgePosition == 1

content = content.replace("if (AppState.settings.isRightEdge)", "if (AppState.settings.edgePosition != 0)")
content = content.replace("if (!AppState.settings.isRightEdge)", "if (AppState.settings.edgePosition == 0)")
content = content.replace("AppState.settings.isRightEdge", "AppState.settings.edgePosition != 0")

with open("/app/applet/app/src/main/java/com/yash/shortw/MainActivity.kt", "w") as f:
    f.write(content)
