package yandex.cloud.toolkit.ui.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.DumbAwareAction
import icons.CloudIcons
import yandex.cloud.toolkit.api.resource.impl.model.CloudFolder
import yandex.cloud.toolkit.api.resource.impl.model.CloudFunction
import yandex.cloud.toolkit.api.resource.impl.user
import yandex.cloud.toolkit.api.service.CloudOperationService
import yandex.cloud.toolkit.ui.dialog.CreateOrEditFunctionDialog
import yandex.cloud.toolkit.util.task.backgroundTask
import yandex.cloud.toolkit.util.showAuthenticationNotification

class CreateOrEditFunctionAction(val folder: CloudFolder, val function: CloudFunction?) : DumbAwareAction(
    if (function == null) "Create Cloud Function" else "Edit",
    null,
    if (function == null) CloudIcons.Resources.Function else AllIcons.Actions.Edit
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

            val functions by CloudOperationService.instance.fetchFunctions(project, folder)

            runInEdt {
                CreateOrEditFunctionDialog(
                    project,
                    authData,
                    folder,
                    function,
                    functions
                ).show()
            }
        }
    }
}