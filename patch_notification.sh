cat << 'INNER_EOF' > /app/applet/app/src/main/java/com/yash/shortw/NotificationService.kt
package com.yash.shortw

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.os.Handler
import android.os.Looper

class NotificationService : NotificationListenerService() {
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn != null && sbn.packageName != packageName && sbn.isClearable) {
            mainHandler.post {
                AppState.notificationTrigger = AppState.notificationTrigger + 1
            }
        }
    }
}
INNER_EOF
