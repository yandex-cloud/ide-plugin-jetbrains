package yandex.cloud.toolkit.ui.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import yandex.cloud.toolkit.api.service.CloudOperationService

class ShowOperationLogsAction(val operationId: String, val url: String) :
    DumbAwareAction("View Logs", null, AllIcons.Nodes.LogFolder) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        CloudOperationService.instance.showOperationLogs(project, operationId, url)
    }
}