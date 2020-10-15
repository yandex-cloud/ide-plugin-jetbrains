package yandex.cloud.toolkit.ui.component

import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.AnActionButton
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import yandex.cloud.api.iam.v1.RoleOuterClass
import yandex.cloud.toolkit.ui.action.CopyToBufferAction
import yandex.cloud.toolkit.util.*
import java.awt.BorderLayout
import javax.swing.DefaultComboBoxModel
import javax.swing.JLabel

class ServiceAccountRolesList(
    val availableRoles: List<RoleOuterClass.Role>
) : YCPanel(BorderLayout()) {

    private val listModel = CollectionListModel<String>()
    private val list = JBList(listModel)

    var roles: Set<String>
        get() = listModel.items.toSet()
        set(value) = listModel.setItems(value.toList())

    init {
        list.setEmptyText("No Roles")

        YCUI.separator("Roles") addAs BorderLayout.NORTH

        ToolbarDecorator.createDecorator(list).apply {
            setToolbarPosition(ActionToolbarPosition.RIGHT)
            setMoveDownAction(null)
            setMoveUpAction(null)
            setEditAction(null)

            setAddAction {
                showAddDialog()
            }

            val copyAction = AnActionButton.fromAction(CopyToBufferAction { list.singleSelectedValue ?: "" })
            copyAction.addCustomUpdater { list.isSingleValueSelected }
            addExtraAction(copyAction)
        }.createPanel() addAs BorderLayout.CENTER
    }

    private fun showAddDialog() {
        DialogBuilder(this).apply {
            title("Add Role")

            val currentRoles = roles
            val availableRoles = availableRoles.mapNotNull {
                if (it.id in currentRoles) null else it.id
            }.toTypedArray()

            val rolesBox = ComboBox<String>()
            rolesBox.model = DefaultComboBoxModel(availableRoles)

            addOkAction()
            addCancelAction()
            okAction.setText("Add")
            setOkActionEnabled(availableRoles.isNotEmpty())

            centerPanel(
                YCUI.gridPanel {
                    YCUI.gridBag(horizontal = true) {
                        JLabel("Role") addAs nextln(0.2)
                        rolesBox addAs next(0.8)
                    }
                }.withPreferredWidth(350)
            )

            setOkOperation {
                listModel.add(rolesBox.item!!)
                dialogWrapper.close(DialogWrapper.OK_EXIT_CODE)
            }

            setCancelOperation {
                dialogWrapper.close(DialogWrapper.CANCEL_EXIT_CODE)
            }
        }.show()
    }
}