import re

with open("/app/applet/app/src/main/java/com/yash/shortw/OverlayService.kt", "r") as f:
    content = f.read()

# Let's find the SidebarComponent and replace the onAppClick lambda
start_str = "onAppClick = {"
end_str = "AppState.sidebarVisible = false"

start_idx = content.find(start_str)
end_idx = content.find(end_str, start_idx)

new_lambda = """onAppClick = { appInfo, isFloating ->
                            if (isFloating) {
                                AppState.windows.add(
                                    FloatingWindowData(
                                        id = AppState.windowIdCounter++,
                                        title = appInfo.name,
                                        icon = null,
                                        appIcon = appInfo.icon,
                                        offset = androidx.compose.ui.unit.IntOffset(100, 200 + (AppState.windowIdCounter * 50)),
                                        size = androidx.compose.ui.unit.IntSize(800, 1000),
                                        color = androidx.compose.ui.graphics.Color(0xFF00E5FF),
                                        packageName = appInfo.packageName
                                    )
                                )
                            } else {
                                val launchIntent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
                                if (launchIntent != null) {
                                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                                    startActivity(launchIntent)
                                } else {
                                    android.widget.Toast.makeText(this@OverlayService, "Cannot launch this app", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                            """

content = content[:start_idx] + new_lambda + content[end_idx:]

with open("/app/applet/app/src/main/java/com/yash/shortw/OverlayService.kt", "w") as f:
    f.write(content)

