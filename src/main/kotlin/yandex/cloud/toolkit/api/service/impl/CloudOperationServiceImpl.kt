package yandex.cloud.toolkit.api.service.impl

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.net.ssl.CertificateManager
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.utils.URIBuilder
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import yandex.cloud.api.access.Access
import yandex.cloud.api.iam.v1.ServiceAccountOuterClass
import yandex.cloud.toolkit.api.auth.CloudAuthData
import yandex.cloud.toolkit.api.resource.getOrLoad
import yandex.cloud.toolkit.api.resource.impl.*
import yandex.cloud.toolkit.api.resource.impl.model.*
import yandex.cloud.toolkit.api.service.CloudOperationService
import yandex.cloud.toolkit.api.service.CloudRepository
import yandex.cloud.toolkit.api.service.awaitEnd
import yandex.cloud.toolkit.api.util.LogLine
import yandex.cloud.toolkit.process.FunctionRunRequest
import yandex.cloud.toolkit.util.*
import yandex.cloud.toolkit.util.remote.list.RemoteList
import yandex.cloud.toolkit.util.remote.list.RemoteListPointer
import yandex.cloud.toolkit.util.remote.list.loadRemoteList
import yandex.cloud.toolkit.util.remote.resource.UnauthenticatedException
import yandex.cloud.toolkit.util.task.*
import java.util.*
import java.util.concurrent.CompletableFuture


class CloudOperationServiceImpl : CloudOperationService {

    companion object {
        private const val CONNECTION_TIMEOUT = 60 * 100
        private const val SOCKET_TIMEOUT = 300 * 1000

        private val DEFAULT_FUNCTION_INVOKE_URL = "https://functions.yandexcloud.net/"
        private val BACKGROUND_TASK_TITLE = "Yandex.Cloud"
    }

    private var httpClient: CloseableHttpClient = HttpClients.custom()
        .setDefaultRequestConfig(
            RequestConfig.custom()
                .setConnectTimeout(CONNECTION_TIMEOUT)
                .setSocketTimeout(SOCKET_TIMEOUT)
                .build()
        )
        .setUserAgent(CloudAuthData.USER_AGENT)
        .setSSLContext(CertificateManager.getInstance().sslContext)
        .build()

    override fun fetchOperationLogs(operationId: String, url: String) =
        tryLazy("Fetching operation logs...") {
            val logsDir = FileUtil.createTempDirectory("yc-operation", null)
            val logsFile = logsDir.resolve("$operationId.log")
            val request = HttpGet(url)

            logsFile.outputStream().use { logsStream ->
                httpClient.execute(request) {
                    it.entity.writeTo(logsStream)
                }
            }

            logsFile
        } onError {
            "Can not fetch operation log file:\n${it.message}"
        }

    override fun showOperationLogs(project: Project, operationId: String, url: String) {
        backgroundTask(project, "Yandex.Cloud Operation Logs") {
            val logsFile by fetchOperationLogs(operationId, url)
            val virtualLogsFile by logsFile.asVirtual()

            runInEdt {
                val editorManager = FileEditorManager.getInstance(project)
                editorManager.openFile(virtualLogsFile, true)
            }
        }
    }

    override fun getDefaultFunctionInvokeUrl(functionId: String): String =
        DEFAULT_FUNCTION_INVOKE_URL + functionId

    override fun runFunction(
        project: Project,
        functionId: String,
        request: FunctionRunRequest
    ): AsyncHttpRequest<SimpleHttpResponse> {
        val future = CompletableFuture<SimpleHttpResponse>()

        val url = URIBuilder(request.invokeUrl).apply {
            setParameter("integration", "raw")
            setParameter("tag", request.versionTag)
        }.build()

        val httpRequest = HttpPost(url).apply {
            if (!request.iamToken.isNullOrEmpty()) addHeader("Authorization", "Bearer ${request.iamToken}")
            addHeader("X-Client-Trace-Id", request.traceId.toString())
            addHeader("X-Client-Request-Id", request.requestId.toString())
            addHeader("X-Trace-Id", request.traceId.toString())
            addHeader("X-Request-Id", request.requestId.toString())

            this.entity = StringEntity(request.body)
        }

        backgroundTask(project, "Waiting for function '$functionId' response") {
            tryDo {
                httpClient.execute(httpRequest) {
                    future.complete(it.asSimple())
                }
            } onFail {
                future.completeExceptionally(it)
            }
        }

        return AsyncHttpRequest(future, httpRequest::abort)
    }

