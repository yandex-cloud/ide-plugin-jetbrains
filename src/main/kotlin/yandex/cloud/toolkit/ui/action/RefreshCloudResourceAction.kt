package yandex.cloud.toolkit.ui.action

import com.intellij.icons.AllIcons
import com.intellij.ide.actions.RefreshAction
import com.intellij.openapi.actionSystem.AnActionEvent
import yandex.cloud.toolkit.ui.explorer.CloudExplorerPanel
import yandex.cloud.toolkit.ui.node.RefreshTreeNode

class RefreshCloudResourceAction(val explorerPanel: CloudExplorerPanel) :
    RefreshAction("Refresh", "Reload resource from Yandex.Cloud", AllIcons.Actions.Refresh) {

    override fun update(e: AnActionEvent) {
        val selectedNode = explorerPanel.selectedNode
        e.presentation.isEnabled = selectedNode is RefreshTreeNode && selectedNode.canBeRefreshed()
    }

    override fun actionPerformed(e: AnActionEvent) {
        explorerPanel.onRefreshAction()
    }
}