import re

with open("/app/applet/app/src/main/java/com/yash/shortw/MainActivity.kt", "r") as f:
    content = f.read()

bad_code = """                    if (AppState.settings.edgePosition != 0 && dx < -50) {
                        AppState.settings = AppState.settings.copy(isRightEdge = false)
                    } else if (!AppState.settings.edgePosition != 0 && dx > 50) {
                        AppState.settings = AppState.settings.copy(isRightEdge = true)
                    }"""

fixed_code = """                    if (isRight && dx < -50) {
                        AppState.settings = AppState.settings.copy(edgePosition = 0)
                    } else if (!isRight && dx > 50) {
                        AppState.settings = AppState.settings.copy(edgePosition = 1)
                    }"""

content = content.replace(bad_code, fixed_code)

with open("/app/applet/app/src/main/java/com/yash/shortw/MainActivity.kt", "w") as f:
    f.write(content)