    override fun generateFunctionRequestFile(
        project: Project,
        authData: CloudAuthData,
        function: CloudFunction,
        tag: String
    ) {
        val errorMessage = "Can not create function '$function' request file:"

        backgroundTask(project, "Yandex.Cloud Function Run") {
            text = "Creating http request file..."

            val tempDir = tryDo {
                FileUtil.createTempDirectory("yc-function", null)
            } notifyOnFail {
                "$errorMessage\nFailed to create temp directory:${it.message}"
            }

            val configFile = tempDir.resolve("http-client.private.env.json")

            val url = tryDo {
                val url = URIBuilder(function.data.httpInvokeUrl).apply {
                    setParameter("integration", "raw")
                    setParameter("tag", tag)
                }.build().toString()

                val iamToken = authData.iamToken
                val traceId = UUID.randomUUID().toString()
                val requestId = UUID.randomUUID().toString()

                configFile.printWriter().use {
                    it.println(
                        """
                    { 
                        "authorized" : {
                            "token": "Bearer $iamToken",
                            "trace-id": "$traceId",
                            "request-id": "$requestId"
                        }
                    }
                """.trimIndent()
                    )
                }

                url
            } notifyOnFail {
                "$errorMessage\nFailed to create config file for http client:${it.message}"
            }

            val httpFile = tempDir.resolve("$function.http")

            tryDo {
                httpFile.printWriter().use {
                    it.println("POST $url")

                    it.println("Content-Type: ${ContentType.TEXT_PLAIN.mimeType}")
                    it.println("Authorization: {{token}}")
                    it.println("X-Client-Trace-Id: {{trace-id}}")
                    it.println("X-Client-Request-Id: {{request-id}}")
                    it.println("X-Trace-Id: {{trace-id}}")
                    it.println("X-Request-Id: {{request-id}}")
                    it.println("\n# your content here")
                }
            } notifyOnFail {
                "$errorMessage\nFailed to create http file:${it.message}"
            }

            val virtualRequestFile by httpFile.asVirtual()

            runInEdt {
                val editorManager = FileEditorManager.getInstance(project)
                editorManager.openFile(virtualRequestFile, true)
            }
        }
    }

    override fun fetchFunction(project: Project, folder: CloudFolder, functionId: String): LazyTask<CloudFunction> =
        folder.functionGroup.getOrLoad(
            CloudFunctionGroup.Function,
            project,
            "Fetching function '$functionId'"
        ) {
            CloudFunctionLoader(functionId)
        } onError {
            "Failed to fetch function '$functionId' at folder '${folder.id}'"
        }

    override fun fetchFunctionVersions(project: Project, function: CloudFunction, useCache: Boolean) =
        function.getOrLoad(
            CloudFunction.FunctionVersions,
            project,
            "Fetching '$function' function versions",
            !useCache
        ) {
            CloudFunctionVersionsLoader
        } onError {
            "Failed to fetch previous '$function' function versions"
        }

    override fun fetchFunctionScalingPolicies(project: Project, function: CloudFunction, useCache: Boolean) =
        function.getOrLoad(
            CloudFunction.ScalingPolicies,
            project,
            "Fetching '$function' function scaling policies",
            !useCache
        ) {
            CloudFunctionScalingPoliciesLoader
        } onError {
            "Failed to fetch '$function' function scaling policies"
        }

    override fun fetchServiceAccounts(project: Project, folder: CloudFolder) =
        folder.serviceAccountGroup.getOrLoad(
            CloudServiceAccountGroup.ServiceAccounts,
            project,
            if (folder.isVirtual) "Fetching service accounts" else "Fetching '$folder' service accounts"
        ) {
            CloudServiceAccountsLoader
        } onError {
            "Failed to fetch service accounts of folder '$folder'"
        }

    override fun fetchApiGateways(project: Project, folder: CloudFolder) =
        folder.gatewayGroup.getOrLoad(
            CloudGatewayGroup.ApiGateways,
            project,
            "Fetching '$folder' API gateways"
        ) {
            CloudApiGatewaysLoader
        } onError {
            "Failed to fetch API gateways of folder '$folder'"
        }

    override fun fetchFunctions(project: Project, folder: CloudFolder) =
        folder.functionGroup.getOrLoad(
            CloudFunctionGroup.Functions,
            project,
            "Fetching '$folder' functions"
        ) {
            CloudFunctionsLoader
        } onError {
            "Failed to fetch cloud functions of folder '$folder'"
        }

