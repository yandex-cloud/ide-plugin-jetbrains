package yandex.cloud.toolkit.api.resource.impl

import yandex.cloud.api.access.Access
import yandex.cloud.api.iam.v1.RoleOuterClass
import yandex.cloud.api.serverless.functions.v1.FunctionOuterClass
import yandex.cloud.toolkit.api.resource.CloudDependency
import yandex.cloud.toolkit.api.resource.CloudDependencyLoader
import yandex.cloud.toolkit.api.resource.CloudDependencyLoadingResult
import yandex.cloud.toolkit.api.resource.CloudResource
import yandex.cloud.toolkit.api.resource.impl.model.*
import yandex.cloud.toolkit.api.service.CloudRepository
import yandex.cloud.toolkit.util.Maybe
import yandex.cloud.toolkit.util.doMaybe
import yandex.cloud.toolkit.util.getOrThrow
import yandex.cloud.toolkit.util.remote.list.RemoteList
import yandex.cloud.toolkit.util.remote.list.RemoteListController
import yandex.cloud.toolkit.util.remote.list.RemoteListPointer
import yandex.cloud.toolkit.util.remote.resource.PresentableResourceStatus
import yandex.cloud.toolkit.util.remote.resource.ResourceLoadingError
import yandex.cloud.toolkit.util.remote.resource.UnauthenticatedException
import java.lang.IllegalStateException

abstract class DependentResourceLoader<P : CloudResource, R>(
    val dependency: CloudDependency<P, R>
) : CloudDependencyLoader<P, R> {

    companion object {
        const val DEFAULT_PAGE_SIZE = 100
    }

    fun <E> loadRemoteList(
        pageSize: Int = DEFAULT_PAGE_SIZE,
        onlyNext: Boolean = true,
        loader: (RemoteListPointer) -> RemoteList<E>
    ): Maybe<RemoteList<E>> {
        return RemoteListController(pageSize) {
            doMaybe {
                loader(it)
            }
        }.loadAllPages(onlyNext)
    }

    fun error(text: String, status: PresentableResourceStatus): Nothing = throw ResourceLoadingError(text, status)
    fun unauthorizedError(): Nothing = throw UnauthenticatedException()
}

object CloudsLoader : DependentResourceLoader<CloudUser, List<Cloud>>(CloudUser.Clouds) {

    override fun load(parent: CloudUser, result: CloudDependencyLoadingResult) {
        val authData = parent.authData ?: unauthorizedError()

        val cloudsList = loadRemoteList {
            CloudRepository.instance.getCloudList(authData, it)
        }.getOrThrow()

        val clouds = cloudsList.map { cloudData ->
            val cloud = Cloud(cloudData, parent)
            result.put(cloud, Cloud.CloudData, cloudData)
            cloud
        }

        result.put(parent, dependency, clouds)
    }
}

object CloudFoldersLoader : DependentResourceLoader<Cloud, List<CloudFolder>>(Cloud.Folders) {

    override fun load(parent: Cloud, result: CloudDependencyLoadingResult) {
        val authData = parent.user.authData ?: unauthorizedError()

        val foldersList = loadRemoteList {
            CloudRepository.instance.getFolderList(authData, parent.id, it)
        }.getOrThrow()

        val folders = foldersList.map { folderData ->
            val folder = HierarchicalCloudFolder(folderData, parent)

            result.put(folder, CloudFolder.FolderData, folderData)
            result.put(folder, CloudFolder.FunctionGroup, folder.functionGroup)
            result.put(folder, CloudFolder.GatewayGroup, folder.gatewayGroup)
            result.put(folder, CloudFolder.TriggerGroup, folder.triggerGroup)
            result.put(folder, CloudFolder.ServiceAccountGroup, folder.serviceAccountGroup)
            folder
        }

        result.put(parent, dependency, folders)
    }
}

object CloudFunctionsLoader :
    DependentResourceLoader<CloudFunctionGroup, List<CloudFunction>>(CloudFunctionGroup.Functions) {

    override fun load(parent: CloudFunctionGroup, result: CloudDependencyLoadingResult) {
        val authData = parent.user.authData ?: unauthorizedError()

        val functionsList = loadRemoteList {
            CloudRepository.instance.getFunctionList(authData, parent.folder.id, it)
        }.getOrThrow()

        val functions = functionsList.map { functionData ->
            val function = CloudFunction(functionData, parent)
            result.put(function, CloudFunction.SelfDependency, function)
            function
        }

        result.put(parent, dependency, functions)
    }
}

class CloudFunctionLoader(val functionId: String) :
    DependentResourceLoader<CloudFunctionGroup, CloudFunction>(CloudFunctionGroup.Function) {

    override fun load(parent: CloudFunctionGroup, result: CloudDependencyLoadingResult) {
        val authData = parent.user.authData ?: CloudFunctionVersionsLoader.unauthorizedError()
        val function = CloudRepository.instance.getFunction(authData, functionId)
        result.put(parent, dependency, CloudFunction(function, parent))
    }
}

