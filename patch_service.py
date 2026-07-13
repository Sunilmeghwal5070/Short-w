import re

def update_service(filepath):
    with open(filepath, "r") as f:
        content = f.read()

    # 1. Add triggerViewLeft and triggerViewRight
    content = content.replace("private var triggerView: ComposeView? = null", "private var triggerViewLeft: ComposeView? = null\n    private var triggerViewRight: ComposeView? = null")

    # 2. Replace setupTrigger
    setup_trigger_old = """    private fun setupTrigger() {
        triggerView = ComposeView(this).apply {
            setupForCompose()
            setContent {
                MyApplicationTheme {
                    TriggerComponent()
                }
            }
        }
        val isFullScreen = AppState.settings.triggerMode == 1
        val params = WindowManager.LayoutParams(
            if (isFullScreen) dpToPx(12f) else dpToPx(AppState.settings.triggerThickness),
            if (isFullScreen) WindowManager.LayoutParams.MATCH_PARENT else dpToPx(AppState.settings.triggerHeight),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = if (AppState.settings.isRightEdge) Gravity.CENTER_VERTICAL or Gravity.END else Gravity.CENTER_VERTICAL or Gravity.START
            x = dpToPx(AppState.settings.triggerOffsetX)
            y = if (isFullScreen) 0 else dpToPx(AppState.settings.triggerOffsetY)
        }
        if (android.provider.Settings.canDrawOverlays(this)) {
            try {
                windowManager.addView(triggerView, params)
            } catch (e: Exception) {
                e.printStackTrace()
                stopSelf()
                return
            }
        } else {
            stopSelf()
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
    }"""
    
    setup_trigger_new = """    private fun setupTrigger() {
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
    }"""
    
    # We may have edgePosition replacements in setup_trigger old since we ran patch_right_edge.py!
    # Let's just find `private fun setupTrigger() {` and `    private fun showSidebar() {`
    start_idx = content.find("    private fun setupTrigger() {")
    end_idx = content.find("    private fun showSidebar() {")
    if start_idx != -1 and end_idx != -1:
        content = content[:start_idx] + setup_trigger_new + "\n\n" + content[end_idx:]

    # 3. Fix onDestroy
    content = content.replace("triggerView?.let { windowManager.removeView(it) }", "triggerViewLeft?.let { if(it.parent!=null) windowManager.removeView(it) }\n        triggerViewRight?.let { if(it.parent!=null) windowManager.removeView(it) }")

    # 4. Fix onAppClick to use native freeform mode
    on_app_click_old = """                        onAppClick = { appInfo, isFloating ->
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
                        },"""
                        
    on_app_click_new = """                        onAppClick = { appInfo, isFloating ->
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
                        },"""
                        
    content = content.replace(on_app_click_old, on_app_click_new)

    with open(filepath, "w") as f:
        f.write(content)

update_service("/app/applet/app/src/main/java/com/yash/shortw/OverlayService.kt")
update_service("/app/applet/app/src/main/java/com/yash/shortw/OverlayAccessibilityService.kt")
