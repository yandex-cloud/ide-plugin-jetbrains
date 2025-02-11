package yandex.cloud.toolkit.ui.explorer

import com.intellij.ide.util.treeView.TreeState
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.ActionManagerEx
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.*
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.ui.treeStructure.SimpleNode
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.tree.TreeUtil
import yandex.cloud.toolkit.api.explorer.CloudExplorer
import yandex.cloud.toolkit.api.explorer.CloudResourceNode
import yandex.cloud.toolkit.api.explorer.UnresolvedPinnedResourceNode
import yandex.cloud.toolkit.api.explorer.getNodes
import yandex.cloud.toolkit.api.profile.CloudProfile
import yandex.cloud.toolkit.api.profile.impl.profileStorage
import yandex.cloud.toolkit.api.resource.CloudResourceUpdateEvent
import yandex.cloud.toolkit.api.resource.impl.HierarchicalCloudResource
import yandex.cloud.toolkit.ui.action.*
import yandex.cloud.toolkit.ui.node.*
import yandex.cloud.toolkit.util.getTreePath
import yandex.cloud.toolkit.util.invokeLaterAt
import java.awt.Component
import java.awt.event.MouseEvent
import javax.swing.ToolTipManager
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeExpansionListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

class CloudExplorerPanel(val explorer: CloudExplorer) : SimpleToolWindowPanel(true, true), Disposable {

    private val project = explorer.project

    private val explorerStructure = CloudExplorerTreeStructure(this)
    private val explorerModel = StructureTreeModel<CloudExplorerTreeStructure>(explorerStructure, this)
    private var explorerTree: Tree = Tree(AsyncTreeModel(explorerModel, this))

    val selectedNode: DynamicTreeNode? get() = explorerTree.selectionPath?.dynamicNode
    private val toolbarActions = DefaultActionGroup()

    init {
        setupTree()
        setupToolbar()
    }

    private fun setupTree() {
        explorerTree.isRootVisible = false
        explorerTree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION

        TreeUIHelper.getInstance().installTreeSpeedSearch(explorerTree)
        TreeUtil.installActions(explorerTree)
        ToolTipManager.sharedInstance().registerComponent(explorerTree)

        setContent(ScrollPaneFactory.createScrollPane(explorerTree))

        explorer.userNode?.onExpanded(project)

        object : DoubleClickListener() {
            override fun onDoubleClick(event: MouseEvent): Boolean {
                val node = explorerTree.getTreePath(event.x, event.y)?.dynamicNode
                node?.onDoubleClick(project, this@CloudExplorerPanel, event.x, event.y)
                return true
            }
        }.installOn(explorerTree)

        object : ClickListener() {
            override fun onClick(event: MouseEvent, clickCount: Int): Boolean {
                val node = explorerTree.getTreePath(event.x, event.y)?.dynamicNode
                node?.onClick(project, this@CloudExplorerPanel, event.x, event.y)
                return true
            }
        }.installOn(explorerTree)

        explorerTree.addTreeExpansionListener(
            object : TreeExpansionListener {
                override fun treeExpanded(event: TreeExpansionEvent) {
                    event.path.dynamicNode?.onExpanded(project)
                }

                override fun treeCollapsed(event: TreeExpansionEvent) {
                    event.path.dynamicNode?.onCollapsed(project)
                }
            }
        )

        explorerTree.addMouseListener(
            object : PopupHandler() {
                override fun invokePopup(comp: Component?, x: Int, y: Int) {
                    val node = selectedNode ?: return
                    val defaultActions = mutableListOf<AnAction>()

                    if (node is RefreshTreeNode && node.canBeRefreshed()) {
                        defaultActions += RefreshCloudResourceAction(this@CloudExplorerPanel)
                    }

                    if (node is CloudResourceNode) {
                        if (node.resource.isTrueCloudResource) {
                            defaultActions += CopyResourceIDAction(node.resource)
                        }

                        val pinned = explorer.getPinned(node.resource.getPath())
                        if (node.isPin()) {
                            if (pinned != null) defaultActions += UnpinResourceAction(explorer, pinned)
                        } else if (node.resource.descriptor.isPinnable) {
                            defaultActions += PinResourceAction(explorer, node.resource, pinned != null)
                        }
                    }

                    val actions = mutableListOf<AnAction>()
                    node.getPopupActions(project, actions)
                    if (actions.size > 1) actions += Separator.create()
                    actions += defaultActions

                    if (actions.isNotEmpty()) {
                        val actionManager = ActionManagerEx.getInstanceEx()
                        val actionGroup = DefaultActionGroup(actions)

                        val popupMenu = actionManager.createActionPopupMenu("CloudExplorerPanel", actionGroup)
                        popupMenu.component.show(comp, x, y)
                    }
                }
            }
        )
    }