    override fun fetchVPCNetworks(project: Project, folder: CloudFolder) =
        folder.networkGroup.getOrLoad(
            VPCNetworkGroup.Networks,
            project,
            if (folder.isVirtual) "Fetching networks" else "Fetching '$folder' networks"
        ) {
            VPCNetworksLoader
        } onError {
            "Failed to fetch networks of folder '$folder'"
        }

    override fun fetchFolderSubnets(user: CloudUser, folderId: String) = doLazy("Fetching subnets...") {
        val authData = user.authData ?: throw UnauthenticatedException()
        loadRemoteList {
            CloudRepository.instance.getFolderSubnets(authData, folderId, it)
        }.mapValue { it.sortedBy { subnet -> subnet.name } }
    } onError {
        "Failed to fetch subnets of folder '$folderId'"
    }

    override fun fetchRuntimes(project: Project, user: CloudUser) =
        user.getOrLoad(CloudUser.AvailableRuntimes, project, "Fetching available runtimes") {
            AvailableRuntimesLoader
        } onError {
            "Failed to fetch list of runtimes"
        }

    override fun fetchRoles(project: Project, user: CloudUser) =
        user.getOrLoad(CloudUser.AvailableRoles, project, "Fetching available roles") {
            AvailableRolesLoader
        } onError {
            "Failed to fetch list of roles"
        }

    override fun fetchFolderAccessBindings(project: Project, folder: CloudFolder) =
        folder.getOrLoad(CloudFolder.AccessBindings, project, "Fetching folder access bindings") {
            FolderAccessBindingsLoader
        } onError {
            "Failed to fetch folder access bindings"
        }

    override fun fetchFunctionLogs(
        authData: CloudAuthData,
        logGroupId: String,
        streamName: String,
        fromSecondsIn: Long,
        toSecondsEx: Long,
        pointer: RemoteListPointer
    ): LazyTask<RemoteList<LogLine>> =
        tryLazy("Reading logs...") {
            CloudRepository.instance.readLogs(
                authData, logGroupId, streamName, fromSecondsIn, toSecondsEx - 1, pointer
            ).map(::LogLine)
        } onError {
            "Failed to read function '$streamName' logs: ${it.message}"
        }

    override fun setFunctionVersionTags(
        project: Project,
        authData: CloudAuthData,
        version: CloudFunctionVersion,
        tags: Set<String>
    ) {
        val oldTags = version.data.tagsList.toSet()
        val addedTags = tags.filter { it !in oldTags }
        val removedTags = oldTags.filter { it !in tags }
        if (addedTags.isEmpty() && removedTags.isEmpty()) return

        backgroundTask(project, "Yandex Cloud Functions") {
            text = "Updating version tags..."

            val operations = mutableListOf<CloudOperation>()

            for (tag in removedTags) {
                operations += CloudRepository.instance.removeFunctionTag(authData, version.id, tag)
            }

            for (tag in addedTags) {
                operations += CloudRepository.instance.setFunctionTag(authData, version.id, tag)
            }

            val operationResults = operations.awaitEnd(project, authData)
            val versionName = "${version.function.name}/${version.id}"

            if (operationResults.none { it.result.hasError }) {
                operationResults.notifySuccess(project, "Function version tags updated: '$versionName'")
                version.function.update(project, false)
            } else {
                operationResults.notifyError(project, "Failed to set one or more tags of version '$versionName'")
            }
        }
    }

    override fun setFolderAccessBindings(
        authData: CloudAuthData,
        folder: CloudFolder,
        oldAccessBindings: Set<Access.AccessBinding>,
        newAccessBindings: Set<Access.AccessBinding>
    ): CloudOperation? {
        val addedAccessBindings = newAccessBindings.filter { it !in oldAccessBindings }
        val removedAccessBindings = oldAccessBindings.filter { it !in newAccessBindings }
        if (addedAccessBindings.isEmpty() && removedAccessBindings.isEmpty()) return null

        val accessBindingDeltas = mutableListOf<Access.AccessBindingDelta>()

        addedAccessBindings.forEach {
            accessBindingDeltas += Access.AccessBindingDelta.newBuilder()
                .setAccessBinding(it)
                .setAction(Access.AccessBindingAction.ADD)
                .build()
        }

        removedAccessBindings.forEach {
            accessBindingDeltas += Access.AccessBindingDelta.newBuilder()
                .setAccessBinding(it)
                .setAction(Access.AccessBindingAction.REMOVE)
                .build()
        }

        return CloudRepository.instance.updateFolderAccessBindings(authData, folder.id, accessBindingDeltas)
    }

