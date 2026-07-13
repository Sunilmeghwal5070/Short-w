import re

with open("/app/applet/app/src/main/java/com/yash/shortw/OverlayService.kt", "r") as f:
    content = f.read()

# We need a new composeView for dismiss
# AppState needs a global dragging state
