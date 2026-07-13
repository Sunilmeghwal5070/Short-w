import re

with open("/app/applet/app/src/main/java/com/yash/shortw/OverlayService.kt", "r") as f:
    content = f.read()

# 1. Add dismissView
content = content.replace("private var triggerViewRight: ComposeView? = null", "private var triggerViewRight: ComposeView? = null\n    private var dismissView: ComposeView? = null")

# 2. Add showDismiss / hideDismiss / flow collector
start_idx = content.find("scope.launch {")
flow_code = """        scope.launch {
            snapshotFlow { AppState.isDraggingBubble }.collect { dragging ->
                if (dragging) showDismissView() else hideDismissView()
            }
        }
        
        """
content = content[:start_idx] + flow_code + content[start_idx:]

dismiss_methods = """
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
                                androidx.compose.material.icons.Icons.Rounded.Close,
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
"""
content = content + dismiss_methods

with open("/app/applet/app/src/main/java/com/yash/shortw/OverlayService.kt", "w") as f:
    f.write(content)

with open("/app/applet/app/src/main/java/com/yash/shortw/OverlayAccessibilityService.kt", "r") as f:
    content2 = f.read()

# Do the same for Accessibility service
content2 = content2.replace("private var triggerViewRight: ComposeView? = null", "private var triggerViewRight: ComposeView? = null\n    private var dismissView: ComposeView? = null")

start_idx2 = content2.find("scope.launch {")
content2 = content2[:start_idx2] + flow_code + content2[start_idx2:]
content2 = content2 + dismiss_methods

with open("/app/applet/app/src/main/java/com/yash/shortw/OverlayAccessibilityService.kt", "w") as f:
    f.write(content2)

