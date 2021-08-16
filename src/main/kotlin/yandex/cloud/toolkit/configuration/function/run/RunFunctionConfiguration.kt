package yandex.cloud.toolkit.configuration.function.run

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationMinimalBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import yandex.cloud.toolkit.process.FunctionRunProcess
import yandex.cloud.toolkit.process.RunContentController
import yandex.cloud.toolkit.util.logger

class RunFunctionConfiguration(name: String?, factory: ConfigurationFactory, project: Project) :
    RunConfigurationMinimalBase<FunctionRunSpec>(name, factory, project) {

    override fun clone() = RunFunctionConfiguration(name, factory, project).apply {
        state.copyFrom(this@RunFunctionConfiguration.state)
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
        RunFunctionConfigurationEditor(project, null)

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        if (state.functionId.isNullOrEmpty()) throw ExecutionException("No function ID defined")

        return RunProfileState { _, _ ->
            val console = TextConsoleBuilderFactory.getInstance().createBuilder(project).apply {
                setViewer(true)
            }.console

            val processController = RunContentController()
            val process = FunctionRunProcess(project, state, processController, console.logger)

            console.attachToProcess(processController)
            invokeLater(ModalityState.NON_MODAL, process::start)
            DefaultExecutionResult(console, processController)
        }
    }

    override fun getState(): FunctionRunSpec = options
}

class FunctionRunSpec : BaseState() {
    var functionId by string()
    var versionTag by string()
    var request by string()
    var authorizeRequest by property(true)
    var showRequest by property(true)
    var formatResponse by property(true)
}