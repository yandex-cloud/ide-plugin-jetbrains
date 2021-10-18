package yandex.cloud.toolkit.ui.action


import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import yandex.cloud.api.serverless.functions.v1.FunctionOuterClass
import yandex.cloud.toolkit.api.resource.findById
import yandex.cloud.toolkit.api.resource.impl.model.CloudFunction
import yandex.cloud.toolkit.api.resource.impl.user
import yandex.cloud.toolkit.api.service.CloudOperationService
import yandex.cloud.toolkit.configuration.function.deploy.DeployFunctionConfiguration
import yandex.cloud.toolkit.ui.dialog.FunctionDeployDialog
import yandex.cloud.toolkit.util.showAuthenticationNotification
import yandex.cloud.toolkit.util.task.backgroundTask

class RedeployFunctionVersionAction(var project: Project, val function: CloudFunction, val versionId: String) :
    DumbAwareAction("Redeploy", null, AllIcons.Actions.Upload) {

    override fun actionPerformed(e: AnActionEvent) {
        val authData = function.user.authData

        if (authData == null) {
            project.showAuthenticationNotification()
            return
        }

        backgroundTask(project, "Yandex.Cloud Functions") {
            text = "Preparing for function deploy..."

            val versions by CloudOperationService.instance.fetchFunctionVersions(project, function)
            val serviceAccounts by CloudOperationService.instance.fetchServiceAccounts(project, function.group.folder)
            val runtimes by CloudOperationService.instance.fetchRuntimes(project, function.user)
            val networks by CloudOperationService.instance.fetchVPCNetworks(project, function.group.folder)

            val templateVersion: FunctionOuterClass.Version? = versions.findById(versionId)?.data
            val configuration = DeployFunctionConfiguration.createTemplateConfiguration(project, templateVersion)

            runInEdt {
                FunctionDeployDialog(
                    project,
                    authData,
                    function,
                    versions,
                    serviceAccounts,
                    networks,
                    runtimes,
                    configuration,
                    useTemplateTags = true
                ).show()
            }
        }
    }
}