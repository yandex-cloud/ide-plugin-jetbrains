package yandex.cloud.toolkit.configuration.apigateway.deploy

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import yandex.cloud.toolkit.process.ApiGatewayDeployProcess
import yandex.cloud.toolkit.process.RunContentController
import yandex.cloud.toolkit.util.logger

class DeployApiGatewayConfiguration(name: String?, factory: ConfigurationFactory, project: Project) :
    RunConfigurationMinimalBase<ApiGatewayDeploySpec>(name, factory, project) {

    override fun clone() = DeployApiGatewayConfiguration(name, factory, project).apply {
        state.copyFrom(this@DeployApiGatewayConfiguration.state)
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
        DeployApiGatewayConfigurationEditor(project, null)

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
        if (state.apiGatewayId.isNullOrEmpty()) {
            throw ExecutionException("No API gateway ID defined")
        }

        return RunProfileState { _, _ ->
            val console = TextConsoleBuilderFactory.getInstance().createBuilder(project).apply {
                setViewer(true)
            }.console

            val processController = RunContentController()
            val process = ApiGatewayDeployProcess(project, state, processController, console.logger)

            console.attachToProcess(processController)
            invokeLater(ModalityState.NON_MODAL, process::start)
            DefaultExecutionResult(console, processController)
        }
    }

    override fun getState(): ApiGatewayDeploySpec = options
}

class ApiGatewayDeploySpec : BaseState() {
    var apiGatewayId by string()
    var specFile by string()
}