import re

with open("/app/applet/app/src/main/java/com/yash/shortw/MainActivity.kt", "r") as f:
    content = f.read()

content = content.replace("var hudVisible by mutableStateOf(false)", "var hudVisible by mutableStateOf(false)\n    var isDraggingBubble by mutableStateOf(false)")

with open("/app/applet/app/src/main/java/com/yash/shortw/MainActivity.kt", "w") as f:
    f.write(content)
