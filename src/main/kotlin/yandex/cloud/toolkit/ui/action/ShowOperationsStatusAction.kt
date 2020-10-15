package yandex.cloud.toolkit.ui.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import yandex.cloud.toolkit.api.resource.impl.model.CloudOperationResult
import yandex.cloud.toolkit.ui.dialog.OperationsStatusDialog

class ShowOperationsStatusAction(
    val operationResults: List<CloudOperationResult>
) : DumbAwareAction("More Information", null, AllIcons.General.ShowInfos) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        OperationsStatusDialog(project, operationResults).show()
    }
}