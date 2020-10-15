package yandex.cloud.toolkit.ui.explorer

import com.intellij.ui.treeStructure.SimpleTreeStructure

class CloudExplorerTreeStructure(val explorerPanel: CloudExplorerPanel) : SimpleTreeStructure() {

    override fun getRootElement(): Any = explorerPanel.explorer.rootNode

    override fun commit() {}
    override fun hasSomethingToCommit(): Boolean = false
    override fun isToBuildChildrenInBackground(element: Any): Boolean = false
}