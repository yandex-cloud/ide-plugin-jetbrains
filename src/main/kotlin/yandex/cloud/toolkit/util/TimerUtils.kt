package yandex.cloud.toolkit.util

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.invokeLater
import java.awt.Component
import java.time.Duration
import java.util.*
import kotlin.concurrent.schedule

fun Timer.scheduleDelayed(delay: Duration?, task: () -> Unit): TimerTask? {
    return if (delay == null || delay.isZero) {
        task()
        null
    } else schedule(delay.toMillis()) { task() }
}

fun invokeLaterAt(component: Component, block: () -> Unit) =
    invokeLater(ModalityState.stateForComponent(component), block)