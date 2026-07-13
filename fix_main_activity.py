import re

with open("/app/applet/app/src/main/java/com/yash/shortw/MainActivity.kt", "r") as f:
    content = f.read()

# Fix ControlIcon
if "fun ControlIcon" not in content:
    control_icon_code = """
@Composable
fun ControlIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }.width(64.dp)) {
        Box(modifier = Modifier.size(48.dp).background(SurfaceDark, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, tint = GhostWhite)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = GhostWhite, fontSize = 10.sp, maxLines = 1, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
"""
    content = content + "\n" + control_icon_code

# Fix tabIndicatorOffset
content = content.replace(
    'androidx.compose.material3.TabRowDefaults.Indicator(\n                                    modifier = androidx.compose.material3.TabRowDefaults.tabIndicatorOffset(tabPositions[currentTab]),\n                                    color = ElectricBlue\n                                )',
    'androidx.compose.material3.TabRowDefaults.Indicator(\n                                    modifier = androidx.compose.material3.TabRowDefaults.tabIndicatorOffset(currentTab, tabPositions),\n                                    color = ElectricBlue\n                                )'
)

# Actually, tabIndicatorOffset is an extension function: `Modifier.tabIndicatorOffset(currentTabPosition)`.
# We need to import it or use it correctly.
content = content.replace('modifier = androidx.compose.material3.TabRowDefaults.tabIndicatorOffset(tabPositions[currentTab])', 'modifier = Modifier.padding(horizontal = 16.dp)') # Just fallback to simple or no modifier if it's too complex.
# A better fallback for Indicator in TabRow:
content = content.replace(
    """                            indicator = { tabPositions ->
                                androidx.compose.material3.TabRowDefaults.Indicator(
                                    modifier = androidx.compose.material3.TabRowDefaults.tabIndicatorOffset(tabPositions[currentTab]),
                                    color = ElectricBlue
                                )
                            }""",
    """                            indicator = { tabPositions ->
                                if (currentTab < tabPositions.size) {
                                    androidx.compose.material3.TabRowDefaults.Indicator(
                                        modifier = Modifier.wrapContentSize(Alignment.BottomStart).offset(x = tabPositions[currentTab].left).width(tabPositions[currentTab].width),
                                        color = ElectricBlue
                                    )
                                }
                            }"""
)

with open("/app/applet/app/src/main/java/com/yash/shortw/MainActivity.kt", "w") as f:
    f.write(content)

