package yandex.cloud.toolkit.process

import com.google.protobuf.ByteString
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.Compressor
import yandex.cloud.api.serverless.functions.v1.FunctionOuterClass
import yandex.cloud.toolkit.api.profile.impl.profileStorage
import yandex.cloud.toolkit.api.resource.get
import yandex.cloud.toolkit.api.resource.impl.model.CloudFunction
import yandex.cloud.toolkit.api.service.CloudRepository
import yandex.cloud.toolkit.api.service.awaitEnd
import yandex.cloud.toolkit.configuration.function.deploy.FunctionDeploySpec
import yandex.cloud.toolkit.ui.action.TestFunctionByIdAction
import yandex.cloud.toolkit.util.*
import yandex.cloud.toolkit.util.task.backgroundTask
import yandex.cloud.toolkit.util.task.notAuthenticatedError
import java.io.File

class FunctionDeployProcess(
    private val project: Project,
    private val spec: FunctionDeploySpec,
    private val controller: ProcessController,
    private val logger: ConsoleLogger
) {

    fun start() {
        controller.tryStartProcess()

        backgroundTask(project, "Deploying function '${spec.functionId}'", canBeCancelled = true) {
            val steps = steps(4) + controller + logger

            logger.println("Deploying function '${spec.functionId}'\n")

            logger.println(
                ("""
                Runtime: '${spec.runtime}'
                Entrypoint: '${spec.entryPoint}'
                Memory: ${spec.memoryBytes / 1024 / 1024}MB
                Timeout: ${spec.timeoutSeconds}s
                Service Account: ${if (spec.serviceAccountId.isNullOrEmpty()) "none" else "'${spec.serviceAccountId}'"}
                Tags: ${if (spec.tags.isEmpty()) "[]" else spec.tags.joinToString { "'$it'" }}
            """).trimIndent()
            )

            if (spec.envVariables.isNotEmpty()){
                logger.println("\nEnvironment Variables:")
                spec.envVariables.forEach {
                    logger.println("- ${it.key} = '${it.value}'")
                }
            }

            if (spec.secrets.isNotEmpty()) {
                logger.println("\nSecrets:")
                spec.secrets.forEach {
                    logger.println("- $it")
                }
            }

            if (spec.hasConnectivity()) {
                if (spec.useSubnets) {
                    logger.println("\nVPC Subnets:")
                    spec.subnets.forEach {
                        logger.println("- $it")
                    }
                } else {
                    logger.println("\nVPC Network: " + spec.networkId)
                }
            }

            logger.println(
                "\nSource Files: ${
                    "\n" + spec.sourceFiles.joinToString("") { "- $it\n" }
                }(Policy: ${SourceFolderPolicy.byId(spec.sourceFolderPolicy).displayName})"
            )

            val profile = project.profileStorage.profile
            val authData = profile?.getAuthData(toUse = true) ?: steps.notAuthenticatedError()

            if (spec.sourceFiles.isEmpty()) steps.error("No source files selected")

            logger.println()
            steps.next("Creating archive")

            val archiveFile = tryDo {
                FileUtil.createTempFile("yc-toolkit-function-deploy", spec.functionId, true)
            } onFail steps.handleError()

            steps.next("Packing source files")

            tryDo {
                Compressor.Zip(archiveFile).use { compressor ->
                    val sourceFolderPolicy = SourceFolderPolicy.byId(spec.sourceFolderPolicy)

                    fun pack(file: File, depth: Int) {
                        if (file.isDirectory) {
                            if (depth == 0 && sourceFolderPolicy == SourceFolderPolicy.EXTRACT || sourceFolderPolicy == SourceFolderPolicy.FLATTEN) {
                                file.listFiles()?.forEach { pack(it, depth + 1) }
                            } else {
                                compressor.addDirectory(file.name, file)
                            }
                            return
                        }

                        compressor.addFile(file.name, file)
                    }

                    for (sourceFile in spec.sourceFiles) {
                        val file = File(sourceFile)
                        pack(file, 0)
                    }
                }
            } onFail steps.handleError { "Failed to pack source files" }

            steps.next("Deploying function")

            val operation = CloudRepository.instance.createFunctionVersion(
                authData,
                spec,
                ByteString.readFrom(archiveFile.inputStream())
            )
            val error = operation.data.error
            if (error != null) steps.error(error.message)

            steps.next("Waiting for operation end")

            val operationResult = operation.awaitEnd(project, authData)

            when (val result = operationResult.result) {
                is JustValue -> {
                    val version = operationResult.operation.data.getOrNull()?.response?.unpack(
                        FunctionOuterClass.Version::class.java
                    )

                    logger.info("\nNew function version successfully created\n")
                    if (version != null) logger.info("Version ID: ${version.id}\n")

                    result.value.getActions().forEach(logger::printAction)

                    val tag = version?.tagsList?.first()
                    logger.printAction(
                        TestFunctionByIdAction(profile.resourceUser, spec.functionId ?: "", tag)
                    )
                }
                is NoValue -> steps.error(result.error.message ?: "Failed to deploy function")
            }

            controller.tryStopProcess(false)

            // should update function versions in explorer
            val function = CloudFunction.forUser(profile.resourceUser, spec.functionId ?: "").get()
            function?.update(project, false)
        }
    }
}