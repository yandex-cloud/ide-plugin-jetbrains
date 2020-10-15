package yandex.cloud.toolkit.util.task

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import yandex.cloud.toolkit.util.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class TaskContext(
    val project: Project,
    val indicator: ProgressIndicator,
    val title: String
) {

    var withProgress = !indicator.isIndeterminate
        set(value) {
            field = value
            indicator.isIndeterminate = !value
        }

    var text: String
        get() = indicator.text
        set(value) {
            indicator.text = value
        }

    var subtext: String
        get() = indicator.text2
        set(value) {
            indicator.text2 = value
        }

    var isCanceled: Boolean
        get() = indicator.isCanceled
        set(value) {
            if (value) indicator.cancel()
        }

    fun progress(value: Double) {
        require(withProgress) { "You can not set progress to task without enabled progress bar" }
        indicator.fraction = value
    }

    fun progress(value: Int, max: Int) = progress(value.toDouble() / max)

    fun <R> TaskContext.action(action: () -> Maybe<R>): TaskAction<R> = TaskAction(action)

    fun <R> TaskContext.tryDo(action: () -> R): TaskAction<R> = TaskAction { doMaybe(action) }

    infix fun <R> TaskAction<R>.onFail(errorHandler: TaskExceptionHandler): R = handle().getOrThrow {
        errorHandler.handleError(it)
        TaskInterruptException(it)
    }

    fun <R> TaskAction<R>.returnOnFail(): R = handle().getOrThrow(::TaskInterruptException)

    inline infix fun <R> TaskAction<R>.notifyOnFail(crossinline message: (Throwable) -> String): R = onFail {
        notifyError(it, message)
    }

    inline fun notifyError(e: Throwable, message: (Throwable) -> String) {
        errorNotification(
            "Yandex Cloud Background Task",
            title,
            message(e),
            e
        ).showAt(project)
    }

    fun <R> LazyTask<R>.perform(): Maybe<R> = performIn(this@TaskContext)

    fun notifyInfo(actions: ActionsBundle? = null, message: String) {
        val notification = infoNotification(
            "Yandex Cloud Background Task",
            title,
            message
        )
        actions?.let(notification::withActions)
        notification.showAt(project)
    }

    fun notifyError(actions: ActionsBundle? = null, message: String) {
        val notification = errorNotification(
            "Yandex Cloud Background Task",
            title,
            message
        )
        actions?.let(notification::withActions)
        notification.showAt(project)
    }

    fun interrupt() = TaskExceptionHandler {}

    operator fun <R> LazyTask<R>.provideDelegate(
        thisRef: Any?,
        prop: KProperty<*>
    ): ReadOnlyProperty<Any?, R> = object : ReadOnlyProperty<Any?, R> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): R = tryPerform(this@TaskContext)
    }

    fun steps(steps: Int) : TaskSteps = TaskSteps(steps) + this
}

fun interface TaskExceptionHandler {

    fun handleError(error: Throwable)
}

open class TaskAction<R>(private val action: () -> Maybe<R>) {
    fun handle(): Maybe<R> = action()
}

class TaskInterruptException(override val cause: Throwable? = null) : Exception(cause)

fun backgroundTask(project: Project, title: String, canBeCancelled: Boolean = false, action: TaskContext.() -> Unit) {
    ProgressManager.getInstance().run(
        object : Task.Backgroundable(project, title, canBeCancelled, ALWAYS_BACKGROUND) {
            override fun run(indicator: ProgressIndicator) {
                val context = TaskContext(project, indicator, title)

                try {
                    action.invoke(context)
                } catch (e: TaskInterruptException) {
                }
            }
        }
    )
}

fun modalTask(project: Project, title: String, canBeCancelled: Boolean = false, action: TaskContext.() -> Unit) {
    ProgressManager.getInstance().run(
        object : Task.Modal(project, title, canBeCancelled) {
            override fun run(indicator: ProgressIndicator) {
                val context = TaskContext(project, indicator, title)

                try {
                    action.invoke(context)
                } catch (e: TaskInterruptException) {
                }
            }
        }
    )
}