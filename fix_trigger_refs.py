import re

def fix_trigger(filepath):
    with open(filepath, "r") as f:
        content = f.read()

    content = content.replace("triggerView?.visibility", "triggerViewLeft?.visibility")
    content = content.replace("triggerViewLeft?.visibility = View.GONE", "triggerViewLeft?.visibility = View.GONE\n                    triggerViewRight?.visibility = View.GONE")
    content = content.replace("triggerViewLeft?.visibility = View.VISIBLE", "triggerViewLeft?.visibility = View.VISIBLE\n                    triggerViewRight?.visibility = View.VISIBLE")

    with open(filepath, "w") as f:
        f.write(content)

fix_trigger("/app/applet/app/src/main/java/com/yash/shortw/OverlayService.kt")
fix_trigger("/app/applet/app/src/main/java/com/yash/shortw/OverlayAccessibilityService.kt")

