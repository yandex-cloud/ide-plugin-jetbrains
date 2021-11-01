package yandex.cloud.toolkit.ui.component

import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.*
import com.intellij.ui.components.JBList
import icons.CloudIcons
import yandex.cloud.api.vpc.v1.SubnetOuterClass
import yandex.cloud.toolkit.ui.action.CopyToBufferAction
import yandex.cloud.toolkit.util.*
import java.awt.BorderLayout
import javax.swing.JList

class VPCSubnetsList(
    val project: Project,
    val folderId: String?
) : YCPanel(BorderLayout()) {

    private val listModel = CollectionListModel<SubnetData>()
    private val list = JBList(listModel)

    var subnets: List<String>
        get() = listModel.items.map(SubnetData::id)
        set(value) = listModel.setItems(
            value.map { SubnetData(it) }
        )

    private var availableSubnets: List<SubnetOuterClass.Subnet>? = null

    init {
        list.setEmptyText("No Subnets")
        list.cellRenderer = ListRenderer()

        ToolbarDecorator.createDecorator(list).apply {
            setToolbarPosition(ActionToolbarPosition.RIGHT)
            setMoveDownAction(null)
            setMoveUpAction(null)
            setEditAction {
                showSubnetDialog(list.selectedIndex)
            }
            setAddAction {
                showSubnetDialog(-1)
            }

            setRemoveActionUpdater {
                val selectedValues = list.selectedValuesList
                selectedValues.isNotEmpty()
            }
            setEditActionUpdater { list.isSingleValueSelected }

            val copyAction = AnActionButton.fromAction(CopyToBufferAction { list.singleSelectedValue?.id ?: "" })
            copyAction.addCustomUpdater { list.isSingleValueSelected }
            addExtraAction(copyAction)
        }.createPanel() addAs BorderLayout.CENTER
    }

    private fun showSubnetDialog(replaceIndex: Int) {
        DialogBuilder(this).apply {
            title("Add Subnet")
            resizable(false)

            val subnetField = VPCSubnetField(project, folderId, availableSubnets, BorderLayout.SOUTH, "Subnet ID")
            if (replaceIndex != -1) {
                subnetField.value = list.model.getElementAt(replaceIndex).id
            }

            subnetField.field.withCaretListener {
                setOkActionEnabled(it.text.isNotEmpty())
            }

            addOkAction().setText("Add")
            addCancelAction()
            setOkActionEnabled(false)

            centerPanel(
                subnetField.withPreferredWidth(400)
            )

            setOkOperation {
                val subnetId = subnetField.value
                val subnet = when (val subnetOption = subnetField.getSelectedOption()) {
                    null -> SubnetData(subnetId)
                    else -> SubnetData(subnetId, subnetOption.name, subnetOption.zoneId)
                }

                if (replaceIndex == -1) {
                    listModel.add(subnet)
                } else {
                    listModel.setElementAt(subnet, replaceIndex)
                }
                availableSubnets = subnetField.getAvailableOptions()
                dialogWrapper.close(DialogWrapper.OK_EXIT_CODE)
            }

            setCancelOperation {
                availableSubnets = subnetField.getAvailableOptions()
                dialogWrapper.close(DialogWrapper.CANCEL_EXIT_CODE)
            }
        }.show()
    }

    private inner class ListRenderer : ColoredListCellRenderer<SubnetData>() {
        override fun customizeCellRenderer(
            list: JList<out SubnetData>,
            subnet: SubnetData,
            index: Int,
            selected: Boolean,
            hasFocus: Boolean
        ) {
            icon = CloudIcons.Resources.Subnet
            append(subnet.id, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            if (subnet.name != null) {
                val status = when {
                    subnet.name.contains(subnet.zoneId ?: "") -> " ${subnet.name}"
                    else -> " ${subnet.name} (${subnet.zoneId})"
                }
                append(status, SimpleTextAttributes.GRAY_SMALL_ATTRIBUTES)
            }
        }
    }

    private data class SubnetData(
        val id: String,
        val name: String? = null,
        val zoneId: String? = null,
    )
}