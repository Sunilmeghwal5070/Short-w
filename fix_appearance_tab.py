import re

with open("/app/applet/app/src/main/java/com/yash/shortw/DashboardTabs.kt", "r") as f:
    content = f.read()

if "fun AppearanceTab() {\n    val context" not in content and "fun AppearanceTab() {\n    Column" in content:
    content = content.replace("fun AppearanceTab() {\n    Column", "fun AppearanceTab() {\n    val context = LocalContext.current\n    Column")

with open("/app/applet/app/src/main/java/com/yash/shortw/DashboardTabs.kt", "w") as f:
    f.write(content)

