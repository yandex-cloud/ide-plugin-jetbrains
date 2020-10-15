package yandex.cloud.toolkit.ui.explorer

import com.intellij.ui.treeStructure.SimpleNode
import yandex.cloud.toolkit.api.explorer.CloudExplorer
import yandex.cloud.toolkit.ui.node.SelectProfileNode

class CloudExplorerRootNode(val explorer: CloudExplorer) : SimpleNode(explorer.project) {

    override fun getChildren(): Array<SimpleNode> {
        val nodes = mutableListOf<SimpleNode>()
        if(explorer.hasPinned()) nodes += explorer.pinnedGroupNode
        nodes += explorer.userNode ?: SelectProfileNode()
        return nodes.toTypedArray()
    }
}