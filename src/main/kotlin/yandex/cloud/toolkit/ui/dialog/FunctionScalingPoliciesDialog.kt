package yandex.cloud.toolkit.ui.dialog

import com.intellij.ide.util.treeView.TreeState
import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.ui.*
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.fields.IntegerField
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.ui.treeStructure.SimpleNode
import com.intellij.ui.treeStructure.SimpleTreeStructure
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.tree.TreeUtil
import yandex.cloud.toolkit.api.auth.CloudAuthData
import yandex.cloud.toolkit.api.explorer.CloudResourceNode
import yandex.cloud.toolkit.api.resource.impl.model.CloudFunction
import yandex.cloud.toolkit.api.resource.impl.model.CloudFunctionScalingPolicy
import yandex.cloud.toolkit.api.resource.impl.model.CloudFunctionVersion
import yandex.cloud.toolkit.api.service.CloudOperationService
import yandex.cloud.toolkit.ui.component.CloudResourceField
import yandex.cloud.toolkit.ui.component.FunctionTagField
import yandex.cloud.toolkit.ui.node.ScalingPolicyNode
import yandex.cloud.toolkit.ui.node.ScalingPolicyPropertyNode
import yandex.cloud.toolkit.util.*
import java.awt.BorderLayout
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.ToolTipManager
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

