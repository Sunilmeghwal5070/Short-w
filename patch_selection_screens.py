import re

with open("/app/applet/app/src/main/java/com/yash/shortw/MainActivity.kt", "r") as f:
    content = f.read()

# For AppSelectionScreen
old_app_click = """                                .clickable {
                                    if (isSelected) {
                                        currentList.remove(app.packageName)
                                    } else if (currentList.size < limit) {
                                        currentList.add(app.packageName)
                                    } else {
                                        // Show toast or something
                                    }
                                }"""

new_app_click = """                                .clickable {
                                    if (isSelected) {
                                        currentList.remove(app.packageName)
                                        AppState.saveSettings(context)
                                    } else if (currentList.size < limit) {
                                        currentList.add(app.packageName)
                                        AppState.saveSettings(context)
                                    } else {
                                        android.widget.Toast.makeText(context, "Limit reached", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }"""

content = content.replace(old_app_click, new_app_click)
# also add val context = LocalContext.current to AppSelectionScreen
if "val context = LocalContext.current" not in content.split("fun AppSelectionScreen")[1][:100]:
    content = content.replace("fun AppSelectionScreen(onBack: () -> Unit) {", "fun AppSelectionScreen(onBack: () -> Unit) {\n    val context = LocalContext.current")


# For ItemSelectionScreen
old_item_click = """                        .clickable {
                            if (isSelected) {
                                selectedItems.remove(item)
                            } else {
                                if (selectedItems.size < maxSelection) {
                                    selectedItems.add(item)
                                } else {
                                    android.widget.Toast.makeText(context, "Maximum limit reached", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }"""

new_item_click = """                        .clickable {
                            if (isSelected) {
                                selectedItems.remove(item)
                                AppState.saveSettings(context)
                            } else {
                                if (selectedItems.size < maxSelection) {
                                    selectedItems.add(item)
                                    AppState.saveSettings(context)
                                } else {
                                    android.widget.Toast.makeText(context, "Maximum limit reached", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }"""

content = content.replace(old_item_click, new_item_click)

with open("/app/applet/app/src/main/java/com/yash/shortw/MainActivity.kt", "w") as f:
    f.write(content)

