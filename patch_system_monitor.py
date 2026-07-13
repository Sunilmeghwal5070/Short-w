import re

with open("/app/applet/app/src/main/java/com/yash/shortw/MainActivity.kt", "r") as f:
    content = f.read()

content = content.replace(".background(Color.Black.copy(alpha = 0.7f))", ".background(Color.DarkGray.copy(alpha = 0.95f))")

with open("/app/applet/app/src/main/java/com/yash/shortw/MainActivity.kt", "w") as f:
    f.write(content)

