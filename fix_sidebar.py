import re

with open("/app/applet/app/src/main/java/com/yash/shortw/MainActivity.kt", "r") as f:
    content = f.read()

start_idx = content.find("@Composable\nfun SidebarComponent")
end_idx = content.find("@Composable\nfun ToolIcon")

if start_idx != -1 and end_idx != -1:
    new_sidebar = """@Composable
fun SidebarComponent(onAppClick: (AppInfo, Boolean) -> Unit, onToggleHud: () -> Unit, isHudVisible: Boolean, onExit: () -> Unit) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf("Apps") }
    
    val topStartRadius = if (AppState.settings.isRightEdge) 32.dp else 0.dp
    val bottomStartRadius = if (AppState.settings.isRightEdge) 32.dp else 0.dp
    val topEndRadius = if (!AppState.settings.isRightEdge) 32.dp else 0.dp
    val bottomEndRadius = if (!AppState.settings.isRightEdge) 32.dp else 0.dp
    val shape = RoundedCornerShape(topStartRadius, topEndRadius, bottomEndRadius, bottomStartRadius)

    androidx.compose.animation.AnimatedVisibility(
        visible = AppState.sidebarVisible,
        enter = androidx.compose.animation.fadeIn(),
        exit = androidx.compose.animation.fadeOut()
    ) {
        Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onTap = { AppState.sidebarVisible = false }) }) {
            androidx.compose.animation.AnimatedVisibility(
                visible = AppState.sidebarVisible,
                modifier = Modifier.align(if (AppState.settings.isRightEdge) Alignment.CenterEnd else Alignment.CenterStart),
                enter = androidx.compose.animation.slideInHorizontally(animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.7f, stiffness = 400f), initialOffsetX = { if (AppState.settings.isRightEdge) it else -it }),
                exit = androidx.compose.animation.slideOutHorizontally(targetOffsetX = { if (AppState.settings.isRightEdge) it else -it })
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.85f)
                        .width(320.dp)
                        .clip(shape)
                        .background(Obsidian.copy(alpha = AppState.settings.sidebarOpacity))
                        .border(1.dp, ElectricBlue.copy(alpha = 0.3f), shape)
                        .pointerInput(Unit) { detectTapGestures { /* consume tap inside */ } }
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        // Header
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Short Panel", color = GhostWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            IconButton(onClick = onExit) {
                                Icon(Icons.Rounded.PowerSettingsNew, "Close", tint = HyperPink)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Tabs
                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SidebarTab("Apps", currentTab == "Apps") { currentTab = "Apps" }
                            SidebarTab("Tools", currentTab == "Tools") { currentTab = "Tools" }
                            SidebarTab("Controls", currentTab == "Controls") { currentTab = "Controls" }
                            SidebarTab("System", currentTab == "System") { currentTab = "System" }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Content
                        Column(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            when (currentTab) {
                                "Apps" -> {
                                    Text("Floating Window Apps", color = ElectricBlue, fontWeight = FontWeight.Bold)
                                    val floatingAppsList = AppState.installedApps.filter { AppState.floatingApps.contains(it.packageName) }
                                    if (floatingAppsList.isEmpty()) {
                                        Text("No floating apps configured. Add them in settings.", color = SoftGray, fontSize = 12.sp)
                                    } else {
                                        // Use a lazy vertical grid layout manually built with rows
                                        floatingAppsList.chunked(4).forEach { rowApps ->
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                                rowApps.forEach { app ->
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onAppClick(app, true) }) {
                                                        Box(modifier = Modifier.size(56.dp).background(SurfaceDark, RoundedCornerShape(12.dp)).padding(8.dp), contentAlignment = Alignment.Center) {
                                                            Image(bitmap = app.icon, contentDescription = app.name, modifier = Modifier.size(40.dp))
                                                        }
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(app.name, color = GhostWhite, fontSize = 10.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.width(60.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Pinned Shortcuts", color = NeonPurple, fontWeight = FontWeight.Bold)
                                    val pinnedAppsList = AppState.installedApps.filter { AppState.pinnedApps.contains(it.packageName) }
                                    if (pinnedAppsList.isEmpty()) {
                                        Text("No pinned apps. Add them in settings.", color = SoftGray, fontSize = 12.sp)
                                    } else {
                                        pinnedAppsList.chunked(4).forEach { rowApps ->
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                                rowApps.forEach { app ->
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onAppClick(app, false) }) {
                                                        Image(bitmap = app.icon, contentDescription = app.name, modifier = Modifier.size(48.dp).clip(CircleShape))
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(app.name, color = GhostWhite, fontSize = 10.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.width(60.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                "Tools" -> {
                                    if (AppState.visibleTools.contains("Macro & Auto-Clicker")) {
                                        ToolItem(Icons.Rounded.TouchApp, "Macro & Auto-Clicker", "Automate repetitive clicks", NeonPurple) {
                                            AppState.windows.add(FloatingWindowData(AppState.windowIdCounter++, "Macro & Auto-Clicker", Icons.Rounded.TouchApp, null, androidx.compose.ui.unit.IntOffset(100, 200), androidx.compose.ui.unit.IntSize(600, 400), NeonPurple, null))
                                            AppState.sidebarVisible = false
                                        }
                                    }
                                    if (AppState.visibleTools.contains("Live Screen OCR")) {
                                        ToolItem(Icons.Rounded.TextSnippet, "Live Screen OCR", "Extract text from screen", ElectricBlue) {
                                            AppState.windows.add(FloatingWindowData(AppState.windowIdCounter++, "Live Screen OCR", Icons.Rounded.TextSnippet, null, androidx.compose.ui.unit.IntOffset(150, 250), androidx.compose.ui.unit.IntSize(700, 500), ElectricBlue, null))
                                            AppState.sidebarVisible = false
                                        }
                                    }
                                    if (AppState.visibleTools.contains("Universal Clipboard")) {
                                        ToolItem(Icons.Rounded.ContentPaste, "Universal Clipboard", "Manage copied text history", HyperPink) {
                                            AppState.windows.add(FloatingWindowData(AppState.windowIdCounter++, "Universal Clipboard", Icons.Rounded.ContentPaste, null, androidx.compose.ui.unit.IntOffset(50, 100), androidx.compose.ui.unit.IntSize(800, 900), HyperPink, null))
                                            AppState.sidebarVisible = false
                                        }
                                    }
                                    if (AppState.visibleTools.contains("Floating System Monitor")) {
                                        ToolItemToggle(Icons.Rounded.Memory, "Floating System Monitor", "Live CPU/RAM overlay", Color(0xFF00FF87), isHudVisible) {
                                            onToggleHud()
                                        }
                                    }
                                }
                                "Controls" -> {
                                    AppState.visibleControls.chunked(3).forEach { rowControls ->
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                            rowControls.forEach { control ->
                                                val icon = when(control) {
                                                    "Wi-Fi" -> Icons.Rounded.Wifi
                                                    "Bluetooth" -> Icons.Rounded.Bluetooth
                                                    "Data" -> Icons.Rounded.DataUsage
                                                    "Ghost Mode" -> Icons.Rounded.VisibilityOff
                                                    "Brightness" -> Icons.Rounded.BrightnessMedium
                                                    "Volume" -> Icons.Rounded.VolumeUp
                                                    "Location" -> Icons.Rounded.LocationOn
                                                    "Airplane Mode" -> Icons.Rounded.AirplanemodeActive
                                                    "Flashlight" -> Icons.Rounded.Highlight
                                                    "Hotspot" -> Icons.Rounded.WifiTethering
                                                    "Do Not Disturb" -> Icons.Rounded.DoNotDisturbOn
                                                    "Screen Rotation" -> Icons.Rounded.ScreenRotation
                                                    "Screen Record" -> Icons.Rounded.Videocam
                                                    "Dark Mode" -> Icons.Rounded.DarkMode
                                                    "Battery Saver" -> Icons.Rounded.BatterySaver
                                                    "NFC" -> Icons.Rounded.Nfc
                                                    else -> Icons.Rounded.Settings
                                                }
                                                ControlIcon(icon, control) {
                                                    Toast.makeText(context, "$control toggled", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                }
                                "System" -> {
                                    Text("System Resources", color = ElectricBlue, fontWeight = FontWeight.Bold)
                                    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceDark).padding(16.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("CPU Usage", color = GhostWhite, fontSize = 12.sp)
                                            Text("45%", color = Color(0xFF00FF87), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(progress = { 0.45f }, modifier = Modifier.fillMaxWidth().height(6.dp), color = Color(0xFF00FF87), trackColor = Color.DarkGray)
                                        
                                        Spacer(modifier = Modifier.height(16.dp))
                                        
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("RAM Usage", color = GhostWhite, fontSize = 12.sp)
                                            Text("62%", color = Color(0xFF00E5FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(progress = { 0.62f }, modifier = Modifier.fillMaxWidth().height(6.dp), color = Color(0xFF00E5FF), trackColor = Color.DarkGray)
                                        
                                        Spacer(modifier = Modifier.height(16.dp))
                                        
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Storage", color = GhostWhite, fontSize = 12.sp)
                                            Text("89%", color = HyperPink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(progress = { 0.89f }, modifier = Modifier.fillMaxWidth().height(6.dp), color = HyperPink, trackColor = Color.DarkGray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
"""
    content = content[:start_idx] + new_sidebar + content[end_idx:]
    with open("/app/applet/app/src/main/java/com/yash/shortw/MainActivity.kt", "w") as f:
        f.write(content)
    print("Sidebar replaced successfully!")
else:
    print(f"Could not find bounds. start: {start_idx}, end: {end_idx}")

