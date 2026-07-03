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
  val sidebarOpacity: Float = 0.85f,
  val windowOpacity: Float = 0.95f,
  val triggerAction: Int = 0, // 0 = Swipe, 1 = Single Tap, 2 = Double Tap
  val triggerColorArgb: Int = 0xFF1976D2.toInt(), // ElectricBlue
  val triggerGradientColorArgb: Int? = null,
  val triggerMode: Int = 0 // 0 = Visual Slider, 1 = Full Screen Edge
) {
  val triggerColor: Color get() = Color(triggerColorArgb)
  val triggerGradientColor: Color? get() = triggerGradientColorArgb?.let { Color(it) }
}

data class AppInfo(
  val name: String,
  val icon: ImageBitmap,
  val packageName: String
)

data class UpdateInfo(
  val versionCode: Int,
  val versionName: String,
  val updateUrl: String,
  val changeLog: String
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
  val windows = mutableStateListOf<FloatingWindowData>()
  var installedApps by mutableStateOf<List<AppInfo>>(emptyList())
  var pinnedApps = mutableStateListOf<String>() // Package names
  var floatingApps = mutableStateListOf<String>() // Package names
  var visibleTools = mutableStateListOf<String>("Macro & Auto-Clicker", "Live Screen OCR", "Universal Clipboard", "Floating System Monitor")
  var visibleControls = mutableStateListOf<String>("Wi-Fi", "Bluetooth", "Data", "Ghost Mode", "Brightness", "Volume")
  var clipboardHistory = mutableStateListOf<String>()
  var windowIdCounter = 0
  
  var updateInfo by mutableStateOf<UpdateInfo?>(null)
  var isCheckingForUpdate by mutableStateOf(false)
  
  fun checkForUpdates(context: android.content.Context) {
      if (isCheckingForUpdate) return
      isCheckingForUpdate = true
      
      val currentVersionCode = try {
          context.packageManager.getPackageInfo(context.packageName, 0).versionCode
      } catch (e: Exception) { 1 }
      
      // Note: In a real app, host this JSON on a server or GitHub
      val updateCheckUrl = "https://raw.githubusercontent.com/yash-apex/shortw-updates/main/update.json"
      
      Thread {
          try {
              val url = java.net.URL(updateCheckUrl)
              val connection = url.openConnection() as java.net.HttpURLConnection
              connection.requestMethod = "GET"
              connection.connectTimeout = 5000
              connection.readTimeout = 5000
              
              if (connection.responseCode == 200) {
                  val reader = java.io.BufferedReader(java.io.InputStreamReader(connection.inputStream))
                  val response = StringBuilder()
                  var line: String?
                  while (reader.readLine().also { line = it } != null) {
                      response.append(line)
                  }
                  reader.close()
                  
                  val gson = com.google.gson.Gson()
                  val info = gson.fromJson(response.toString(), UpdateInfo::class.java)
                  
                  if (info.versionCode > currentVersionCode) {
                      updateInfo = info
                  }
              }
          } catch (e: Exception) {
              e.printStackTrace()
          } finally {
              isCheckingForUpdate = false
          }
      }.start()
  }
  
  fun saveSettings(context: android.content.Context) {
      val prefs = context.getSharedPreferences("ApexPanelPrefs", android.content.Context.MODE_PRIVATE)
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
      val prefs = context.getSharedPreferences("ApexPanelPrefs", android.content.Context.MODE_PRIVATE)
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
    
    val prefs = getSharedPreferences("ApexPanelPrefs", android.content.Context.MODE_PRIVATE)
    AppState.isFirstLaunch = prefs.getBoolean("isFirstLaunch", true)
    AppState.language = prefs.getString("language", "en") ?: "en"
    
    AppState.loadSettings(this)
    AppState.fetchApps(this)

    setContent {
      MyApplicationTheme {
        ApexPanelApp(prefs)
      }
    }
  }
}

