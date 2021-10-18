package yandex.cloud.toolkit.api.resource.impl.model

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.intellij.ui.treeStructure.SimpleNode
import yandex.cloud.api.access.Access
import yandex.cloud.api.resourcemanager.v1.FolderOuterClass
import yandex.cloud.toolkit.api.explorer.getNode
import yandex.cloud.toolkit.api.resource.CloudDependency
import yandex.cloud.toolkit.api.resource.CloudDependencyLoadingResult
import yandex.cloud.toolkit.api.resource.CloudResourcePath
import yandex.cloud.toolkit.api.resource.impl.CloudResourceDescriptor
import yandex.cloud.toolkit.api.resource.impl.FolderAccessBindingsLoader
import yandex.cloud.toolkit.api.resource.impl.HierarchicalCloudResource
import yandex.cloud.toolkit.api.resource.impl.loadDependency
import yandex.cloud.toolkit.ui.action.CreateOrEditApiGatewayAction
import yandex.cloud.toolkit.ui.action.CreateOrEditFunctionAction
import yandex.cloud.toolkit.ui.action.CreateOrEditServiceAccountAction
import yandex.cloud.toolkit.ui.explorer.CloudExplorerContext
import yandex.cloud.toolkit.util.Maybe
import yandex.cloud.toolkit.util.just

abstract class CloudFolder : HierarchicalCloudResource() {

    abstract val fullName: String

    override val descriptor: CloudResourceDescriptor get() = Descriptor

    val functionGroup = CloudFunctionGroup(this)
    val gatewayGroup = CloudGatewayGroup(this)
    val triggerGroup = CloudTriggerGroup(this)
    val serviceAccountGroup = CloudServiceAccountGroup(this)
    val networkGroup = VPCNetworkGroup(this)

    private val groups: List<HierarchicalCloudResource>
        get() = listOf(
            functionGroup,
            gatewayGroup,
            triggerGroup,
            serviceAccountGroup,
            networkGroup
        )

    override fun getChildNodes(context: CloudExplorerContext): List<SimpleNode> =
        groups.map { it.getNode(context.path) }

    override fun canBeUpdated(): Boolean = groups.any { it.canBeUpdated() }
    override fun wasUpdatedPreviously(): Boolean = groups.any { it.wasUpdatedPreviously() }

    override fun update(project: Project, forceInDepth: Boolean, depth: Int): Boolean {
        var wasUpdated = false
        if (depth > 0) {
            for (group in groups) {
                val canUpdateGroup = group.canBeUpdated() && group.wasUpdatedPreviously()
                if (canUpdateGroup && group.update(project, forceInDepth, depth - 1)) {
                    wasUpdated = true
                }
            }
        }
        return wasUpdated
    }

    override fun resolve(
        project: Project,
        path: CloudResourcePath,
        tryLoad: Boolean
    ): Maybe<HierarchicalCloudResource> {
        val group = if (path.dependency == "groups") groups.find { it.id == path.resourceId } else null

        return if (group != null) {
            when (val nextPath = path.next) {
                null -> just(group)
                else -> group.resolve(project, nextPath, tryLoad)
            }
        } else super.resolve(project, path, tryLoad)
    }

    override fun getExplorerPopupActions(context: CloudExplorerContext, actions: MutableList<AnAction>) {
        actions += CreateOrEditFunctionAction(this, null)
        actions += CreateOrEditServiceAccountAction(this, null)
        actions += CreateOrEditApiGatewayAction(this, null)
    }

    fun getGroupPath(group: FolderGroup, innerPath: CloudResourcePath?): CloudResourcePath =
        getPath(CloudResourcePath("groups", group.id, innerPath))

    fun updateAccessBindings(project: Project) {
        loadDependency(project, AccessBindings, FolderAccessBindingsLoader)
    }

    fun onServiceAccountCreated(project: Project) {
        updateAccessBindings(project)
        serviceAccountGroup.update(project, false)
    }

    object FolderData : CloudDependency<CloudFolder, FolderOuterClass.Folder>()
    object FunctionGroup : CloudDependency<CloudFolder, CloudFunctionGroup>()
    object TriggerGroup : CloudDependency<CloudFolder, CloudTriggerGroup>()
    object GatewayGroup : CloudDependency<CloudFolder, CloudGatewayGroup>()
    object ServiceAccountGroup : CloudDependency<CloudFolder, CloudServiceAccountGroup>()
    object NetworkGroup : CloudDependency<CloudFolder, VPCNetworkGroup>()
    object AccessBindings : CloudDependency<CloudFolder, List<Access.AccessBinding>>()

    object Descriptor : CloudResourceDescriptor(
        "folder",
        "Folder",
        icon = AllIcons.Nodes.Folder,
        isPinnable = true
    )
}

class HierarchicalCloudFolder(
    val data: FolderOuterClass.Folder,
    val cloud: Cloud
) : CloudFolder() {
    override val id: String get() = data.id
    override val name: String get() = data.name
    override val parent: HierarchicalCloudResource get() = cloud
    override val fullName: String get() = "$cloud/$name"

    override fun getPath(innerPath: CloudResourcePath?): CloudResourcePath = cloud.getChildPath(this, innerPath)

    fun addDependencies(deps: CloudDependencyLoadingResult) {
        deps.put(this, FunctionGroup, functionGroup)
        deps.put(this, GatewayGroup, gatewayGroup)
        deps.put(this, TriggerGroup, triggerGroup)
        deps.put(this, ServiceAccountGroup, serviceAccountGroup)
        deps.put(this, NetworkGroup, networkGroup)
    }
}

class VirtualCloudFolder(val user: CloudUser, override val id: String) : CloudFolder() {
    override val name: String get() = "Virtual Folder"
    override val parent: HierarchicalCloudResource get() = user
    override val fullName: String get() = name
    override val isVirtual: Boolean get() = true

    override fun getPath(innerPath: CloudResourcePath?): CloudResourcePath = user.getChildPath(this, innerPath)
}

interface FolderGroup {
    val id: String
}