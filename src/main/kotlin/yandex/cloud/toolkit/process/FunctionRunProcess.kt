package yandex.cloud.toolkit.process

import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.json.psi.JsonElementGenerator
import com.intellij.json.psi.impl.JsonFileImpl
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.util.text.nullize
import org.apache.commons.lang.time.DurationFormatUtils
import yandex.cloud.toolkit.api.auth.CloudAuthData
import yandex.cloud.toolkit.api.profile.impl.profileStorage
import yandex.cloud.toolkit.api.resource.impl.model.CloudFunctionVersion
import yandex.cloud.toolkit.api.service.CloudOperationService
import yandex.cloud.toolkit.configuration.function.run.FunctionRunSpec
import yandex.cloud.toolkit.ui.action.ShowFunctionLogsByIdAction
import yandex.cloud.toolkit.ui.action.TrackFunctionLogsByIdAction
import yandex.cloud.toolkit.util.ConsoleLogger
import yandex.cloud.toolkit.util.doMaybe
import yandex.cloud.toolkit.util.task.backgroundTask
import yandex.cloud.toolkit.util.task.notAuthenticatedError
import yandex.cloud.toolkit.util.value
import java.util.*

class FunctionRunProcess(
    private val project: Project,
    private val spec: FunctionRunSpec,
    private val controller: ProcessController,
    private val logger: ConsoleLogger
) {

    fun start() {
        controller.tryStartProcess()

        backgroundTask(project, "Running function '${spec.functionId}'", canBeCancelled = true) {
            val steps = steps(1) + controller + logger
            logger.println("Running function '${spec.functionId}'\n")

            val functionId = spec.functionId ?: ""
            val requestId = UUID.randomUUID()
            val traceId = UUID.randomUUID()
            val invocationLink = CloudOperationService.instance.getFunctionInvocationLink(functionId)
            val versionTag = spec.versionTag.nullize() ?: CloudFunctionVersion.LATEST_TAG

            logger.println("Invocation Link: $invocationLink")
            logger.println("Request Authorized: " + spec.authorizeRequest)

            val profile = project.profileStorage.profile
            val authData = profile?.getAuthData(toUse = true)
            if (spec.authorizeRequest && authData == null) steps.notAuthenticatedError()

            logger.println("Request ID: $requestId")
            logger.println("Trace ID: $traceId")
            logger.println("Version Tag: $versionTag\n")

            if (spec.showRequest) {
                logger.print("Request Body: ")
                if (spec.request.isNullOrEmpty()) {
                    logger.println("empty")
                } else {
                    logger.user("\n" + spec.request + "\n")
                }
                logger.println()
            }

            val runRequest = FunctionRunRequest(
                requestId,
                traceId,
                if (spec.authorizeRequest) authData?.iamToken else null,
                versionTag,
                spec.request ?: ""
            )

            val httpRequest = tryDo {
                CloudOperationService.instance.runFunction(project, functionId, runRequest)
            } onFail steps.handleError { "Failed to perform invocation request" }

            val startTime = System.currentTimeMillis()
            var i = 0
            text = "Waiting for response"

            while (true) {
                i++
                Thread.sleep(100)

                val currentTime = System.currentTimeMillis()
                val passedTime = currentTime - startTime
                val formattedTime = DurationFormatUtils.formatDuration(passedTime, "mm:ss", true)
                logger.print("\r[${"|/-\\"[i / 2 % 4]}] Waiting for response. Time Passed - $formattedTime")

                if (httpRequest.isDone) {
                    break
                } else if (checkIfCancelled(controller)) {
                    logger.error("\rResponse waiting process aborted!")
                    return@backgroundTask
                }
            }

            val response = tryDo {
                httpRequest.response.get()
            } onFail steps.handleError { "Failed to fetch response" }

            val statusCode = response.status.statusCode
            val statusPhrase = response.status.reasonPhrase
            val passedTime = DurationFormatUtils.formatDuration(response.endTime - startTime, "mm:ss:SSS", true);

            logger.println("\r$statusCode ($statusPhrase); Content: ${response.contentLength}B; Time $passedTime")

            if (response.text.isNullOrEmpty()) {
                logger.println("> Response is empty <")
            } else if (!spec.formatResponse) {
                logger.info(response.text)
            } else {
                val formatted = doMaybe {
                    val jsonFile = runReadAction {
                        JsonElementGenerator(project).createDummyFile(response.text)
                    } as? JsonFileImpl

                    ReformatCodeProcessor(project, jsonFile, null, false).runWithoutProgress()
                    jsonFile?.text
                }
                logger.info(formatted.value ?: response.text)
            }

            if (profile != null) {
                logger.println()
                logger.printAction(TrackFunctionLogsByIdAction(profile.resourceUser, functionId, versionTag))
                logger.printAction(ShowFunctionLogsByIdAction(profile.resourceUser, functionId, versionTag))
            }

            controller.tryStopProcess(false)
        }
    }
}

class FunctionRunRequest(
    val requestId: UUID,
    val traceId: UUID,
    val iamToken: String?,
    val versionTag: String,
    val body: String
) {
    constructor(authData: CloudAuthData, versionTag: String, body: String) : this(
        UUID.randomUUID(),
        UUID.randomUUID(),
        authData.iamToken,
        versionTag,
        body
    )
}