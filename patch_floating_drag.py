import re

with open("/app/applet/app/src/main/java/com/yash/shortw/MainActivity.kt", "r") as f:
    content = f.read()

old_drag = """                  detectDragGestures(
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
                  }"""

new_drag = """                  detectDragGestures(
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
                          if (params.y > screenHeight / 2 - 200 && params.x > -150 && params.x < 150) {
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
                  }"""
                  
content = content.replace(old_drag, new_drag)

with open("/app/applet/app/src/main/java/com/yash/shortw/MainActivity.kt", "w") as f:
    f.write(content)

