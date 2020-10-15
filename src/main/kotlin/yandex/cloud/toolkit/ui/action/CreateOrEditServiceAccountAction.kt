package yandex.cloud.toolkit.ui.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.DumbAwareAction
import icons.CloudIcons
import yandex.cloud.toolkit.api.resource.impl.model.CloudFolder
import yandex.cloud.toolkit.api.resource.impl.model.CloudServiceAccount
import yandex.cloud.toolkit.api.resource.impl.user
import yandex.cloud.toolkit.api.service.CloudOperationService
import yandex.cloud.toolkit.ui.dialog.CreateOrEditServiceAccountDialog
import yandex.cloud.toolkit.util.task.backgroundTask
import yandex.cloud.toolkit.util.showAuthenticationNotification

class CreateOrEditServiceAccountAction(val folder: CloudFolder, val account: CloudServiceAccount?) : DumbAwareAction(
    if (account == null) "Create Service Account" else "Edit",
    null,
    if (account == null) CloudIcons.Resources.ServiceAccount else AllIcons.Actions.Edit
) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val authData = folder.user.authData

        if (authData == null) {
            project.showAuthenticationNotification()
            return
        }

        backgroundTask(project, "Yandex.Cloud") {
            text = "Preparing..."

            val folderAccessBindings by CloudOperationService.instance.fetchFolderAccessBindings(project, folder)
            val availableRoles by CloudOperationService.instance.fetchRoles(project, folder.user)
            val serviceAccounts by CloudOperationService.instance.fetchServiceAccounts(project, folder)

            runInEdt {
                CreateOrEditServiceAccountDialog(
                    project,
                    authData,
                    folder,
                    account,
                    folderAccessBindings,
                    availableRoles,
                    serviceAccounts
                ).show()
            }
        }
    }
}