package yandex.cloud.toolkit.ui.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.DumbAwareAction
import icons.CloudIcons
import yandex.cloud.toolkit.api.resource.impl.model.CloudFunction
import yandex.cloud.toolkit.api.resource.impl.user
import yandex.cloud.toolkit.api.service.CloudOperationService
import yandex.cloud.toolkit.ui.dialog.FunctionScalingPoliciesDialog
import yandex.cloud.toolkit.util.showAuthenticationNotification
import yandex.cloud.toolkit.util.task.backgroundTask

class EditFunctionScalingPoliciesAction(val function: CloudFunction) :
    DumbAwareAction("Scaling Policies", null, CloudIcons.Nodes.ScalingPolicy) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val authData = function.user.authData

        if (authData == null) {
            project.showAuthenticationNotification()
            return
        }

        backgroundTask(project, "Yandex.Cloud Functions") {
            val policies by CloudOperationService.instance.fetchFunctionScalingPolicies(project, function, useCache = false)
            val versions by CloudOperationService.instance.fetchFunctionVersions(project, function)

            runInEdt {
                FunctionScalingPoliciesDialog(project, authData, function, versions, policies).show()
            }
        }
    }
}