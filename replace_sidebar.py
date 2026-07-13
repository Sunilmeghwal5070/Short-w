import re

with open("/app/applet/app/src/main/java/com/yash/shortw/MainActivity.kt", "r") as f:
    content = f.read()

# We need to replace the entire SidebarComponent
new_sidebar = """@Composable
fun SidebarComponent(onAppClick: (AppInfo, Boolean) -> Unit, onToggleHud: () -> Unit, isHudVisible: Boolean, onExit: () -> Unit) {
    val context = LocalContext.current
    androidx.compose.animation.AnimatedVisibility(
        visible = AppState.sidebarVisible,
        enter = androidx.compose.animation.fadeIn(),
        exit = androidx.compose.animation.fadeOut()
    ) {
        Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onTap = { AppState.sidebarVisible = false }) }) {
            androidx.compose.animation.AnimatedVisibility(
                visible = AppState.sidebarVisible,
                modifier = Modifier.align(if (AppState.settings.isRightEdge) Alignment.CenterEnd else Alignment.CenterStart),
                enter = androidx.compose.animation.slideInHorizontally(initialOffsetX = { if (AppState.settings.isRightEdge) it else -it }),
                exit = androidx.compose.animation.slideOutHorizontally(targetOffsetX = { if (AppState.settings.isRightEdge) it else -it })
            ) {
                if (AppState.settings.sidebarDesign == 0) {
                    SciFiGamerSidebar(onAppClick, onToggleHud, isHudVisible, onExit)
                } else {
                    MinimalSidebar(onAppClick, onToggleHud, isHudVisible, onExit)
                }
            }
        }
    }
}

@Composable
fun SciFiGamerSidebar(onAppClick: (AppInfo, Boolean) -> Unit, onToggleHud: () -> Unit, isHudVisible: Boolean, onExit: () -> Unit) {
    val context = LocalContext.current
    val floatingAppsList = AppState.installedApps.filter { AppState.floatingApps.contains(it.packageName) }.take(2)
    val pinnedAppsList = AppState.installedApps.filter { AppState.pinnedApps.contains(it.packageName) }.take(2)
    val toolsList = AppState.visibleTools.take(3)
    val controlsList = AppState.visibleControls.take(2)

    Box(
        modifier = Modifier
            .width(220.dp)
            .padding(end = if (AppState.settings.isRightEdge) 16.dp else 0.dp, start = if (!AppState.settings.isRightEdge) 16.dp else 0.dp)
            .pointerInput(Unit) { detectTapGestures { /* consume */ } }
    ) {
        // Sci-Fi Background and Border
        Box(modifier = Modifier
            .matchParentSize()
            .background(Color(0xFF10151E), shape = RoundedCornerShape(16.dp))
            .border(2.dp, Brush.verticalGradient(listOf(Color(0xFF00FF87), Color(0xFF00E5FF))), shape = RoundedCornerShape(16.dp))
            .shadow(16.dp)
        )
        
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // CPU / GPU bars
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("CPU", color = Color(0xFF00FF87), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(progress = { 0.6f }, modifier = Modifier.height(6.dp).fillMaxWidth().padding(end = 4.dp), color = Color(0xFF00FF87), trackColor = Color.DarkGray)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("GPU", color = Color(0xFF00E5FF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(progress = { 0.4f }, modifier = Modifier.height(6.dp).fillMaxWidth().padding(start = 4.dp), color = Color(0xFF00E5FF), trackColor = Color.DarkGray)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            // Apps (Chrome, WhatsApp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                (floatingAppsList + pinnedAppsList).distinctBy { it.packageName }.take(2).forEach { app ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onAppClick(app, true) }) {
                        Box(modifier = Modifier.size(56.dp).background(Color(0xFF1A212D), RoundedCornerShape(12.dp)).border(1.dp, Color(0xFF00E5FF).copy(alpha=0.5f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                            Image(bitmap = app.icon, contentDescription = app.name, modifier = Modifier.size(36.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(app.name, color = Color.White, fontSize = 10.sp, maxLines = 1)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            // Tools (Screenshot, Screen record, Memory clean)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ToolIcon(Icons.Rounded.CropFree, "Screenshot") {
                    // Placeholder for screenshot
                    android.widget.Toast.makeText(context, "Screenshot taking...", android.widget.Toast.LENGTH_SHORT).show()
                }
                ToolIcon(Icons.Rounded.Videocam, "Screen Rec") {
                    android.widget.Toast.makeText(context, "Recording...", android.widget.Toast.LENGTH_SHORT).show()
                }
                ToolIcon(Icons.Rounded.CleaningServices, "Memory") {
                    android.widget.Toast.makeText(context, "Memory cleaned", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            // Toggles
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ControlIcon(Icons.Rounded.CallEnd, "Reject Call") {
                    android.widget.Toast.makeText(context, "Call rejection toggled", android.widget.Toast.LENGTH_SHORT).show()
                }
                ControlIcon(Icons.Rounded.NotificationsOff, "DND") {
                    val intent = Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try { context.startActivity(intent) } catch (e:Exception) {}
                }
            }
        }
    }
}

@Composable
fun MinimalSidebar(onAppClick: (AppInfo, Boolean) -> Unit, onToggleHud: () -> Unit, isHudVisible: Boolean, onExit: () -> Unit) {
    val context = LocalContext.current
    val floatingAppsList = AppState.installedApps.filter { AppState.floatingApps.contains(it.packageName) }.take(4)
    val toolsList = AppState.visibleTools.take(4)

    Box(
        modifier = Modifier
            .width(80.dp)
            .padding(end = if (AppState.settings.isRightEdge) 16.dp else 0.dp, start = if (!AppState.settings.isRightEdge) 16.dp else 0.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xE6FFFFFF))
            .border(1.dp, Color(0x33000000), RoundedCornerShape(32.dp))
            .pointerInput(Unit) { detectTapGestures { /* consume */ } }
            .padding(vertical = 24.dp, horizontal = 12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            floatingAppsList.forEach { app ->
                Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(48.dp).clip(CircleShape).clickable { onAppClick(app, true) })
            }
            HorizontalDivider(color = Color.LightGray)
            Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = Color.DarkGray, modifier = Modifier.size(32.dp).clickable {
                val intent = Intent(context, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                AppState.sidebarVisible = false
            })
            Icon(Icons.Rounded.PowerSettingsNew, contentDescription = "Exit", tint = Color.Red, modifier = Modifier.size(32.dp).clickable {
                onExit()
            })
        }
    }
}

@Composable
fun ToolIcon(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(modifier = Modifier.size(40.dp).background(Color(0xFF2A3143), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = Color.White, fontSize = 8.sp, maxLines = 1)
    }
}

@Composable
fun ControlIcon(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(modifier = Modifier.size(48.dp).background(Color(0xFF2A3143), RoundedCornerShape(16.dp)).border(1.dp, Color(0xFF00FF87).copy(alpha=0.5f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = Color.White, fontSize = 9.sp, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
"""

# Extract everything before SidebarComponent
before = content[:content.find("@Composable\nfun SidebarComponent")]
# Extract everything after the end of SidebarComponent
# We found it ends around line 1618. We can use a regex to find SidebarTab which is the next composable
after = content[content.find("@Composable\nfun SidebarTab"):]

with open("/app/applet/app/src/main/java/com/yash/shortw/MainActivity.kt", "w") as f:
    f.write(before + new_sidebar + "\n" + after)

