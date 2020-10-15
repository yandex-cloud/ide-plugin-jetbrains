package yandex.cloud.toolkit.ui.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import icons.CloudIcons
import yandex.cloud.toolkit.api.resource.impl.model.CloudApiGateway
import yandex.cloud.toolkit.api.resource.impl.model.isSuccess
import yandex.cloud.toolkit.api.resource.impl.model.notifyError
import yandex.cloud.toolkit.api.resource.impl.model.notifySuccess
import yandex.cloud.toolkit.api.resource.impl.user
import yandex.cloud.toolkit.api.service.CloudRepository
import yandex.cloud.toolkit.api.service.awaitEnd
import yandex.cloud.toolkit.ui.dialog.ActionConfirmationDialog
import yandex.cloud.toolkit.util.YCUI
import yandex.cloud.toolkit.util.task.backgroundTask
import yandex.cloud.toolkit.util.showAuthenticationNotification

class DeleteApiGatewayAction(private val apiGateway: CloudApiGateway) : DumbAwareAction(
    YCUI.Messages.Delete, null, CloudIcons.Actions.Delete
) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val authData = apiGateway.user.authData

        if (authData == null) {
            project.showAuthenticationNotification()
            return
        }

        val confirmed = ActionConfirmationDialog(
            "Delete API Gateway",
            "API gateway '${apiGateway.name}' will be permanently deleted",
            "delete"
        ).showAndGet()
        if (!confirmed) return

        backgroundTask(project, "Yandex.Cloud") {
            text = "Deleting API gateway..."

            val deleteOperation = CloudRepository.instance.deleteApiGateway(authData, apiGateway.id)
            val deleteOperationResult = deleteOperation.awaitEnd(project, authData)

            if (deleteOperationResult.isSuccess) {
                deleteOperationResult.notifySuccess(project, "API gateway '${apiGateway.name}' deleted")
                apiGateway.group.update(project, false)
            } else {
                deleteOperationResult.notifyError(project, "Failed to delete API gateway '${apiGateway.name}'")
            }
        }
    }
}