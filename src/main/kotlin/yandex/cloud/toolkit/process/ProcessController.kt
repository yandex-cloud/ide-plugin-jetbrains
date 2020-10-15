package yandex.cloud.toolkit.process

import com.intellij.execution.process.ProcessHandler
import yandex.cloud.toolkit.util.task.TaskContext
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicReference

interface ProcessController {

    val wasStarted: Boolean
    val isStopped: Boolean

    val isRunning: Boolean get() = wasStarted && !isStopped

    fun startProcess()
    fun stopProcess(terminate: Boolean)

    fun tryStopProcess(terminate: Boolean): Boolean
    fun tryStartProcess(): Boolean
}

class StatedProcessController : ProcessController {

    private enum class ProcessState {
        INITIAL,
        RUNNING,
        STOPPED,
        TERMINATED
    }

    private val currentState = AtomicReference(ProcessState.INITIAL)

    override val wasStarted: Boolean get() = currentState.get() != ProcessState.INITIAL

    override val isStopped: Boolean
        get() {
            val state = currentState.get()
            return state == ProcessState.TERMINATED || state == ProcessState.STOPPED
        }

    override fun startProcess() {
        currentState.set(ProcessState.RUNNING)
    }

    override fun stopProcess(terminate: Boolean) {
        currentState.set(if (terminate) ProcessState.TERMINATED else ProcessState.STOPPED)
    }

    override fun tryStopProcess(terminate: Boolean): Boolean = currentState.compareAndSet(
        ProcessState.RUNNING,
        if (terminate) ProcessState.TERMINATED else ProcessState.STOPPED
    )

    override fun tryStartProcess(): Boolean = currentState.compareAndSet(
        ProcessState.INITIAL,
        ProcessState.RUNNING
    )
}

class RunContentController(private val isDetachDefault: Boolean = true) : ProcessController, ProcessHandler() {

    private val stateLock = Object()

    override val wasStarted: Boolean get() = isStartNotified
    override val isStopped: Boolean get() = isProcessTerminated

    override fun startProcess() = synchronized(stateLock) {
        startNotify()
    }

    override fun stopProcess(terminate: Boolean) = synchronized(stateLock) {
        when (terminate) {
            false -> notifyProcessDetached()
            true -> notifyProcessTerminated(0)
        }
    }

    override fun tryStartProcess(): Boolean = synchronized(stateLock) {
        if (!wasStarted) {
            startProcess()
            true
        } else false
    }

    override fun tryStopProcess(terminate: Boolean): Boolean = synchronized(stateLock) {
        if (isRunning) {
            stopProcess(terminate)
            true
        } else false
    }

    override fun destroyProcessImpl() = stopProcess(true)
    override fun detachProcessImpl() = stopProcess(false)

    override fun detachIsDefault(): Boolean = isDetachDefault
    override fun getProcessInput(): OutputStream? = null
}

fun TaskContext.checkIfCancelled(controller: ProcessController): Boolean {
    if (controller.isStopped) return true // if cancelled as run process task

    return if (indicator.isCanceled) { // if cancelled as background task
        controller.tryStopProcess(false)
        true
    } else false
}