package com.yash.shortw

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotificationService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn != null && sbn.packageName != packageName && sbn.isClearable) {
            AppState.notificationTrigger = AppState.notificationTrigger + 1
        }
    }
}
