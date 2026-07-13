import re

with open("/app/applet/app/src/main/java/com/yash/shortw/DashboardTabs.kt", "r") as f:
    content = f.read()

# Make sure TriggerTab has context
if "fun TriggerTab() {\n    val context" not in content and "fun TriggerTab() {\n    Column" in content:
    content = content.replace("fun TriggerTab() {\n    Column", "fun TriggerTab() {\n    val context = LocalContext.current\n    Column")

with open("/app/applet/app/src/main/java/com/yash/shortw/DashboardTabs.kt", "w") as f:
    f.write(content)

