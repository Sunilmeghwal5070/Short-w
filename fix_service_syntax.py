import re

def fix_file(filepath):
    with open(filepath, "r") as f:
        content = f.read()

    # The methods were appended at the end of the file, let's remove them from the end
    end_methods = """    private fun showDismissView() {
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
    }"""
    
    # We will remove them from the very end of the file, and then insert them before the last `}`.
    # The last `}` is the class closer.
    
    # We also need to add missing imports if they aren't imported: Box, Modifier, etc are in compose.
    
    if end_methods in content:
        content = content.replace(end_methods, "")
        
        # Add imports if necessary
        if "import androidx.compose.foundation.layout.Box" not in content:
            content = content.replace("import android.view.WindowManager", "import android.view.WindowManager\nimport androidx.compose.foundation.layout.Box\nimport androidx.compose.ui.Alignment\nimport androidx.compose.foundation.layout.padding\nimport androidx.compose.ui.unit.dp\nimport androidx.compose.foundation.layout.size\nimport androidx.compose.foundation.layout.fillMaxSize\nimport androidx.compose.ui.Modifier\nimport androidx.compose.ui.draw.clip\nimport androidx.compose.foundation.background")
        
        # Insert before the last `}`
        last_brace = content.rfind("}")
        content = content[:last_brace] + end_methods + "\n" + content[last_brace:]

    # Also there are unresolved references for `triggerView`! 
    # In my previous script I forgot `triggerView` was used elsewhere (in hideSidebar/syncWindows)? 
    # Let's find any remaining `triggerView?.` and replace with `triggerViewLeft` and `triggerViewRight` 
    content = content.replace("triggerView?.let", "triggerViewLeft?.let { windowManager.removeView(it) }; triggerViewRight?.let")
    
    with open(filepath, "w") as f:
        f.write(content)

fix_file("/app/applet/app/src/main/java/com/yash/shortw/OverlayService.kt")
fix_file("/app/applet/app/src/main/java/com/yash/shortw/OverlayAccessibilityService.kt")

