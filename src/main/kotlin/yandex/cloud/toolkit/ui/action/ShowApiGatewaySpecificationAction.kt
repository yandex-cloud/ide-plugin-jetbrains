package yandex.cloud.toolkit.ui.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.DumbAwareAction
import yandex.cloud.toolkit.api.resource.impl.model.CloudApiGateway
import yandex.cloud.toolkit.api.resource.impl.user
import yandex.cloud.toolkit.api.service.CloudOperationService
import yandex.cloud.toolkit.ui.dialog.ApiGatewaySpecificationDialog
import yandex.cloud.toolkit.util.task.backgroundTask
import yandex.cloud.toolkit.util.showAuthenticationNotification

class ShowApiGatewaySpecificationAction(val gateway: CloudApiGateway) : DumbAwareAction(
    "View Specification", null, AllIcons.Actions.ShowCode
) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val authData = gateway.user.authData

        if (authData == null) {
            project.showAuthenticationNotification()
            return
        }

        backgroundTask(project, "Yandex.Cloud") {
            text = "Preparing..."

            val spec by CloudOperationService.instance.fetchApiGatewaySpec(project, authData, gateway)

            runInEdt {
                ApiGatewaySpecificationDialog(project, authData, gateway, spec).show()
            }
        }
    }
}