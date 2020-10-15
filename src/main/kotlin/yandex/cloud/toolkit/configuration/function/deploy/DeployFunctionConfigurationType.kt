package yandex.cloud.toolkit.configuration.function.deploy

import com.intellij.execution.configurations.SimpleConfigurationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import icons.CloudIcons

class DeployFunctionConfigurationType : SimpleConfigurationType(
    "yandex-cloud-deploy-function",
    "Deploy YC Function",
    "run configuration for Yandex.Cloud function deployment",
    NotNullLazyValue.createValue {
        CloudIcons.Resources.Function
    }
) {
    override fun createTemplateConfiguration(project: Project) =
        DeployFunctionConfiguration("Deploy Function", this, project).apply {
            loadState(FunctionDeploySpec.template())
        }
}