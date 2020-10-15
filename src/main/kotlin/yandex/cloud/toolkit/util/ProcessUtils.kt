package yandex.cloud.toolkit.util

import java.time.Duration
import java.util.*
import kotlin.concurrent.schedule

private val timeoutTimer = Timer(true)

fun Process.setupTimeout(timeout: Duration): TimerTask = timeoutTimer.schedule(timeout.toMillis()) {
    this@setupTimeout.destroyForcibly()
}

inline fun <R> Process.withTimeout(timeout: Duration, task: Process.() -> R): R {
    val timeoutTask = setupTimeout(timeout)

    return try {
        task(this)
    } finally {
        timeoutTask.cancel()
    }
}