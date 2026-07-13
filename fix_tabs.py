import re

with open("/app/applet/app/src/main/java/com/yash/shortw/MainActivity.kt", "r") as f:
    content = f.read()

content = content.replace("androidx.compose.material3.TabRowDefaults.tabIndicatorOffset(currentTab, tabPositions)", "androidx.compose.material3.TabRowDefaults.tabIndicatorOffset(tabPositions[currentTab])")

with open("/app/applet/app/src/main/java/com/yash/shortw/MainActivity.kt", "w") as f:
    f.write(content)

