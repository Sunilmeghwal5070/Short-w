fun SidebarComponent(onAppClick: (AppInfo, Boolean) -> Unit, onToggleHud: () -> Unit, isHudVisible: Boolean, onExit: () -> Unit) {
  var currentTab by remember { mutableStateOf("Apps") }
  
  val topStartRadius = if (AppState.settings.isRightEdge) 32.dp else 0.dp
  val bottomStartRadius = if (AppState.settings.isRightEdge) 32.dp else 0.dp
  val topEndRadius = if (!AppState.settings.isRightEdge) 32.dp else 0.dp
  val bottomEndRadius = if (!AppState.settings.isRightEdge) 32.dp else 0.dp
  val shape = RoundedCornerShape(topStartRadius, topEndRadius, bottomEndRadius, bottomStartRadius)

  val context = LocalContext.current
  val audioManager = remember { context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager }
  val maxVolume = remember { audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).toFloat() }
  var volumeState by remember { mutableFloatStateOf(audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) / maxVolume) }

  var brightnessState by remember { 
      mutableFloatStateOf(
          try {
              android.provider.Settings.System.getInt(context.contentResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS) / 255f
          } catch (e: android.provider.Settings.SettingNotFoundException) {
              0.5f
          }
      )
  }
  
  var toastMessage by remember { mutableStateOf<String?>(null) }
  LaunchedEffect(toastMessage) {
      if (toastMessage != null) {
          kotlinx.coroutines.delay(2000)
          toastMessage = null
      }
  }

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
        Box(
          modifier = Modifier
            .width(340.dp)
            .fillMaxHeight()
            .pointerInput(Unit) { detectTapGestures { /* consume */ } }
            .padding(
          start = if (AppState.settings.isRightEdge) 16.dp else 0.dp,
          end = if (!AppState.settings.isRightEdge) 16.dp else 0.dp
        )
        .clip(shape)
        .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = AppState.settings.sidebarOpacity), SurfaceDark.copy(alpha = AppState.settings.sidebarOpacity))))
        .border(1.dp, Color.White.copy(alpha = 0.5f), shape)
        .padding(24.dp)
    ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Tab Row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(SurfaceDark),
        horizontalArrangement = Arrangement.SpaceEvenly
      ) {
        SidebarTab("Apps", currentTab == "Apps") { currentTab = "Apps" }
        SidebarTab("Tools", currentTab == "Tools") { currentTab = "Tools" }
        SidebarTab("Control", currentTab == "Control") { currentTab = "Control" }
      }
      
      Spacer(modifier = Modifier.height(24.dp))
      
      Column(
        modifier = Modifier
          .weight(1f)
          .verticalScroll(rememberScrollState())
      ) {
        when (currentTab) {
          "Apps" -> {
            val floatingAppsList = AppState.installedApps.filter { AppState.floatingApps.contains(it.packageName) }
            val pinnedAppsList = AppState.installedApps.filter { AppState.pinnedApps.contains(it.packageName) }

            Text("Floating Apps (${floatingAppsList.size}/3)", color = GhostWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.heightIn(max = 200.dp)
            ) {
                items(floatingAppsList) { app ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(4.dp).clickable { onAppClick(app, true) }
                    ) {
                        Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)))
                        Text(app.name, color = GhostWhite, fontSize = 10.sp, maxLines = 1, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Direct Launch Apps (${pinnedAppsList.size}/10)", color = GhostWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                items(pinnedAppsList) { app ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(4.dp).clickable { onAppClick(app, false) }
                    ) {
                        Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)))
                        Text(app.name, color = GhostWhite, fontSize = 10.sp, maxLines = 1, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
          }
          "Tools" -> {
            Text("Automation & AI Tools", color = GhostWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            if (AppState.visibleTools.contains("Macro & Auto-Clicker")) {
                ToolItem(Icons.Rounded.SmartButton, "Macro & Auto-Clicker", "Record & loop touch sequences", ElectricBlue)
            }
            if (AppState.visibleTools.contains("Live Screen OCR")) {
                ToolItem(Icons.Rounded.DocumentScanner, "Live Screen OCR", "Extract text from anywhere", HyperPink)
            }
            if (AppState.visibleTools.contains("Universal Clipboard")) {
                ToolItem(Icons.Rounded.ContentPaste, "Universal Clipboard", "History of copied assets", NeonPurple)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            Text("System Utilities", color = GhostWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            if (AppState.visibleTools.contains("Floating System Monitor")) {
                ToolItemToggle(Icons.Rounded.Speed, "Floating System Monitor", "Show FPS, RAM, CPU", Color(0xFFFFC107), isHudVisible, onToggleHud)
            }
          }
          "Control" -> {
            Text("Control Center", color = GhostWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            
            val toggleControls = mutableListOf<@Composable () -> Unit>()
            if (AppState.visibleControls.contains("Wi-Fi")) toggleControls.add { ToggleItem(Icons.Rounded.Wifi, "Wi-Fi", true, ElectricBlue) { toastMessage = it } }
            if (AppState.visibleControls.contains("Bluetooth")) toggleControls.add { ToggleItem(Icons.Rounded.Bluetooth, "Bluetooth", false, HyperPink) { toastMessage = it } }
            if (AppState.visibleControls.contains("Data")) toggleControls.add { ToggleItem(Icons.Rounded.DataUsage, "Data", true, NeonPurple) { toastMessage = it } }
            if (AppState.visibleControls.contains("Ghost Mode")) toggleControls.add { ToggleItem(Icons.Rounded.Gamepad, "Ghost Mode", false, Color.Red) { toastMessage = it } }
            if (AppState.visibleControls.contains("Location")) toggleControls.add { ToggleItem(Icons.Rounded.LocationOn, "Location", true, Color(0xFF4CAF50)) { toastMessage = it } }
            if (AppState.visibleControls.contains("Airplane Mode")) toggleControls.add { ToggleItem(Icons.Rounded.AirplanemodeActive, "Airplane", false, Color(0xFFFFC107)) { toastMessage = it } }
            if (AppState.visibleControls.contains("Flashlight")) toggleControls.add { ToggleItem(Icons.Rounded.Highlight, "Flashlight", false, Color.White) { toastMessage = it } }
            if (AppState.visibleControls.contains("Hotspot")) toggleControls.add { ToggleItem(Icons.Rounded.WifiTethering, "Hotspot", false, Color.Cyan) { toastMessage = it } }
            if (AppState.visibleControls.contains("Do Not Disturb")) toggleControls.add { ToggleItem(Icons.Rounded.DoNotDisturbOn, "DND", false, Color.Magenta) { toastMessage = it } }
            if (AppState.visibleControls.contains("Screen Rotation")) toggleControls.add { ToggleItem(Icons.Rounded.ScreenRotation, "Rotation", true, ElectricBlue) { toastMessage = it } }
            if (AppState.visibleControls.contains("Screen Record")) toggleControls.add { ToggleItem(Icons.Rounded.Videocam, "Record", false, Color.Red) { toastMessage = it } }
            if (AppState.visibleControls.contains("Dark Mode")) toggleControls.add { ToggleItem(Icons.Rounded.DarkMode, "Dark Mode", true, Color.Gray) { toastMessage = it } }
            if (AppState.visibleControls.contains("Battery Saver")) toggleControls.add { ToggleItem(Icons.Rounded.BatterySaver, "Battery", false, Color(0xFF4CAF50)) { toastMessage = it } }
            if (AppState.visibleControls.contains("NFC")) toggleControls.add { ToggleItem(Icons.Rounded.Nfc, "NFC", false, Color.Cyan) { toastMessage = it } }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                toggleControls.chunked(4).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.Start, modifier = Modifier.fillMaxWidth()) {
                        rowItems.forEach { 
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                it()
                            }
                        }
                        for (i in rowItems.size until 4) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            if (AppState.visibleControls.contains("Brightness")) {
                PremiumSliderItem(
                  icon = Icons.Rounded.LightMode, 
                  label = "Brightness",
                  value = brightnessState,
                  onValueChange = {
                      brightnessState = it
                      if (android.provider.Settings.System.canWrite(context)) {
                          android.provider.Settings.System.putInt(context.contentResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS, (it * 255).toInt())
                      } else {
                          val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                              data = android.net.Uri.parse("package:${context.packageName}")
                              addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                          }
                          context.startActivity(intent)
                          AppState.sidebarVisible = false
                      }
                  },
                  color = ElectricBlue
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            if (AppState.visibleControls.contains("Volume")) {
                PremiumSliderItem(
                  icon = Icons.Rounded.VolumeUp, 
                  label = "Media Volume",
                  value = volumeState,
                  onValueChange = {
                      volumeState = it
                      audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, (it * maxVolume).toInt(), 0)
                  },
                  color = HyperPink
                )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
      Button(
        onClick = { AppState.sidebarVisible = false },
        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text("Close Sidebar", color = GhostWhite)
      }
      Spacer(modifier = Modifier.height(8.dp))
      Button(
        onClick = onExit,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text("Stop Master Service", color = Color.Red)
      }
    }
  }
  
  androidx.compose.animation.AnimatedVisibility(
      visible = toastMessage != null,
      modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp),
      enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { 50 }),
      exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { 50 })
  ) {
      Box(
          modifier = Modifier
              .clip(RoundedCornerShape(16.dp))
              .background(Obsidian.copy(alpha = 0.95f))
              .border(1.dp, ElectricBlue.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
              .padding(horizontal = 24.dp, vertical = 12.dp)
      ) {
          Text(toastMessage ?: "", color = GhostWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
      }
  }
}
  }
}

@Composable
fun SidebarTab(title: String, isSelected: Boolean, onClick: () -> Unit) {
