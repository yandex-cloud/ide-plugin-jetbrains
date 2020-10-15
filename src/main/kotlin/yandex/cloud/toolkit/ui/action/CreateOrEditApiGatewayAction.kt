package yandex.cloud.toolkit.ui.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import icons.CloudIcons
import yandex.cloud.toolkit.api.auth.CloudAuthData
import yandex.cloud.toolkit.api.resource.impl.model.CloudApiGateway
import yandex.cloud.toolkit.api.resource.impl.model.CloudFolder
import yandex.cloud.toolkit.api.resource.impl.user
import yandex.cloud.toolkit.api.service.CloudOperationService
import yandex.cloud.toolkit.ui.dialog.CreateOrEditApiGatewayDialog
import yandex.cloud.toolkit.util.task.backgroundTask
import yandex.cloud.toolkit.util.showAuthenticationNotification

class CreateOrEditApiGatewayAction(val folder: CloudFolder, val gateway: CloudApiGateway?) : DumbAwareAction(
    if (gateway == null) "Create API Gateway" else "Edit",
    null,
    if (gateway == null) CloudIcons.Resources.ApiGateway else AllIcons.Actions.Edit
) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val authData = folder.user.authData

        if (authData == null) {
            project.showAuthenticationNotification()
            return
        }

        openDialog(project, authData, folder, gateway)
    }

    companion object {
        fun openDialog(project: Project, authData: CloudAuthData, folder: CloudFolder, gateway: CloudApiGateway?) {
            backgroundTask(project, "Yandex.Cloud") {
                text = "Preparing..."

                val spec = if (gateway != null) {
                    CloudOperationService.instance.fetchApiGatewaySpec(project, authData, gateway).tryPerform(this)
                } else ""

                val apiGateways by CloudOperationService.instance.fetchApiGateways(project, folder)

                runInEdt {
                    CreateOrEditApiGatewayDialog(
                        project,
                        authData,
                        folder,
                        gateway,
                        spec,
                        apiGateways
                    ).show()
                }
            }
        }
    }
}