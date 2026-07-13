import re

with open("/app/applet/app/src/main/java/com/yash/shortw/MainActivity.kt", "r") as f:
    content = f.read()

start_idx = content.find("@Composable\nfun SettingsDashboard")
end_idx = content.find("@Composable\nfun LegalMenuItem")

if start_idx != -1 and end_idx != -1:
    content = content[:start_idx] + content[end_idx:]
    with open("/app/applet/app/src/main/java/com/yash/shortw/MainActivity.kt", "w") as f:
        f.write(content)
    print("Deleted old SettingsDashboard")
else:
    print("Could not find bounds")