    override fun setFunctionScalingPolicies(
        project: Project,
        authData: CloudAuthData,
        function: CloudFunction,
        oldPolicies: Map<String, CloudFunctionScalingPolicy>,
        newPolicies: Map<String, CloudFunctionScalingPolicy>
    ) {
        val removedPolicies = oldPolicies.values.filter { it.tag !in newPolicies }
        val changedPolicies = newPolicies.values.filter { it != oldPolicies[it.tag] }
        if (removedPolicies.isEmpty() && changedPolicies.isEmpty()) return

        backgroundTask(project, BACKGROUND_TASK_TITLE) {
            text = "Updating function scaling policies..."

            val operations = mutableListOf<CloudOperation>()
            operations += removedPolicies.map {
                CloudRepository.instance.removeFunctionScalingPolicy(
                    authData,
                    function.id,
                    it.tag
                )
            }
            operations += changedPolicies.map {
                CloudRepository.instance.setFunctionScalingPolicy(
                    authData,
                    function.id,
                    it
                )
            }

            val operationResults = operations.awaitEnd(project, authData)

            if (operationResults.none { it.result.hasError }) {
                operationResults.notifySuccess(project, "Function '${function.name}' scaling policies updated")
                function.updateScalingPolicies(project)
            } else {
                operationResults.notifyError(project, "Failed to update function '${function.name}' scaling policies")
            }
        }
    }

    override fun createServiceAccount(
        project: Project,
        authData: CloudAuthData,
        folder: CloudFolder,
        spec: CloudServiceAccountSpec
    ) {
        backgroundTask(project, BACKGROUND_TASK_TITLE) {
            text = "Creating service account..."

            val createOperationResult = CloudRepository.instance.createServiceAccount(
                authData,
                folder.id,
                spec.name,
                spec.description
            ).awaitEnd(project, authData)

            val operationResults = mutableListOf(createOperationResult)

            if (createOperationResult.isSuccess && spec.roles.isNotEmpty()) {
                text = "Setting service account roles..."

                val serviceAccount = createOperationResult.operation.data.value!!.response.unpack(
                    ServiceAccountOuterClass.ServiceAccount::class.java
                )

                val accessBindings = spec.roles.map {
                    Access.AccessBindingDelta.newBuilder().apply {
                        accessBinding = Access.AccessBinding.newBuilder().apply {
                            roleId = it
                            subject = Access.Subject.newBuilder().apply {
                                type = AccessBindingType.SERVICE_ACCOUNT.id
                                id = serviceAccount.id
                            }.build()
                        }.build()
                        action = Access.AccessBindingAction.ADD
                    }.build()
                }

                val setAccessBindingsOperationResult = CloudRepository.instance.updateFolderAccessBindings(
                    authData,
                    folder.id,
                    accessBindings
                ).awaitEnd(project, authData)

                operationResults += setAccessBindingsOperationResult
            }

            if (operationResults.none { it.result.hasError }) {
                operationResults.notifySuccess(project, "Service account '${spec.name}' created")
                folder.onServiceAccountCreated(project)
            } else {
                operationResults.notifyError(project, "Failed to create service account '${spec.name}'")
            }
        }
    }

    override fun updateServiceAccount(
        project: Project,
        authData: CloudAuthData,
        folderAccessBindings: List<Access.AccessBinding>,
        serviceAccount: CloudServiceAccount,
        spec: CloudServiceAccountSpec
    ) {
        val update = CloudServiceAccountUpdate(
            if (serviceAccount.name != spec.name) spec.name else null,
            if (serviceAccount.data.description != spec.description) spec.description else null
        )

        backgroundTask(project, BACKGROUND_TASK_TITLE) {
            text = "Updating service account..."

            val updateServiceAccountOperation = CloudRepository.instance.updateServiceAccount(
                authData,
                serviceAccount.id,
                update
            )

            val newAccessBindings = mutableSetOf<Access.AccessBinding>()
            folderAccessBindings.forEach {
                if (!it.hasSubject(serviceAccount)) {
                    newAccessBindings.add(it)
                }
            }
            spec.roles.forEach {
                newAccessBindings.add(
                    Access.AccessBinding.newBuilder().apply {
                        roleId = it
                        subject = Access.Subject.newBuilder().apply {
                            type = AccessBindingType.SERVICE_ACCOUNT.id
                            id = serviceAccount.id
                        }.build()
                    }.build()
                )
            }

            val updateFolderAccessBindingsOperation = CloudOperationService.instance.setFolderAccessBindings(
                authData,
                serviceAccount.group.folder,
                folderAccessBindings.toSet(),
                newAccessBindings
            )

            val operations = listOfNotNull(
                updateServiceAccountOperation,
                updateFolderAccessBindingsOperation
            )
            if (operations.isEmpty()) return@backgroundTask

            val operationResults = operations.awaitEnd(project, authData)
            val accountName = serviceAccount.name

            if (operationResults.none { it.result.hasError }) {
                operationResults.notifySuccess(project, "Service account '$accountName' updated")
                serviceAccount.group.folder.onServiceAccountCreated(project)
            } else {
                operationResults.notifyError(project, "Failed to updated service account '$accountName'")
            }
        }
    }

