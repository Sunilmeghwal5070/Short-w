import re

# Fix ControlManager.kt
with open("/app/applet/app/src/main/java/com/yash/shortw/ControlManager.kt", "r") as f:
    cm_content = f.read()

cm_content = cm_content.replace('os.writeBytes("$command\n")'.replace('\\n', '\n'), 'os.writeBytes("$command\\n")')
cm_content = cm_content.replace('os.writeBytes("exit\n")'.replace('\\n', '\n'), 'os.writeBytes("exit\\n")')

with open("/app/applet/app/src/main/java/com/yash/shortw/ControlManager.kt", "w") as f:
    f.write(cm_content)

# Fix DashboardTabs.kt
with open("/app/applet/app/src/main/java/com/yash/shortw/DashboardTabs.kt", "r") as f:
    dt_content = f.read()

dt_content = dt_content.replace('AppState.settings = AppState.settings.copy(triggerColorArgb = color.toArgb(); AppState.saveSettings(context), triggerGradientColorArgb = null)', 'AppState.settings = AppState.settings.copy(triggerColorArgb = color.toArgb(), triggerGradientColorArgb = null); AppState.saveSettings(context)')
dt_content = dt_content.replace('; AppState.saveSettings(context))', ') ; AppState.saveSettings(context)')
dt_content = dt_content.replace('AppState.settings = AppState.settings.copy(sidebarDesign = 0; AppState.saveSettings(context))', 'AppState.settings = AppState.settings.copy(sidebarDesign = 0); AppState.saveSettings(context)')
dt_content = dt_content.replace('AppState.settings = AppState.settings.copy(sidebarDesign = 1; AppState.saveSettings(context))', 'AppState.settings = AppState.settings.copy(sidebarDesign = 1); AppState.saveSettings(context)')

with open("/app/applet/app/src/main/java/com/yash/shortw/DashboardTabs.kt", "w") as f:
    f.write(dt_content)

