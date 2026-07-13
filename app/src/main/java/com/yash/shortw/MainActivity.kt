package com.yash.shortw

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.yash.shortw.ui.theme.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.max

import androidx.compose.ui.graphics.toArgb

data class AppSettings(
  val triggerThickness: Float = 6f,
  val triggerHeight: Float = 100f,
  val triggerOffsetX: Float = 0f,
  val triggerOffsetY: Float = 0f,
  val isRightEdge: Boolean = true,
  val edgePosition: Int = 2,
  val sidebarOpacity: Float = 0.85f,
  val windowOpacity: Float = 0.95f,
  val triggerAction: Int = 0, // 0 = Swipe, 1 = Single Tap, 2 = Double Tap
  val triggerColorArgb: Int = 0xFF1976D2.toInt(), // ElectricBlue
  val triggerGradientColorArgb: Int? = null,
  val triggerMode: Int = 0, // 0 = Visual Slider, 1 = Full Screen Edge
  val sidebarDesign: Int = 0 // 0 = Sci-Fi Gamer, 1 = Minimal Sleek
) {
  val triggerColor: Color get() = Color(triggerColorArgb)
  val triggerGradientColor: Color? get() = triggerGradientColorArgb?.let { Color(it) }
}

data class AppInfo(
  val name: String,
  val icon: ImageBitmap,
  val packageName: String
)

data class FloatingWindowData(
  val id: Int,
  val title: String,
  val icon: ImageVector?,
  val appIcon: ImageBitmap? = null,
  var offset: IntOffset,
  var size: IntSize,
  var isMinimized: Boolean = false,
  var isMaximized: Boolean = false,
  var opacity: Float = 0.95f,
  var isLocked: Boolean = false,
  val color: Color,
  val packageName: String = ""
)

object AppState {
  var isFirstLaunch by mutableStateOf(true)
  var language by mutableStateOf("en")
  var isServiceRunning by mutableStateOf(false)
  var hasOverlayPermission by mutableStateOf(false)
  var hasNotificationPermission by mutableStateOf(false)
  var hasAccessibilityPermission by mutableStateOf(false)
  var hasNotificationListenerPermission by mutableStateOf(false)
  var notificationTrigger by mutableStateOf(0)
  var hasWriteSettingsPermission by mutableStateOf(false)
  var settings by mutableStateOf(AppSettings())
  var sidebarVisible by mutableStateOf(false)
  var hudVisible by mutableStateOf(false)
    var lastTriggeredSide by mutableStateOf(1)
    var isDraggingBubble by mutableStateOf(false)
  val windows = mutableStateListOf<FloatingWindowData>()
  var installedApps by mutableStateOf<List<AppInfo>>(emptyList())
  var pinnedApps = mutableStateListOf<String>() // Package names
  var floatingApps = mutableStateListOf<String>() // Package names
  var visibleTools = mutableStateListOf<String>("Macro & Auto-Clicker", "Live Screen OCR", "Universal Clipboard", "Floating System Monitor")
  var visibleControls = mutableStateListOf<String>("Wi-Fi", "Bluetooth", "Data", "Ghost Mode", "Brightness", "Volume")
  var clipboardHistory = mutableStateListOf<String>()
  var windowIdCounter = 0
  
  fun saveSettings(context: android.content.Context) {
      val prefs = context.getSharedPreferences("Short PanelPrefs", android.content.Context.MODE_PRIVATE)
      val gson = com.google.gson.Gson()
      prefs.edit().apply {
          putString("settings", gson.toJson(settings))
          putString("pinnedApps", gson.toJson(pinnedApps.toList()))
          putString("floatingApps", gson.toJson(floatingApps.toList()))
          putString("visibleTools", gson.toJson(visibleTools.toList()))
          putString("visibleControls", gson.toJson(visibleControls.toList()))
          putString("clipboardHistory", gson.toJson(clipboardHistory.toList()))
      }.apply()
  }
  
  fun loadSettings(context: android.content.Context) {
      val prefs = context.getSharedPreferences("Short PanelPrefs", android.content.Context.MODE_PRIVATE)
      val gson = com.google.gson.Gson()
      try {
          prefs.getString("settings", null)?.let { settings = gson.fromJson(it, AppSettings::class.java) }
      } catch (e: Exception) {
          e.printStackTrace()
      }
      
      val type = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
      prefs.getString("clipboardHistory", null)?.let { 
          try {
              val list: List<String> = gson.fromJson(it, type)
              clipboardHistory.clear()
              clipboardHistory.addAll(list)
          } catch(e: Exception) {}
      }
      
      try {
          prefs.getString("pinnedApps", null)?.let { 
              val list: List<String> = gson.fromJson(it, type)
              pinnedApps.clear()
              pinnedApps.addAll(list)
          }
      } catch (e: Exception) { e.printStackTrace() }
      
      try {
          prefs.getString("floatingApps", null)?.let { 
              val list: List<String> = gson.fromJson(it, type)
              floatingApps.clear()
              floatingApps.addAll(list)
          }
      } catch (e: Exception) { e.printStackTrace() }
      
      try {
          prefs.getString("visibleTools", null)?.let { 
              val list: List<String> = gson.fromJson(it, type)
              visibleTools.clear()
              visibleTools.addAll(list)
          }
      } catch (e: Exception) { e.printStackTrace() }
      
      try {
          prefs.getString("visibleControls", null)?.let { 
              val list: List<String> = gson.fromJson(it, type)
              visibleControls.clear()
              visibleControls.addAll(list)
          }
      } catch (e: Exception) { e.printStackTrace() }
  }

  fun fetchApps(context: android.content.Context) {
    if (installedApps.isNotEmpty()) return
    Thread {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val apps = pm.queryIntentActivities(intent, 0)
        val appList = apps.mapNotNull { resolveInfo ->
            try {
                AppInfo(
                    name = resolveInfo.loadLabel(pm).toString(),
                    icon = resolveInfo.loadIcon(pm).toImageBitmap(),
                    packageName = resolveInfo.activityInfo.packageName
                )
            } catch (e: Exception) {
                null
            }
        }.distinctBy { it.packageName }
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            installedApps = appList
            if (pinnedApps.isEmpty()) {
                pinnedApps.addAll(appList.take(10).map { it.packageName })
            }
            if (floatingApps.isEmpty()) {
                floatingApps.addAll(appList.take(3).map { it.packageName })
            }
        }
    }.start()
  }
}

