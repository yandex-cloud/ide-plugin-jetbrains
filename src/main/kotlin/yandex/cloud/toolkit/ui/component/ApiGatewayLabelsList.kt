package yandex.cloud.toolkit.ui.component

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import yandex.cloud.toolkit.util.*
import java.awt.BorderLayout
import javax.swing.JList

class ApiGatewayLabelsList : YCPanel(BorderLayout()) {

    companion object {
        private const val MAX_KEY_LENGTH = 63
        private const val MAX_VALUE_LENGTH = 63
    }

    private val keyRegex = "[-_0-9a-z]*".toRegex()
    private val valueRegex = "[a-z][-_0-9a-z]*".toRegex()

    private val listModel = CollectionListModel<Pair<String, String>>()
    private val list = JBList(listModel)

    val entries: Map<String, String> get() = listModel.items.toMap()
    val labelsCount: Int get() = listModel.size

    private fun showInputDialog(key: String, value: String) = object : DialogWrapper(true) {
        private val keyField = LimitedTextField("Key", MAX_KEY_LENGTH)
        private val valueField = LimitedTextField("Value", MAX_VALUE_LENGTH)

        private val keyRestrictions = restrictions<String>("Key") {
            textIsNotEmpty()
            textMaxLength(MAX_KEY_LENGTH)
            textPattern(keyRegex)
        }

        private val valueRestrictions = restrictions<String>("Value") {
            check(valueField.restrictions)
            textPattern(valueRegex)
        }

        init {
            keyField.text = key
            valueField.text = value
            title = "Add Label"
            init()
        }

        override fun createCenterPanel() = YCUI.borderPanel {
            keyField.withPreferredWidth(450) addAs BorderLayout.NORTH
            valueField addAs BorderLayout.CENTER
        }

        override fun doValidate(): ValidationInfo? {
            return keyRestrictions.validate(keyField.text, keyField.component)
                ?: valueRestrictions.validate(valueField.text, valueField.component)
        }

        override fun doOKAction() {
            addLabel(keyField.text to valueField.text)
            super.doOKAction()
        }

        override fun doCancelAction() {
            if (key.isNotEmpty()) addLabel(key to value)
            super.doCancelAction()
        }
    }.show()

    init {
        list.setEmptyText("No Labels")
        list.cellRenderer = EnvironmentVariableListRenderer()

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
        }.createPanel() addAs BorderLayout.CENTER
    }

    fun addLabel(entry: Pair<String, String>) {
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
            append("${value.first} : ${value.second}", SimpleTextAttributes.REGULAR_ATTRIBUTES)
        }
    }
}