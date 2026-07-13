import re

with open("/app/applet/app/src/main/java/com/yash/shortw/MainActivity.kt", "r") as f:
    content = f.read()

content = content.replace("fun TriggerComponent() {", "fun TriggerComponent(isRight: Boolean = AppState.settings.edgePosition != 0) {")
content = content.replace("AppState.settings.edgePosition != 0, AppState.settings.triggerAction", "isRight, AppState.settings.triggerAction")
content = content.replace("AppState.settings.edgePosition != 0, AppState.settings.triggerMode", "isRight, AppState.settings.triggerMode")
content = content.replace("if (AppState.settings.edgePosition != 0) -with(density)", "if (isRight) -with(density)")

with open("/app/applet/app/src/main/java/com/yash/shortw/MainActivity.kt", "w") as f:
    f.write(content)

