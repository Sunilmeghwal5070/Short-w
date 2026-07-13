import re

with open("/app/applet/app/src/main/java/com/yash/shortw/MainActivity.kt", "r") as f:
    content = f.read()

content = content.replace("val isRightEdge: Boolean = true,", "val isRightEdge: Boolean = true,\n  val edgePosition: Int = 2,")

content = content.replace('Box(\n                        modifier = Modifier\n                            .clickable { AppState.settings = AppState.settings.copy(isRightEdge = false) }\n                            .background(if (!AppState.settings.isRightEdge) NeonPurple else Color.Transparent)',
'''Box(
                        modifier = Modifier
                            .clickable { AppState.settings = AppState.settings.copy(edgePosition = 0) }
                            .background(if (AppState.settings.edgePosition == 0) NeonPurple else Color.Transparent)''')

content = content.replace('Box(\n                        modifier = Modifier\n                            .clickable { AppState.settings = AppState.settings.copy(isRightEdge = true) }\n                            .background(if (AppState.settings.isRightEdge) NeonPurple else Color.Transparent)',
'''Box(
                        modifier = Modifier
                            .clickable { AppState.settings = AppState.settings.copy(edgePosition = 1) }
                            .background(if (AppState.settings.edgePosition == 1) NeonPurple else Color.Transparent)''')


with open("/app/applet/app/src/main/java/com/yash/shortw/MainActivity.kt", "w") as f:
    f.write(content)
