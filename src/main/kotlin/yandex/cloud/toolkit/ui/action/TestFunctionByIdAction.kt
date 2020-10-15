package yandex.cloud.toolkit.ui.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.DumbAwareAction
import yandex.cloud.toolkit.api.resource.impl.model.CloudUser
import yandex.cloud.toolkit.api.service.CloudOperationService
import yandex.cloud.toolkit.ui.dialog.RunFunctionDialog
import yandex.cloud.toolkit.util.task.backgroundTask
import yandex.cloud.toolkit.util.showAuthenticationNotification

class TestFunctionByIdAction(val user: CloudUser, val functionId: String, private val selectedTag: String?) :
    DumbAwareAction("Test Function", null, AllIcons.Toolwindows.ToolWindowRun) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val authData = user.authData

        if (authData == null) {
            project.showAuthenticationNotification()
            return
        }

        backgroundTask(project, "Yandex.Cloud Functions") {
            text = "Preparing for function test..."

            val function by CloudOperationService.instance.fetchFunction(project, user.dummyFolder, functionId)
            val versions by CloudOperationService.instance.fetchFunctionVersions(project, function, useCache = false)

            runInEdt {
                RunFunctionDialog(project, authData, function, versions, selectedTag).show()
            }
        }
    }
}