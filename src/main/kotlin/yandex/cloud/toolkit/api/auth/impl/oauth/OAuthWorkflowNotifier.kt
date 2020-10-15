package yandex.cloud.toolkit.api.auth.impl.oauth

import com.intellij.notification.Notification
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.util.ui.TextTransferable
import yandex.cloud.toolkit.util.*
import java.io.Closeable
import java.net.URI
import java.time.Duration
import java.util.*

class OAuthWorkflowNotifier : Closeable {

    private val timer = Timer(true)
    private val lock = Any()
    private var stage = OAuthStage.INITIAL
    private var notificationTask: TimerTask? = null

    /**
     * Update progress in OAuth workflow. If passed stage is logically "after" current one - method will:
     *
     *  * Update current state
     *  * Cancel pending notification if any
     *  * Schedule provided notification after delay
     *
     * Does nothing when outdated stage passed.
     */
    private fun transferState(newStage: OAuthStage, notification: Notification?, delay: Duration?) {
        synchronized(lock) {
            if (newStage.ordinal <= stage.ordinal) return

            notificationTask?.cancel()
            stage = newStage

            notificationTask = notification?.let {
                timer.scheduleDelayed(delay) { it.show() }
            }
        }
    }

    fun redirectStarted(redirectTo: URI) {
        val addressString = redirectTo.toASCIIString()

        val notification = infoNotification(
            OAUTH_PROCESS_TITLE,
            "Yandex Cloud OAuth",
            "OAuth confirmation page should open in your browser. " +
                    "If page didn't open, please, browse this address manually " + addressString
        ) withAction CopyUrlAction(addressString)

        transferState(OAuthStage.REDIRECT_STARTED, notification, Duration.ofSeconds(3))
    }

    fun redirectPassed() {
        transferState(OAuthStage.REDIRECT_PASSED, null, null)
    }

    fun tokenReceived() {
        val notification = infoNotification(
            OAUTH_PROCESS_TITLE,
            "Yandex Cloud authorization complete",
            "Now plugin has access to Yandex Cloud"
        )

        transferState(OAuthStage.FINISHED, notification, null)
    }

    fun timeout() {
        val notification = warningNotification(
            OAUTH_PROCESS_TITLE,
            "Yandex Cloud authorization timed out",
            "Plugin didn't receive your OAuth token"
        )
        transferState(OAuthStage.FINISHED, notification, null)
    }

    fun failure() {
        val notification = errorNotification(
            OAUTH_PROCESS_TITLE,
            "Yandex Cloud authorization failed",
            "Something went wrong. Please, try again or contact plugin developers"
        )
        transferState(OAuthStage.FINISHED, notification, null)
    }

    override fun close() {
        timer.cancel()
    }

    private enum class OAuthStage {
        INITIAL, REDIRECT_STARTED, REDIRECT_PASSED, FINISHED
    }

    private class CopyUrlAction(private val url: CharSequence) : DumbAwareAction("Copy OAuth address") {
        override fun actionPerformed(e: AnActionEvent) {
            CopyPasteManager.getInstance().setContents(TextTransferable(url))
        }
    }

    companion object {
        const val OAUTH_PROCESS_TITLE = "Yandex Cloud OAuth"
    }
}