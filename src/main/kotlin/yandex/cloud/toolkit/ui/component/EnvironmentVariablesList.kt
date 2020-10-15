package yandex.cloud.toolkit.ui.component

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import yandex.cloud.toolkit.util.*
import java.awt.GridBagLayout
import javax.swing.JLabel
import javax.swing.JList

class EnvironmentVariablesList : YCPanel(GridBagLayout()) {

    private val listModel = CollectionListModel<Pair<String, String>>()
    private val list = JBList(listModel)

    var entries: Map<String, String>
        get() = listModel.items.toMap()
        set(value) {
            listModel.setItems(value.entries.map { it.toPair() })
        }

    private fun showInputDialog(name: String, value: String) = DialogBuilder(this).apply {
        title("Add Environment Variable")

        val nameField = JBTextField(name).withCaretListener {
            setOkActionEnabled(it.text.isNotEmpty())
        }
        val valueArea = JBTextArea(value)

        setOkActionEnabled(name.isNotEmpty())

        centerPanel(
            YCUI.gridPanel {
                YCUI.gridBag(horizontal = true) {
                    JLabel("Name") addAs nextln(0.2)
                    nameField addAs next(0.8)
                    YCUI.separator("Value") addAs fullLine()
                    YCUI.scrollPane(valueArea).withPreferredHeight(80) addAs fullLine().coverColumn()
                }
            }.withPreferredWidth(400)
        )

        setOkOperation {
            addEnvVariable(nameField.text to valueArea.text)
            dialogWrapper.close(DialogWrapper.OK_EXIT_CODE)
        }

        setCancelOperation {
            if (name.isNotEmpty()) addEnvVariable(name to value)
            dialogWrapper.close(DialogWrapper.CANCEL_EXIT_CODE)
        }
    }.show()

    init {
        list.setEmptyText("No Environment Variables")
        list.cellRenderer = EnvironmentVariableListRenderer()

        YCUI.gridBag(horizontal = true) {
            YCUI.separator("Environment variables") addAs fullLine()

            ToolbarDecorator.createDecorator(list).apply {
                setToolbarPosition(ActionToolbarPosition.RIGHT)
                setMoveDownAction(null)
                setMoveUpAction(null)
                setAddAction {
                    showInputDialog("", "")
                }
                setEditAction {
                    val selectedValue = list.singleSelectedValue ?: return@setEditAction
                    listModel.remove(selectedValue.first to selectedValue.second)
                    showInputDialog(selectedValue.first, selectedValue.second)
                }
                setEditActionUpdater { list.isSingleValueSelected }
            }.createPanel().withPreferredHeight(100) addAs fullLine()
        }
    }

    fun clear() {
        listModel.removeAll()
    }

    fun addEnvVariable(entry: Pair<String, String>) {
        listModel.replaceAll(listModel.items.filter { it.first != entry.first })
        listModel.add(entry)
    }

    private class EnvironmentVariableListRenderer : ColoredListCellRenderer<Pair<String, String>>() {
        override fun customizeCellRenderer(
            list: JList<out Pair<String, String>>,
            value: Pair<String, String>,
            index: Int,
            selected: Boolean,
            hasFocus: Boolean
        ) {
            icon = AllIcons.Nodes.Variable
            append("'${value.first}' = '${value.second}'", SimpleTextAttributes.REGULAR_ATTRIBUTES)
        }
    }
}