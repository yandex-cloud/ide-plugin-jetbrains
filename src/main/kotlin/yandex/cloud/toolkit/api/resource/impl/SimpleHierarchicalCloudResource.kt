package yandex.cloud.toolkit.api.resource.impl

import com.intellij.openapi.project.Project
import com.intellij.ui.treeStructure.SimpleNode
import yandex.cloud.toolkit.api.explorer.getNode
import yandex.cloud.toolkit.api.resource.*
import yandex.cloud.toolkit.ui.explorer.CloudExplorerContext
import yandex.cloud.toolkit.ui.node.EmptySimpleNode
import yandex.cloud.toolkit.util.Maybe
import yandex.cloud.toolkit.util.just
import yandex.cloud.toolkit.util.map
import yandex.cloud.toolkit.util.noResource
import yandex.cloud.toolkit.util.remote.resource.PresentableResourceStatus
import yandex.cloud.toolkit.util.remote.resource.asPresentableStatus
import yandex.cloud.toolkit.util.remote.resource.isLoading
import yandex.cloud.toolkit.util.remote.resource.wasLoaded

abstract class SimpleHierarchicalCloudResource<SELF : HierarchicalCloudResource, CHILD : HierarchicalCloudResource> :
    HierarchicalCloudResource() {

    abstract val mainDependency: CloudDependency<SELF, List<CHILD>>
    abstract val mainDependencyLoader: CloudDependencyLoader<SELF, List<CHILD>>
    protected open val mainDependencyName: String get() = "main"

    override val status: PresentableResourceStatus? get() = mainDependency.of(this).instance.asPresentableStatus()
    protected open val emptyNode: SimpleNode get() = EmptySimpleNode

    override fun notifyListenersOnDependencyUpdate(dependency: CloudDependency<*, *>): Boolean =
        dependency == mainDependency

    override fun getChildNodes(context: CloudExplorerContext): List<SimpleNode>? {
        val nodes = mainDependency.of(this.userId, this.id).get()?.map { it.getNode(context.path) }
        return if (nodes?.isNullOrEmpty() == true) listOf(emptyNode) else nodes
    }

    override fun canBeUpdated(): Boolean = !mainDependency.of(this).instance.isLoading
    override fun wasUpdatedPreviously(): Boolean = mainDependency.of(this).instance.wasLoaded

    override fun update(project: Project, forceInDepth: Boolean, depth: Int): Boolean {
        val onSuccess: (() -> Unit)? = if (depth < 1) null else ({
            mainDependency.of(this).get(ifSuccess = true)?.forEach { child ->
                if (forceInDepth || child.wasUpdatedPreviously()) child.update(project, forceInDepth, depth - 1)
            }
        })

        val context = CloudDependencyLoadingContext(project, onSuccess)
        updateMainDependency(context)
        return true
    }

    @Suppress("UNCHECKED_CAST")
    private fun it() = this as SELF

    override fun resolve(project: Project, path: CloudResourcePath, tryLoad: Boolean): Maybe<HierarchicalCloudResource> {
        fun resolveChild(resource: CHILD?): Maybe<HierarchicalCloudResource> = when (val nextPath = path.next) {
            null -> resource?.let(::just)
            else -> resource?.resolve(project, nextPath, tryLoad)
        } ?: noResource("Resource not found", PresentableResourceStatus.NotFound)

        if (path.dependency == mainDependencyName) {
            val resource = mainDependency.of(this).get(ifSuccess = true)?.findById(path.resourceId)
            if (resource != null || !tryLoad) return resolveChild(resource)

            return it().loadDependencyNow(project, mainDependency, mainDependencyLoader).map {
                val child = it.findById(path.resourceId)
                resolveChild(child)
            }
        }
        return super.resolve(project, path, tryLoad)
    }

    fun updateMainDependency(context: CloudDependencyLoadingContext) {
        it().loadDependency(context, mainDependency, mainDependencyLoader)
    }

    fun getChildPath(resource: HierarchicalCloudResource, innerPath: CloudResourcePath?): CloudResourcePath =
        getPath(CloudResourcePath(mainDependencyName, resource.id, innerPath))

}