package yandex.cloud.toolkit.api.service.impl

import com.google.protobuf.ByteString
import com.google.protobuf.Timestamp
import com.intellij.openapi.Disposable
import com.intellij.util.text.nullize
import io.grpc.Channel
import io.grpc.ManagedChannel
import io.grpc.stub.AbstractStub
import yandex.cloud.api.access.Access
import yandex.cloud.api.iam.v1.*
import yandex.cloud.api.logging.v1.LogEntryOuterClass
import yandex.cloud.api.logging.v1.LogGroupServiceGrpc
import yandex.cloud.api.logging.v1.LogGroupServiceOuterClass.ListLogGroupsRequest
import yandex.cloud.api.logging.v1.LogReadingServiceGrpc
import yandex.cloud.api.logging.v1.LogReadingServiceOuterClass.Criteria
import yandex.cloud.api.logging.v1.LogReadingServiceOuterClass.ReadRequest
import yandex.cloud.api.operation.OperationOuterClass
import yandex.cloud.api.operation.OperationServiceGrpc
import yandex.cloud.api.operation.OperationServiceOuterClass
import yandex.cloud.api.resourcemanager.v1.*
import yandex.cloud.api.serverless.apigateway.v1.ApiGatewayServiceGrpc
import yandex.cloud.api.serverless.apigateway.v1.Apigateway
import yandex.cloud.api.serverless.apigateway.v1.ApigatewayService
import yandex.cloud.api.serverless.functions.v1.FunctionOuterClass
import yandex.cloud.api.serverless.functions.v1.FunctionServiceGrpc
import yandex.cloud.api.serverless.functions.v1.FunctionServiceOuterClass
import yandex.cloud.api.serverless.triggers.v1.TriggerOuterClass
import yandex.cloud.api.serverless.triggers.v1.TriggerServiceGrpc
import yandex.cloud.api.serverless.triggers.v1.TriggerServiceOuterClass
import yandex.cloud.api.vpc.v1.*
import yandex.cloud.toolkit.api.auth.CloudAuthData
import yandex.cloud.toolkit.api.resource.ResourceType
import yandex.cloud.toolkit.api.resource.impl.model.*
import yandex.cloud.toolkit.api.service.CloudRepository
import yandex.cloud.toolkit.configuration.function.deploy.FunctionDeploySpec
import yandex.cloud.toolkit.util.Maybe
import yandex.cloud.toolkit.util.doMaybe
import yandex.cloud.toolkit.util.remote.list.RemoteList
import yandex.cloud.toolkit.util.remote.list.RemoteListPointer
import yandex.cloud.toolkit.util.remote.list.RemoteListState
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.function.Function

class CloudRepositoryImpl : CloudRepository {

    private val services = ConcurrentHashMap<CloudAuthData, CloudGrpcServices>()

    override fun invalidateAuthData(authData: CloudAuthData) {
        services.remove(authData)?.dispose()
    }

    private operator fun CloudAuthData.invoke(): CloudGrpcServices =
        services.getOrPut(this) { CloudGrpcServices(this) }

    override fun getCloudList(authData: CloudAuthData, pointer: RemoteListPointer): RemoteList<CloudOuterClass.Cloud> {
        val request = CloudServiceOuterClass.ListCloudsRequest.newBuilder().apply {
            if (pointer.pageToken != null) pageToken = pointer.pageToken
            pageSize = pointer.pageSize.toLong()
        }.build()

        val response = authData().cloudService.list(request)

        return RemoteList(
            response.cloudsList,
            RemoteListState(response.nextPageToken)
        )
    }

    override fun getFolderAccessBindingList(
        authData: CloudAuthData,
        folderId: String,
        pointer: RemoteListPointer
    ): RemoteList<Access.AccessBinding> {
        val request = Access.ListAccessBindingsRequest.newBuilder().apply {
            resourceId = folderId
            if (pointer.pageToken != null) pageToken = pointer.pageToken
            pageSize = pointer.pageSize.toLong()
        }.build()

        val response = authData().folderService.listAccessBindings(request)

        return RemoteList(
            response.accessBindingsList,
            RemoteListState(response.nextPageToken)
        )
    }

