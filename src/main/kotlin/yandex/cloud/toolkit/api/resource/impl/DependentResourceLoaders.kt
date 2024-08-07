package yandex.cloud.toolkit.api.resource.impl

import yandex.cloud.api.access.Access
import yandex.cloud.api.iam.v1.RoleOuterClass
import yandex.cloud.toolkit.api.resource.CloudDependency
import yandex.cloud.toolkit.api.resource.CloudDependencyLoader
import yandex.cloud.toolkit.api.resource.CloudDependencyLoadingResult
import yandex.cloud.toolkit.api.resource.CloudResource
import yandex.cloud.toolkit.api.resource.impl.model.*
import yandex.cloud.toolkit.api.service.CloudRepository
import yandex.cloud.toolkit.util.getOrThrow
import yandex.cloud.toolkit.util.remote.list.loadRemoteList
import yandex.cloud.toolkit.util.remote.resource.PresentableResourceStatus
import yandex.cloud.toolkit.util.remote.resource.ResourceLoadingError
import yandex.cloud.toolkit.util.remote.resource.UnauthenticatedException

abstract class DependentResourceLoader<P : CloudResource, R>(
    val dependency: CloudDependency<P, R>
) : CloudDependencyLoader<P, R> {

    fun error(text: String, status: PresentableResourceStatus): Nothing = throw ResourceLoadingError(text, status)
    fun unauthenticatedError(): Nothing = throw UnauthenticatedException()
}

object CloudsLoader : DependentResourceLoader<CloudUser, List<Cloud>>(CloudUser.Clouds) {

    override fun load(parent: CloudUser, result: CloudDependencyLoadingResult) {
        val authData = parent.authData ?: unauthenticatedError()

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
        val authData = parent.user.authData ?: unauthenticatedError()

        val foldersList = loadRemoteList {
            CloudRepository.instance.getFolderList(authData, parent.id, it)
        }.getOrThrow()

        val folders = foldersList.map { folderData ->
            val folder = HierarchicalCloudFolder(folderData, parent)
            result.put(folder, CloudFolder.FolderData, folderData)
            folder.addDependencies(result)
            folder
        }

        result.put(parent, dependency, folders)
    }
}

object CloudFunctionsLoader :
    DependentResourceLoader<CloudFunctionGroup, List<CloudFunction>>(CloudFunctionGroup.Functions) {

    override fun load(parent: CloudFunctionGroup, result: CloudDependencyLoadingResult) {
        val authData = parent.user.authData ?: unauthenticatedError()

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
        val authData = parent.user.authData ?: unauthenticatedError()
        val function = CloudRepository.instance.getFunction(authData, functionId)
        result.put(parent, dependency, CloudFunction(function, parent))
    }
}

object CloudFunctionVersionsLoader :
    DependentResourceLoader<CloudFunction, List<CloudFunctionVersion>>(CloudFunction.FunctionVersions) {

    override fun load(parent: CloudFunction, result: CloudDependencyLoadingResult) {
        val authData = parent.user.authData ?: unauthenticatedError()

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
        val authData = parent.user.authData ?: unauthenticatedError()

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
        val authData = parent.user.authData ?: unauthenticatedError()

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
        val authData = parent.user.authData ?: unauthenticatedError()

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
        val authData = parent.user.authData ?: unauthenticatedError()
        val runtimes = CloudRepository.instance.getFunctionRuntimeList(authData)
        result.put(parent, dependency, runtimes)
    }
}

object CloudFunctionScalingPoliciesLoader :
    DependentResourceLoader<CloudFunction, List<CloudFunctionScalingPolicy>>(CloudFunction.ScalingPolicies) {

    override fun load(parent: CloudFunction, result: CloudDependencyLoadingResult) {
        val authData = parent.user.authData ?: unauthenticatedError()

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
        val authData = parent.user.authData ?: unauthenticatedError()
        val roles = CloudRepository.instance.getAvailableRolesList(authData)
        result.put(parent, dependency, roles)
    }
}

object FolderAccessBindingsLoader :
    DependentResourceLoader<CloudFolder, List<Access.AccessBinding>>(CloudFolder.AccessBindings) {

    override fun load(parent: CloudFolder, result: CloudDependencyLoadingResult) {
        val authData = parent.user.authData ?: unauthenticatedError()

        val accessBindings = loadRemoteList {
            CloudRepository.instance.getFolderAccessBindingList(authData, parent.id, it)
        }.getOrThrow()

        result.put(parent, dependency, accessBindings)
    }
}

object VPCNetworksLoader : DependentResourceLoader<VPCNetworkGroup, List<VPCNetwork>>(VPCNetworkGroup.Networks) {

    override fun load(parent: VPCNetworkGroup, result: CloudDependencyLoadingResult) {
        val authData = parent.user.authData ?: unauthenticatedError()

        val networksList = loadRemoteList {
            CloudRepository.instance.getNetworkList(authData, parent.folder.id, it)
        }.getOrThrow()

        val networks = networksList.map { networkData ->
            VPCNetwork(networkData, parent)
        }

        result.put(parent, dependency, networks)
    }
}

object VPCSubnetsLoader : DependentResourceLoader<VPCNetwork, List<VPCSubnet>>(VPCNetwork.Subnets) {

    override fun load(parent: VPCNetwork, result: CloudDependencyLoadingResult) {
        val authData = parent.user.authData ?: unauthenticatedError()

        val subnetsList = loadRemoteList {
            CloudRepository.instance.getSubnetList(authData, parent.id, it)
        }.getOrThrow()

        val subnets = subnetsList.map { subnetData ->
            VPCSubnet(subnetData, parent)
        }

        result.put(parent, dependency, subnets)
    }
}