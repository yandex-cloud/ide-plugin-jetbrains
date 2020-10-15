package yandex.cloud.toolkit.ui.dialog

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ex.ActionManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.ColoredTableCellRenderer
import com.intellij.ui.PopupHandler
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBLabel
import com.intellij.ui.table.TableView
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.ListTableModel
import icons.CloudIcons
import yandex.cloud.toolkit.api.resource.impl.model.CloudOperationResult
import yandex.cloud.toolkit.ui.action.StubAction
import yandex.cloud.toolkit.util.*
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JTable
import javax.swing.SwingConstants
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableCellRenderer

class OperationsStatusDialog(
    val project: Project,
    val operationResults: List<CloudOperationResult>
) : DialogWrapper(true) {

    init {
        title = "Status of Yandex.Cloud Operations"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val tableModel = ListTableModel<CloudOperationResult>(
            OperationIdColumn(),
            OperationNameColumn(),
            OperationStatusColumn(),
            OperationDescriptionColumn(),
            OperationResultColumn()
        )
        tableModel.addRows(operationResults)

        val table = TableView(tableModel)

        table.addMouseListener(
            object : PopupHandler() {
                override fun invokePopup(comp: Component?, x: Int, y: Int) {
                    val operationResult = table.selectedObject ?: return
                    val actions = operationResult.getActions()
                    if (actions.isEmpty()) actions += StubAction("No actions with this operation")

                    val actionManager = ActionManagerEx.getInstanceEx()
                    val actionGroup = DefaultActionGroup(actions)
                    val popupMenu = actionManager.createActionPopupMenu("CloudOperationStatusDialog", actionGroup)
                    popupMenu.component.show(comp, x, y)
                }
            }
        )

        return YCUI.panel(BorderLayout()) {
            ToolbarDecorator.createDecorator(table).apply {
                clearActions()
            }.createPanel().withPreferredSize(800, 300) addAs BorderLayout.CENTER

            JBLabel(
                "Use popup menu to do some actions",
                AllIcons.General.Information,
                SwingConstants.LEFT
            ).apply {
                border = JBUI.Borders.emptyTop(4)
            } addAs BorderLayout.SOUTH
        }
    }

    private class OperationIdColumn : ColumnInfo<CloudOperationResult, String>("Id") {
        override fun getRenderer(result: CloudOperationResult): TableCellRenderer = DefaultTableCellRenderer()
        override fun valueOf(result: CloudOperationResult): String = result.operation.data.getOrNull()?.id ?: "-"
    }

    private class OperationNameColumn : ColumnInfo<CloudOperationResult, String>("Operation") {
        override fun getRenderer(result: CloudOperationResult): TableCellRenderer = DefaultTableCellRenderer()
        override fun valueOf(result: CloudOperationResult): String = result.operation.name
    }

    private inner class OperationStatusColumn : ColumnInfo<CloudOperationResult, String>("Status") {
        override fun getRenderer(result: CloudOperationResult): TableCellRenderer =
            object : ColoredTableCellRenderer() {
                override fun customizeCellRenderer(
                    table: JTable?,
                    value: Any?,
                    selected: Boolean,
                    hasFocus: Boolean,
                    row: Int,
                    column: Int
                ) {
                    val (text, icon) = result.getStatus()
                    this.icon = icon
                    append(text, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                }
            }

        override fun valueOf(result: CloudOperationResult): String = ""
    }

    private class OperationDescriptionColumn : ColumnInfo<CloudOperationResult, String>("Description") {
        override fun getRenderer(result: CloudOperationResult): TableCellRenderer = DefaultTableCellRenderer()
        override fun valueOf(result: CloudOperationResult): String =
            result.operation.data.getOrNull()?.description ?: ""
    }

    private class OperationResultColumn : ColumnInfo<CloudOperationResult, String>("Result") {
        override fun getRenderer(result: CloudOperationResult): TableCellRenderer = DefaultTableCellRenderer()
        override fun valueOf(result: CloudOperationResult): String = result.result.error?.message ?: ""
    }

    private fun CloudOperationResult.getStatus(): Pair<String, Icon> {
        return when (val operation = operation.data) {
            is JustValue -> when {
                operation.value.done -> when (operation.value.hasError()) {
                    true -> "Failed" to AllIcons.General.Error
                    else -> "Completed" to CloudIcons.Status.Success
                }
                else -> "In process" to AllIcons.Vcs.History
            }
            is NoValue -> "Not started" to AllIcons.General.Error
        }
    }

    private fun CloudOperationResult.getActions(): MutableList<AnAction> = when (val result = result) {
        is JustValue -> result.value.getActions()
        is NoValue -> (result.error as? ActionsBundle)?.getActions() ?: mutableListOf()
    }
}