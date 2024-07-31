package yandex.cloud.toolkit.process

import com.intellij.openapi.project.Project
import yandex.cloud.toolkit.api.auth.CloudAuthData
import yandex.cloud.toolkit.api.resource.impl.model.CloudFunctionVersion
import yandex.cloud.toolkit.api.service.CloudOperationService
import yandex.cloud.toolkit.util.*
import yandex.cloud.toolkit.util.remote.list.RemoteListPointer
import yandex.cloud.toolkit.util.task.TaskContext
import yandex.cloud.toolkit.util.task.backgroundTask
import java.time.Instant
import java.util.*
import javax.swing.event.EventListenerList

class FunctionLogsTrackingProcess(
    val project: Project,
    val controller: ProcessController,
    val version: CloudFunctionVersion,
    val title: String,
    private val authDataGetter: () -> CloudAuthData?
) {
    companion object {
        private const val LOGS_PAGE_SIZE = 100
        private const val FETCHING_DELAY_SECONDS = 8
        private const val MAX_LOG_LINES = 1024 // 1-2 MB
    }

    private val listeners = EventListenerList()
    private val intervals = ArrayDeque<LogsInterval>()

    fun addLogsListener(listener: FunctionLogsTrackingListener) {
        listeners.add(FunctionLogsTrackingListener::class.java, listener)
    }

    fun removeLogsListener(listener: FunctionLogsTrackingListener) {
        listeners.remove(FunctionLogsTrackingListener::class.java, listener)
    }

    fun stopTracking(destroy: Boolean) {
        controller.tryStopProcess(destroy)
    }

    private fun TaskContext.loadInterval(authData: CloudAuthData, interval: LogsInterval): List<String>? {
        interval.endLine = interval.startLine

        var nextPageToken: String? = null
        val result = mutableListOf<String>()

        do {
            val logs = CloudOperationService.instance.fetchFunctionLogs(
                authData,
                version,
                interval.sinceSeconds,
                interval.untilSeconds,
                RemoteListPointer(LOGS_PAGE_SIZE, nextPageToken)
            ).perform()

            if (checkIfCancelled(controller)) return null

            when (logs) {
                is NoValue -> {
                    stopTracking(false)
                    return null
                }
                is JustValue -> {
                    nextPageToken = if (logs.value.isNotEmpty()) {
                        interval.endLine += logs.value.size
                        logs.value.forEach { result.add(it.toString()) }
                        logs.value.state.nextPageToken
                    } else null
                }
            }
        } while (nextPageToken != null)

        return result
    }

    private fun publishLogs(startLine: Int, endLine: Int, lines: List<String>) {
        if (controller.isStopped) return
        val event = FunctionLogsFetchedEvent(startLine, endLine, lines)
        listeners.getListeners(FunctionLogsTrackingListener::class.java).forEach { it.onLogsFetched(event) }
    }

    private fun appendLogs(interval: LogsInterval, lines: List<String>) =
        publishLogs(interval.startLine, interval.startLine, lines)

    fun clearLogs() {
        synchronized(intervals) {
            if (intervals.isEmpty()) return
            publishLogs(0, intervals.last.endLine, emptyList())
            intervals.clear()
        }
    }

    fun startTracking() {
        if (!controller.tryStartProcess()) return

        backgroundTask(project, title, canBeCancelled = true) {
            while (true) {
                text = "Tracking logs..."
                withProgress = true

                repeat(FETCHING_DELAY_SECONDS) {
                    progress((it + 1) / FETCHING_DELAY_SECONDS.toDouble())
                    Thread.sleep(1000)
                    if (checkIfCancelled(controller)) return@backgroundTask
                }
                withProgress = true

                val authData = authDataGetter()
                if (authData == null) {
                    project.showAuthenticationNotification()
                    stopTracking(false)
                    break
                }

                synchronized(intervals) {
                    val lastInterval = intervals.peekLast()

                    if (lastInterval != null) {
                        val oldEndLine = lastInterval.endLine
                        val newLastLogs = loadInterval(authData, lastInterval) ?: return@backgroundTask
                        if (oldEndLine != lastInterval.endLine) publishLogs(
                            lastInterval.startLine,
                            oldEndLine,
                            newLastLogs
                        )
                    }

                    val untilSeconds = Instant.now().epochSecond + 1
                    val sinceSeconds = lastInterval?.untilSeconds ?: (untilSeconds - FETCHING_DELAY_SECONDS)
                    val lastLine = lastInterval?.endLine ?: 0

                    val newInterval = LogsInterval(sinceSeconds, untilSeconds, lastLine, lastLine)
                    val logsLines = loadInterval(authData, newInterval) ?: return@backgroundTask
                    intervals.addLast(newInterval)
                    if (logsLines.isNotEmpty()) appendLogs(newInterval, logsLines)

                    if (newInterval.endLine > MAX_LOG_LINES && intervals.size > 1) {
                        val linesToRemove = intervals.removeFirst().endLine
                        publishLogs(0, linesToRemove, emptyList())

                        intervals.forEach {
                            it.startLine -= linesToRemove
                            it.endLine -= linesToRemove
                        }
                    }
                }
            }
        }
    }

    private data class LogsInterval(
        val sinceSeconds: Long, // included
        val untilSeconds: Long, // excluded
        var startLine: Int,
        var endLine: Int
    )
}

class FunctionLogsFetchedEvent(
    val startLine: Int,
    val endLine: Int,
    val lines: List<String>
)

interface FunctionLogsTrackingListener : EventListener {
    fun onLogsFetched(event: FunctionLogsFetchedEvent)
}
