package yandex.cloud.toolkit.util.task

import com.intellij.openapi.actionSystem.AnAction
import yandex.cloud.toolkit.ui.action.ManageProfilesAction
import yandex.cloud.toolkit.process.ProcessController
import yandex.cloud.toolkit.util.ConsoleLogger

class TaskSteps(private val steps: Int) {

    private var step = 0

    private var context: TaskContext? = null
    private var progressController: ProcessController? = null
    private var logger: ConsoleLogger? = null

    operator fun plus(context: TaskContext): TaskSteps {
        this.context = context
        context.withProgress = true
        return this
    }

    operator fun plus(logger: ConsoleLogger): TaskSteps {
        this.logger = logger
        return this
    }

    operator fun plus(progressController: ProcessController): TaskSteps {
        this.progressController = progressController
        return this
    }

    fun next(stepName: String? = null) {
        step++
        val progress = (step.toDouble() / steps).coerceIn(0.0, 1.0)

        context?.progress(progress)
        if (stepName != null) {
            context?.text = "$stepName..."
            logger?.print("> $stepName\n")
        }

        if (progressController?.isStopped == true) throw TaskInterruptException()
        if (context?.indicator?.isCanceled == true) stop()
    }

    fun error(message: String? = null, vararg actions: AnAction): Nothing {
        if (message != null) logger?.error("\n[ERROR] $message\n")
        actions.forEach { logger?.printAction(it) }
        stop()
    }

    fun handleError() = TaskExceptionHandler { error ->
        this.error(error.message)
    }

    fun handleError(title: () -> String) = TaskExceptionHandler { error ->
        this.error(title() + "\n" + error.message)
    }

    private fun stop(): Nothing {
        progressController?.tryStopProcess(false)
        throw TaskInterruptException()
    }
}

fun TaskSteps.notAuthenticatedError(): Nothing {
    error(
        "Failed to authenticate user in Yandex.Cloud. Select profile or reauthenticate selected!",
        ManageProfilesAction()
    )
}