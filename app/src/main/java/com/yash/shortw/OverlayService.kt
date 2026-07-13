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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
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
    private var triggerViewLeft: ComposeView? = null
    private var triggerViewRight: ComposeView? = null
    private var dismissView: ComposeView? = null
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
            .setContentTitle("Short Panel Pro")
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
            snapshotFlow { AppState.isDraggingBubble }.collect { dragging ->
                if (dragging) showDismissView() else hideDismissView()
            }
        }
        
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
        if (!android.provider.Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        triggerViewLeft = ComposeView(this).apply {
            setupForCompose()
            setContent { MyApplicationTheme { TriggerComponent(isRight = false) } }
        }
        triggerViewRight = ComposeView(this).apply {
            setupForCompose()
            setContent { MyApplicationTheme { TriggerComponent(isRight = true) } }
        }

        try {
            if (AppState.settings.edgePosition == 0 || AppState.settings.edgePosition == 2) {
                windowManager.addView(triggerViewLeft, createTriggerParams(false))
            }
            if (AppState.settings.edgePosition == 1 || AppState.settings.edgePosition == 2) {
                windowManager.addView(triggerViewRight, createTriggerParams(true))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        scope.launch {
            snapshotFlow { AppState.settings }.collect { settings ->
                try {
                    // Left Trigger
                    if (settings.edgePosition == 0 || settings.edgePosition == 2) {
                        if (triggerViewLeft?.parent == null) {
                            windowManager.addView(triggerViewLeft, createTriggerParams(false))
                        } else {
                            windowManager.updateViewLayout(triggerViewLeft, createTriggerParams(false))
                        }
                    } else {
                        if (triggerViewLeft?.parent != null) {
                            windowManager.removeView(triggerViewLeft)
                        }
                    }
                    
                    // Right Trigger
                    if (settings.edgePosition == 1 || settings.edgePosition == 2) {
                        if (triggerViewRight?.parent == null) {
                            windowManager.addView(triggerViewRight, createTriggerParams(true))
                        } else {
                            windowManager.updateViewLayout(triggerViewRight, createTriggerParams(true))
                        }
                    } else {
                        if (triggerViewRight?.parent != null) {
                            windowManager.removeView(triggerViewRight)
                        }
                    }
                } catch(e: Exception) { e.printStackTrace() }
            }
        }
    }

    private fun createTriggerParams(isRight: Boolean): WindowManager.LayoutParams {
        val isFullScreen = AppState.settings.triggerMode == 1
        return WindowManager.LayoutParams(
            if (isFullScreen) dpToPx(12f) else dpToPx(AppState.settings.triggerThickness),
            if (isFullScreen) WindowManager.LayoutParams.MATCH_PARENT else dpToPx(AppState.settings.triggerHeight),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = if (isRight) Gravity.CENTER_VERTICAL or Gravity.END else Gravity.CENTER_VERTICAL or Gravity.START
            x = dpToPx(AppState.settings.triggerOffsetX)
            y = if (isFullScreen) 0 else dpToPx(AppState.settings.triggerOffsetY)
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
                            val launchIntent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
                            if (launchIntent != null) {
                                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                                if (isFloating) {
                                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
                                    val bounds = android.graphics.Rect(100, 100, 800, 1200)
                                    val options = android.app.ActivityOptions.makeBasic().setLaunchBounds(bounds)
                                    try {
                                        startActivity(launchIntent, options.toBundle())
                                    } catch (e: Exception) {
                                        startActivity(launchIntent)
                                    }
                                } else {
                                    startActivity(launchIntent)
                                }
                            } else {
                                android.widget.Toast.makeText(this@OverlayService, "Cannot launch this app", android.widget.Toast.LENGTH_SHORT).show()
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
            try {
                windowManager.addView(sidebarView, params)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        triggerViewLeft?.visibility = View.GONE
                    triggerViewRight?.visibility = View.GONE
    }

    private fun hideSidebar() {
        sidebarView?.let { windowManager.removeView(it) }
        sidebarView = null
        triggerViewLeft?.visibility = View.VISIBLE
                    triggerViewRight?.visibility = View.VISIBLE
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
            try {
                windowManager.addView(hudView, params)
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
                try {
                    windowManager.addView(view, params)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            windowViews[id] = view
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        triggerViewLeft?.let { if(it.parent!=null) windowManager.removeView(it) }
        triggerViewRight?.let { if(it.parent!=null) windowManager.removeView(it) }
        sidebarView?.let { windowManager.removeView(it) }
        hudView?.let { windowManager.removeView(it) }
        windowViews.values.forEach { windowManager.removeView(it) }
    }
    private fun showDismissView() {
        if (dismissView != null) return
        dismissView = ComposeView(this).apply {
            setupForCompose()
            setContent {
                MyApplicationTheme {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                        Box(
                            modifier = Modifier
                                .padding(bottom = 32.dp)
                                .size(64.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(androidx.compose.ui.graphics.Color.Red.copy(alpha = 0.8f)),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        )
        if (android.provider.Settings.canDrawOverlays(this)) {
            try {
                windowManager.addView(dismissView, params)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun hideDismissView() {
        dismissView?.let { 
            if (it.parent != null) windowManager.removeView(it)
        }
        dismissView = null
    }
}


