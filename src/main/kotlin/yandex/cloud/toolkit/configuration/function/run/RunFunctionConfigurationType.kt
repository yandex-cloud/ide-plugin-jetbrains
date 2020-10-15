package yandex.cloud.toolkit.configuration.function.run

import com.intellij.execution.configurations.SimpleConfigurationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import icons.CloudIcons

class RunFunctionConfigurationType : SimpleConfigurationType(
    "yandex-cloud-run-function",
    "Run YC Function",
    "run configuration for Yandex.Cloud function remote run",
    NotNullLazyValue.createValue {
        CloudIcons.Resources.Function
    }
) {

    override fun createTemplateConfiguration(project: Project) =
        RunFunctionConfiguration("Run Function", this, project)
}