fun Drawable.toImageBitmap(): ImageBitmap {
    if (this is BitmapDrawable) {
        if (this.bitmap != null) {
            return this.bitmap.asImageBitmap()
        }
    }
    val width = if (intrinsicWidth > 0) intrinsicWidth else 100
    val height = if (intrinsicHeight > 0) intrinsicHeight else 100
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap.asImageBitmap()
}

class MainActivity : ComponentActivity() {
  override fun onPause() {
    super.onPause()
    AppState.saveSettings(this)
  }

  override fun onResume() {
    super.onResume()
    AppState.hasOverlayPermission = Settings.canDrawOverlays(this)
    
    // Check Accessibility Permission
    val expectedComponentName = android.content.ComponentName(this, OverlayAccessibilityService::class.java)
    val enabledServicesSetting = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
    var isAccessibilityEnabled = false
    if (enabledServicesSetting != null) {
        val colonSplitter = android.text.TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)
        while (colonSplitter.hasNext()) {
            val componentNameString = colonSplitter.next()
            val enabledService = android.content.ComponentName.unflattenFromString(componentNameString)
            if (enabledService != null && enabledService == expectedComponentName) {
                isAccessibilityEnabled = true
                break
            }
        }
    }
    AppState.hasAccessibilityPermission = isAccessibilityEnabled

    AppState.hasWriteSettingsPermission = Settings.System.canWrite(this)
    AppState.hasNotificationPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
    AppState.hasNotificationListenerPermission = androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    val prefs = getSharedPreferences("Short PanelPrefs", android.content.Context.MODE_PRIVATE)
    AppState.isFirstLaunch = prefs.getBoolean("isFirstLaunch", true)
    AppState.language = prefs.getString("language", "en") ?: "en"
    
    AppState.loadSettings(this)
    AppState.fetchApps(this)

    setContent {
      MyApplicationTheme {
        ShortPanelApp(prefs)
      }
    }
  }
}

@Composable
fun ShortPanelApp(prefs: android.content.SharedPreferences) {
  val context = LocalContext.current
  var currentRoute by remember { mutableStateOf("splash") }

  val allPermissionsGranted = (AppState.hasOverlayPermission || AppState.hasAccessibilityPermission) && AppState.hasWriteSettingsPermission && AppState.hasNotificationPermission

  if (currentRoute == "splash") {
      SplashScreen {
          currentRoute = if (AppState.isFirstLaunch) "onboarding" else "main"
      }
  } else if (AppState.isFirstLaunch && currentRoute == "onboarding") {
      OnboardingScreen(
          onComplete = {
              prefs.edit().putBoolean("isFirstLaunch", false).putString("language", AppState.language).apply()
              AppState.isFirstLaunch = false
              currentRoute = "main"
          }
      )
  } else if (!allPermissionsGranted) {
    PermissionScreen()
  } else {
    when (currentRoute) {
        "main" -> SettingsDashboard(onNavigate = { currentRoute = it })
        "terms" -> LegalScreen("Terms & Conditions", onBack = { currentRoute = "main" })
        "privacy" -> LegalScreen("Privacy Policy", onBack = { currentRoute = "main" })
        "disclaimer" -> LegalScreen("Disclaimer", onBack = { currentRoute = "main" })
        "about" -> LegalScreen("About", onBack = { currentRoute = "main" })
        "app_selection" -> AppSelectionScreen(onBack = { currentRoute = "main" })
        "tool_selection" -> ItemSelectionScreen(
            title = "Customize Tools",
            allItems = listOf("Macro & Auto-Clicker", "Live Screen OCR", "Universal Clipboard", "Floating System Monitor"),
            selectedItems = AppState.visibleTools,
            onBack = { currentRoute = "main" }
        )
        "control_selection" -> ItemSelectionScreen(
            title = "Customize Controls (Max 10)",
            allItems = listOf("Wi-Fi", "Bluetooth", "Data", "Ghost Mode", "Brightness", "Volume", "Location", "Airplane Mode", "Flashlight", "Hotspot", "Do Not Disturb", "Screen Rotation", "Screen Record", "Dark Mode", "Battery Saver", "NFC"),
            selectedItems = AppState.visibleControls,
            onBack = { currentRoute = "main" },
            maxSelection = 10
        )
    }
  }
}

@Composable
fun SplashScreen(onFinish: () -> Unit) {
    val scale = remember { androidx.compose.animation.core.Animatable(0.5f) }
    val alpha = remember { androidx.compose.animation.core.Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            alpha.animateTo(1f, animationSpec = androidx.compose.animation.core.tween(1000))
        }
        delay(2000)
        onFinish()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Obsidian),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.ic_app_logo),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer(scaleX = scale.value, scaleY = scale.value, alpha = alpha.value)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Short Panel Pro",
                color = GhostWhite,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer(alpha = alpha.value)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Ultimate Floating Workspace",
                color = SoftGray,
                fontSize = 14.sp,
                modifier = Modifier.graphicsLayer(alpha = alpha.value)
            )
        }
    }
}