    private fun setupToolbar() {
        toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.TOOLBAR, toolbarActions, true).
            apply {
                targetComponent = this@CloudExplorerPanel
            }.component
        updateToolbar()
    }

    private fun updateToolbar() {
        toolbarActions.removeAll()
        toolbarActions.add(RefreshCloudResourceAction(this@CloudExplorerPanel))
        if (project.profileStorage.profile != null) toolbarActions.add(ManageProfilesAction())
    }

    fun redrawTree(invalidate: Boolean) {
        updateTree {
            if (invalidate) {
                explorerModel.invalidate()
            } else {
                explorerModel.invalidate(explorer.rootNode, false)
            }
        }
    }

    fun onProfileSelected(profile: CloudProfile?) {
        invokeLaterAt(this) {
            updateToolbar()
            redrawTree(invalidate = true)
            explorer.userNode?.onExpanded(project)
        }
    }

    fun onProfileRenamed() {
        val userNode = explorer.userNode ?: return
        explorerModel.invalidate(userNode, false)
    }

    fun onRefreshAction() {
        explorerTree.selectionPath?.refreshNode?.onRefresh(project)
    }

    private fun updateTree(block: () -> Unit) {
        invokeLaterAt(this) {
            val treeState = TreeState.createOn(explorerTree)
            explorerModel.invoker.invoke(block)
            treeState.applyTo(explorerTree)
        }
    }

    fun onCloudResourcesUpdate(events: List<CloudResourceUpdateEvent>) {
        updateTree {
            var updatePinnedGroup = false
            events.forEach {
                val resource = it.resource as HierarchicalCloudResource
                for (node in resource.getNodes(explorer)) {
                    // if updated resource is pinned we update resolved instance & invalidate pinned group
                    if (node.isPin()) {
                        val pinned = explorer.getPinned(resource.getPath())
                        updatePinnedGroup = updatePinnedGroup or (pinned?.tryUpdateResolved(resource) ?: false)
                    }
                    explorerModel.invalidate(node, it.isChildrenChanged)
                }
            }
            if (updatePinnedGroup) {
                explorerModel.invalidate(explorer.pinnedGroupNode, true)
            }
        }
    }

    override fun dispose() {
        ToolTipManager.sharedInstance().unregisterComponent(explorerTree)
        explorerModel.dispose()
    }

    fun onPinListChanged(updateGroup: Boolean, newNode: SimpleNode?) {
        updateTree {
            if (!updateGroup) {
                explorerModel.invalidate(explorer.pinnedGroupNode, true)
            } else {
                explorerModel.invalidate(explorer.rootNode, true)
            }

            if (newNode != null) {
                explorerModel.expand(newNode, explorerTree) {}
            }
        }
    }

    fun onPinNodeChanged(pinNode: UnresolvedPinnedResourceNode) {
        explorerModel.invalidate(pinNode, false)
    }

    private val TreePath.dynamicNode: DynamicTreeNode?
        get() = (this.lastPathComponent as? DefaultMutableTreeNode)?.userObject as? DynamicTreeNode

    private val TreePath.refreshNode: RefreshTreeNode?
        get() = (this.lastPathComponent as? DefaultMutableTreeNode)?.userObject as? RefreshTreeNode
}