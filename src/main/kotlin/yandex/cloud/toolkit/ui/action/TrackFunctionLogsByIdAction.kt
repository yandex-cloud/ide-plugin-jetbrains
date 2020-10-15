package yandex.cloud.toolkit.ui.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.DumbAwareAction
import yandex.cloud.toolkit.api.resource.impl.model.CloudUser
import yandex.cloud.toolkit.api.resource.impl.model.findWithTag
import yandex.cloud.toolkit.api.service.CloudOperationService
import yandex.cloud.toolkit.ui.view.TrackFunctionLogsView
import yandex.cloud.toolkit.util.showAuthenticationNotification
import yandex.cloud.toolkit.util.showNoFunctionVersionWithTagNotification
import yandex.cloud.toolkit.util.task.backgroundTask

class TrackFunctionLogsByIdAction(val user: CloudUser, val functionId: String, val versionTag: String) :
    DumbAwareAction("Track Logs") {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val authData = user.authData

        if (authData == null) {
            project.showAuthenticationNotification()
            return
        }

        backgroundTask(project, "Yandex Cloud Functions") {
            text = "Fetching function versions..."

            val function by CloudOperationService.instance.fetchFunction(project, user.dummyFolder, functionId)
            val versions by CloudOperationService.instance.fetchFunctionVersions(project, function, useCache = false)

            runInEdt {
                val version = versions.findWithTag(versionTag)
                if (version == null) {
                    project.showNoFunctionVersionWithTagNotification(function, versionTag)
                    return@runInEdt
                }
                TrackFunctionLogsView(project, version).display()
            }
        }
    }
}