@Composable
fun LegalScreen(title: String, onBack: () -> Unit) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text(title, color = GhostWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = GhostWhite) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Obsidian)
            )
        },
        containerColor = Obsidian
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            if (title == "About") {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_app_logo),
                        contentDescription = "Logo",
                        modifier = Modifier.size(80.dp).clip(RoundedCornerShape(16.dp)).background(SurfaceDark).padding(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text("About Short Panel Pro", color = GhostWhite, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(8.dp))
                val currentVersion = try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                } catch (e: Exception) { "1.0.0" }
                Text("Version: $currentVersion", color = SoftGray)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Short Panel Pro is a comprehensive floating overlay toolkit designed to boost your productivity. It provides quick access to your favorite apps, system toggles, and utilities directly from any screen without interrupting your workflow.\n\nDeveloped by Sunil Meghwal", color = SoftGray, lineHeight = 20.sp)
            } else {
                val text = when (title) {
                    "Privacy Policy" -> "Privacy Policy\n\n1. Information Collection: We do not collect or store any personal data. All app preferences and configurations are stored locally on your device.\n2. Permissions: The 'Display Over Other Apps' permission is required strictly for providing the floating overlay functionality. We do not track your usage of other apps.\n3. Third-party Services: We do not use any third-party tracking or analytics services."
                    "Terms & Conditions" -> "Terms & Conditions\n\n1. Acceptance: By using Short Panel Pro, you agree to these terms.\n2. Usage: You agree to use the app responsibly and not for any malicious purposes.\n3. Modification: We reserve the right to modify these terms at any time.\n4. Liability: We are not responsible for any damage to your device caused by improper use of the application."
                    "Disclaimer" -> "Disclaimer\n\nThe automation and system modification tools provided in this app (such as brightness and volume controls) interact with Android system settings. While we strive for stability, we are not liable for any unintended system behavior or data loss resulting from the use of these tools."
                    else -> "Content for $title"
                }
                Text(text, color = SoftGray, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
fun AppSelectionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    
    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Customize Apps", color = GhostWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = GhostWhite) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Obsidian)
            )
        },
        containerColor = Obsidian
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab, containerColor = SurfaceDark, contentColor = ElectricBlue) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Direct Launch") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Floating Apps") })
            }
            
            val appList = AppState.installedApps
            val currentList = if (selectedTab == 0) AppState.pinnedApps else AppState.floatingApps
            val limit = if (selectedTab == 0) 10 else 3
            
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("Selected: ${currentList.size} / $limit", color = SoftGray, modifier = Modifier.padding(bottom = 8.dp))
                LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.fillMaxSize()) {
                    items(appList) { app ->
                        val isSelected = currentList.contains(app.packageName)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .padding(8.dp)
                                .clickable {
                                    if (isSelected) {
                                        currentList.remove(app.packageName)
                                        AppState.saveSettings(context)
                                    } else if (currentList.size < limit) {
                                        currentList.add(app.packageName)
                                        AppState.saveSettings(context)
                                    } else {
                                        android.widget.Toast.makeText(context, "Limit reached", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.TopEnd) {
                                Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)))
                                if (isSelected) {
                                    Icon(Icons.Rounded.CheckCircle, null, tint = ElectricBlue, modifier = Modifier.size(16.dp).background(Color.White, CircleShape))
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(app.name, color = GhostWhite, fontSize = 10.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ItemSelectionScreen(title: String, allItems: List<String>, selectedItems: androidx.compose.runtime.snapshots.SnapshotStateList<String>, maxSelection: Int = Int.MAX_VALUE, onBack: () -> Unit) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text(title, color = GhostWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = GhostWhite) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Obsidian)
            )
        },
        containerColor = Obsidian
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            items(allItems) { item ->
                val isSelected = selectedItems.contains(item)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark)
                        .clickable {
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
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(item, color = GhostWhite, fontSize = 16.sp)
                    Switch(
                        checked = isSelected,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (selectedItems.size < maxSelection) {
                                    selectedItems.add(item)
                                } else {
                                    android.widget.Toast.makeText(context, "Maximum limit reached", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                selectedItems.remove(item)
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = ElectricBlue, checkedTrackColor = ElectricBlue.copy(alpha = 0.3f))
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionScreen() {
    val context = LocalContext.current
    
    val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        AppState.hasNotificationPermission = isGranted
    }

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Obsidian, SurfaceDark))), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Rounded.Security, null, tint = ElectricBlue, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("Required Permissions", color = GhostWhite, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Short Panel Pro requires the following permissions to function fully.", color = SoftGray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(32.dp))
            
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PermissionItem(
                    title = "Display Over Other Apps",
                    description = "Required for sidebar and floating windows",
                    isGranted = AppState.hasOverlayPermission,
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot open settings", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                
                if (!AppState.hasOverlayPermission) {
                    PermissionItem(
                        title = "Alternative: Accessibility Service",
                        description = "If 'Display Over Other Apps' is missing, enable Short Panel Pro in Accessibility Services.",
                        isGranted = AppState.hasAccessibilityPermission,
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot open Accessibility Settings", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    
                    if (!AppState.hasAccessibilityPermission) {
                        Text(
                            text = "Note: If you see 'Restricted Setting', go to App Info -> Top Right 3 Dots -> 'Allow restricted settings'.",
                            color = Color.Yellow,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
                        ) {
                            Text("Open App Info", color = GhostWhite)
                        }
                    }
                }
                
                PermissionItem(
                    title = "Modify System Settings",
                    description = "Required for brightness control",
                    isGranted = AppState.hasWriteSettingsPermission,
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot open settings", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    PermissionItem(
                        title = "Notifications",
                        description = "Required for keeping the service alive",
                        isGranted = AppState.hasNotificationPermission,
                        onClick = {
                            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                    )
                }
                
                PermissionItem(
                    title = "Notification Access",
                    description = "Required for slider glow on notification",
                    isGranted = AppState.hasNotificationListenerPermission,
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot open settings", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun PermissionItem(title: String, description: String, isGranted: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isGranted) ElectricBlue.copy(alpha = 0.2f) else SurfaceDark)
            .clickable(enabled = !isGranted, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = GhostWhite, fontWeight = FontWeight.Bold)
            Text(description, color = SoftGray, fontSize = 12.sp)
        }
        if (isGranted) {
            Icon(Icons.Rounded.CheckCircle, null, tint = ElectricBlue)
        } else {
            Icon(Icons.Rounded.ChevronRight, null, tint = SoftGray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalMenuItem(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, color = GhostWhite)
        Icon(Icons.Rounded.ChevronRight, null, tint = SoftGray)
    }
}

@Composable
fun TriggerComponent(isRight: Boolean = AppState.lastTriggeredSide == 1) {
    val haptics = LocalHapticFeedback.current
    var isDragging by remember { mutableStateOf(false) }
    var isNotifying by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val context = LocalContext.current
    
    LaunchedEffect(AppState.notificationTrigger) {
        if (AppState.notificationTrigger > 0) {
            isNotifying = true
            kotlinx.coroutines.delay(150)
            isNotifying = false
            kotlinx.coroutines.delay(150)
            isNotifying = true
            kotlinx.coroutines.delay(150)
            isNotifying = false
        }
    }
    
    val animatedWidth by animateFloatAsState(
        targetValue = if (AppState.settings.triggerMode == 1) 12f else if (isNotifying) AppState.settings.triggerThickness * 2.5f else if (isDragging) AppState.settings.triggerThickness * 1.5f else AppState.settings.triggerThickness
    )
    val animatedHeight by animateFloatAsState(
        targetValue = if (AppState.settings.triggerMode == 1) 300f else if (isNotifying) AppState.settings.triggerHeight * 1.2f else if (isDragging) AppState.settings.triggerHeight * 1.1f else AppState.settings.triggerHeight
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = if (AppState.settings.triggerMode == 1) 0f else if (isNotifying) 1f else if (isDragging) 1f else 0.7f
    )
    val notifyGlowAlpha by animateFloatAsState(targetValue = if (isNotifying) 0.6f else 0f)

    Box(
        modifier = Modifier
        .width(animatedWidth.dp)
        .fillMaxHeight(if (AppState.settings.triggerMode == 1) 1f else 0f)
        .height(if (AppState.settings.triggerMode == 1) 0.dp else animatedHeight.dp)
        .clip(RoundedCornerShape(animatedWidth.dp / 2))
        .background(
            if (AppState.settings.triggerGradientColor != null) 
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(AppState.settings.triggerColor.copy(alpha = animatedAlpha), AppState.settings.triggerGradientColor!!.copy(alpha = animatedAlpha))
                )
            else 
                androidx.compose.ui.graphics.SolidColor(AppState.settings.triggerColor.copy(alpha = animatedAlpha))
        )
        .drawWithContent {
            drawContent()
            if (notifyGlowAlpha > 0f) {
                drawRect(color = Color.White.copy(alpha = notifyGlowAlpha))
            }
        }
        .pointerInput(AppState.settings.triggerAction) {
            detectTapGestures(
                onDoubleTap = { if (AppState.settings.triggerAction == 2) { haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress); AppState.lastTriggeredSide = if (isRight) 1 else 0; AppState.sidebarVisible = true } },
                onTap = { if (AppState.settings.triggerAction == 1) { haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress); AppState.lastTriggeredSide = if (isRight) 1 else 0; AppState.sidebarVisible = true } }
            )
        }
        .pointerInput(isRight, AppState.settings.triggerAction) {
            detectDragGestures(
                onDragStart = { 
                    isDragging = true 
                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                },
                onDragEnd = { 
                    isDragging = false 
                },
                onDragCancel = { 
                    isDragging = false 
                }
            ) { change, dragAmount ->
                change.consume()
                if (AppState.settings.triggerAction == 0) {
                    val dx = if (isRight) -with(density) { dragAmount.x.toDp().value } else with(density) { dragAmount.x.toDp().value }
                    val dy = with(density) { dragAmount.y.toDp().value }
                    if (dx > 5 && kotlin.math.abs(dx) > kotlin.math.abs(dy) && !AppState.sidebarVisible) {
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        AppState.lastTriggeredSide = if (isRight) 1 else 0
                        AppState.sidebarVisible = true
                    }
                }
            }
        }
        .pointerInput(isRight, AppState.settings.triggerMode) {
            if (AppState.settings.triggerMode == 0) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { 
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
                    
                    if (isRight && dx < -50) {
                        AppState.settings = AppState.settings.copy(edgePosition = 0)
                    } else if (!isRight && dx > 50) {
                        AppState.settings = AppState.settings.copy(edgePosition = 1)
                    }
                }
            }
        }
    )
}

