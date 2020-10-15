package yandex.cloud.toolkit.api.explorer

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.treeStructure.SimpleNode
import yandex.cloud.toolkit.api.resource.CloudResourceStorage
import yandex.cloud.toolkit.ui.action.UnpinResourceAction
import yandex.cloud.toolkit.ui.node.DynamicTreeNode
import yandex.cloud.toolkit.ui.node.RefreshTreeNode
import yandex.cloud.toolkit.util.remote.resource.PresentableResourceStatus
import yandex.cloud.toolkit.util.remote.resource.isLoading

class UnresolvedPinnedResourceNode(private val explorer: CloudExplorer, private val pinned: PinnedResource) :
    SimpleNode(), DynamicTreeNode, RefreshTreeNode {

    override fun update(presentation: PresentationData) {
        val descriptor = CloudResourceStorage.instance.getDescriptor(pinned.type)

        presentation.setIcon(if (descriptor != null) descriptor.icon else AllIcons.Nodes.Unknown)

        presentation.addText(pinned.name + " ", SimpleTextAttributes.REGULAR_ATTRIBUTES)
        if (!pinned.location.isNullOrEmpty()) {
            presentation.addText("(${pinned.location}) ", SimpleTextAttributes.GRAY_SMALL_ATTRIBUTES)
        }

        val status = if (pinned.resource.isLoading) PresentableResourceStatus.Resolving else pinned.getResourceStatus()
        status?.display(presentation)
    }

    override fun getParentDescriptor(): NodeDescriptor<*> = explorer.pinnedGroupNode
    override fun getChildren(): Array<SimpleNode> = emptyArray()

    override fun isAutoExpandNode(): Boolean = false
    override fun canBeRefreshed(): Boolean = pinned.canBeResolved()
    override fun isAlwaysShowPlus(): Boolean = !pinned.wasResolvedPreviously()

    override fun onExpanded(project: Project) {
        if (pinned.canBeResolved() && !pinned.wasResolvedPreviously()) {
            pinned.resolve(explorer)
            explorer.updatePinNode(this)
        }
    }

    override fun onRefresh(project: Project) {
        pinned.resolve(explorer)
        explorer.updatePinNode(this)
    }

    override fun getPopupActions(project: Project, actions: MutableList<AnAction>) {
        actions += UnpinResourceAction(explorer, pinned)
    }
}