package yandex.cloud.toolkit.ui.component

import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.*
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import icons.CloudIcons
import yandex.cloud.toolkit.util.*
import java.awt.BorderLayout
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JList

class EnvironmentVariablesList(val project: Project) : YCPanel(BorderLayout()) {

    private val listModel = CollectionListModel<Pair<String, String>>()
    private val list = JBList(listModel)

    var entries: Map<String, String>
        get() = listModel.items.toMap()
        set(value) {
            listModel.setItems(value.entries.map { it.toPair() })
        }

    private inner class VariableEditDialog(val name: String, val value: String) : DialogWrapper(project, true) {

        val nameField = JBTextField(name)
        val valueArea = JBTextArea(value)

        private val keyRestrictions = restrictions<String>("Name") {
            textIsNotEmpty()
            textHasNoSpaces()
            textPattern("[a-zA-Z][a-zA-Z0-9_]*")
        }

        init {
            title = "Add Environment Variable"
            init()
        }

        override fun doValidate(): ValidationInfo? = keyRestrictions.validate(nameField.text, nameField)

        override fun createCenterPanel(): JComponent = YCUI.borderPanel {
            YCUI.gridPanel {
                YCUI.gridBag(horizontal = true) {
                    JLabel("Name ") addAs nextln(0.0)
                    nameField addAs next(1.0)
                    YCUI.separator("Value") addAs fullLine()
                }
            } addAs BorderLayout.NORTH
            YCUI.scrollPane(valueArea).withPreferredHeight(80) addAs BorderLayout.CENTER
        }.withPreferredWidth(400)

        override fun doOKAction() {
            if (!okAction.isEnabled) return
            addEnvVariable(nameField.text to valueArea.text)
            super.doOKAction()
        }

        override fun doCancelAction() {
            if (!cancelAction.isEnabled) return
            if (name.isNotEmpty()) addEnvVariable(name to value)
            super.doCancelAction()
        }
    }

    init {
        list.setEmptyText("No Environment Variables")
        list.cellRenderer = EnvironmentVariableListRenderer()
        withPreferredHeight(110)

        YCUI.separator("Environment Variables") addAs BorderLayout.NORTH

        object : DoubleClickListener() {
            override fun onDoubleClick(event: MouseEvent?): Boolean {
                val selectedValue = list.singleSelectedValue ?: return true
                listModel.remove(selectedValue)
                VariableEditDialog(selectedValue.first, selectedValue.second).show()
                return true
            }
        }.installOn(list)

        ToolbarDecorator.createDecorator(list).apply {
            setToolbarPosition(ActionToolbarPosition.RIGHT)
            setMoveDownAction(null)
            setMoveUpAction(null)
            setAddAction {
                VariableEditDialog("", "").show()
            }
            setEditAction {
                val selectedValue = list.singleSelectedValue ?: return@setEditAction
                listModel.remove(selectedValue)
                VariableEditDialog(selectedValue.first, selectedValue.second).show()
            }
            setEditActionUpdater { list.isSingleValueSelected }
        }.createPanel() addAs BorderLayout.CENTER
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
            icon = CloudIcons.Nodes.Variable
            append("${value.first} = '${value.second}'", SimpleTextAttributes.REGULAR_ATTRIBUTES)
        }
    }
}