package yandex.cloud.toolkit.process

import com.intellij.openapi.project.Project
import yandex.cloud.toolkit.api.profile.impl.profileStorage
import yandex.cloud.toolkit.api.resource.impl.model.CloudApiGatewayUpdate
import yandex.cloud.toolkit.api.service.CloudRepository
import yandex.cloud.toolkit.api.service.awaitEnd
import yandex.cloud.toolkit.configuration.apigateway.deploy.ApiGatewayDeploySpec
import yandex.cloud.toolkit.util.*
import yandex.cloud.toolkit.util.task.backgroundTask
import yandex.cloud.toolkit.util.task.notAuthenticatedError
import java.io.File
import java.nio.charset.Charset

class ApiGatewayDeployProcess(
    private val project: Project,
    private val spec: ApiGatewayDeploySpec,
    private val controller: ProcessController,
    private val logger: ConsoleLogger
) {

    fun start() {
        controller.tryStartProcess()

        backgroundTask(project, "Deploying API gateway", canBeCancelled = true) {
            val steps = steps(3) + controller + logger

            logger.println("Deploying API gateway '${spec.apiGatewayId}'")

            if (spec.specFile.isNullOrEmpty()) steps.error("No spec file selected")
            logger.println("\nSpecification: ${spec.specFile}\n")

            val profile = project.profileStorage.profile
            val authData = profile?.getAuthData(toUse = true) ?: steps.notAuthenticatedError()

            steps.next("Reading specification")

            val specFile = File(spec.specFile ?: "")
            val apiGatewaySpec = tryDo {
                specFile.readText(Charset.defaultCharset())
            } onFail steps.handleError { "Failed to read specification from file" }

            steps.next("Updating API gateway specification")

            val operation =   CloudRepository.instance.updateApiGateway(
                authData,
                spec.apiGatewayId ?: "",
                CloudApiGatewayUpdate(
                    openapiSpec = apiGatewaySpec
                )
            )
            val error = operation.data.error
            if (error != null) steps.error(error.message)

            steps.next("Waiting for operation end")

            val operationResult = operation.awaitEnd(project, authData)
            when (val result = operationResult.result) {
                is JustValue -> {
                    logger.info("\nAPI gateway specification successfully updated\n")
                    result.value.getActions().forEach(logger::printAction)
                }
                is NoValue -> steps.error(result.error.message ?: "Failed to update API gateway specification")
            }

            controller.tryStopProcess(false)
        }
    }
}
