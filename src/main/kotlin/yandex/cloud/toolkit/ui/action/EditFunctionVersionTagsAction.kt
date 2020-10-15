package yandex.cloud.toolkit.ui.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.DumbAwareAction
import icons.CloudIcons
import yandex.cloud.toolkit.api.resource.impl.model.CloudFunctionVersion
import yandex.cloud.toolkit.api.resource.impl.user
import yandex.cloud.toolkit.api.service.CloudOperationService
import yandex.cloud.toolkit.ui.dialog.EditFunctionVersionTagsDialog
import yandex.cloud.toolkit.util.task.backgroundTask
import yandex.cloud.toolkit.util.showAuthenticationNotification

class EditFunctionVersionTagsAction(val version: CloudFunctionVersion) :
    DumbAwareAction("Edit Tags", null, CloudIcons.Nodes.Label) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val authData = version.user.authData

        if (authData == null) {
            project.showAuthenticationNotification()
            return
        }

        backgroundTask(project, "Yandex.Cloud Functions") {
            val versions by CloudOperationService.instance.fetchFunctionVersions(project, version.function)

            runInEdt {
                EditFunctionVersionTagsDialog(project, authData, versions, version).show()
            }
        }
    }
}