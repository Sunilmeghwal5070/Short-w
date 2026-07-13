import re

with open("/app/applet/app/src/main/java/com/yash/shortw/MainActivity.kt", "r") as f:
    content = f.read()

old_logic = """val params = composeView.layoutParams as WindowManager.LayoutParams
                          val screenHeight = context.resources.displayMetrics.heightPixels
                          val screenWidth = context.resources.displayMetrics.widthPixels
                          if (params.y > screenHeight / 2 - 200 && params.x > -150 && params.x < 150) {
                              onClose()
                          }"""

new_logic = """val params = composeView.layoutParams as WindowManager.LayoutParams
                          val screenHeight = context.resources.displayMetrics.heightPixels
                          val screenWidth = context.resources.displayMetrics.widthPixels
                          // The close icon is at bottom center. params.x and y are from top left.
                          // Icon size is 60dp. screenWidth/2 is center.
                          if (params.y > screenHeight - 400 && params.x > screenWidth / 2 - 200 && params.x < screenWidth / 2 + 200) {
                              onClose()
                          }"""

content = content.replace(old_logic, new_logic)

with open("/app/applet/app/src/main/java/com/yash/shortw/MainActivity.kt", "w") as f:
    f.write(content)

