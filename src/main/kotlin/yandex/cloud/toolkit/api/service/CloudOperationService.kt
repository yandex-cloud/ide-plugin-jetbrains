package yandex.cloud.toolkit.api.service

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import yandex.cloud.api.access.Access
import yandex.cloud.api.iam.v1.RoleOuterClass
import yandex.cloud.api.vpc.v1.SubnetOuterClass
import yandex.cloud.toolkit.api.auth.CloudAuthData
import yandex.cloud.toolkit.api.resource.impl.model.*
import yandex.cloud.toolkit.api.util.LogLine
import yandex.cloud.toolkit.process.FunctionRunRequest
import yandex.cloud.toolkit.util.AsyncHttpRequest
import yandex.cloud.toolkit.util.SimpleHttpResponse
import yandex.cloud.toolkit.util.remote.list.RemoteList
import yandex.cloud.toolkit.util.remote.list.RemoteListPointer
import yandex.cloud.toolkit.util.task.LazyTask
import java.io.File

interface CloudOperationService {

    fun fetchOperationLogs(operationId: String, url: String): LazyTask<File>

    fun showOperationLogs(project: Project, operationId: String, url: String)

    fun generateFunctionRequestFile(project: Project, authData: CloudAuthData, function: CloudFunction, tag: String)

    fun runFunction(
        project: Project,
        functionId: String,
        request: FunctionRunRequest
    ): AsyncHttpRequest<SimpleHttpResponse>

    fun getDefaultFunctionInvokeUrl(functionId: String): String

    fun fetchFunction(project: Project, folder: CloudFolder, functionId: String): LazyTask<CloudFunction>

    fun fetchFunctionVersions(project: Project, function: CloudFunction, useCache: Boolean = true): LazyTask<List<CloudFunctionVersion>>

    fun fetchFunctionScalingPolicies(project: Project, function: CloudFunction, useCache: Boolean = true): LazyTask<List<CloudFunctionScalingPolicy>>

    fun fetchServiceAccounts(project: Project, folder: CloudFolder): LazyTask<List<CloudServiceAccount>>

    fun fetchApiGateways(project: Project, folder: CloudFolder): LazyTask<List<CloudApiGateway>>

    fun fetchFunctions(project: Project, folder: CloudFolder): LazyTask<List<CloudFunction>>

    fun fetchRuntimes(project: Project, user: CloudUser): LazyTask<List<String>>

    fun fetchRoles(project: Project, user: CloudUser): LazyTask<List<RoleOuterClass.Role>>

    fun fetchVPCNetworks(project: Project, folder: CloudFolder): LazyTask<List<VPCNetwork>>

    fun fetchFolderAccessBindings(project: Project, folder: CloudFolder): LazyTask<List<Access.AccessBinding>>

    /**
     * @param fromSecondsIn included
     * @param toSecondsEx excluded
     */
    fun fetchFunctionLogs(
        authData: CloudAuthData,
        version: CloudFunctionVersion,
        streamName: String,
        fromSecondsIn: Long,
        toSecondsEx: Long,
        pointer: RemoteListPointer
    ): LazyTask<RemoteList<LogLine>>

    fun fetchFolderSubnets(user: CloudUser, folderId: String): LazyTask<List<SubnetOuterClass.Subnet>>

    fun setFunctionVersionTags(
        project: Project,
        authData: CloudAuthData,
        version: CloudFunctionVersion,
        tags: Set<String>
    )

    fun setFolderAccessBindings(
        authData: CloudAuthData,
        folder: CloudFolder,
        oldAccessBindings: Set<Access.AccessBinding>,
        newAccessBindings: Set<Access.AccessBinding>
    ): CloudOperation?

    fun setFunctionScalingPolicies(
        project: Project,
        authData: CloudAuthData,
        function: CloudFunction,
        oldPolicies: Map<String, CloudFunctionScalingPolicy>,
        newPolicies: Map<String, CloudFunctionScalingPolicy>
    )

    fun createServiceAccount(
        project: Project,
        authData: CloudAuthData,
        folder: CloudFolder,
        spec: CloudServiceAccountSpec
    )

    fun updateServiceAccount(
        project: Project,
        authData: CloudAuthData,
        folderAccessBindings: List<Access.AccessBinding>,
        serviceAccount: CloudServiceAccount,
        spec: CloudServiceAccountSpec
    )

    fun createApiGateway(
        project: Project,
        authData: CloudAuthData,
        folder: CloudFolder,
        spec: CloudApiGatewaySpec
    )

    fun updateApiGateway(
        project: Project,
        authData: CloudAuthData,
        apiGateway: CloudApiGateway,
        openapiSpec: String,
        spec: CloudApiGatewaySpec
    )

    fun fetchApiGatewaySpec(
        project: Project,
        authData: CloudAuthData,
        apiGateway: CloudApiGateway
    ): LazyTask<String>

    fun createFunction(
        project: Project,
        authData: CloudAuthData,
        folder: CloudFolder,
        spec: CloudFunctionSpec
    )

    fun updateFunction(
        project: Project,
        authData: CloudAuthData,
        function: CloudFunction,
        spec: CloudFunctionSpec
    )

    companion object {
        val instance: CloudOperationService get() = ServiceManager.getService(CloudOperationService::class.java)
    }
}
