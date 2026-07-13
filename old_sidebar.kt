                        isDragging = true 
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDragEnd = { 
                        isDragging = false 
                        AppState.saveSettings(context)
                    },
                    onDragCancel = { 
                        isDragging = false 
                        AppState.saveSettings(context)
                    }
                ) { change, dragAmount ->
                    change.consume()
                    val dy = with(density) { dragAmount.y.toDp().value }
                    val dx = with(density) { dragAmount.x.toDp().value }
                    
                    AppState.settings = AppState.settings.copy(
                        triggerOffsetY = (AppState.settings.triggerOffsetY + dy).coerceIn(-400f, 400f)
                    )
                    
                    if (AppState.settings.isRightEdge && dx < -50) {
                        AppState.settings = AppState.settings.copy(isRightEdge = false)
                    } else if (!AppState.settings.isRightEdge && dx > 50) {
                        AppState.settings = AppState.settings.copy(isRightEdge = true)
                    }
                }
            }
        }
    )
}

@Composable
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
  
  LaunchedEffect(Unit) {
      while (true) {
          volumeState = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) / maxVolume
          brightnessState = try {
              android.provider.Settings.System.getInt(context.contentResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS) / 255f
          } catch (e: android.provider.Settings.SettingNotFoundException) {
              0.5f
          }
          kotlinx.coroutines.delay(500)
      }
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
        enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) + 
                androidx.compose.animation.scaleIn(initialScale = 0.3f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(if (AppState.settings.isRightEdge) 1f else 0f, 0.5f), animationSpec = androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)),
        exit = androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(200)) + 
               androidx.compose.animation.scaleOut(targetScale = 0.3f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(if (AppState.settings.isRightEdge) 1f else 0f, 0.5f), animationSpec = androidx.compose.animation.core.tween(200))
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
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("Quick Actions", color = GhostWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            var isFlashlightOn by remember { mutableStateOf(false) }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { 
                        try {
                            val intent = Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