@Composable
fun SidebarComponent(onAppClick: (AppInfo, Boolean) -> Unit, onToggleHud: () -> Unit, isHudVisible: Boolean, onExit: () -> Unit) {
    val context = LocalContext.current
    var currentTab by remember { mutableIntStateOf(0) }
    
    val topStartRadius = if (AppState.lastTriggeredSide == 1) 32.dp else 0.dp
    val bottomStartRadius = if (AppState.lastTriggeredSide == 1) 32.dp else 0.dp
    val topEndRadius = if (AppState.lastTriggeredSide == 0) 32.dp else 0.dp
    val bottomEndRadius = if (AppState.lastTriggeredSide == 0) 32.dp else 0.dp
    val shape = RoundedCornerShape(topStartRadius, topEndRadius, bottomEndRadius, bottomStartRadius)

    androidx.compose.animation.AnimatedVisibility(
        visible = AppState.sidebarVisible,
        enter = androidx.compose.animation.fadeIn(),
        exit = androidx.compose.animation.fadeOut()
    ) {
        Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onTap = { AppState.sidebarVisible = false }) }) {
            androidx.compose.animation.AnimatedVisibility(
                visible = AppState.sidebarVisible,
                modifier = Modifier.align(if (AppState.lastTriggeredSide == 1) Alignment.CenterEnd else Alignment.CenterStart),
                enter = androidx.compose.animation.slideInHorizontally(animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.7f, stiffness = 400f), initialOffsetX = { if (AppState.lastTriggeredSide == 1) it else -it }),
                exit = androidx.compose.animation.slideOutHorizontally(targetOffsetX = { if (AppState.lastTriggeredSide == 1) it else -it })
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.9f)
                        .width(360.dp)
                        .clip(shape)
                        .background(androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(Obsidian.copy(alpha = AppState.settings.sidebarOpacity), SurfaceDark.copy(alpha = AppState.settings.sidebarOpacity))
                        ))
                        .border(1.dp, SurfaceGlass, shape)
                        .pointerInput(Unit) { detectTapGestures { /* consume tap inside */ } }
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Header
                        Row(modifier = Modifier.fillMaxWidth().background(SurfaceGlass.copy(alpha = 0.1f)).padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Short Panel", color = GhostWhite, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                            IconButton(onClick = onExit) {
                                Icon(Icons.Rounded.PowerSettingsNew, "Close", tint = HyperPink)
                            }
                        }
                        
                        // Tabs
                        val tabs = listOf("Apps", "Tools", "Controls")
                        androidx.compose.material3.TabRow(
                            selectedTabIndex = currentTab,
                            containerColor = Color.Transparent,
                            contentColor = ElectricBlue,
                            indicator = { tabPositions ->
                                if (currentTab < tabPositions.size) {
                                    androidx.compose.material3.TabRowDefaults.Indicator(
                                        modifier = Modifier.wrapContentSize(Alignment.BottomStart).offset(x = tabPositions[currentTab].left).width(tabPositions[currentTab].width),
                                        color = ElectricBlue
                                    )
                                }
                            }
                        ) {
                            tabs.forEachIndexed { index, title ->
                                androidx.compose.material3.Tab(
                                    selected = currentTab == index,
                                    onClick = { currentTab = index },
                                    text = { Text(title, color = if (currentTab == index) ElectricBlue else SoftGray, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }
                        
                        // Content
                        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            when (currentTab) {
                                0 -> {
                                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                        val pinnedAppsList = AppState.installedApps.filter { AppState.pinnedApps.contains(it.packageName) }
                                        if (pinnedAppsList.isNotEmpty()) {
                                            Column {
                                                Text("Direct Launch", color = NeonPurple, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Spacer(modifier = Modifier.height(12.dp))
                                                pinnedAppsList.chunked(4).forEach { rowApps ->
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                                        rowApps.forEach { app ->
                                                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onAppClick(app, false) }.width(72.dp)) {
                                                                Image(bitmap = app.icon, contentDescription = app.name, modifier = Modifier.size(52.dp).clip(CircleShape))
                                                                Spacer(modifier = Modifier.height(4.dp))
                                                                Text(app.name, color = GhostWhite, fontSize = 10.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                                            }
                                                        }
                                                        val remaining = 4 - rowApps.size
                                                        for(i in 0 until remaining) { Spacer(Modifier.width(72.dp)) }
                                                    }
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                }
                                            }
                                        }
                                        
                                        val floatingAppsList = AppState.installedApps.filter { AppState.floatingApps.contains(it.packageName) }
                                        if (floatingAppsList.isNotEmpty()) {
                                            Column {
                                                Text("Floating Windows", color = ElectricBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Spacer(modifier = Modifier.height(12.dp))
                                                floatingAppsList.chunked(4).forEach { rowApps ->
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                                        rowApps.forEach { app ->
                                                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onAppClick(app, true) }.width(72.dp)) {
                                                                Box(modifier = Modifier.size(56.dp).background(SurfaceDark.copy(alpha=0.8f), RoundedCornerShape(12.dp)).padding(8.dp), contentAlignment = Alignment.Center) {
                                                                    Image(bitmap = app.icon, contentDescription = app.name, modifier = Modifier.size(40.dp))
                                                                }
                                                                Spacer(modifier = Modifier.height(4.dp))
                                                                Text(app.name, color = GhostWhite, fontSize = 10.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                                            }
                                                        }
                                                        val remaining = 4 - rowApps.size
                                                        for(i in 0 until remaining) { Spacer(Modifier.width(72.dp)) }
                                                    }
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                                1 -> {
                                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                                2 -> {
                                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                        // Sliders
                                        Column(modifier = Modifier.fillMaxWidth().background(SurfaceDark, RoundedCornerShape(16.dp)).padding(16.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Rounded.VolumeUp, "Volume", tint = GhostWhite)
                                                Spacer(modifier = Modifier.width(16.dp))
                                                val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
                                                val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).toFloat()
                                                var volume by remember { mutableFloatStateOf(audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) / maxVolume) }
                                                androidx.compose.material3.Slider(
                                                    value = volume,
                                                    onValueChange = { 
                                                        volume = it
                                                        audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, (it * maxVolume).toInt(), 0)
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = NeonPurple, activeTrackColor = NeonPurple)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Rounded.BrightnessMedium, "Brightness", tint = GhostWhite)
                                                Spacer(modifier = Modifier.width(16.dp))
                                                var brightness by remember { 
                                                    mutableFloatStateOf(
                                                        try {
                                                            android.provider.Settings.System.getInt(context.contentResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS) / 255f
                                                        } catch(e: Exception) { 0.5f }
                                                    ) 
                                                }
                                                androidx.compose.material3.Slider(
                                                    value = brightness,
                                                    onValueChange = { 
                                                        brightness = it
                                                        try {
                                                            if (android.provider.Settings.System.canWrite(context)) {
                                                                android.provider.Settings.System.putInt(context.contentResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE, android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                                                                android.provider.Settings.System.putInt(context.contentResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS, (it * 255).toInt())
                                                            } else {
                                                                val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
                                                                context.startActivity(intent)
                                                                android.widget.Toast.makeText(context, "Grant Write Settings permission", android.widget.Toast.LENGTH_SHORT).show()
                                                            }
                                                        } catch(e: Exception) {}
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = Color(0xFFFFC107), activeTrackColor = Color(0xFFFFC107))
                                                )
                                            }
                                        }
                                        
                                        // Grid
                                        if (AppState.visibleControls.isNotEmpty()) {
                                            Column {
                                                Text("Quick Toggles", color = Color(0xFFFFC107), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Spacer(modifier = Modifier.height(16.dp))
                                                AppState.visibleControls.chunked(4).forEach { rowControls ->
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
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
                                                        for(i in 0 until remaining) { Spacer(Modifier.width(64.dp)) }
                                                    }
                                                    Spacer(modifier = Modifier.height(16.dp))
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
        }
    }
}

@Composable
fun ToolItem(icon: ImageVector, title: String, subtitle: String, color: Color, onClick: () -> Unit) {
  val haptics = LocalHapticFeedback.current
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp)
      .clip(RoundedCornerShape(12.dp))
      .background(Color.White.copy(alpha = 0.05f))
      .clickable {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onClick()
      }
      .padding(16.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
    Spacer(modifier = Modifier.width(16.dp))
    Column {
      Text(title, color = GhostWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
      Text(subtitle, color = SoftGray, fontSize = 12.sp)
    }
  }
}

@Composable
fun ToolItemToggle(icon: ImageVector, title: String, subtitle: String, color: Color, checked: Boolean, onCheckedChange: () -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp)
      .clip(RoundedCornerShape(12.dp))
      .background(if (checked) color.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f))
      .clickable(onClick = onCheckedChange)
      .padding(16.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
    Spacer(modifier = Modifier.width(16.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(title, color = GhostWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
      Text(subtitle, color = SoftGray, fontSize = 12.sp)
    }
    Switch(
      checked = checked,
      onCheckedChange = { onCheckedChange() },
      colors = SwitchDefaults.colors(checkedThumbColor = color, checkedTrackColor = color.copy(alpha = 0.3f))
    )
  }
}

@Composable
fun SystemMonitorHud(windowManager: WindowManager, composeView: ComposeView) {
  val context = LocalContext.current
  var availableRam by remember { mutableStateOf("0.0") }
  var totalRam by remember { mutableStateOf("0.0") }
  var batteryTemp by remember { mutableStateOf("0°C") }
  var batteryLevel by remember { mutableStateOf("100%") }

  LaunchedEffect(Unit) {
      while (true) {
          try {
              val activityManager = context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
              val memoryInfo = android.app.ActivityManager.MemoryInfo()
              activityManager.getMemoryInfo(memoryInfo)
              val used = (memoryInfo.totalMem - memoryInfo.availMem) / (1024 * 1024 * 1024f)
              val total = memoryInfo.totalMem / (1024 * 1024 * 1024f)
              availableRam = String.format("%.1f", used)
              totalRam = String.format("%.1f", total)
              
              val batteryIntent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
              if (batteryIntent != null) {
                  val temp = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, 0)
                  batteryTemp = "${temp / 10f}°C"
                  
                  val level = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
                  val scale = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
                  batteryLevel = "${(level * 100 / scale.toFloat()).toInt()}%"
              }
          } catch(e: Exception) {}
          kotlinx.coroutines.delay(2000)
      }
  }

  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(16.dp))
      .background(Color.DarkGray.copy(alpha = 0.95f))
      .border(1.dp, Color(0xFFFFC107).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
      .pointerInput(Unit) {
        detectDragGestures { change, dragAmount ->
          change.consume()
          val params = composeView.layoutParams as WindowManager.LayoutParams
          params.x += dragAmount.x.toInt()
          params.y += dragAmount.y.toInt()
          windowManager.updateViewLayout(composeView, params)
        }
      }
      .padding(16.dp)
  ) {
    Column {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Memory, null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("RAM: $availableRam / $totalRam GB", color = GhostWhite, fontSize = 12.sp)
      }
      Spacer(modifier = Modifier.height(4.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Speed, null, tint = HyperPink, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Battery Temp: $batteryTemp", color = GhostWhite, fontSize = 12.sp)
      }
      Spacer(modifier = Modifier.height(4.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.BatteryChargingFull, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Battery: $batteryLevel", color = GhostWhite, fontSize = 12.sp)
      }
    }
  }
}

@Composable
fun ToggleItem(icon: ImageVector, label: String, initialActive: Boolean, activeColor: Color, onToggle: (String) -> Unit) {
  val haptics = LocalHapticFeedback.current
  var active by remember { mutableStateOf(initialActive) }
  val color by animateColorAsState(if (active) activeColor else SurfaceDark)
  val iconTint by animateColorAsState(if (active) Obsidian else GhostWhite)

  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Box(
      modifier = Modifier
        .size(56.dp)
        .clip(CircleShape)
        .background(color)
        .clickable { 
          active = !active
          haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          onToggle("$label ${if (active) "Enabled" else "Disabled"}")
        },
      contentAlignment = Alignment.Center
    ) {
      Icon(icon, contentDescription = label, tint = iconTint)
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(label, color = SoftGray, fontSize = 12.sp)
  }
}

@Composable
fun PremiumSliderItem(icon: ImageVector, label: String, value: Float, onValueChange: (Float) -> Unit, color: Color) {
  var isInteracting by remember { mutableStateOf(false) }
  var internalValue by remember { mutableFloatStateOf(value) }
  
  LaunchedEffect(value) {
      if (!isInteracting) {
          internalValue = value
      }
  }

  val animatedValue by animateFloatAsState(targetValue = internalValue, label = "sliderAnimation")
  
  Column {
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = SoftGray, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = GhostWhite, fontSize = 12.sp)
      }
      Text("${(animatedValue * 100).toInt()}%", color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
    Spacer(modifier = Modifier.height(8.dp))
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(36.dp)
        .clip(RoundedCornerShape(18.dp))
        .background(Obsidian)
        .pointerInput(Unit) {
          detectDragGestures(
              onDragStart = { isInteracting = true },
              onDragEnd = { isInteracting = false },
              onDragCancel = { isInteracting = false }
          ) { change, dragAmount ->
            change.consume()
            internalValue = (internalValue + dragAmount.x / size.width).coerceIn(0f, 1f)
            onValueChange(internalValue)
          }
        }
        .pointerInput(Unit) {
          detectTapGestures(
            onPress = {
                isInteracting = true
                tryAwaitRelease()
                isInteracting = false
            },
            onTap = { offset ->
              internalValue = (offset.x / size.width).coerceIn(0f, 1f)
              onValueChange(internalValue)
            }
          )
        }
    ) {
      Box(
        modifier = Modifier
          .fillMaxHeight()
          .fillMaxWidth(animatedValue.coerceAtLeast(0.001f))
          .clip(RoundedCornerShape(18.dp))
          .background(color)
      )
    }
  }
}

@Composable
fun FloatingWindowComponent(
  windowData: FloatingWindowData,
  windowManager: WindowManager,
  composeView: ComposeView,
  onUpdate: (FloatingWindowData) -> Unit,
  onClose: () -> Unit
) {
  val context = LocalContext.current
  val density = LocalDensity.current
  var isDragging by remember { mutableStateOf(false) }
  
  if (windowData.isMinimized) {
      Box(
          modifier = Modifier
              .size(60.dp)
              .alpha(windowData.opacity)
              .clip(CircleShape)
              .background(SurfaceDark)
              .border(2.dp, windowData.color, CircleShape)
              .pointerInput(Unit) {
                  detectDragGestures(
                      onDragStart = { 
                          isDragging = true 
                          AppState.isDraggingBubble = true
                      },
                      onDragEnd = { 
                          isDragging = false 
                          AppState.isDraggingBubble = false
                          val params = composeView.layoutParams as WindowManager.LayoutParams
                          val screenHeight = context.resources.displayMetrics.heightPixels
                          val screenWidth = context.resources.displayMetrics.widthPixels
                          // The close icon is at bottom center. params.x and y are from top left.
                          // Icon size is 60dp. screenWidth/2 is center.
                          if (params.y > screenHeight - 400 && params.x > screenWidth / 2 - 200 && params.x < screenWidth / 2 + 200) {
                              onClose()
                          }
                      },
                      onDragCancel = { 
                          isDragging = false 
                          AppState.isDraggingBubble = false
                      }
                  ) { change, dragAmount ->
                      change.consume()
                      val params = composeView.layoutParams as WindowManager.LayoutParams
                      params.x += dragAmount.x.toInt()
                      params.y += dragAmount.y.toInt()
                      windowManager.updateViewLayout(composeView, params)
                      
                      onUpdate(windowData.copy(offset = IntOffset(params.x, params.y)))
                  }
              }
              .clickable { onUpdate(windowData.copy(isMinimized = false)) },
          contentAlignment = Alignment.Center
      ) {
          if (windowData.appIcon != null) {
              Image(bitmap = windowData.appIcon, contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape))
          } else if (windowData.icon != null) {
              Icon(windowData.icon, null, tint = windowData.color, modifier = Modifier.size(40.dp))
          }
      }
      return
  }

  Box(
    modifier = Modifier
      .size(
        width = with(density) { windowData.size.width.toDp() },
        height = with(density) { windowData.size.height.toDp() }
      )
      .alpha(windowData.opacity)
      .clip(RoundedCornerShape(16.dp))
      .background(Brush.verticalGradient(listOf(Color.White, SurfaceDark)))
      .border(1.dp, if (isDragging) windowData.color else Color.White.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
      .shadow(if (isDragging) 32.dp else 16.dp, RoundedCornerShape(16.dp))
  ) {
    if (windowData.isLocked) {
      Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)).blur(16.dp).zIndex(20f))
      Column(
        modifier = Modifier.fillMaxSize().zIndex(21f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Icon(Icons.Rounded.Fingerprint, null, tint = windowData.color, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Biometric Lock", color = GhostWhite, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Button(
          onClick = { onUpdate(windowData.copy(isLocked = false)) },
          colors = ButtonDefaults.buttonColors(containerColor = windowData.color.copy(alpha = 0.2f))
        ) {
          Text("Simulate Unlock", color = windowData.color)
        }
      }
    }

    Column(modifier = Modifier.fillMaxSize()) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .background(Color.Black.copy(alpha = 0.5f))
          .pointerInput(Unit) {
            detectDragGestures(
              onDragStart = { isDragging = true },
              onDragEnd = { isDragging = false },
              onDragCancel = { isDragging = false }
            ) { change, dragAmount ->
              change.consume()
              val params = composeView.layoutParams as WindowManager.LayoutParams
              params.x += dragAmount.x.toInt()
              params.y += dragAmount.y.toInt()
              windowManager.updateViewLayout(composeView, params)
              
              val newX = params.x
              val newY = params.y
              onUpdate(windowData.copy(offset = IntOffset(newX, newY)))
            }
          }
          .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (windowData.appIcon != null) {
            Image(bitmap = windowData.appIcon, contentDescription = null, modifier = Modifier.size(20.dp))
        } else if (windowData.icon != null) {
            Icon(windowData.icon, null, tint = windowData.color, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(windowData.title, color = GhostWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1)
        
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Rounded.Remove, "Minimize", tint = SoftGray, modifier = Modifier.size(20.dp).clickable { onUpdate(windowData.copy(isMinimized = true)) })
          Icon(Icons.Rounded.ViewAgenda, "Split Screen", tint = ElectricBlue, modifier = Modifier.size(18.dp).clickable {
              val intent = context.packageManager.getLaunchIntentForPackage(windowData.packageName)
              if (intent != null) {
                  intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                  android.widget.Toast.makeText(context, "Launching in Split Screen...", android.widget.Toast.LENGTH_SHORT).show()
                  if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                      try {
                          context.startActivity(intent, android.app.ActivityOptions.makeBasic().toBundle())
                      } catch (e: Exception) {
                          context.startActivity(intent)
                      }
                  } else {
                      context.startActivity(intent)
                  }
              } else {
                  android.widget.Toast.makeText(context, "Cannot launch app", android.widget.Toast.LENGTH_SHORT).show()
              }
          })
          Icon(Icons.Rounded.Close, "Close", tint = HyperPink, modifier = Modifier.size(20.dp).clickable(onClick = onClose))
        }
      }
      
      Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (windowData.title == "Live Screen OCR") {
            var isScanning by remember { mutableStateOf(false) }
            var scannedText by remember { mutableStateOf("Draw a box on the screen to extract text.") }
            Icon(Icons.Rounded.DocumentScanner, null, tint = HyperPink, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("OCR Scanner Active", color = GhostWhite, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(scannedText, color = SoftGray, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = { 
                isScanning = true
                android.widget.Toast.makeText(context, "Scanning screen for text...", android.widget.Toast.LENGTH_SHORT).show() 
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    isScanning = false
                    scannedText = "Found 3 words:\n'Hello World Test'\n\nText copied to clipboard!"
                    val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("ocr", "Hello World Test"))
                }, 2000)
            }, colors = ButtonDefaults.buttonColors(containerColor = HyperPink), enabled = !isScanning) { 
                if(isScanning) CircularProgressIndicator(color=Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp) else Text("Start Scan", color = Color.White) 
            }
        } else if (windowData.title == "Universal Clipboard") {
            var newItemText by remember { mutableStateOf("") }
            val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            
            LaunchedEffect(Unit) {
                if (clipboardManager.hasPrimaryClip()) {
                    val clip = clipboardManager.primaryClip
                    if (clip != null && clip.itemCount > 0) {
                        val text = clip.getItemAt(0).text?.toString() ?: ""
                        if (text.isNotEmpty() && !AppState.clipboardHistory.contains(text)) {
                            AppState.clipboardHistory.add(0, text)
                        }
                    }
                }
            }
            Text("Clipboard History", color = GhostWhite, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = newItemText,
                onValueChange = { newItemText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Add new clip...", color = SoftGray) },
                textStyle = androidx.compose.ui.text.TextStyle(color = GhostWhite),
                trailingIcon = {
                    IconButton(onClick = {
                        if (newItemText.isNotBlank()) {
                            AppState.clipboardHistory.add(0, newItemText)
                            clipboardManager.setPrimaryClip(android.content.ClipData.newPlainText("text", newItemText))
                            newItemText = ""
                            AppState.saveSettings(context)
                        }
                    }) { Icon(Icons.Rounded.Add, "Add", tint = NeonPurple) }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonPurple,
                    unfocusedBorderColor = SoftGray
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (AppState.clipboardHistory.isEmpty()) {
                    item { Text("No items copied yet", color = SoftGray, fontSize = 12.sp) }
                } else {
                    items(AppState.clipboardHistory.size) { index ->
                        val clipText = AppState.clipboardHistory[index]
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceDark).padding(12.dp).clickable {
                            clipboardManager.setPrimaryClip(android.content.ClipData.newPlainText("text", clipText))
                            android.widget.Toast.makeText(context, "Copied!", android.widget.Toast.LENGTH_SHORT).show()
                        }, verticalAlignment = Alignment.CenterVertically) {
                            Text(clipText, color = SoftGray, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { 
                                AppState.clipboardHistory.removeAt(index) 
                                AppState.saveSettings(context)
                            }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Rounded.Delete, "Delete", tint = Color.Red.copy(alpha=0.7f))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { 
                AppState.clipboardHistory.clear()
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    clipboardManager.clearPrimaryClip()
                }
                AppState.saveSettings(context)
                android.widget.Toast.makeText(context, "Clipboard cleared", android.widget.Toast.LENGTH_SHORT).show() 
            }, colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)) { Text("Clear All", color = Color.White) }
        } else if (windowData.title == "Macro & Auto-Clicker") {
            var isRecording by remember { mutableStateOf(false) }
            var isPlaying by remember { mutableStateOf(false) }
            Icon(Icons.Rounded.TouchApp, null, tint = ElectricBlue, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Auto-Clicker Ready", color = GhostWhite, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(if (isRecording) "Recording clicks..." else if (isPlaying) "Playing macro..." else "Idle", color = SoftGray, fontSize = 12.sp)
            Spacer(modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { 
                        if (!isRecording) {
                            isRecording = true
                            android.widget.Toast.makeText(context, "Recording started...", android.widget.Toast.LENGTH_SHORT).show() 
                        } else {
                            isRecording = false
                        }
                    }, 
                    colors = ButtonDefaults.buttonColors(containerColor = if (isRecording) Color.DarkGray else Color.Red)
                ) { Text(if (isRecording) "Stop" else "Record", color = Color.White) }
                Button(
                    onClick = { 
                        if (!isPlaying && !isRecording) {
                            isPlaying = true
                            android.widget.Toast.makeText(context, "Playing macro...", android.widget.Toast.LENGTH_SHORT).show() 
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ isPlaying = false }, 3000)
                        }
                    }, 
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    enabled = !isRecording
                ) { if (isPlaying) CircularProgressIndicator(color=Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp) else Text("Play", color = Color.White) }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
            Text("Floating Content for ${windowData.title}", color = SoftGray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(modifier = Modifier.weight(1f))
        }
      }
    }

    Box(
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .size(48.dp) // Larger touch target
        .pointerInput(Unit) {
          detectDragGestures { change, dragAmount ->
            change.consume()
            val newWidth = kotlin.math.max(200, windowData.size.width + dragAmount.x.toInt())
            val newHeight = kotlin.math.max(200, windowData.size.height + dragAmount.y.toInt())
            onUpdate(windowData.copy(size = IntSize(newWidth, newHeight)))
          }
        }
    ) {
      Box(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(8.dp)
          .size(24.dp)
          .background(windowData.color.copy(alpha = 0.8f), RoundedCornerShape(topStart = 16.dp, bottomEnd = 8.dp, bottomStart = 4.dp, topEnd = 4.dp))
      ) {
         Icon(
           Icons.Rounded.OpenInFull,
           contentDescription = "Resize",
           tint = Color.White,
           modifier = Modifier.size(16.dp).align(Alignment.Center)
         )
      }
    }
  }
}

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    
    val tWelcome = if (AppState.language == "hi") "Short Panel Pro में आपका स्वागत है" else "Welcome to Short Panel Pro"
    val tDesc = if (AppState.language == "hi") "आपका स्मार्ट साइडबार और फ्लोटिंग विंडो मैनेजर" else "Your smart sidebar and floating window manager"
    val tLang = if (AppState.language == "hi") "भाषा चुनें" else "Choose Language"
    val tNext = if (AppState.language == "hi") "अगला" else "Next"
    val tFinish = if (AppState.language == "hi") "शुरू करें" else "Get Started"
    
    val tTut1 = if (AppState.language == "hi") "स्क्रीन के किनारे से स्वाइप करें\nसाइडबार खोलने के लिए" else "Swipe from the screen edge\nto open the sidebar"
    val tTut2 = if (AppState.language == "hi") "किसी भी ऐप पर टैप करें\nउसे फ्लोटिंग विंडो में खोलने के लिए" else "Tap on any app\nto open it in a floating window"
    val tTut3 = if (AppState.language == "hi") "कंट्रोल्स और टूल्स का इस्तेमाल करें\nतेजी से काम करने के लिए" else "Use controls and tools\nfor faster actions"

    Box(modifier = Modifier.fillMaxSize().background(Obsidian), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            if (step == 0) {
                Icon(Icons.Rounded.Language, null, tint = ElectricBlue, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(24.dp))
                Text(tLang, color = GhostWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = { AppState.language = "en" },
                        colors = ButtonDefaults.buttonColors(containerColor = if (AppState.language == "en") ElectricBlue else SurfaceDark)
                    ) {
                        Text("English", color = GhostWhite)
                    }
                    Button(
                        onClick = { AppState.language = "hi" },
                        colors = ButtonDefaults.buttonColors(containerColor = if (AppState.language == "hi") ElectricBlue else SurfaceDark)
                    ) {
                        Text("हिंदी", color = GhostWhite)
                    }
                }
            } else if (step == 1) {
                androidx.compose.animation.AnimatedContent(targetState = step, label = "tutorial") { currentStep ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.SwipeLeft, null, tint = HyperPink, modifier = Modifier.size(80.dp))
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(tWelcome, color = GhostWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(tTut1, color = SoftGray, fontSize = 16.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            } else if (step == 2) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.ViewAgenda, null, tint = NeonPurple, modifier = Modifier.size(80.dp))
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(tTut2, color = GhostWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            } else if (step == 3) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.SettingsEthernet, null, tint = ElectricBlue, modifier = Modifier.size(80.dp))
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(tTut3, color = GhostWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = {
                    if (step < 3) step++ else onComplete()
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                modifier = Modifier.fillMaxWidth(0.8f).height(50.dp)
            ) {
                Text(if (step < 3) tNext else tFinish, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}


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
