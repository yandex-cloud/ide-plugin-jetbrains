package yandex.cloud.toolkit.ui.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import yandex.cloud.toolkit.api.resource.impl.model.CloudFunction
import yandex.cloud.toolkit.api.resource.impl.model.CloudFunctionVersion
import yandex.cloud.toolkit.api.resource.impl.user
import yandex.cloud.toolkit.api.service.CloudOperationService
import yandex.cloud.toolkit.util.task.backgroundTask
import yandex.cloud.toolkit.util.showAuthenticationNotification
import yandex.cloud.toolkit.util.showNoFunctionVersionsNotification

class GenerateFunctionRequestAction(var project: Project, val function: CloudFunction, val selectedTag: String?) :
    DumbAwareAction("Generate HTTP request", null, AllIcons.Actions.Edit) {

    override fun actionPerformed(e: AnActionEvent) {
        val authData = function.user.authData

        if (authData == null) {
            project.showAuthenticationNotification()
            return
        }

        fun generateFile() {
            val tag = selectedTag ?: CloudFunctionVersion.LATEST_TAG
            CloudOperationService.instance.generateFunctionRequestFile(project, authData, function, tag)
        }

        backgroundTask(project, "Yandex.Cloud Functions") {
            val versions by CloudOperationService.instance.fetchFunctionVersions(project, function)

            if (versions.isEmpty()) {
                project.showNoFunctionVersionsNotification(function, ::generateFile)
            } else {
                generateFile()
            }
        }
    }
}