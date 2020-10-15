package yandex.cloud.toolkit.util

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.Alarm
import java.time.Duration

fun infoNotification(groupId: String, title: String, content: String): Notification =
    Notification(groupId, title, content, NotificationType.INFORMATION)

fun warningNotification(groupId: String, title: String, content: String): Notification =
    Notification(groupId, title, content, NotificationType.WARNING)

fun errorNotification(groupId: String, title: String, content: String): Notification =
    Notification(groupId, title, content, NotificationType.ERROR)

fun errorNotification(groupId: String, title: String, content: String, e: Throwable): Notification =
    Notification(groupId, title, content, NotificationType.ERROR).apply {
        (e as? ActionsBundle)?.let(::withActions)
    }

fun Notification.show(timeout: Duration? = Duration.ofSeconds(5)) {
    Notifications.Bus.notify(this)
    if (timeout != null) setTimeout(timeout)
}

fun Notification.showAt(project: Project, timeout: Duration? = Duration.ofSeconds(5)) {
    Notifications.Bus.notify(this, project)
    if (timeout != null) setTimeout(timeout)
}

fun Notification.setTimeout(timeout: Duration) {
    val alarm = Alarm()
    alarm.addRequest({
        this.hideBalloon()
        Disposer.dispose(alarm)
    }, timeout.toMillis())
}

infix fun Notification.withAction(action: AnAction): Notification = apply {
    addAction(action)
}

fun Notification.withActions(bundle: ActionsBundle): Notification {
    bundle.getActions().forEach { addAction(it) }
    return this
}

