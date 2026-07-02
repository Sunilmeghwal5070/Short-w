package com.yash.shortw

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.ui.platform.ComposeView
import com.yash.shortw.ui.theme.MyApplicationTheme
import kotlinx.coroutines.*
import androidx.compose.runtime.*

class OverlayAccessibilityService : AccessibilityService() {
    private lateinit var windowManager: WindowManager
    private var triggerView: ComposeView? = null
    private var sidebarView: ComposeView? = null
    private var hudView: ComposeView? = null
    private val windowViews = mutableMapOf<Int, ComposeView>()
    
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used
    }

    override fun onInterrupt() {
        // Not used
    }

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        AppState.loadSettings(this)
        AppState.fetchApps(this)
        
        scope.launch {
            snapshotFlow { AppState.isServiceRunning }.collect { isRunning ->
                val hasOverlay = android.provider.Settings.canDrawOverlays(this@OverlayAccessibilityService)
                if (!isRunning || hasOverlay) {
                    triggerView?.visibility = View.GONE
                    sidebarView?.visibility = View.GONE
                    hudView?.visibility = View.GONE
                } else {
                    if (AppState.sidebarVisible) {
                        triggerView?.visibility = View.GONE
                        sidebarView?.visibility = View.VISIBLE
                    } else {
                        triggerView?.visibility = View.VISIBLE
                    }
                    if (AppState.hudVisible) hudView?.visibility = View.VISIBLE
                }
            }
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
            visibility = if (AppState.isServiceRunning && !android.provider.Settings.canDrawOverlays(this@OverlayAccessibilityService)) View.VISIBLE else View.GONE
        }
        val isFullScreen = AppState.settings.triggerMode == 1
        val params = WindowManager.LayoutParams(
            if (isFullScreen) dpToPx(12f) else dpToPx(AppState.settings.triggerThickness),
            if (isFullScreen) WindowManager.LayoutParams.MATCH_PARENT else dpToPx(AppState.settings.triggerHeight),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = if (AppState.settings.isRightEdge) Gravity.CENTER_VERTICAL or Gravity.END else Gravity.CENTER_VERTICAL or Gravity.START
            x = dpToPx(AppState.settings.triggerOffsetX)
            y = if (isFullScreen) 0 else dpToPx(AppState.settings.triggerOffsetY)
        }
        try {
            windowManager.addView(triggerView, params)
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }
        
        scope.launch {
            snapshotFlow { AppState.settings }.collect { settings ->
                triggerView?.let { view ->
                    val p = view.layoutParams as WindowManager.LayoutParams
                    val isFullScreen = settings.triggerMode == 1
                    p.width = if (isFullScreen) dpToPx(12f) else dpToPx(settings.triggerThickness)
                    p.height = if (isFullScreen) WindowManager.LayoutParams.MATCH_PARENT else dpToPx(settings.triggerHeight)
                    p.x = dpToPx(settings.triggerOffsetX)
                    p.y = if (isFullScreen) 0 else dpToPx(settings.triggerOffsetY)
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
                                    android.widget.Toast.makeText(this@OverlayAccessibilityService, "Cannot launch this app", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                            AppState.sidebarVisible = false
                        },
                        onToggleHud = { AppState.hudVisible = !AppState.hudVisible },
                        isHudVisible = AppState.hudVisible,
                        onExit = { 
                            AppState.isServiceRunning = false
                        }
                    )
                }
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        try {
            windowManager.addView(sidebarView, params)
        } catch (e: Exception) {
            e.printStackTrace()
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
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 100
        }
        try {
            windowManager.addView(hudView, params)
        } catch (e: Exception) {
            e.printStackTrace()
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
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                flags,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = windowData.offset.x
                y = windowData.offset.y
            }
            try {
                windowManager.addView(view, params)
            } catch (e: Exception) {
                e.printStackTrace()
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
