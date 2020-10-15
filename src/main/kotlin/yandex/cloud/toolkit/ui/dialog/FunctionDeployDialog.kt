package yandex.cloud.toolkit.ui.dialog

import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.RunConfigurationLevel
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import yandex.cloud.toolkit.api.auth.CloudAuthData
import yandex.cloud.toolkit.api.resource.impl.model.CloudFunction
import yandex.cloud.toolkit.api.resource.impl.model.CloudFunctionVersion
import yandex.cloud.toolkit.api.resource.impl.model.CloudServiceAccount
import yandex.cloud.toolkit.api.resource.impl.model.latestVersion
import yandex.cloud.toolkit.configuration.function.deploy.DeployFunctionConfiguration
import yandex.cloud.toolkit.configuration.function.deploy.DeployFunctionConfigurationEditor
import yandex.cloud.toolkit.util.disposeWith
import yandex.cloud.toolkit.util.text
import yandex.cloud.toolkit.util.withPreferredWidth
import java.awt.event.ActionEvent
import javax.swing.Action
import javax.swing.JComponent

class FunctionDeployDialog(
    val project: Project,
    val authData: CloudAuthData,
    val function: CloudFunction,
    val versions: List<CloudFunctionVersion>,
    val serviceAccounts: List<CloudServiceAccount>,
    runtimes: List<String>,
    templateConfiguration: DeployFunctionConfiguration?,
    useTemplateTags: Boolean,
) : DialogWrapper(true) {

    private val editor = DeployFunctionConfigurationEditor(project, function, versions, serviceAccounts, runtimes)
    private val saveConfigurationAction = SaveConfigurationAction()

    private val configuration: DeployFunctionConfiguration = templateConfiguration?.clone()
        ?: DeployFunctionConfiguration.createTemplateConfiguration(project, versions.latestVersion?.data)

    init {
        if (!useTemplateTags) configuration.state.tags.clear()
        configuration.name = "Deploy " + function.name

        okAction.text = "Deploy"

        editor.resetFrom(configuration)
        editor.disposeWith(myDisposable)
        init()
        title = "Deploy Yandex.Cloud Function"
    }

    override fun doValidate(): ValidationInfo? = editor.doValidate()

    override fun createCenterPanel(): JComponent = editor.component.withPreferredWidth(700)

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
                SaveConfigurationDialog(project, editor.snapshot).show()
            }
        }
    }
}