package yandex.cloud.toolkit.ui.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import icons.CloudIcons
import yandex.cloud.toolkit.api.resource.impl.model.CloudServiceAccount
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

class DeleteServiceAccountAction(private val account: CloudServiceAccount) : DumbAwareAction(
    YCUI.Messages.Delete, null, CloudIcons.Actions.Delete
) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val authData = account.user.authData

        if (authData == null) {
            project.showAuthenticationNotification()
            return
        }

        val confirmed = ActionConfirmationDialog(
            "Delete Service Account",
            "Service account '${account.name}' will be permanently deleted",
            "delete"
        ).showAndGet()
        if (!confirmed) return

        backgroundTask(project, "Yandex.Cloud") {
            text = "Deleting service account..."

            val deleteOperation = CloudRepository.instance.deleteServiceAccount(authData, account.id)
            val deleteOperationResult = deleteOperation.awaitEnd(project, authData)

            if (deleteOperationResult.isSuccess) {
                deleteOperationResult.notifySuccess(project, "Service account '${account.name}' deleted")
                account.group.update(project, false)
            } else {
                deleteOperationResult.notifyError(project, "Failed to delete service account '${account.name}'")
            }
        }
    }
}