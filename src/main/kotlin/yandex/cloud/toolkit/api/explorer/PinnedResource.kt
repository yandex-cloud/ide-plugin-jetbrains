package yandex.cloud.toolkit.api.explorer

import com.intellij.openapi.components.BaseState
import com.intellij.ui.treeStructure.SimpleNode
import yandex.cloud.toolkit.api.resource.CloudResourcePath
import yandex.cloud.toolkit.api.resource.impl.HierarchicalCloudResource
import yandex.cloud.toolkit.api.resource.impl.getLocation
import yandex.cloud.toolkit.util.*
import yandex.cloud.toolkit.util.remote.resource.*
import yandex.cloud.toolkit.util.task.backgroundTask

class PinnedResource(
    val type: String,
    val name: String,
    val location: String?,
    val path: CloudResourcePath
) {

    var resource = RemoteResource<HierarchicalCloudResource>()
        private set

    constructor(resource: HierarchicalCloudResource) : this(
        resource.descriptor.type,
        resource.name,
        if (!resource.isRoot) resource.parent.getLocation() else null,
        resource.getPath()
    ) {
        this.resource.updateState { it.asLoaded(resource) }
    }

    fun getSaveData(): PinnedResourceData = PinnedResourceData().apply {
        val resolved = resource.loadedValue
        type = this@PinnedResource.type
        name = resolved?.name ?: this@PinnedResource.name
        location = resolved?.getLocation() ?: this@PinnedResource.location
        path = this@PinnedResource.path.toString()
    }

    fun getNode(explorer: CloudExplorer): SimpleNode {
        return when (val resolved = resource.loadedValue) {
            null -> UnresolvedPinnedResourceNode(explorer, this)
            else -> resolved.getNode(explorer.rootPath.addBranch(resolved))
        }
    }

    fun canBeResolved(): Boolean = !resource.isLoading
    fun wasResolvedPreviously(): Boolean = resource.wasLoaded
    fun getResourceStatus(): PresentableResourceStatus? = resource.error?.status
    fun getResourceParentId(): String? = resource.loadedValue?.parent?.id

    fun canBeRefreshed(explorer: CloudExplorer): Boolean = when (val resolved = this.resource.loadedValue) {
        null -> canBeResolved()
        else -> resolved.getNode(explorer.rootPath.addBranch(resolved)).canBeRefreshed()
    }

    fun refresh(explorer: CloudExplorer) {
        when (val resolved = this.resource.loadedValue) {
            null -> resolve(explorer, expandResolved = false)
            else -> resolved.getNode(explorer.rootPath.addBranch(resolved)).onRefresh(explorer.project)
        }
    }

    fun reset() {
        resource = RemoteResource()
    }

    fun resolve(explorer: CloudExplorer, expandResolved: Boolean = true) {
        val resource = resource
        resource.tryLoad({ resolveInternal(explorer, it) }) {
            if (resource !== this.resource) return@tryLoad
            explorer.updatePinList(updateGroup = false, if (expandResolved) getNode(explorer) else null)
        }
    }

    private fun resolveInternal(explorer: CloudExplorer, callback: (Maybe<HierarchicalCloudResource>) -> Unit) {
        backgroundTask(explorer.project, "Resolving pins") {
            text = "Resolving $name" + if (location == null) "" else " at $location"

            val user = explorer.user
            val resource = user.map { it.resolve(explorer.project, path, tryLoad = true) }
            callback(resource)
        }
    }

    fun tryUpdateResolved(newResource: HierarchicalCloudResource): Boolean {
        if (resource.isLoading) return false
        var updated = false
        resource.updateState { state ->
            if (state is LoadedResourceState && state.value.version < newResource.version) {
                updated = true
                state.asLoaded(newResource)
            } else state
        }
        return updated
    }

    companion object {

        fun fromData(data: PinnedResourceData): PinnedResource? {
            val type = data.type ?: return null
            val name = data.name ?: return null
            val location = data.location
            val path = data.path?.let(CloudResourcePath::fromString) ?: return null
            return PinnedResource(type, name, location, path)
        }
    }
}

class PinnedResourceData : BaseState() {
    var type by string()
    var name by string()
    var location by string()
    var path by string()
}