    override fun updateFolderAccessBindings(
        authData: CloudAuthData,
        folderId: String,
        accessBindingDeltas: List<Access.AccessBindingDelta>
    ): CloudOperation {
        val request = Access.UpdateAccessBindingsRequest.newBuilder().apply {
            resourceId = folderId
            accessBindingDeltas.forEach { addAccessBindingDeltas(it) }
        }.build()

        val operation = doMaybe {
            authData().folderService.updateAccessBindings(request)
        }

        return CloudOperation("Update folder access bindings", operation)
    }

    override fun getFolderList(
        authData: CloudAuthData,
        cloudId: String,
        pointer: RemoteListPointer
    ): RemoteList<FolderOuterClass.Folder> {
        val request = FolderServiceOuterClass.ListFoldersRequest.newBuilder().apply {
            this.cloudId = cloudId
            if (pointer.pageToken != null) pageToken = pointer.pageToken
            pageSize = pointer.pageSize.toLong()
        }.build()

        val response = authData().folderService.list(request)

        return RemoteList(
            response.foldersList,
            RemoteListState(response.nextPageToken)
        )
    }

    override fun getFunctionList(
        authData: CloudAuthData,
        folderId: String,
        pointer: RemoteListPointer
    ): RemoteList<FunctionOuterClass.Function> {
        val request = FunctionServiceOuterClass.ListFunctionsRequest.newBuilder().apply {
            this.folderId = folderId
            if (pointer.pageToken != null) pageToken = pointer.pageToken
            pageSize = pointer.pageSize.toLong()
        }.build()

        val response = authData().functionService.list(request)

        return RemoteList(
            response.functionsList,
            RemoteListState(response.nextPageToken)
        )
    }

    override fun getFunctionVersionList(
        authData: CloudAuthData,
        folderId: String,
        functionId: String,
        pointer: RemoteListPointer
    ): RemoteList<FunctionOuterClass.Version> {
        val request = FunctionServiceOuterClass.ListFunctionsVersionsRequest.newBuilder().apply {
            this.folderId = folderId
            this.functionId = functionId
            if (pointer.pageToken != null) pageToken = pointer.pageToken
            pageSize = pointer.pageSize.toLong()
        }.build()

        val response = authData().functionService.listVersions(request)

        return RemoteList(
            response.versionsList,
            RemoteListState(response.nextPageToken)
        )
    }

    override fun getFunctionScalingPolicyList(
        authData: CloudAuthData,
        functionId: String,
        pointer: RemoteListPointer
    ): RemoteList<FunctionOuterClass.ScalingPolicy> {
        val request = FunctionServiceOuterClass.ListScalingPoliciesRequest.newBuilder().apply {
            this.functionId = functionId
            if (pointer.pageToken != null) pageToken = pointer.pageToken
            pageSize = pointer.pageSize.toLong()
        }.build()

        val response = authData().functionService.listScalingPolicies(request)

        return RemoteList(
            response.scalingPoliciesList,
            RemoteListState(response.nextPageToken)
        )
    }

    override fun getFunctionRuntimeList(
        authData: CloudAuthData
    ): List<String> {
        val request = FunctionServiceOuterClass.ListRuntimesRequest.newBuilder().build()
        return authData().functionService.listRuntimes(request).runtimesList
    }

    override fun getAvailableRolesList(authData: CloudAuthData): List<RoleOuterClass.Role> {
        val request = RoleServiceOuterClass.ListRolesRequest.newBuilder().build()
        return authData().roleService.list(request).rolesList
    }

