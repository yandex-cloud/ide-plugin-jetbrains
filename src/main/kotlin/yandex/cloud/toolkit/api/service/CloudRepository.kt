package yandex.cloud.toolkit.api.service

import com.google.protobuf.ByteString
import com.intellij.openapi.components.ServiceManager
import yandex.cloud.api.access.Access
import yandex.cloud.api.iam.v1.RoleOuterClass
import yandex.cloud.api.iam.v1.ServiceAccountOuterClass
import yandex.cloud.api.lockbox.v1.PayloadOuterClass
import yandex.cloud.api.logs.v1.LogEventOuterClass
import yandex.cloud.api.operation.OperationOuterClass
import yandex.cloud.api.resourcemanager.v1.CloudOuterClass
import yandex.cloud.api.resourcemanager.v1.FolderOuterClass
import yandex.cloud.api.serverless.apigateway.v1.Apigateway
import yandex.cloud.api.serverless.functions.v1.FunctionOuterClass
import yandex.cloud.api.serverless.triggers.v1.TriggerOuterClass
import yandex.cloud.api.vpc.v1.NetworkOuterClass
import yandex.cloud.api.vpc.v1.SubnetOuterClass
import yandex.cloud.toolkit.api.auth.CloudAuthData
import yandex.cloud.toolkit.api.resource.impl.model.*
import yandex.cloud.toolkit.configuration.function.deploy.FunctionDeploySpec
import yandex.cloud.toolkit.util.Maybe
import yandex.cloud.toolkit.util.remote.list.RemoteList
import yandex.cloud.toolkit.util.remote.list.RemoteListPointer

interface CloudRepository {

    fun invalidateAuthData(authData: CloudAuthData)

    fun getCloudList(authData: CloudAuthData, pointer: RemoteListPointer): RemoteList<CloudOuterClass.Cloud>

    fun getFolderList(
        authData: CloudAuthData,
        cloudId: String,
        pointer: RemoteListPointer
    ): RemoteList<FolderOuterClass.Folder>

    fun getFunctionList(
        authData: CloudAuthData,
        folderId: String,
        pointer: RemoteListPointer
    ): RemoteList<FunctionOuterClass.Function>

    fun getFolderAccessBindingList(
        authData: CloudAuthData,
        folderId: String,
        pointer: RemoteListPointer
    ): RemoteList<Access.AccessBinding>

    fun getAvailableRolesList(authData: CloudAuthData): List<RoleOuterClass.Role>

    fun getFunctionVersionList(
        authData: CloudAuthData,
        folderId: String,
        functionId: String,
        pointer: RemoteListPointer
    ): RemoteList<FunctionOuterClass.Version>

    fun getFunctionScalingPolicyList(
        authData: CloudAuthData,
        functionId: String,
        pointer: RemoteListPointer
    ): RemoteList<FunctionOuterClass.ScalingPolicy>

    fun getFunctionRuntimeList(authData: CloudAuthData): List<String>

    fun getTriggerList(
        authData: CloudAuthData,
        folderId: String,
        pointer: RemoteListPointer
    ): RemoteList<TriggerOuterClass.Trigger>

    fun getServiceAccountList(
        authData: CloudAuthData,
        folderId: String,
        pointer: RemoteListPointer
    ): RemoteList<ServiceAccountOuterClass.ServiceAccount>

    fun getApiGatewayList(
        authData: CloudAuthData,
        folderId: String,
        pointer: RemoteListPointer
    ): RemoteList<Apigateway.ApiGateway>

    fun createFunctionVersion(
        authData: CloudAuthData,
        spec: FunctionDeploySpec,
        content: ByteString
    ): CloudOperation

    fun setFunctionScalingPolicy(
        authData: CloudAuthData,
        functionId: String,
        scalingPolicy: CloudFunctionScalingPolicy
    ): CloudOperation

    fun removeFunctionScalingPolicy(
        authData: CloudAuthData,
        functionId: String,
        tag: String
    ): CloudOperation

    fun getLockboxSecret(
        authData: CloudAuthData,
        secretId: String,
        versionId: String?
    ): PayloadOuterClass.Payload

    fun getOperation(authData: CloudAuthData, operationId: String): Maybe<OperationOuterClass.Operation>

    fun getFunction(authData: CloudAuthData, functionId: String): FunctionOuterClass.Function

    /**
     * @param fromSeconds included
     * @param toSeconds included
     */
    fun readLogs(
        authData: CloudAuthData,
        logGroupId: String,
        streamName: String,
        fromSeconds: Long,
        toSeconds: Long,
        pointer: RemoteListPointer
    ): RemoteList<LogEventOuterClass.LogEvent>

    fun setFunctionTag(authData: CloudAuthData, versionId: String, tag: String): CloudOperation

    fun removeFunctionTag(authData: CloudAuthData, versionId: String, tag: String): CloudOperation

    fun updateFolderAccessBindings(
        authData: CloudAuthData,
        folderId: String,
        accessBindingDeltas: List<Access.AccessBindingDelta>
    ): CloudOperation

    fun createServiceAccount(
        authData: CloudAuthData,
        folderId: String,
        name: String,
        description: String?
    ): CloudOperation

    fun updateServiceAccount(
        authData: CloudAuthData,
        serviceAccountId: String,
        update: CloudServiceAccountUpdate
    ): CloudOperation?

    fun deleteServiceAccount(
        authData: CloudAuthData,
        serviceAccountId: String
    ): CloudOperation

    fun createApiGateway(
        authData: CloudAuthData,
        folderId: String,
        spec: CloudApiGatewaySpec
    ): CloudOperation

    fun updateApiGateway(
        authData: CloudAuthData,
        apiGatewayId: String,
        update: CloudApiGatewayUpdate
    ): CloudOperation

    fun deleteApiGateway(
        authData: CloudAuthData,
        apiGatewayId: String
    ): CloudOperation

    fun getApiGatewaySpecification(
        authData: CloudAuthData,
        apiGatewayId: String
    ): Maybe<String>

    fun createFunction(
        authData: CloudAuthData,
        folderId: String,
        spec: CloudFunctionSpec
    ): CloudOperation

    fun updateFunction(
        authData: CloudAuthData,
        functionId: String,
        update: CloudFunctionUpdate
    ): CloudOperation

    fun deleteFunction(authData: CloudAuthData, functionId: String): CloudOperation

    fun getNetworkList(
        authData: CloudAuthData,
        folderId: String,
        pointer: RemoteListPointer
    ): RemoteList<NetworkOuterClass.Network>

    fun getSubnetList(
        authData: CloudAuthData,
        networkId: String,
        pointer: RemoteListPointer
    ): RemoteList<SubnetOuterClass.Subnet>

    fun getFolderSubnets(
        authData: CloudAuthData,
        folderId: String,
        pointer: RemoteListPointer
    ): RemoteList<SubnetOuterClass.Subnet>

    companion object {
        val instance: CloudRepository get() = ServiceManager.getService(CloudRepository::class.java)
    }
}