object CloudFunctionVersionsLoader :
    DependentResourceLoader<CloudFunction, List<CloudFunctionVersion>>(CloudFunction.FunctionVersions) {

    override fun load(parent: CloudFunction, result: CloudDependencyLoadingResult) {
        val authData = parent.user.authData ?: unauthorizedError()

        val versionsList = loadRemoteList {
            CloudRepository.instance.getFunctionVersionList(authData, parent.group.folder.id, parent.id, it)
        }.getOrThrow()

        val versions = versionsList.map { versionData ->
            CloudFunctionVersion(versionData, parent)
        }

        result.put(parent, dependency, versions)
    }
}

object CloudTriggersLoader :
    DependentResourceLoader<CloudTriggerGroup, List<CloudTrigger>>(CloudTriggerGroup.Triggers) {

    override fun load(parent: CloudTriggerGroup, result: CloudDependencyLoadingResult) {
        val authData = parent.user.authData ?: unauthorizedError()

        val triggersList = loadRemoteList {
            CloudRepository.instance.getTriggerList(authData, parent.folder.id, it)
        }.getOrThrow()

        val triggers = triggersList.map { triggerData ->
            CloudTrigger(triggerData, parent)
        }

        result.put(parent, dependency, triggers)
    }
}


object CloudApiGatewaysLoader :
    DependentResourceLoader<CloudGatewayGroup, List<CloudApiGateway>>(CloudGatewayGroup.ApiGateways) {

    override fun load(parent: CloudGatewayGroup, result: CloudDependencyLoadingResult) {
        val authData = parent.user.authData ?: unauthorizedError()

        val gatewayList = loadRemoteList {
            CloudRepository.instance.getApiGatewayList(authData, parent.folder.id, it)
        }.getOrThrow()

        val gateways = gatewayList.map { gatewayData ->
            CloudApiGateway(gatewayData, parent)
        }

        result.put(parent, dependency, gateways)
    }
}


object CloudServiceAccountsLoader :
    DependentResourceLoader<CloudServiceAccountGroup, List<CloudServiceAccount>>(CloudServiceAccountGroup.ServiceAccounts) {

    override fun load(parent: CloudServiceAccountGroup, result: CloudDependencyLoadingResult) {
        val authData = parent.user.authData ?: unauthorizedError()

        val serviceAccountsList = loadRemoteList {
            CloudRepository.instance.getServiceAccountList(authData, parent.folder.id, it)
        }.getOrThrow()

        val serviceAccounts = serviceAccountsList.map { serviceAccountData ->
            CloudServiceAccount(serviceAccountData, parent)
        }

        result.put(parent, dependency, serviceAccounts)
    }
}

object AvailableRuntimesLoader : DependentResourceLoader<CloudUser, List<String>>(CloudUser.AvailableRuntimes) {
    override fun load(parent: CloudUser, result: CloudDependencyLoadingResult) {
        val authData = parent.user.authData ?: unauthorizedError()
        val runtimes = CloudRepository.instance.getFunctionRuntimeList(authData)
        result.put(parent, dependency, runtimes)
    }
}

object CloudFunctionScalingPoliciesLoader :
    DependentResourceLoader<CloudFunction, List<CloudFunctionScalingPolicy>>(CloudFunction.ScalingPolicies) {

    override fun load(parent: CloudFunction, result: CloudDependencyLoadingResult) {
        val authData = parent.user.authData ?: unauthorizedError()

        val policiesList = loadRemoteList {
            CloudRepository.instance.getFunctionScalingPolicyList(authData, parent.id, it)
        }.getOrThrow()

        val policies = policiesList.map {
            CloudFunctionScalingPolicy(it.tag, it.zoneInstancesLimit, it.zoneRequestsLimit)
        }

        result.put(parent, dependency, policies)
    }
}

object AvailableRolesLoader : DependentResourceLoader<CloudUser, List<RoleOuterClass.Role>>(CloudUser.AvailableRoles) {
    override fun load(parent: CloudUser, result: CloudDependencyLoadingResult) {
        val authData = parent.user.authData ?: unauthorizedError()
        val roles = CloudRepository.instance.getAvailableRolesList(authData)
        result.put(parent, dependency, roles)
    }
}

object FolderAccessBindingsLoader :
    DependentResourceLoader<CloudFolder, List<Access.AccessBinding>>(CloudFolder.AccessBindings) {

    override fun load(parent: CloudFolder, result: CloudDependencyLoadingResult) {
        val authData = parent.user.authData ?: unauthorizedError()

        val accessBindings = loadRemoteList {
            CloudRepository.instance.getFolderAccessBindingList(authData, parent.id, it)
        }.getOrThrow()

        result.put(parent, dependency, accessBindings)
    }
}