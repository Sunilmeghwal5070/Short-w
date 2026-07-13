import re

with open("/app/applet/app/src/main/java/com/yash/shortw/MainActivity.kt", "r") as f:
    content = f.read()

# Replace the old resize handle with a better one.
old_resize_handle = """    Box(
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
    }"""

new_resize_handle = """    Box(
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
    }"""

content = content.replace(old_resize_handle, new_resize_handle)

with open("/app/applet/app/src/main/java/com/yash/shortw/MainActivity.kt", "w") as f:
    f.write(content)

