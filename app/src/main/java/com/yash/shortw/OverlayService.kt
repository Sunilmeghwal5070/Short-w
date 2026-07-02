package com.yash.shortw

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.yash.shortw.ui.theme.MyApplicationTheme
import kotlinx.coroutines.*
import androidx.compose.runtime.*

class FakeLifecycleOwner : SavedStateRegistryOwner, ViewModelStoreOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store

    fun start() {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun stop() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}

fun View.setupForCompose() {
    val lifecycleOwner = FakeLifecycleOwner()
    lifecycleOwner.start()
    setViewTreeLifecycleOwner(lifecycleOwner)
    setViewTreeSavedStateRegistryOwner(lifecycleOwner)
    setViewTreeViewModelStoreOwner(lifecycleOwner)
    
    addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {}
        override fun onViewDetachedFromWindow(v: View) {
            lifecycleOwner.stop()
        }
    })
}

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var triggerView: ComposeView? = null
    private var sidebarView: ComposeView? = null
    private var hudView: ComposeView? = null
    private val windowViews = mutableMapOf<Int, ComposeView>()
    
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    override fun onBind(intent: Intent?): IBinder? = null

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        AppState.loadSettings(this)
        AppState.fetchApps(this)
        
        val channelId = "overlay_service_channel"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(channelId, "Overlay Service", android.app.NotificationManager.IMPORTANCE_MIN)
            val manager = getSystemService(android.app.NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val notification = androidx.core.app.NotificationCompat.Builder(this, channelId)
            .setContentTitle("ApexPanel Pro")
            .setContentText("Master Service is running")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(pendingIntent)
            .build()
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1, notification)
        }
        
        setupTrigger()
        
        scope.launch {
            snapshotFlow { AppState.sidebarVisible }.collect { visible ->
                if (visible) {
                    showSidebar()
                } else {
                    kotlinx.coroutines.delay(300)
                    hideSidebar()
                }
            }
        }
        
        scope.launch {
            snapshotFlow { AppState.hudVisible }.collect { visible ->
                if (visible) showHud() else hideHud()
            }
        }
        
        scope.launch {
            snapshotFlow { AppState.windows.toList() }.collect { windows ->
                syncWindows(windows)
            }
        }
    }
    
    private fun setupTrigger() {
        triggerView = ComposeView(this).apply {
            setupForCompose()
            setContent {
                MyApplicationTheme {
                    TriggerComponent()
                }
            }
        }
        val params = WindowManager.LayoutParams(
            dpToPx(AppState.settings.triggerThickness),
            dpToPx(AppState.settings.triggerHeight),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = if (AppState.settings.isRightEdge) Gravity.CENTER_VERTICAL or Gravity.END else Gravity.CENTER_VERTICAL or Gravity.START
            x = dpToPx(AppState.settings.triggerOffsetX)
            y = dpToPx(AppState.settings.triggerOffsetY)
        }
        if (android.provider.Settings.canDrawOverlays(this)) {
            windowManager.addView(triggerView, params)
        } else {
            stopSelf()
            return
        }
        
        scope.launch {
            snapshotFlow { AppState.settings }.collect { settings ->
                triggerView?.let { view ->
                    val p = view.layoutParams as WindowManager.LayoutParams
                    p.width = dpToPx(settings.triggerThickness)
                    p.height = dpToPx(settings.triggerHeight)
                    p.x = dpToPx(settings.triggerOffsetX)
                    p.y = dpToPx(settings.triggerOffsetY)
                    p.gravity = if (settings.isRightEdge) Gravity.CENTER_VERTICAL or Gravity.END else Gravity.CENTER_VERTICAL or Gravity.START
                    windowManager.updateViewLayout(view, p)
                }
            }
        }
    }

    private fun showSidebar() {
        if (sidebarView != null) return
        sidebarView = ComposeView(this).apply {
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == android.view.KeyEvent.KEYCODE_BACK && event.action == android.view.KeyEvent.ACTION_UP) {
                    AppState.sidebarVisible = false
                    true
                } else {
                    false
                }
            }
            setupForCompose()
            setContent {
                MyApplicationTheme {
                    SidebarComponent(
                        onAppClick = { appInfo, isFloating ->
                            if (isFloating) {
                                AppState.windows.add(
                                    FloatingWindowData(
                                        id = AppState.windowIdCounter++,
                                        title = appInfo.name,
                                        icon = null,
                                        appIcon = appInfo.icon,
                                        offset = androidx.compose.ui.unit.IntOffset(100, 200 + (AppState.windowIdCounter * 50)),
                                        size = androidx.compose.ui.unit.IntSize(800, 1000),
                                        color = androidx.compose.ui.graphics.Color(0xFF00E5FF),
                                        packageName = appInfo.packageName
                                    )
                                )
                            } else {
                                val launchIntent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
                                if (launchIntent != null) {
                                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                                    startActivity(launchIntent)
                                } else {
                                    android.widget.Toast.makeText(this@OverlayService, "Cannot launch this app", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                            AppState.sidebarVisible = false
                        },
                        onToggleHud = { AppState.hudVisible = !AppState.hudVisible },
                        isHudVisible = AppState.hudVisible,
                        onExit = { 
                            AppState.isServiceRunning = false
                            stopSelf() 
                        }
                    )
                }
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        if (android.provider.Settings.canDrawOverlays(this)) {
            windowManager.addView(sidebarView, params)
        }
        triggerView?.visibility = View.GONE
    }

    private fun hideSidebar() {
        sidebarView?.let { windowManager.removeView(it) }
        sidebarView = null
        triggerView?.visibility = View.VISIBLE
    }

    private fun showHud() {
        if (hudView != null) return
        hudView = ComposeView(this).apply {
            setupForCompose()
            setContent {
                MyApplicationTheme {
                    SystemMonitorHud(windowManager = windowManager, composeView = this@apply)
                }
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 100
        }
        if (android.provider.Settings.canDrawOverlays(this)) {
            windowManager.addView(hudView, params)
        }
    }

    private fun hideHud() {
        hudView?.let { windowManager.removeView(it) }
        hudView = null
    }

    private fun syncWindows(windows: List<FloatingWindowData>) {
        val currentIds = windows.map { it.id }.toSet()
        val existingIds = windowViews.keys.toSet()
        
        val toRemove = existingIds - currentIds
        for (id in toRemove) {
            windowViews[id]?.let { windowManager.removeView(it) }
            windowViews.remove(id)
        }
        
        val toAdd = currentIds - existingIds
        for (id in toAdd) {
            val windowData = windows.first { it.id == id }
            val view = ComposeView(this).apply {
                setOnKeyListener { _, keyCode, event ->
                    if (keyCode == android.view.KeyEvent.KEYCODE_BACK && event.action == android.view.KeyEvent.ACTION_UP) {
                        AppState.windows.removeAll { it.id == id }
                        true
                    } else {
                        false
                    }
                }
                setupForCompose()
                setContent {
                    MyApplicationTheme {
                        val currentData = AppState.windows.firstOrNull { it.id == id }
                        if (currentData != null) {
                            FloatingWindowComponent(
                                windowData = currentData, 
                                windowManager = windowManager, 
                                composeView = this@apply,
                                onUpdate = { updated ->
                                    val idx = AppState.windows.indexOfFirst { it.id == updated.id }
                                    if (idx != -1) AppState.windows[idx] = updated
                                },
                                onClose = {
                                    AppState.windows.removeAll { it.id == id }
                                }
                            )
                        }
                    }
                }
            }
            val needsFocus = windowData.title == "Universal Clipboard"
            val flags = if (needsFocus) {
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            } else {
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            }
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                flags,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = windowData.offset.x
                y = windowData.offset.y
            }
            if (android.provider.Settings.canDrawOverlays(this)) {
                windowManager.addView(view, params)
            }
            windowViews[id] = view
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        triggerView?.let { windowManager.removeView(it) }
        sidebarView?.let { windowManager.removeView(it) }
        hudView?.let { windowManager.removeView(it) }
        windowViews.values.forEach { windowManager.removeView(it) }
    }
}
