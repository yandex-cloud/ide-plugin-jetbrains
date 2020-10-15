package yandex.cloud.toolkit.ui.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.DumbAwareAction
import yandex.cloud.toolkit.api.resource.impl.model.CloudFunction
import yandex.cloud.toolkit.api.resource.impl.model.CloudFunctionVersion
import yandex.cloud.toolkit.api.resource.impl.model.latestVersion
import yandex.cloud.toolkit.api.resource.impl.user
import yandex.cloud.toolkit.api.service.CloudOperationService
import yandex.cloud.toolkit.ui.view.TrackFunctionLogsView
import yandex.cloud.toolkit.util.task.backgroundTask
import yandex.cloud.toolkit.util.showAuthenticationNotification
import yandex.cloud.toolkit.util.showNoFunctionVersionsNotification

class TrackFunctionLogsAction(val function: CloudFunction, val version: CloudFunctionVersion?) :
    DumbAwareAction("Track Logs", null, AllIcons.Actions.ChangeView) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val authData = function.user.authData

        if (authData == null) {
            project.showAuthenticationNotification()
            return
        }

        fun displayView(version: CloudFunctionVersion) = runInEdt {
            TrackFunctionLogsView(project, version).display()
        }

        if (version != null) {
            displayView(version)
            return
        }

        backgroundTask(project, "Yandex.Cloud Functions") {
            text = "Fetching function versions..."

            val versions by CloudOperationService.instance.fetchFunctionVersions(project, function)

            if (versions.isEmpty()) {
                project.showNoFunctionVersionsNotification(function, null)
            } else {
                val latestVersion = versions.latestVersion ?: return@backgroundTask
                displayView(latestVersion)
            }
        }
    }
}