package yandex.cloud.toolkit.ui.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.DumbAwareAction
import yandex.cloud.toolkit.api.resource.impl.model.CloudFunction
import yandex.cloud.toolkit.api.resource.impl.model.CloudFunctionVersion
import yandex.cloud.toolkit.api.resource.impl.user
import yandex.cloud.toolkit.api.service.CloudOperationService
import yandex.cloud.toolkit.ui.dialog.RunFunctionDialog
import yandex.cloud.toolkit.util.task.backgroundTask
import yandex.cloud.toolkit.util.showAuthenticationNotification
import yandex.cloud.toolkit.util.showNoFunctionVersionsNotification

class RunFunctionAction(val function: CloudFunction, private val selectedTag: String?) :
    DumbAwareAction("Run", null, AllIcons.Toolwindows.ToolWindowRun) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val authData = function.user.authData

        if (authData == null) {
            project.showAuthenticationNotification()
            return
        }

        fun openDialog(versions: List<CloudFunctionVersion>) = runInEdt {
            RunFunctionDialog(project, authData, function, versions, selectedTag).show()
        }

        backgroundTask(project, "Yandex.Cloud Functions") {
            text = "Preparing for function running..."

            val versions by CloudOperationService.instance.fetchFunctionVersions(project, function)

            if (versions.isEmpty()) {
                project.showNoFunctionVersionsNotification(function) {
                    openDialog(versions)
                }
            } else {
                openDialog(versions)
            }
        }
    }
}