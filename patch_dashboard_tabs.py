import re

with open("/app/applet/app/src/main/java/com/yash/shortw/DashboardTabs.kt", "r") as f:
    content = f.read()

# Replace assignments to AppState.settings with ones that also call saveSettings
# The sliders and clickables do: `AppState.settings = AppState.settings.copy(...)`

new_content = re.sub(
    r'(AppState\.settings = AppState\.settings\.copy\([^)]+\))',
    r'\1; AppState.saveSettings(context)',
    content
)

if new_content != content:
    with open("/app/applet/app/src/main/java/com/yash/shortw/DashboardTabs.kt", "w") as f:
        f.write(new_content)
    print("Patched AppState.settings assignments")
else:
    print("No changes made")

