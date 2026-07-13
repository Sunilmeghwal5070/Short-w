import re

with open("/app/applet/app/src/main/java/com/yash/shortw/DashboardTabs.kt", "r") as f:
    content = f.read()

content = content.replace("""                    Box(
                        modifier = Modifier
                            .clickable { AppState.settings = AppState.settings.copy(isRightEdge = false) }
                            .background(if (!AppState.settings.isRightEdge) NeonPurple else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) { Text("Left", color = GhostWhite) }
                    Box(
                        modifier = Modifier
                            .clickable { AppState.settings = AppState.settings.copy(isRightEdge = true) }
                            .background(if (AppState.settings.isRightEdge) NeonPurple else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) { Text("Right", color = GhostWhite) }""",
"""                    Box(
                        modifier = Modifier
                            .clickable { AppState.settings = AppState.settings.copy(edgePosition = 0) }
                            .background(if (AppState.settings.edgePosition == 0) NeonPurple else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) { Text("Left", color = GhostWhite) }
                    Box(
                        modifier = Modifier
                            .clickable { AppState.settings = AppState.settings.copy(edgePosition = 1) }
                            .background(if (AppState.settings.edgePosition == 1) NeonPurple else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) { Text("Right", color = GhostWhite) }
                    Box(
                        modifier = Modifier
                            .clickable { AppState.settings = AppState.settings.copy(edgePosition = 2) }
                            .background(if (AppState.settings.edgePosition == 2) NeonPurple else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) { Text("Both", color = GhostWhite) }""")

with open("/app/applet/app/src/main/java/com/yash/shortw/DashboardTabs.kt", "w") as f:
    f.write(content)