    override fun createFunctionVersion(
        authData: CloudAuthData,
        spec: FunctionDeploySpec,
        content: ByteString
    ): CloudOperation {
        val operation = doMaybe {
            val request = FunctionServiceOuterClass.CreateFunctionVersionRequest.newBuilder().apply {
                functionId = spec.functionId ?: ""
                runtime = spec.runtime ?: ""
                entrypoint = spec.entryPoint ?: ""
                resources = FunctionOuterClass.Resources.newBuilder().setMemory(spec.memoryBytes).build()

                executionTimeout = com.google.protobuf.Duration.newBuilder().setSeconds(
                    spec.timeoutSeconds
                ).build()

                spec.description.nullize()?.let { description = it }
                spec.serviceAccountId.nullize()?.let { serviceAccountId = it }

                if (spec.envVariables.isNotEmpty()) putAllEnvironment(spec.envVariables)
                if (spec.tags.isNotEmpty()) addAllTag(spec.tags)

                setContent(content)

                if (spec.hasConnectivity()) {
                    connectivity = if (spec.useSubnets) {
                        FunctionOuterClass.Connectivity.newBuilder().addAllSubnetId(spec.subnets).build()
                    } else {
                        FunctionOuterClass.Connectivity.newBuilder().setNetworkId(spec.networkId).build()
                    }
                }
            }.build()

            authData().functionService.createVersion(request)
        }

        return CloudOperation("Function '${spec.functionId}' deployment", operation)
    }

    override fun setFunctionScalingPolicy(
        authData: CloudAuthData,
        functionId: String,
        scalingPolicy: CloudFunctionScalingPolicy
    ): CloudOperation {
        val operation = doMaybe {
            val request = FunctionServiceOuterClass.SetScalingPolicyRequest.newBuilder().apply {
                this.functionId = functionId
                this.tag = scalingPolicy.tag
                this.zoneInstancesLimit = scalingPolicy.zoneInstancesLimit
                this.zoneRequestsLimit = scalingPolicy.zoneRequestsLimit
            }.build()

            authData().functionService.setScalingPolicy(request)
        }

        return CloudOperation("Set scaling policy '${scalingPolicy.tag}'", operation)
    }

    override fun removeFunctionScalingPolicy(authData: CloudAuthData, functionId: String, tag: String): CloudOperation {
        val operation = doMaybe {
            val request = FunctionServiceOuterClass.RemoveScalingPolicyRequest.newBuilder().apply {
                this.functionId = functionId
                this.tag = tag
            }.build()

            authData().functionService.removeScalingPolicy(request)
        }

        return CloudOperation("Remove scaling policy '$tag'", operation)
    }

    override fun getOperation(authData: CloudAuthData, operationId: String): Maybe<OperationOuterClass.Operation> {
        return doMaybe {
            val request = OperationServiceOuterClass.GetOperationRequest.newBuilder()
                .setOperationId(operationId)
                .build()

            authData().operationService.get(request) ?: throw RuntimeException("Operation not found")
        }
    }

    override fun getFunction(authData: CloudAuthData, functionId: String): FunctionOuterClass.Function {
        val request = FunctionServiceOuterClass.GetFunctionRequest.newBuilder()
            .setFunctionId(functionId)
            .build()
        return authData().functionService.get(request)
    }

    override fun getTriggerList(
        authData: CloudAuthData,
        folderId: String,
        pointer: RemoteListPointer
    ): RemoteList<TriggerOuterClass.Trigger> {
        val request = TriggerServiceOuterClass.ListTriggersRequest.newBuilder().apply {
            this.folderId = folderId
            if (pointer.pageToken != null) pageToken = pointer.pageToken
            pageSize = pointer.pageSize.toLong()
        }.build()

        val response = authData().triggerService.list(request)

        return RemoteList(
            response.triggersList,
            RemoteListState(response.nextPageToken)
        )
    }

    override fun getServiceAccountList(
        authData: CloudAuthData,
        folderId: String,
        pointer: RemoteListPointer
    ): RemoteList<ServiceAccountOuterClass.ServiceAccount> {
        val request = ServiceAccountServiceOuterClass.ListServiceAccountsRequest.newBuilder().apply {
            this.folderId = folderId
            if (pointer.pageToken != null) pageToken = pointer.pageToken
            pageSize = pointer.pageSize.toLong()
        }.build()

        val response = authData().accountService.list(request)

        return RemoteList(
            response.serviceAccountsList,
            RemoteListState(response.nextPageToken)
        )
    }

