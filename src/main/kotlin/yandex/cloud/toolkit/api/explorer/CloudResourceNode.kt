package yandex.cloud.toolkit.api.explorer

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.intellij.ui.treeStructure.SimpleNode
import yandex.cloud.toolkit.api.resource.impl.HierarchicalCloudResource
import yandex.cloud.toolkit.api.resource.impl.icon
import yandex.cloud.toolkit.ui.explorer.CloudExplorerContext
import yandex.cloud.toolkit.ui.explorer.CloudExplorerPath
import yandex.cloud.toolkit.ui.node.DynamicTreeNode
import yandex.cloud.toolkit.ui.node.RefreshTreeNode
import yandex.cloud.toolkit.util.remote.resource.PresentableResourceStatus

class CloudResourceNode(
    val explorer: CloudExplorer,
    val path: CloudExplorerPath,
    val resource: HierarchicalCloudResource
) : SimpleNode(explorer.project, null), DynamicTreeNode, RefreshTreeNode {

    val displayStatus: PresentableResourceStatus? get() = resource.status

    init {
        icon = resource.icon
    }

    override fun update(presentation: PresentationData) {
        val context = CloudExplorerContext(explorer, path)

        presentation.let { data ->
            data.clearText()

            resource.getNodeName(context, data)
            data.setIcon(resource.getNodeIcon(context))

            displayStatus?.display(presentation)
        }
    }

    override fun isAutoExpandNode(): Boolean = false

    override fun toString(): String = resource.name

    override fun getParentDescriptor(): NodeDescriptor<*>? {
        val context = CloudExplorerContext(explorer, path)
        return resource.getParentNode(context)
    }

    override fun getChildren(): Array<SimpleNode> {
        val context = CloudExplorerContext(explorer, path)
        return resource.getChildNodes(context)?.toTypedArray() ?: emptyArray()
    }

    override fun isAlwaysShowPlus(): Boolean = resource.canBeUpdated()

    override fun onExpanded(project: Project) {
        if (resource.canBeUpdated() && !resource.wasUpdatedPreviously()) {
            resource.update(project, true, resource.firstExpansionDepth)
        }
    }

    override fun canBeRefreshed(): Boolean = resource.canBeUpdated()

    override fun onRefresh(project: Project) {
        resource.update(project, false, resource.defaultRefreshDepth)
    }

    override fun getPopupActions(project: Project, actions: MutableList<AnAction>) {
        val context = CloudExplorerContext(explorer, path)
        resource.getExplorerPopupActions(context, actions)
    }

    fun isPin(): Boolean = path.pinnedId == resource.id
}


fun HierarchicalCloudResource.getNodes(explorer: CloudExplorer): Collection<CloudResourceNode> =
    explorer.nodeStorage.getNodes(this)

fun HierarchicalCloudResource.getNode(path: CloudExplorerPath): CloudResourceNode = path.getNode(this)