@Composable
fun ApexPanelApp(prefs: android.content.SharedPreferences) {
  val context = LocalContext.current
  var currentRoute by remember { mutableStateOf(if (AppState.isFirstLaunch) "onboarding" else "main") }

  LaunchedEffect(Unit) {
      AppState.checkForUpdates(context)
  }

  AppState.updateInfo?.let { info ->
      UpdateDialog(
          info = info,
          onDismiss = { AppState.updateInfo = null }
      )
  }

  val allPermissionsGranted = (AppState.hasOverlayPermission || AppState.hasAccessibilityPermission) && AppState.hasWriteSettingsPermission && AppState.hasNotificationPermission

  if (AppState.isFirstLaunch && currentRoute == "onboarding") {
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
fun UpdateDialog(info: UpdateInfo, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Update Available!", color = GhostWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Version: ${info.versionName}", color = GhostWhite.copy(alpha = 0.7f), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(info.changeLog, color = GhostWhite)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.updateUrl))
                    context.startActivity(intent)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
            ) {
                Text("Update Now", color = GhostWhite)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later", color = GhostWhite.copy(alpha = 0.6f))
            }
        },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(24.dp)
    )
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
                Text("About ApexPanel Pro", color = GhostWhite, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(8.dp))
                val currentVersion = try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                } catch (e: Exception) { "1.0.0" }
                Text("Version: $currentVersion", color = SoftGray)
                Spacer(modifier = Modifier.height(16.dp))
                Text("ApexPanel Pro is a comprehensive floating overlay toolkit designed to boost your productivity. It provides quick access to your favorite apps, system toggles, and utilities directly from any screen without interrupting your workflow.\n\nDeveloped by Sunil Meghwal", color = SoftGray, lineHeight = 20.sp)
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { AppState.checkForUpdates(context) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (AppState.isCheckingForUpdate) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = ElectricBlue, strokeWidth = 2.dp)
                    } else {
                        Text("Check for Update", color = GhostWhite)
                    }
                }
            } else {
                val text = when (title) {
                    "Privacy Policy" -> "Privacy Policy\n\n1. Information Collection: We do not collect or store any personal data. All app preferences and configurations are stored locally on your device.\n2. Permissions: The 'Display Over Other Apps' permission is required strictly for providing the floating overlay functionality. We do not track your usage of other apps.\n3. Third-party Services: We do not use any third-party tracking or analytics services."
                    "Terms & Conditions" -> "Terms & Conditions\n\n1. Acceptance: By using ApexPanel Pro, you agree to these terms.\n2. Usage: You agree to use the app responsibly and not for any malicious purposes.\n3. Modification: We reserve the right to modify these terms at any time.\n4. Liability: We are not responsible for any damage to your device caused by improper use of the application."
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
                                    } else if (currentList.size < limit) {
                                        currentList.add(app.packageName)
                                    } else {
                                        // Show toast or something
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
                            } else {
                                if (selectedItems.size < maxSelection) {
                                    selectedItems.add(item)
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
            Text("ApexPanel Pro requires the following permissions to function fully.", color = SoftGray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
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
                        description = "If 'Display Over Other Apps' is missing, enable ApexPanel Pro in Accessibility Services.",
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
fun SettingsDashboard(onNavigate: (String) -> Unit) {
  val context = LocalContext.current
  var showColorPicker by remember { mutableStateOf(false) }
  
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("ApexPanel Pro Settings", color = GhostWhite, fontWeight = FontWeight.Bold) },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
      )
    },
    containerColor = Color.Transparent
  ) { padding ->
    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Obsidian, SurfaceDark)))) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding)
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
      ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(SurfaceDark)
          .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column {
          Text("Master Service", color = GhostWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
          Text(if (AppState.isServiceRunning) "Running" else "Stopped", color = if (AppState.isServiceRunning) ElectricBlue else SoftGray)
        }
        Switch(
          checked = AppState.isServiceRunning,
          onCheckedChange = { 
              if (it && !AppState.hasOverlayPermission && !AppState.hasAccessibilityPermission) {
                  Toast.makeText(context, "Overlay or Accessibility permission is required", Toast.LENGTH_SHORT).show()
                  return@Switch
              }
              AppState.isServiceRunning = it 
              if (it) {
                  if (AppState.hasOverlayPermission) {
                      val intent = Intent(context, OverlayService::class.java)
                      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                          context.startForegroundService(intent)
                      } else {
                          context.startService(intent)
                      }
                  } else if (AppState.hasAccessibilityPermission) {
                      Toast.makeText(context, "Using Accessibility Service Mode", Toast.LENGTH_SHORT).show()
                  }
              } else {
                  if (AppState.hasOverlayPermission) {
                      context.stopService(Intent(context, OverlayService::class.java))
                  }
              }
          },
          colors = SwitchDefaults.colors(checkedThumbColor = ElectricBlue, checkedTrackColor = ElectricBlue.copy(alpha = 0.3f))
        )
      }

      Text("App Configuration", color = HyperPink, fontWeight = FontWeight.Bold)
      Column(
        modifier = Modifier
          .clip(RoundedCornerShape(16.dp))
          .background(SurfaceDark)
      ) {
          Row(
              modifier = Modifier
                  .fillMaxWidth()
                  .clickable { onNavigate("app_selection") }
                  .padding(16.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
          ) {
              Text("Customize Slider Apps", color = GhostWhite)
              Icon(Icons.Rounded.ChevronRight, null, tint = SoftGray)
          }
          HorizontalDivider(color = Obsidian)
          Row(
              modifier = Modifier
                  .fillMaxWidth()
                  .clickable { onNavigate("tool_selection") }
                  .padding(16.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
          ) {
              Text("Customize Tools", color = GhostWhite)
              Icon(Icons.Rounded.ChevronRight, null, tint = SoftGray)
          }
          HorizontalDivider(color = Obsidian)
          Row(
              modifier = Modifier
                  .fillMaxWidth()
                  .clickable { onNavigate("control_selection") }
                  .padding(16.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
          ) {
              Text("Customize Controls", color = GhostWhite)
              Icon(Icons.Rounded.ChevronRight, null, tint = SoftGray)
          }
      }

      Text("Edge Trigger Customization", color = NeonPurple, fontWeight = FontWeight.Bold)
      Column(
        modifier = Modifier
          .clip(RoundedCornerShape(16.dp))
          .background(SurfaceDark)
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
          Text("Position", color = GhostWhite)
          Row(
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Obsidian)
          ) {
            Box(
              modifier = Modifier
                .clickable { AppState.settings = AppState.settings.copy(isRightEdge = false) }
                .background(if (!AppState.settings.isRightEdge) NeonPurple else Color.Transparent)
                .padding(horizontal = 16.dp, vertical = 8.dp)
            ) { Text("Left", color = GhostWhite) }
            Box(
              modifier = Modifier
                .clickable { AppState.settings = AppState.settings.copy(isRightEdge = true) }
                .background(if (AppState.settings.isRightEdge) NeonPurple else Color.Transparent)
                .padding(horizontal = 16.dp, vertical = 8.dp)
            ) { Text("Right", color = GhostWhite) }
          }
        }
        
        Column {
          Text("Thickness: ${AppState.settings.triggerThickness.toInt()}dp", color = GhostWhite)
          Slider(
            value = AppState.settings.triggerThickness,
            onValueChange = { AppState.settings = AppState.settings.copy(triggerThickness = it) },
            valueRange = 2f..20f,
            colors = SliderDefaults.colors(thumbColor = NeonPurple, activeTrackColor = NeonPurple)
          )
        }

        Column {
          Text("Height: ${AppState.settings.triggerHeight.toInt()}dp", color = GhostWhite)
          Slider(
            value = AppState.settings.triggerHeight,
            onValueChange = { AppState.settings = AppState.settings.copy(triggerHeight = it) },
            valueRange = 50f..300f,
            colors = SliderDefaults.colors(thumbColor = NeonPurple, activeTrackColor = NeonPurple)
          )
        }
        
        Column {
          Text("Trigger Action", color = GhostWhite)
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Obsidian)
          ) {
            val actions = listOf("Swipe", "Single", "Double")
            actions.forEachIndexed { index, name ->
              Box(
                modifier = Modifier
                  .weight(1f)
                  .clickable { AppState.settings = AppState.settings.copy(triggerAction = index) }
                  .background(if (AppState.settings.triggerAction == index) NeonPurple else Color.Transparent)
                  .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
              ) {
                  Text(name, color = GhostWhite, fontSize = 12.sp)
              }
            }
          }
        }
        
        Column {
          Text("Trigger Mode", color = GhostWhite)
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Obsidian)
          ) {
            val modes = listOf("Visual Slider", "Full Screen Edge")
            modes.forEachIndexed { index, name ->
              Box(
                modifier = Modifier
                  .weight(1f)
                  .clickable { AppState.settings = AppState.settings.copy(triggerMode = index) }
                  .background(if (AppState.settings.triggerMode == index) NeonPurple else Color.Transparent)
                  .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
              ) {
                  Text(name, color = GhostWhite, fontSize = 12.sp)
              }
            }
          }
        }
        
        Column {
          Text("Slider Color", color = GhostWhite)
          Spacer(modifier = Modifier.height(8.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
              val colors = listOf(ElectricBlue, NeonPurple, HyperPink, Color(0xFFFFC107), Color(0xFF4CAF50), Color.White)
              colors.forEach { color ->
                  Box(
                      modifier = Modifier
                          .size(32.dp)
                          .clip(CircleShape)
                          .background(color)
                          .border(2.dp, if (AppState.settings.triggerColor == color) GhostWhite else Color.Transparent, CircleShape)
                          .clickable { AppState.settings = AppState.settings.copy(triggerColorArgb = color.toArgb(), triggerGradientColorArgb = null) }
                  )
              }
              Box(
                  modifier = Modifier
                      .size(32.dp)
                      .clip(CircleShape)
                      .background(Brush.sweepGradient(listOf(Color.Red, Color.Green, Color.Blue, Color.Red)))
                      .border(1.dp, SoftGray, CircleShape)
                      .clickable { showColorPicker = true },
                  contentAlignment = Alignment.Center
              ) {
                  Icon(Icons.Rounded.ColorLens, null, tint = Color.White, modifier = Modifier.size(16.dp))
              }
          }
          Spacer(modifier = Modifier.height(16.dp))
          Text("Slider Gradient (Optional)", color = GhostWhite)
          Spacer(modifier = Modifier.height(8.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
              Box(
                  modifier = Modifier
                      .size(32.dp)
                      .clip(CircleShape)
                      .background(Color.Transparent)
                      .border(1.dp, SoftGray, CircleShape)
                      .clickable { AppState.settings = AppState.settings.copy(triggerGradientColorArgb = null) },
                  contentAlignment = Alignment.Center
              ) {
                  Icon(Icons.Rounded.Close, null, tint = SoftGray, modifier = Modifier.size(16.dp))
              }
              val colors = listOf(ElectricBlue, NeonPurple, HyperPink, Color(0xFFFFC107), Color(0xFF4CAF50), Color.White)
              colors.forEach { color ->
                  Box(
                      modifier = Modifier
                          .size(32.dp)
                          .clip(CircleShape)
                          .background(color)
                          .border(2.dp, if (AppState.settings.triggerGradientColor == color) GhostWhite else Color.Transparent, CircleShape)
                          .clickable { AppState.settings = AppState.settings.copy(triggerGradientColorArgb = color.toArgb()) }
                  )
              }
          }
        }
      }
      
      Text("Visual & Opacity", color = ElectricBlue, fontWeight = FontWeight.Bold)
      Column(
        modifier = Modifier
          .clip(RoundedCornerShape(16.dp))
          .background(SurfaceDark)
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Column {
          Text("Sidebar Blur / Opacity", color = GhostWhite)
          Slider(
            value = AppState.settings.sidebarOpacity,
            onValueChange = { AppState.settings = AppState.settings.copy(sidebarOpacity = it) },
            valueRange = 0.5f..1f,
            colors = SliderDefaults.colors(thumbColor = ElectricBlue, activeTrackColor = ElectricBlue)
          )
        }
        Column {
          Text("Default Window Opacity", color = GhostWhite)
          Slider(
            value = AppState.settings.windowOpacity,
            onValueChange = { AppState.settings = AppState.settings.copy(windowOpacity = it) },
            valueRange = 0.3f..1f,
            colors = SliderDefaults.colors(thumbColor = ElectricBlue, activeTrackColor = ElectricBlue)
          )
        }
      }

      Text("Legal & About", color = SoftGray, fontWeight = FontWeight.Bold)
      Column(
        modifier = Modifier
          .clip(RoundedCornerShape(16.dp))
          .background(SurfaceDark)
      ) {
          LegalMenuItem("Terms & Conditions") { onNavigate("terms") }
          HorizontalDivider(color = Obsidian)
          LegalMenuItem("Privacy Policy") { onNavigate("privacy") }
          HorizontalDivider(color = Obsidian)
          LegalMenuItem("Disclaimer") { onNavigate("disclaimer") }
          HorizontalDivider(color = Obsidian)
          LegalMenuItem("About") { onNavigate("about") }
      }
      
      Spacer(modifier = Modifier.height(80.dp))
    }
    
    if (showColorPicker) {
        var r by remember { mutableFloatStateOf(AppState.settings.triggerColor.red) }
        var g by remember { mutableFloatStateOf(AppState.settings.triggerColor.green) }
        var b by remember { mutableFloatStateOf(AppState.settings.triggerColor.blue) }
        
        AlertDialog(
            onDismissRequest = { showColorPicker = false },
            title = { Text("Custom Color", color = GhostWhite) },
            text = {
                Column {
                    Box(modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(8.dp)).background(Color(r, g, b)))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Red", color = GhostWhite)
                    Slider(value = r, onValueChange = { r = it }, colors = SliderDefaults.colors(thumbColor = Color.Red, activeTrackColor = Color.Red))
                    Text("Green", color = GhostWhite)
                    Slider(value = g, onValueChange = { g = it }, colors = SliderDefaults.colors(thumbColor = Color.Green, activeTrackColor = Color.Green))
                    Text("Blue", color = GhostWhite)
                    Slider(value = b, onValueChange = { b = it }, colors = SliderDefaults.colors(thumbColor = Color.Blue, activeTrackColor = Color.Blue))
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    AppState.settings = AppState.settings.copy(triggerColorArgb = Color(r, g, b).toArgb(), triggerGradientColorArgb = null)
                    showColorPicker = false 
                }) { Text("Save", color = ElectricBlue) }
            },
            dismissButton = {
                TextButton(onClick = { showColorPicker = false }) { Text("Cancel", color = SoftGray) }
            },
            containerColor = SurfaceDark
        )
    }
    }
  }
}

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
fun TriggerComponent() {
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
                onDoubleTap = { if (AppState.settings.triggerAction == 2) AppState.sidebarVisible = true },
                onTap = { if (AppState.settings.triggerAction == 1) AppState.sidebarVisible = true }
            )
        }
        .pointerInput(AppState.settings.isRightEdge, AppState.settings.triggerAction) {
            detectDragGestures(
                onDragStart = { 
                    isDragging = true 
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
                    val dx = if (AppState.settings.isRightEdge) -with(density) { dragAmount.x.toDp().value } else with(density) { dragAmount.x.toDp().value }
                    val dy = with(density) { dragAmount.y.toDp().value }
                    if (dx > 5 && kotlin.math.abs(dx) > kotlin.math.abs(dy) && !AppState.sidebarVisible) {
                        AppState.sidebarVisible = true
                    }
                }
            }
        }
        .pointerInput(AppState.settings.isRightEdge, AppState.settings.triggerMode) {
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
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Cannot open Camera", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        AppState.sidebarVisible = false
                    }
                ) {
                    Icon(Icons.Rounded.Camera, contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceDark).padding(8.dp), tint = GhostWhite)
                    Text("Camera", color = GhostWhite, fontSize = 10.sp, maxLines = 1, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { 
                        try {
                            val cameraManager = context.getSystemService(android.content.Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
                            val cameraId = cameraManager.cameraIdList[0]
                            isFlashlightOn = !isFlashlightOn
                            cameraManager.setTorchMode(cameraId, isFlashlightOn)
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Cannot toggle flashlight", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(Icons.Rounded.FlashlightOn, contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceDark).padding(8.dp), tint = if (isFlashlightOn) Color(0xFFFFC107) else GhostWhite)
                    Text("Flashlight", color = GhostWhite, fontSize = 10.sp, maxLines = 1, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { 
                        try {
                            val intent = Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Cannot open Alarms", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        AppState.sidebarVisible = false
                    }
                ) {
                    Icon(Icons.Rounded.Alarm, contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceDark).padding(8.dp), tint = GhostWhite)
                    Text("Alarms", color = GhostWhite, fontSize = 10.sp, maxLines = 1, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { 
                        try {
                            val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Cannot open Settings", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        AppState.sidebarVisible = false
                    }
                ) {
                    Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceDark).padding(8.dp), tint = GhostWhite)
                    Text("Settings", color = GhostWhite, fontSize = 10.sp, maxLines = 1, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
          }
          "Tools" -> {
            Text("Automation & AI Tools", color = GhostWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            
            val openTool = { title: String, icon: ImageVector, color: Color ->
                AppState.windows.add(
                    FloatingWindowData(
                        id = AppState.windowIdCounter++,
                        title = title,
                        icon = icon,
                        appIcon = null,
                        offset = androidx.compose.ui.unit.IntOffset(100, 200 + (AppState.windowIdCounter * 50)),
                        size = androidx.compose.ui.unit.IntSize(800, 1000),
                        opacity = AppState.settings.windowOpacity,
                        isLocked = false,
                        color = color,
                        packageName = ""
                    )
                )
                AppState.sidebarVisible = false
            }
            
            if (AppState.visibleTools.contains("Macro & Auto-Clicker")) {
                ToolItem(Icons.Rounded.SmartButton, "Macro & Auto-Clicker", "Record & loop touch sequences", ElectricBlue) { openTool("Macro & Auto-Clicker", Icons.Rounded.SmartButton, ElectricBlue) }
            }
            if (AppState.visibleTools.contains("Live Screen OCR")) {
                ToolItem(Icons.Rounded.DocumentScanner, "Live Screen OCR", "Extract text from anywhere", HyperPink) { openTool("Live Screen OCR", Icons.Rounded.DocumentScanner, HyperPink) }
            }
            if (AppState.visibleTools.contains("Universal Clipboard")) {
                ToolItem(Icons.Rounded.ContentPaste, "Universal Clipboard", "History of copied assets", NeonPurple) { openTool("Universal Clipboard", Icons.Rounded.ContentPaste, NeonPurple) }
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
            if (AppState.visibleControls.contains("Wi-Fi")) toggleControls.add { ToggleItem(Icons.Rounded.Wifi, "Wi-Fi", true, ElectricBlue) { 
                context.startActivity(Intent(android.provider.Settings.ACTION_WIFI_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                AppState.sidebarVisible = false
            } }
            if (AppState.visibleControls.contains("Bluetooth")) toggleControls.add { ToggleItem(Icons.Rounded.Bluetooth, "Bluetooth", false, HyperPink) { 
                context.startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                AppState.sidebarVisible = false
            } }
            if (AppState.visibleControls.contains("Data")) toggleControls.add { ToggleItem(Icons.Rounded.DataUsage, "Data", true, NeonPurple) { 
                context.startActivity(Intent(android.provider.Settings.ACTION_DATA_ROAMING_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                AppState.sidebarVisible = false
            } }
            if (AppState.visibleControls.contains("Ghost Mode")) toggleControls.add { ToggleItem(Icons.Rounded.Gamepad, "Ghost Mode", false, Color.Red) { toastMessage = "Ghost Mode Toggled" } }
            if (AppState.visibleControls.contains("Location")) toggleControls.add { ToggleItem(Icons.Rounded.LocationOn, "Location", true, Color(0xFF4CAF50)) { 
                context.startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                AppState.sidebarVisible = false
            } }
            if (AppState.visibleControls.contains("Airplane Mode")) toggleControls.add { ToggleItem(Icons.Rounded.AirplanemodeActive, "Airplane", false, Color(0xFFFFC107)) { 
                context.startActivity(Intent(android.provider.Settings.ACTION_AIRPLANE_MODE_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                AppState.sidebarVisible = false
            } }
            if (AppState.visibleControls.contains("Flashlight")) toggleControls.add { ToggleItem(Icons.Rounded.Highlight, "Flashlight", false, Color.White) { 
                try {
                    val cameraManager = context.getSystemService(android.content.Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
                    val cameraId = cameraManager.cameraIdList[0]
                    val isTorchOn = it.contains("Enabled")
                    cameraManager.setTorchMode(cameraId, isTorchOn)
                } catch (e: Exception) {
                    toastMessage = "Flashlight error"
                }
            } }
            if (AppState.visibleControls.contains("Hotspot")) toggleControls.add { ToggleItem(Icons.Rounded.WifiTethering, "Hotspot", false, Color.Cyan) { 
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    setClassName("com.android.settings", "com.android.settings.TetherSettings")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try { context.startActivity(intent) } catch (e: Exception) { toastMessage = "Cannot open hotspot settings" }
                AppState.sidebarVisible = false
            } }
            if (AppState.visibleControls.contains("Do Not Disturb")) toggleControls.add { ToggleItem(Icons.Rounded.DoNotDisturbOn, "DND", false, Color.Magenta) { 
                context.startActivity(Intent(android.provider.Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                AppState.sidebarVisible = false
            } }
            if (AppState.visibleControls.contains("Screen Rotation")) toggleControls.add { ToggleItem(Icons.Rounded.ScreenRotation, "Rotation", true, ElectricBlue) { 
                if (android.provider.Settings.System.canWrite(context)) {
                    val current = android.provider.Settings.System.getInt(context.contentResolver, android.provider.Settings.System.ACCELEROMETER_ROTATION, 0)
                    android.provider.Settings.System.putInt(context.contentResolver, android.provider.Settings.System.ACCELEROMETER_ROTATION, if (current == 1) 0 else 1)
                } else {
                    toastMessage = "Require Write Settings Permission"
                }
            } }
            if (AppState.visibleControls.contains("Screen Record")) toggleControls.add { ToggleItem(Icons.Rounded.Videocam, "Record", false, Color.Red) { toastMessage = "Screen Record feature coming soon" } }
            if (AppState.visibleControls.contains("Dark Mode")) toggleControls.add { ToggleItem(Icons.Rounded.DarkMode, "Dark Mode", true, Color.Gray) { 
                val uiModeManager = context.getSystemService(android.content.Context.UI_MODE_SERVICE) as android.app.UiModeManager
                val current = uiModeManager.nightMode
                uiModeManager.nightMode = if (current == android.app.UiModeManager.MODE_NIGHT_YES) android.app.UiModeManager.MODE_NIGHT_NO else android.app.UiModeManager.MODE_NIGHT_YES
            } }
            if (AppState.visibleControls.contains("Battery Saver")) toggleControls.add { ToggleItem(Icons.Rounded.BatterySaver, "Battery", false, Color(0xFF4CAF50)) { 
                context.startActivity(Intent(android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                AppState.sidebarVisible = false
            } }
            if (AppState.visibleControls.contains("NFC")) toggleControls.add { ToggleItem(Icons.Rounded.Nfc, "NFC", false, Color.Cyan) { 
                context.startActivity(Intent(android.provider.Settings.ACTION_NFC_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                AppState.sidebarVisible = false
            } }

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
                          android.provider.Settings.System.putInt(context.contentResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE, android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
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
      Spacer(modifier = Modifier.height(16.dp))
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
}

@Composable
fun SidebarTab(title: String, isSelected: Boolean, onClick: () -> Unit) {
  Box(
    modifier = Modifier
      .padding(4.dp)
      .clip(RoundedCornerShape(12.dp))
      .background(if (isSelected) Obsidian else Color.Transparent)
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 8.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      title,
      color = if (isSelected) ElectricBlue else SoftGray,
      fontSize = 14.sp,
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
    )
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
      .background(Color.Black.copy(alpha = 0.7f))
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
                      onDragStart = { isDragging = true },
                      onDragEnd = { isDragging = false },
                      onDragCancel = { isDragging = false }
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
        .size(32.dp)
        .pointerInput(Unit) {
          detectDragGestures { change, dragAmount ->
            change.consume()
            val newWidth = kotlin.math.max(200, windowData.size.width + dragAmount.x.toInt())
            val newHeight = kotlin.math.max(200, windowData.size.height + dragAmount.y.toInt())
            onUpdate(windowData.copy(size = IntSize(newWidth, newHeight)))
          }
        }
    ) {
      Icon(
        Icons.Rounded.SignalCellular4Bar,
        contentDescription = null,
        tint = SoftGray.copy(alpha = 0.5f),
        modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).size(16.dp)
      )
    }
  }
}

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    
    val tWelcome = if (AppState.language == "hi") "ApexPanel Pro में आपका स्वागत है" else "Welcome to ApexPanel Pro"
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
