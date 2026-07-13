import re

with open("/app/applet/app/src/main/java/com/yash/shortw/MainActivity.kt", "r") as f:
    content = f.read()

# simpler indicator:
old_indicator = """                            indicator = { tabPositions ->
                                androidx.compose.material3.TabRowDefaults.Indicator(
                                    modifier = androidx.compose.material3.TabRowDefaults.tabIndicatorOffset(tabPositions[currentTab]),
                                    color = ElectricBlue
                                )
                            }"""

new_indicator = """                            indicator = { tabPositions ->
                                if (currentTab < tabPositions.size) {
                                    androidx.compose.material3.TabRowDefaults.Indicator(
                                        modifier = Modifier.wrapContentSize(Alignment.BottomStart).offset(x = tabPositions[currentTab].left).width(tabPositions[currentTab].width),
                                        color = ElectricBlue
                                    )
                                }
                            }"""

content = content.replace(old_indicator, new_indicator)

with open("/app/applet/app/src/main/java/com/yash/shortw/MainActivity.kt", "w") as f:
    f.write(content)

