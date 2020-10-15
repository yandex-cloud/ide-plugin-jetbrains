package yandex.cloud.toolkit.api.resource.impl

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.treeStructure.SimpleNode
import yandex.cloud.toolkit.api.explorer.getNode
import yandex.cloud.toolkit.api.resource.*
import yandex.cloud.toolkit.api.resource.impl.model.CloudUser
import yandex.cloud.toolkit.ui.explorer.CloudExplorerContext
import yandex.cloud.toolkit.util.Maybe
import yandex.cloud.toolkit.util.noResource
import yandex.cloud.toolkit.util.remote.resource.PresentableResourceStatus
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import javax.swing.Icon

abstract class HierarchicalCloudResource : CloudResource {

    abstract val parent: HierarchicalCloudResource
    abstract val descriptor: CloudResourceDescriptor

    open val isRoot: Boolean = false
    open val defaultRefreshDepth: Int = Int.MAX_VALUE
    open val firstExpansionDepth: Int = 0
    open val status: PresentableResourceStatus? = null

    override val userId: String get() = this.user.id
    override val isVirtual: Boolean get() = if (isRoot) false else parent.isVirtual
    override val isTrueCloudResource: Boolean get() = true

    val version = lastVersion.getAndIncrement()

    open fun getParentNode(context: CloudExplorerContext): NodeDescriptor<*>? = when (context.pinnedId) {
        id -> context.explorer.pinnedGroupNode
        else -> parent.getNode(context.path)
    }

    open fun getNodeIcon(context: CloudExplorerContext): Icon? = descriptor.icon
    open fun getChildNodes(context: CloudExplorerContext): List<SimpleNode>? = null

    open fun getNodeName(context: CloudExplorerContext, presentation: PresentationData) {
        presentation.addText("$name ", SimpleTextAttributes.REGULAR_ATTRIBUTES)
        if (context.pinnedId == id) {
            val location = parent.getLocation()
            if (location.isNotEmpty()) presentation.addText("($location) ", SimpleTextAttributes.GRAY_SMALL_ATTRIBUTES)
        }
    }

    open fun canBeUpdated(): Boolean = false
    open fun wasUpdatedPreviously(): Boolean = false

    override fun toString(): String = name
    override fun equals(other: Any?): Boolean = (other as? CloudResource)?.id == id
    override fun hashCode(): Int = id.hashCode()

    open fun update(project: Project, forceInDepth: Boolean, depth: Int = defaultRefreshDepth): Boolean = false

    open fun resolve(project: Project, path: CloudResourcePath, tryLoad: Boolean): Maybe<HierarchicalCloudResource> =
        noResource("Unresolved resource path $path at ${descriptor.name} $id", PresentableResourceStatus.Unresolved)

    override fun notifyListenersOnDependencyUpdate(dependency: CloudDependency<*, *>): Boolean = false

    open fun getExplorerPopupActions(context: CloudExplorerContext, actions: MutableList<AnAction>) {

    }

    fun hasInPath(resource: HierarchicalCloudResource): Boolean = when {
        isRoot -> false
        resource.id == this.id -> true
        else -> parent.hasInPath(resource)
    }

    open fun getLocation(joiner: StringJoiner) {
        if (!isRoot) parent.getLocation(joiner)
        joiner.add(name)
    }

    abstract fun getPath(innerPath: CloudResourcePath? = null): CloudResourcePath

    companion object {
        private val lastVersion = AtomicLong(-1)
    }
}

open class CloudResourceDescriptor(val type: String, val name: String, val icon: Icon?, val isPinnable: Boolean = false)

val HierarchicalCloudResource.icon: Icon? get() = descriptor.icon

fun HierarchicalCloudResource.getLocation() = StringJoiner("/").apply(this::getLocation).toString()

fun <P : HierarchicalCloudResource, R> P.loadDependency(
    project: Project,
    dependency: CloudDependency<P, R>,
    loader: CloudDependencyLoader<P, R>,
) {
    val context = CloudDependencyLoadingContext(project, null)
    loadDependency(context, dependency, loader)
}

fun <P : HierarchicalCloudResource, R> P.loadDependency(
    context: CloudDependencyLoadingContext,
    dependency: CloudDependency<P, R>,
    loader: CloudDependencyLoader<P, R>,
) {
    CloudResourceStorage.instance.loadDependencyAsync(context, this, dependency, loader)
}

fun <P : HierarchicalCloudResource, R> P.loadDependencyNow(
    project: Project,
    dependency: CloudDependency<P, R>,
    loader: CloudDependencyLoader<P, R>,
): Maybe<R> =
    CloudResourceStorage.instance.loadDependencySync(project, this, dependency, loader, showErrorNotifications = false)

val HierarchicalCloudResource.user: CloudUser
    get() = when (this) {
        is CloudUser -> this
        else -> this.parent.user
    }

/**
 * Works only for resource with self dependency inside storage
 * @return DependentResource in user by self dependency
 */
fun <R: CloudResource> CloudUser.resourceReference(resourceId: String, selfDependency: CloudDependency<R, R>): SelfReference<R> = DependentResource(
    user.id,
    resourceId,
    selfDependency
)

