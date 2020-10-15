package yandex.cloud.toolkit.ui.node

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.treeStructure.SimpleNode
import yandex.cloud.toolkit.api.explorer.CloudExplorer
import yandex.cloud.toolkit.ui.action.UnpinAllResourcesAction

class PinnedResourcesNode(val explorer: CloudExplorer) : SimpleNode(explorer.project, explorer.rootNode),
    RefreshTreeNode, DynamicTreeNode {

    override fun update(presentation: PresentationData) {
        presentation.setIcon(AllIcons.General.Pin_tab)
        presentation.addText("Pinned", SimpleTextAttributes.REGULAR_ATTRIBUTES)
    }

    override fun isAlwaysShowPlus(): Boolean = explorer.getPinnedResources().isNotEmpty()
    override fun getChildren(): Array<SimpleNode> =
        explorer.getPinnedResources().map { it.getNode(explorer) }.toTypedArray()

    override fun canBeRefreshed(): Boolean = explorer.getPinnedResources().any { it.canBeRefreshed(explorer) }

    override fun onRefresh(project: Project) {
        explorer.getPinnedResources().forEach { it.refresh(explorer) }
    }

    override fun getPopupActions(project: Project, actions: MutableList<AnAction>) {
        if(explorer.hasPinned()) actions += UnpinAllResourcesAction(explorer)
    }
}