    override fun getApiGatewayList(
        authData: CloudAuthData,
        folderId: String,
        pointer: RemoteListPointer
    ): RemoteList<Apigateway.ApiGateway> {
        val request = ApigatewayService.ListApiGatewayRequest.newBuilder().apply {
            this.folderId = folderId
            if (pointer.pageToken != null) pageToken = pointer.pageToken
            pageSize = pointer.pageSize.toLong()
        }.build()

        val response = authData().gatewayService.list(request)

        return RemoteList(
            response.apiGatewaysList,
            RemoteListState(response.nextPageToken)
        )
    }

    override fun readLogs(
        authData: CloudAuthData,
        logGroupId: String,
        resourceType: ResourceType?,
        streamName: String,
        fromSeconds: Long,
        toSeconds: Long,
        pointer: RemoteListPointer,
    ): RemoteList<LogEntryOuterClass.LogEntry> {

        val request = ReadRequest.newBuilder()

        if (pointer.pageToken == null) {
            request.criteria = Criteria.newBuilder().apply {
                setLogGroupId(logGroupId)
                setSince(Timestamp.newBuilder().setSeconds(fromSeconds))
                setUntil(Timestamp.newBuilder().setSeconds(toSeconds))
                pageSize = pointer.pageSize.toLong()
                if (resourceType != null) {
                    addResourceTypes(resourceType.id)
                }
            }.build()
        } else {
            request.pageToken = pointer.pageToken
        }

        val response = authData().logReadingService.read(request.build())

        return RemoteList(
            response.entriesList,
            RemoteListState(response.previousPageToken, response.nextPageToken)
        )
    }

    override fun getDefaultLogGroup(authData: CloudAuthData, folderId: String): String {
        val request = ListLogGroupsRequest.newBuilder().apply {
            this.folderId = folderId
            filter = "name=\"default\""
        }.build()

        val response = authData().logGroupService.list(request)
        if (response.groupsCount == 0) {
            throw IllegalStateException("Folder doesn't have default log group")
        }

        return response.getGroups(0).id
    }

    override fun setFunctionTag(
        authData: CloudAuthData,
        versionId: String,
        tag: String
    ): CloudOperation {
        val operation = doMaybe {
            val request = FunctionServiceOuterClass.SetFunctionTagRequest.newBuilder()
                .setFunctionVersionId(versionId)
                .setTag(tag)
                .build()
            authData().functionService.setTag(request)
        }
        return CloudOperation("Set tag '$tag'", operation)
    }

    override fun removeFunctionTag(
        authData: CloudAuthData,
        versionId: String,
        tag: String
    ): CloudOperation {
        val operation = doMaybe {
            val request = FunctionServiceOuterClass.RemoveFunctionTagRequest.newBuilder()
                .setFunctionVersionId(versionId)
                .setTag(tag)
                .build()
            authData().functionService.removeTag(request)
        }

        return CloudOperation("Remove tag '$tag'", operation)
    }

    override fun createServiceAccount(
        authData: CloudAuthData,
        folderId: String,
        name: String,
        description: String?
    ): CloudOperation {
        val request = ServiceAccountServiceOuterClass.CreateServiceAccountRequest.newBuilder().apply {
            this.folderId = folderId
            this.name = name
            if (description != null) this.description = description
        }.build()

        val operation = doMaybe {
            authData().accountService.create(request)
        }

        return CloudOperation("Create service account '$name'", operation)
    }

    override fun updateServiceAccount(
        authData: CloudAuthData,
        serviceAccountId: String,
        update: CloudServiceAccountUpdate
    ): CloudOperation? {
        if (update.isEmpty) return null
        val request = ServiceAccountServiceOuterClass.UpdateServiceAccountRequest.newBuilder().apply {
            this.serviceAccountId = serviceAccountId
            if (update.name != null) this.name = update.name
            if (update.description != null) this.description = update.description
        }.build()

        val operation = doMaybe {
            authData().accountService.update(request)
        }

        return CloudOperation("Update service account", operation)
    }

    override fun deleteServiceAccount(authData: CloudAuthData, serviceAccountId: String): CloudOperation {
        val request = ServiceAccountServiceOuterClass.DeleteServiceAccountRequest.newBuilder()
            .setServiceAccountId(serviceAccountId)
            .build()

        val operation = doMaybe {
            authData().accountService.delete(request)
        }

        return CloudOperation("Delete service account", operation)
    }