    override fun createApiGateway(
        project: Project,
        authData: CloudAuthData,
        folder: CloudFolder,
        spec: CloudApiGatewaySpec
    ) {
        backgroundTask(project, BACKGROUND_TASK_TITLE) {
            text = "Creating API gateway..."

            val operationResult = CloudRepository.instance.createApiGateway(
                authData,
                folder.id,
                spec
            ).awaitEnd(project, authData)

            if (!operationResult.result.hasError) {
                operationResult.notifySuccess(project, "API gateway '${spec.name}' created")
                folder.gatewayGroup.update(project, false)
            } else {
                operationResult.notifyError(project, "Failed to create API gateway '${spec.name}'")
            }
        }
    }

    override fun updateApiGateway(
        project: Project,
        authData: CloudAuthData,
        apiGateway: CloudApiGateway,
        openapiSpec: String,
        spec: CloudApiGatewaySpec
    ) {
        val update = CloudApiGatewayUpdate(
            if (spec.name != apiGateway.name) spec.name else null,
            if (spec.description != apiGateway.data.description) spec.description else null,
            if (spec.labels != apiGateway.data.labelsMap) spec.labels else null,
            if (spec.openapiSpec != openapiSpec) spec.openapiSpec else null
        )
        if (update.isEmpty) return

        backgroundTask(project, BACKGROUND_TASK_TITLE) {
            text = "Updating API gateway..."

            val operationResult = CloudRepository.instance.updateApiGateway(
                authData,
                apiGateway.id,
                update
            ).awaitEnd(project, authData)

            if (!operationResult.result.hasError) {
                operationResult.notifySuccess(project, "API gateway '${apiGateway.name}' updated")
                apiGateway.group.update(project, false)
            } else {
                operationResult.notifyError(project, "Failed to update API gateway '${apiGateway.name}'")
            }
        }
    }

    override fun fetchApiGatewaySpec(
        project: Project,
        authData: CloudAuthData,
        apiGateway: CloudApiGateway
    ): LazyTask<String> = doLazy("Fetching '$apiGateway' specification") {
        CloudRepository.instance.getApiGatewaySpecification(authData, apiGateway.id)
    } onError {
        "Can not fetch '$apiGateway' specification:\n${it.message}"
    }

    override fun createFunction(
        project: Project,
        authData: CloudAuthData,
        folder: CloudFolder,
        spec: CloudFunctionSpec
    ) {
        backgroundTask(project, BACKGROUND_TASK_TITLE) {
            text = "Creating function..."

            val operationResult = CloudRepository.instance.createFunction(
                authData,
                folder.id,
                spec
            ).awaitEnd(project, authData)

            if (!operationResult.result.hasError) {
                operationResult.notifySuccess(project, "Cloud function '${spec.name}' created")
                folder.functionGroup.update(project, false)
            } else {
                operationResult.notifyError(project, "Failed to create cloud function '${spec.name}'")
            }
        }
    }

    override fun updateFunction(
        project: Project,
        authData: CloudAuthData,
        function: CloudFunction,
        spec: CloudFunctionSpec
    ) {
        val update = CloudFunctionUpdate(
            if (spec.name != function.name) spec.name else null,
            if (spec.description != function.data.description) spec.description else null
        )
        if (update.isEmpty) return

        backgroundTask(project, BACKGROUND_TASK_TITLE) {
            text = "Updating function..."

            val operationResult = CloudRepository.instance.updateFunction(
                authData,
                function.id,
                update
            ).awaitEnd(project, authData)

            if (!operationResult.result.hasError) {
                operationResult.notifySuccess(project, "Cloud function '${function.name}' updated")
                function.group.update(project, false)
            } else {
                operationResult.notifyError(project, "Failed to update cloud function '${function.name}'")
            }
        }
    }
}