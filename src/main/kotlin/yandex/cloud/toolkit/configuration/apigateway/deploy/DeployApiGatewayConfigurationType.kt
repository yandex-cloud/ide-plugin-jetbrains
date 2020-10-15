package yandex.cloud.toolkit.configuration.apigateway.deploy

import com.intellij.execution.configurations.SimpleConfigurationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import icons.CloudIcons

class DeployApiGatewayConfigurationType : SimpleConfigurationType(
    "yandex-cloud-deploy-apigw",
    "Deploy YC API Gateway",
    "run configuration for Yandex.Cloud API gateway deployment",
    NotNullLazyValue.createValue {
        CloudIcons.Resources.ApiGateway
    }
) {
    override fun createTemplateConfiguration(project: Project) =
        DeployApiGatewayConfiguration("Deploy API Gateway", this, project)
}