    override fun createApiGateway(
        authData: CloudAuthData,
        folderId: String,
        spec: CloudApiGatewaySpec
    ): CloudOperation {
        val request = ApigatewayService.CreateApiGatewayRequest.newBuilder().apply {
            this.folderId = folderId
            this.name = spec.name
            if (spec.description != null) this.description = spec.description
            if (spec.labels.isNotEmpty()) putAllLabels(spec.labels)
            this.openapiSpec = spec.openapiSpec
        }.build()

        val operation = doMaybe {
            authData().gatewayService.create(request)
        }

        return CloudOperation("Create API gateway '${spec.name}'", operation)
    }

    override fun updateApiGateway(
        authData: CloudAuthData,
        apiGatewayId: String,
        update: CloudApiGatewayUpdate
    ): CloudOperation {
        val request = ApigatewayService.UpdateApiGatewayRequest.newBuilder().apply {
            this.apiGatewayId = apiGatewayId
            if (update.name != null) this.name = update.name
            if (update.description != null) this.description = update.description
            if (update.labels != null) putAllLabels(update.labels)
            if (update.openapiSpec != null) this.openapiSpec = update.openapiSpec
        }.build()

        val operation = doMaybe {
            authData().gatewayService.update(request)
        }

        return CloudOperation("Update API gateway", operation)
    }

    override fun deleteApiGateway(authData: CloudAuthData, apiGatewayId: String): CloudOperation {
        val request = ApigatewayService.DeleteApiGatewayRequest.newBuilder()
            .setApiGatewayId(apiGatewayId)
            .build()

        val operation = doMaybe {
            authData().gatewayService.delete(request)
        }

        return CloudOperation("Delete API gateway", operation)
    }

    override fun getApiGatewaySpecification(authData: CloudAuthData, apiGatewayId: String): Maybe<String> {
        val request = ApigatewayService.GetOpenapiSpecRequest.newBuilder()
            .setApiGatewayId(apiGatewayId)
            .build()

        return doMaybe {
            authData().gatewayService.getOpenapiSpec(request).openapiSpec
        }
    }

    override fun createFunction(
        authData: CloudAuthData,
        folderId: String,
        spec: CloudFunctionSpec
    ): CloudOperation {
        val request = FunctionServiceOuterClass.CreateFunctionRequest.newBuilder().apply {
            this.folderId = folderId
            this.name = spec.name
            if (spec.description != null) this.description = spec.description
        }.build()

        val operation = doMaybe {
            authData().functionService.create(request)
        }

        return CloudOperation("Create function '${spec.name}'", operation)
    }

    override fun updateFunction(
        authData: CloudAuthData,
        functionId: String,
        update: CloudFunctionUpdate
    ): CloudOperation {
        val request = FunctionServiceOuterClass.UpdateFunctionRequest.newBuilder().apply {
            this.functionId = functionId
            if (update.name != null) this.name = update.name
            if (update.description != null) this.description = update.description
        }.build()

        val operation = doMaybe {
            authData().functionService.update(request)
        }

        return CloudOperation("Update function", operation)
    }

    override fun deleteFunction(authData: CloudAuthData, functionId: String): CloudOperation {
        val request = FunctionServiceOuterClass.DeleteFunctionRequest.newBuilder()
            .setFunctionId(functionId)
            .build()

        val operation = doMaybe {
            authData().functionService.delete(request)
        }

        return CloudOperation("Delete function", operation)
    }

    override fun getNetworkList(
        authData: CloudAuthData,
        folderId: String,
        pointer: RemoteListPointer
    ): RemoteList<NetworkOuterClass.Network> {
        val request = NetworkServiceOuterClass.ListNetworksRequest.newBuilder().apply {
            this.folderId = folderId
            if (pointer.pageToken != null) pageToken = pointer.pageToken
            pageSize = pointer.pageSize.toLong()
        }.build()

        val response = authData().networkService.list(request)

        return RemoteList(
            response.networksList,
            RemoteListState(response.nextPageToken)
        )
    }

