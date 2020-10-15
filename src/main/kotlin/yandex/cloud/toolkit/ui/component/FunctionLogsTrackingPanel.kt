package yandex.cloud.toolkit.ui.component

import com.intellij.diff.util.DiffUtil
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actions.ScrollToTheEndToolbarAction
import com.intellij.openapi.editor.actions.ToggleUseSoftWrapsToolbarAction
import com.intellij.openapi.editor.impl.softwrap.SoftWrapAppliancePlaces
import com.intellij.openapi.project.Project
import com.intellij.util.DocumentUtil
import yandex.cloud.toolkit.api.resource.impl.model.CloudFunctionVersion
import yandex.cloud.toolkit.api.resource.impl.user
import yandex.cloud.toolkit.ui.dialog.ShowFunctionLogsDialog
import yandex.cloud.toolkit.process.FunctionLogsFetchedEvent
import yandex.cloud.toolkit.process.FunctionLogsTrackingListener
import yandex.cloud.toolkit.process.FunctionLogsTrackingProcess
import yandex.cloud.toolkit.process.ProcessController
import yandex.cloud.toolkit.util.YCPanel
import yandex.cloud.toolkit.util.invokeLaterAt
import yandex.cloud.toolkit.util.showAuthenticationNotification
import java.awt.BorderLayout

class FunctionLogsTrackingPanel<C : ProcessController>(
    val project: Project,
    val version: CloudFunctionVersion,
    val controller: C
) : YCPanel(BorderLayout()), FunctionLogsTrackingListener, Disposable {

    val console: ConsoleViewImpl = TextConsoleBuilderFactory.getInstance().createBuilder(project).apply {
        setViewer(true)
    }.console as ConsoleViewImpl

    val title = "YC Function '${version.function.name}/${version.id}"

    val process = FunctionLogsTrackingProcess(project, controller, version, title) { version.user.authData }

    val actions = DefaultActionGroup()
    val toolbar = ActionManager.getInstance().createActionToolbar("FunctionLogsTrackingPanel", actions, false)

    init {
        toolbar.component addAs BorderLayout.WEST
        console.component addAs BorderLayout.CENTER
    }

    fun setupActions(block: DefaultActionGroup.() -> Unit = {}) {
        actions.apply {
            block(this)
            addSeparator()
            add(ClearAction())
            add(LogsHistoryAction())
            addSeparator()
            add(object : ToggleUseSoftWrapsToolbarAction(SoftWrapAppliancePlaces.CONSOLE) {
                override fun getEditor(e: AnActionEvent): Editor? = console.editor
            })
            add(ScrollToTheEndToolbarAction(console.editor))
        }
    }

    fun startTracking() {
        process.addLogsListener(this)
        process.startTracking()
    }

    fun stopTracking() {
        process.removeLogsListener(this)
        process.stopTracking(false)
    }

    override fun dispose() {
        process.stopTracking(true)
        console.dispose()
    }

    override fun onLogsFetched(event: FunctionLogsFetchedEvent) {
        invokeLaterAt(console) {
            val doc = console.editor.document
            DocumentUtil.writeInRunUndoTransparentAction {
                DiffUtil.applyModification(doc, event.startLine, event.endLine, event.lines)
            }
        }
    }

    private inner class LogsHistoryAction :
        AnAction("Show Logs History", null, AllIcons.Vcs.History) {

        override fun actionPerformed(e: AnActionEvent) {
            val authData = version.user.authData
            if (authData == null) {
                project.showAuthenticationNotification()
                return
            }
            runInEdt {
                ShowFunctionLogsDialog(project, authData, version).show()
            }
        }
    }

    private inner class ClearAction : AnAction("Clear Console", null, AllIcons.Actions.GC) {

        override fun actionPerformed(e: AnActionEvent) {
            process.clearLogs()
        }
    }
}