package yandex.cloud.toolkit.ui.dialog

import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.RunConfigurationLevel
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import yandex.cloud.toolkit.api.auth.CloudAuthData
import yandex.cloud.toolkit.api.resource.impl.model.CloudFunction
import yandex.cloud.toolkit.api.resource.impl.model.latestVersion
import yandex.cloud.toolkit.api.resource.impl.user
import yandex.cloud.toolkit.api.service.CloudOperationService
import yandex.cloud.toolkit.configuration.function.deploy.DeployFunctionConfiguration
import yandex.cloud.toolkit.configuration.function.deploy.DeployFunctionConfigurationEditor
import yandex.cloud.toolkit.configuration.function.deploy.FunctionDeployResources
import yandex.cloud.toolkit.util.disposeWith
import yandex.cloud.toolkit.util.showAuthenticationNotification
import yandex.cloud.toolkit.util.task.backgroundTask
import yandex.cloud.toolkit.util.text
import yandex.cloud.toolkit.util.withPreferredWidth
import java.awt.event.ActionEvent
import javax.swing.Action
import javax.swing.JComponent

class FunctionDeployDialog(
    val project: Project,
    val authData: CloudAuthData,
    val function: CloudFunction,
    val resources: FunctionDeployResources,
    templateConfiguration: DeployFunctionConfiguration?,
    useTemplateTags: Boolean,
) : DialogWrapper(true) {

    private val editor = DeployFunctionConfigurationEditor(project, function, resources)
    private val saveConfigurationAction = SaveConfigurationAction()

    private val configuration: DeployFunctionConfiguration = templateConfiguration?.clone()
        ?: DeployFunctionConfiguration.createTemplateConfiguration(project, resources.versions?.latestVersion?.data)

    init {
        if (!useTemplateTags) configuration.state.tags.clear()
        configuration.name = "Deploy " + function.name

        okAction.text = "Deploy"

        editor.disposeWith(myDisposable)

        init()
        title = "Deploy Yandex.Cloud Function"
    }

    override fun doValidate(): ValidationInfo? = editor.doValidate()

    override fun createCenterPanel(): JComponent {
        val panel = editor.component.withPreferredWidth(700)
        editor.resetFrom(configuration)
        return panel
    }

    override fun doOKAction() {
        if (!okAction.isEnabled) return

        val configuration = configuration.clone()
        editor.applyTo(configuration)
        val settings = RunnerAndConfigurationSettingsImpl(
            RunManagerImpl.getInstanceImpl(project),
            configuration,
            isTemplate = false,
            RunConfigurationLevel.TEMPORARY
        )

        val executor = DefaultRunExecutor.getRunExecutorInstance()
        ProgramRunnerUtil.executeConfiguration(settings, executor)

        super.doOKAction()
    }

    override fun createLeftSideActions(): Array<Action> = arrayOf(saveConfigurationAction)

    private inner class SaveConfigurationAction : DialogWrapperAction("Save Configuration") {
        override fun doAction(e: ActionEvent?) {
            close(CANCEL_EXIT_CODE)

            invokeLater {
                val configuration = configuration.clone()
                editor.applyTo(configuration)
                SaveConfigurationDialog(project, configuration).show()
            }
        }
    }

    companion object {
        fun createAndShow(
            project: Project,
            function: CloudFunction,
            configuration: DeployFunctionConfiguration
        ) {
            val authData = function.user.authData

            if (authData == null) {
                project.showAuthenticationNotification()
                return
            }

            backgroundTask(project, "Yandex.Cloud Functions") {
                text = "Preparing for function deploy..."

                val versions by CloudOperationService.instance.fetchFunctionVersions(project, function)
                val serviceAccounts by CloudOperationService.instance.fetchServiceAccounts(
                    project,
                    function.group.folder
                )
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
                        configuration,
                        useTemplateTags = true
                    ).show()
                }
            }
        }
    }
}
