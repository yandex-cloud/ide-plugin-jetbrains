package yandex.cloud.toolkit.ui.action

import com.intellij.execution.configurations.runConfigurationType
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import yandex.cloud.toolkit.api.resource.impl.model.CloudFunction
import yandex.cloud.toolkit.api.resource.impl.user
import yandex.cloud.toolkit.api.service.CloudOperationService
import yandex.cloud.toolkit.configuration.function.deploy.DeployFunctionConfiguration
import yandex.cloud.toolkit.configuration.function.deploy.DeployFunctionConfigurationType
import yandex.cloud.toolkit.configuration.function.deploy.FunctionDeployResources
import yandex.cloud.toolkit.ui.dialog.FunctionDeployDialog
import yandex.cloud.toolkit.util.showAuthenticationNotification
import yandex.cloud.toolkit.util.task.backgroundTask

class DeployFunctionAction(val project: Project, val function: CloudFunction) : ActionGroup(
    "Deploy",
    null,
    AllIcons.Actions.Upload
), DumbAware {

    private val configurations: List<DeployFunctionConfiguration>

    init {
        val runManager = RunManagerImpl.getInstanceImpl(project)
        val configurationType = runConfigurationType<DeployFunctionConfigurationType>()

        configurations = runManager.getConfigurationsList(configurationType).mapNotNull { it ->
            val configuration = it as? DeployFunctionConfiguration
            configuration?.takeIf { it.state.functionId == function.id }
        }

        isPopup = configurations.isNotEmpty()
    }

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        if (e == null) return EMPTY_ARRAY
        val actions = mutableListOf<AnAction>()

        actions += DeployAction(configurations.isNotEmpty())
        if (configurations.isNotEmpty()) {
            configurations.forEach { actions += UseRunConfiguration(it) }
        }

        return actions.toTypedArray()
    }

    private inner class UseRunConfiguration(val configuration: DeployFunctionConfiguration) : DumbAwareAction(
        "Use '${configuration.name}' Configuration",
        null,
        configuration.icon
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            openDeployDialog(e.project ?: return, configuration)
        }
    }

    private inner class DeployAction(hasConfigurations: Boolean) : DumbAwareAction(
        if (hasConfigurations) "Use Empty Template" else this@DeployFunctionAction.templateText,
        null,
        if (hasConfigurations) AllIcons.General.Add else this@DeployFunctionAction.templatePresentation.icon
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            openDeployDialog(e.project ?: return, null)
        }
    }

    private fun openDeployDialog(project: Project, template: DeployFunctionConfiguration?) {
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

            runInEdt {
                FunctionDeployDialog(
                    project,
                    authData,
                    function,
                    FunctionDeployResources(
                        versions,
                        serviceAccounts,
                        networks,
                        runtimes,
                    ),
                    template,
                    useTemplateTags = false
                ).show()
            }
        }
    }
}