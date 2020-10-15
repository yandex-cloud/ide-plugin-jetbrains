package yandex.cloud.toolkit.ui.view

import com.intellij.execution.actions.StopProcessAction
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentManager
import com.intellij.execution.ui.actions.CloseAction
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import icons.CloudIcons
import yandex.cloud.toolkit.api.resource.impl.model.CloudFunctionVersion
import yandex.cloud.toolkit.ui.component.FunctionLogsTrackingPanel
import yandex.cloud.toolkit.process.RunContentController
import yandex.cloud.toolkit.util.StateLatch
import yandex.cloud.toolkit.util.disposeWith

/**
 * Util to open function logs tracking run content.
 */
class TrackFunctionLogsView(
    val project: Project,
    val version: CloudFunctionVersion
) {

    private val processController = RunContentController()
    private val executor = DefaultRunExecutor.getRunExecutorInstance()
    private val logsPanel = FunctionLogsTrackingPanel(project, version, processController)

    private val descriptor: RunContentDescriptor

    private var displayedLatch = StateLatch()

    init {
        descriptor = RunContentDescriptor(
            logsPanel.console,
            processController,
            logsPanel,
            logsPanel.title,
            CloudIcons.Resources.Function
        )
        descriptor.isActivateToolWindowWhenAdded = true

        logsPanel.setupActions {
            add(CloseAction(executor, descriptor, project))
            add(RestartAction())
            add(StopProcessAction("Stop Tracking", null, processController).apply {
                registerCustomShortcutSet(shortcutSet, logsPanel)
            })
        }
    }

    fun display() {
        displayedLatch.close { "Logs tracking view already displayed" }

        logsPanel.disposeWith(descriptor)
        logsPanel.console.attachToProcess(processController)
        RunContentManager.getInstance(project).showRunContent(executor, descriptor)

        logsPanel.startTracking()
    }

    private fun restartProcess() {
        val removed = RunContentManager.getInstance(project).removeRunContent(executor, descriptor)
        invokeLater {
            if (removed) TrackFunctionLogsView(project, version).display()
        }
    }

    private inner class RestartAction : AnAction("Restart Logs Tracking", null, AllIcons.Actions.Restart) {

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = processController.isStopped
        }

        override fun actionPerformed(e: AnActionEvent) {
            restartProcess()
        }
    }
}