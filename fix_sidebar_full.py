import re

with open("/app/applet/app/src/main/java/com/yash/shortw/MainActivity.kt", "r") as f:
    content = f.read()

start_idx = content.find("@Composable\nfun SidebarComponent")
end_idx = content.find("@Composable\nfun ToolIcon")

if start_idx != -1 and end_idx != -1:
    new_sidebar = """@Composable
fun SidebarComponent(onAppClick: (AppInfo, Boolean) -> Unit, onToggleHud: () -> Unit, isHudVisible: Boolean, onExit: () -> Unit) {
    val context = LocalContext.current
    
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
                        .fillMaxHeight(0.9f)
                        .width(340.dp)
                        .clip(shape)
                        .background(androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(Obsidian.copy(alpha = AppState.settings.sidebarOpacity), SurfaceDark.copy(alpha = AppState.settings.sidebarOpacity))
                        ))
                        .border(1.dp, SurfaceGlass, shape)
                        .pointerInput(Unit) { detectTapGestures { /* consume tap inside */ } }
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Header
                        Row(modifier = Modifier.fillMaxWidth().background(SurfaceGlass.copy(alpha = 0.1f)).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Short Panel", color = GhostWhite, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                            IconButton(onClick = onExit) {
                                Icon(Icons.Rounded.PowerSettingsNew, "Close", tint = HyperPink)
                            }
                        }
                        
                        // Content
                        Column(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                            
                            // Direct Apps
                            val pinnedAppsList = AppState.installedApps.filter { AppState.pinnedApps.contains(it.packageName) }
                            if (pinnedAppsList.isNotEmpty()) {
                                Column {
                                    Text("Direct Launch", color = NeonPurple, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    pinnedAppsList.chunked(4).forEach { rowApps ->
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                            rowApps.forEach { app ->
                                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onAppClick(app, false) }) {
                                                    Image(bitmap = app.icon, contentDescription = app.name, modifier = Modifier.size(52.dp).clip(CircleShape))
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(app.name, color = GhostWhite, fontSize = 10.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.width(64.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                                }
                                            }
                                            val remaining = 4 - rowApps.size
                                            for(i in 0 until remaining) { Spacer(Modifier.width(64.dp)) }
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                }
                            }

                            // Floating Apps
                            val floatingAppsList = AppState.installedApps.filter { AppState.floatingApps.contains(it.packageName) }
                            if (floatingAppsList.isNotEmpty()) {
                                Column {
                                    Text("Floating Windows", color = ElectricBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    floatingAppsList.chunked(4).forEach { rowApps ->
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                            rowApps.forEach { app ->
                                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onAppClick(app, true) }) {
                                                    Box(modifier = Modifier.size(56.dp).background(SurfaceDark.copy(alpha=0.8f), RoundedCornerShape(12.dp)).padding(8.dp), contentAlignment = Alignment.Center) {
                                                        Image(bitmap = app.icon, contentDescription = app.name, modifier = Modifier.size(40.dp))
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(app.name, color = GhostWhite, fontSize = 10.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.width(64.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                                }
                                            }
                                            val remaining = 4 - rowApps.size
                                            for(i in 0 until remaining) { Spacer(Modifier.width(64.dp)) }
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                }
                            }

                            // Tools
                            if (AppState.visibleTools.isNotEmpty()) {
                                Column {
                                    Text("Tools", color = HyperPink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (AppState.visibleTools.contains("Macro & Auto-Clicker")) {
                                        ToolItem(Icons.Rounded.TouchApp, "Macro & Auto-Clicker", "Automate repetitive clicks", NeonPurple) {
                                            AppState.windows.add(FloatingWindowData(id = AppState.windowIdCounter++, title = "Macro & Auto-Clicker", icon = Icons.Rounded.TouchApp, offset = androidx.compose.ui.unit.IntOffset(100, 200), size = androidx.compose.ui.unit.IntSize(600, 400), color = NeonPurple))
                                            AppState.sidebarVisible = false
                                        }
                                    }
                                    if (AppState.visibleTools.contains("Live Screen OCR")) {
                                        ToolItem(Icons.Rounded.TextSnippet, "Live Screen OCR", "Extract text from screen", ElectricBlue) {
                                            AppState.windows.add(FloatingWindowData(id = AppState.windowIdCounter++, title = "Live Screen OCR", icon = Icons.Rounded.TextSnippet, offset = androidx.compose.ui.unit.IntOffset(150, 250), size = androidx.compose.ui.unit.IntSize(700, 500), color = ElectricBlue))
                                            AppState.sidebarVisible = false
                                        }
                                    }
                                    if (AppState.visibleTools.contains("Universal Clipboard")) {
                                        ToolItem(Icons.Rounded.ContentPaste, "Universal Clipboard", "Manage copied text history", HyperPink) {
                                            AppState.windows.add(FloatingWindowData(id = AppState.windowIdCounter++, title = "Universal Clipboard", icon = Icons.Rounded.ContentPaste, offset = androidx.compose.ui.unit.IntOffset(50, 100), size = androidx.compose.ui.unit.IntSize(800, 900), color = HyperPink))
                                            AppState.sidebarVisible = false
                                        }
                                    }
                                    if (AppState.visibleTools.contains("Floating System Monitor")) {
                                        ToolItemToggle(Icons.Rounded.Memory, "Floating System Monitor", "Live CPU/RAM overlay", Color(0xFF00FF87), isHudVisible) {
                                            onToggleHud()
                                        }
                                    }
                                }
                            }

                            // Controls
                            if (AppState.visibleControls.isNotEmpty()) {
                                Column {
                                    Text("Controls", color = Color(0xFFFFC107), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    AppState.visibleControls.chunked(4).forEach { rowControls ->
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
                                                    ControlManager.toggleSetting(context, control)
                                                }
                                            }
                                            val remaining = 4 - rowControls.size
                                            for(i in 0 until remaining) { Spacer(Modifier.width(56.dp)) }
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
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
    print("Sidebar unified replaced successfully!")
else:
    print(f"Could not find bounds. start: {start_idx}, end: {end_idx}")