    override fun getSubnetList(
        authData: CloudAuthData,
        networkId: String,
        pointer: RemoteListPointer
    ): RemoteList<SubnetOuterClass.Subnet> {
        val request = NetworkServiceOuterClass.ListNetworkSubnetsRequest.newBuilder().apply {
            this.networkId = networkId
            if (pointer.pageToken != null) pageToken = pointer.pageToken
            pageSize = pointer.pageSize.toLong()
        }.build()

        val response = authData().networkService.listSubnets(request)

        return RemoteList(
            response.subnetsList,
            RemoteListState(response.nextPageToken)
        )
    }

    override fun getFolderSubnets(
        authData: CloudAuthData,
        folderId: String,
        pointer: RemoteListPointer
    ): RemoteList<SubnetOuterClass.Subnet> {
        val request = SubnetServiceOuterClass.ListSubnetsRequest.newBuilder().apply {
            this.folderId = folderId
            if (pointer.pageToken != null) pageToken = pointer.pageToken
            pageSize = pointer.pageSize.toLong()
        }.build()

        val response = authData().subnetService.list(request)

        return RemoteList(
            response.subnetsList,
            RemoteListState(response.nextPageToken)
        )
    }

    private class CloudGrpcServices(val authData: CloudAuthData) : Disposable {

        private val channels = mutableListOf<ManagedChannel>()

        fun <SERVICE : AbstractStub<SERVICE>?> createService(
            clazz: Class<SERVICE>,
            serviceFactory: Function<Channel?, SERVICE>
        ): SERVICE {
            val channel = authData.channelFactory.getChannel(clazz)
            val service = authData.serviceFactory.create(channel, serviceFactory, null)!!
            channels += channel
            return service
        }

        override fun dispose() {
            channels.forEach { it.shutdownNow() }
            channels.forEach { it.awaitTermination(1, TimeUnit.SECONDS) }
        }

        val cloudService by lazy {
            createService(
                CloudServiceGrpc.CloudServiceBlockingStub::class.java,
                CloudServiceGrpc::newBlockingStub
            )
        }

        val folderService by lazy {
            createService(
                FolderServiceGrpc.FolderServiceBlockingStub::class.java,
                FolderServiceGrpc::newBlockingStub
            )
        }

        val functionService by lazy {
            createService(
                FunctionServiceGrpc.FunctionServiceBlockingStub::class.java,
                FunctionServiceGrpc::newBlockingStub
            )
        }

        val triggerService by lazy {
            createService(
                TriggerServiceGrpc.TriggerServiceBlockingStub::class.java,
                TriggerServiceGrpc::newBlockingStub
            )
        }

        val operationService by lazy {
            createService(
                OperationServiceGrpc.OperationServiceBlockingStub::class.java,
                OperationServiceGrpc::newBlockingStub
            )
        }

        val accountService by lazy {
            createService(
                ServiceAccountServiceGrpc.ServiceAccountServiceBlockingStub::class.java,
                ServiceAccountServiceGrpc::newBlockingStub
            )
        }

        val logReadingService by lazy {
            createService(
                LogReadingServiceGrpc.LogReadingServiceBlockingStub::class.java,
                LogReadingServiceGrpc::newBlockingStub
            )
        }

        val logGroupService by lazy {
            createService(
                LogGroupServiceGrpc.LogGroupServiceBlockingStub::class.java,
                LogGroupServiceGrpc::newBlockingStub
            )
        }

        val roleService by lazy {
            createService(
                RoleServiceGrpc.RoleServiceBlockingStub::class.java, RoleServiceGrpc::newBlockingStub
            )
        }

        val gatewayService by lazy {
            createService(
                ApiGatewayServiceGrpc.ApiGatewayServiceBlockingStub::class.java,
                ApiGatewayServiceGrpc::newBlockingStub
            )
        }

        val networkService by lazy {
            createService(
                NetworkServiceGrpc.NetworkServiceBlockingStub::class.java,
                NetworkServiceGrpc::newBlockingStub
            )
        }

        val subnetService by lazy {
            createService(
                SubnetServiceGrpc.SubnetServiceBlockingStub::class.java,
                SubnetServiceGrpc::newBlockingStub
            )
        }
    }
}