class FunctionScalingPoliciesDialog(
    val project: Project,
    val authData: CloudAuthData,
    val function: CloudFunction,
    val versions: List<CloudFunctionVersion>,
    val scalingPolicies: List<CloudFunctionScalingPolicy>,
) : DialogWrapper(true) {

    private val policyTreeStructure = PoliciesTreeStructure()
    private val policyTreeModel = StructureTreeModel<PoliciesTreeStructure>(policyTreeStructure, myDisposable)
    private val policyTree: Tree = Tree(AsyncTreeModel(policyTreeModel, myDisposable))

    private val currentScalingPolicies: MutableMap<String, ScalingPolicyNode> =
        scalingPolicies.associateTo(mutableMapOf()) { it.tag to ScalingPolicyNode(policyTreeStructure.root, it.copy()) }

    init {
        policyTree.emptyText.text = "No Scaling Policies"
        policyTree.isRootVisible = false
        policyTree.showsRootHandles = true
        policyTree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION

        TreeUIHelper.getInstance().installTreeSpeedSearch(policyTree)
        TreeUtil.installActions(policyTree)
        TreeUtil.expandAll(policyTree)

        object : DoubleClickListener() {
            override fun onDoubleClick(e: MouseEvent): Boolean {
                val node = policyTree.getTreePath(e.x, e.y)?.node
                if (node is ScalingPolicyPropertyNode) {
                    showPropertyDialog(node)
                    return true
                }
                return false
            }
        }.installOn(policyTree)

        cancelAction.text = "Close"
        okAction.text = "Save"
        title = "Yandex.Cloud Function Scaling Policies"
        init()
    }

    override fun doOKAction() {
        if (okAction.isEnabled) {
            CloudOperationService.instance.setFunctionScalingPolicies(
                project, authData, function,
                scalingPolicies.associateBy { it.tag },
                currentScalingPolicies.mapValues { it.value.scalingPolicy }
            )
        }
        super.doOKAction()
    }

    override fun createCenterPanel(): JComponent {
        return YCUI.borderPanel {
            LabeledComponent.create(
                CloudResourceField(project, function),
                "Function", BorderLayout.WEST
            ) addAs BorderLayout.NORTH

            ToolbarDecorator.createDecorator(policyTree).apply {
                setToolbarPosition(ActionToolbarPosition.RIGHT)
                setMoveDownAction(null)
                setMoveUpAction(null)
                setAddAction {
                    showTagInputDialog(null)
                }
                setRemoveAction {
                    val node = policyTree.selectionPath?.node

                    if (node is ScalingPolicyNode) {
                        currentScalingPolicies.remove(node.scalingPolicy.tag)
                        redrawPolicyTree(null)
                    } else if (node is ScalingPolicyPropertyNode) {
                        node.propertySetter(0L)
                        redrawPolicyTree(null)
                    }
                }
                setRemoveActionUpdater {
                    val node = policyTree.selectionPath?.node
                    node is ScalingPolicyNode || node is ScalingPolicyPropertyNode
                }
                setEditAction {
                    val node = policyTree.selectionPath?.node

                    if (node is ScalingPolicyNode) {
                        showTagInputDialog(node.scalingPolicy)
                    } else if (node is ScalingPolicyPropertyNode) {
                        showPropertyDialog(node)
                    }
                }
                setEditActionUpdater {
                    val node = policyTree.selectionPath?.node
                    node is ScalingPolicyNode || node is ScalingPolicyPropertyNode
                }
            }.createPanel() addAs BorderLayout.CENTER
        }.withPreferredSize(500, 500)
    }

    private fun showTagInputDialog(prev: CloudFunctionScalingPolicy?) = DialogBuilder(this.contentPanel).apply {
        title(if (prev != null) "Edit Scaling Policy Tag" else "Select Scaling Policy Tag")

        val tagField = FunctionTagField(versions, canBeEmpty = true) { tag ->
            tag !in currentScalingPolicies || tag == prev?.tag
        }

        tagField.noTagsMessage = "No free tags"
        if (prev != null) tagField.selectTag(prev.tag)

        tagField.addActionListener {
            setOkActionEnabled(!tagField.selectedTag.isNullOrEmpty())
        }
        setOkActionEnabled(!tagField.selectedTag.isNullOrEmpty())

        centerPanel(
            YCUI.gridPanel {
                YCUI.gridBag(horizontal = true) {
                    JLabel("Tag") addAs nextln(0.2)
                    tagField addAs next(0.8)
                }
            }.withPreferredWidth(400)
        )

        setOkOperation {
            val tag = tagField.selectedTag!!
            if (prev != null && prev.tag != tag) currentScalingPolicies.remove(prev.tag)
            val policy = prev?.copy(tag = tag) ?: CloudFunctionScalingPolicy(tag, 0, 0)
            val policyNode = ScalingPolicyNode(policyTreeStructure.root, policy)
            currentScalingPolicies[tag] = policyNode
            redrawPolicyTree(policyNode)
            dialogWrapper.close(OK_EXIT_CODE)
        }

        setCancelOperation {
            dialogWrapper.close(CANCEL_EXIT_CODE)
        }
    }.show()

    private fun showPropertyDialog(node: ScalingPolicyPropertyNode) = DialogBuilder(this.contentPanel).apply {
        title("Edit Scaling Policy Property")

        val valueField = IntegerField(node.propertyName, 0, Integer.MAX_VALUE)
        valueField.defaultValue = 0
        valueField.value = node.propertyGetter().toInt()

        centerPanel(
            YCUI.gridPanel {
                YCUI.gridBag(horizontal = true) {
                    JLabel(node.propertyName + " ") addAs nextln(0.0)
                    valueField addAs next(1.0)
                }
            }.withPreferredWidth(300)
        )

        setOkOperation {
            node.propertySetter(valueField.value.toLong())
            redrawPolicyTree(null)
            dialogWrapper.close(OK_EXIT_CODE)
        }

        setCancelOperation {
            dialogWrapper.close(CANCEL_EXIT_CODE)
        }
    }.show()

    private fun redrawPolicyTree(toExpand: ScalingPolicyNode?) {
        invokeLaterAt(this.contentPanel) {
            val treeState = TreeState.createOn(policyTree)
            policyTreeModel.invalidate(policyTreeStructure.root, true)
            treeState.applyTo(policyTree)
            if (toExpand != null) policyTreeModel.expand(toExpand, policyTree) {}
        }
    }

    private inner class PoliciesTreeStructure : SimpleTreeStructure() {
        val root = object : SimpleNode() {
            override fun getChildren(): Array<SimpleNode> = currentScalingPolicies.values.toTypedArray()
        }

        override fun getRootElement(): Any = root
    }

    private val TreePath.node: SimpleNode?
        get() = (this.lastPathComponent as? DefaultMutableTreeNode)?.userObject as? SimpleNode
}