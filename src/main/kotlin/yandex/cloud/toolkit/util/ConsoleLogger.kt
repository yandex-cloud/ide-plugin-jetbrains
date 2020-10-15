package yandex.cloud.toolkit.util

import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.execution.filters.HyperlinkInfoBase
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.project.Project
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.MouseEventAdapter

interface ConsoleLogger {

    fun print(message: String, type: ConsoleViewContentType)

    fun printHyperlink(hyperlinkText: String, info: HyperlinkInfo?)

    fun print(message: String) {
        print(message, ConsoleViewContentType.NORMAL_OUTPUT)
    }

    fun println(message: String = "") {
        print(message + "\n")
    }

    fun error(message: String) {
        print(message, ConsoleViewContentType.ERROR_OUTPUT)
    }

    fun info(message: String) {
        print(message, ConsoleViewContentType.LOG_INFO_OUTPUT)
    }

    fun debug(message: String) {
        print(message, ConsoleViewContentType.LOG_DEBUG_OUTPUT)
    }

    fun verbose(message: String) {
        print(message, ConsoleViewContentType.LOG_VERBOSE_OUTPUT)
    }

    fun user(message: String) {
        print(message, ConsoleViewContentType.USER_INPUT)
    }

    fun system(message: String) {
        print(message, ConsoleViewContentType.SYSTEM_OUTPUT)
    }

    fun printAction(action: AnAction) {
        printHyperlink(action.templateText + "\n", object : HyperlinkInfoBase() {
            override fun navigate(project: Project, point: RelativePoint?) {
                ActionUtil.invokeAction(
                    action,
                    SimpleDataContext.getProjectContext(project), ActionPlaces.UNKNOWN, point?.toMouseEvent(), null
                )
            }
        })
    }
}

class ConsoleViewLogger(private val view: ConsoleView) : ConsoleLogger {
    override fun print(message: String, type: ConsoleViewContentType) {
        view.print(message, type)
    }

    override fun printHyperlink(hyperlinkText: String, info: HyperlinkInfo?) {
        (view as? ConsoleViewImpl)?.printHyperlink(hyperlinkText, info)
    }
}

val ConsoleView.logger: ConsoleLogger
    get() = ConsoleViewLogger(this)