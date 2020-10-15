package yandex.cloud.toolkit.ui.component

import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.*
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import icons.CloudIcons
import yandex.cloud.toolkit.api.resource.impl.model.CloudFunctionVersion
import yandex.cloud.toolkit.api.resource.impl.model.findWithTag
import yandex.cloud.toolkit.ui.action.CopyToBufferAction
import yandex.cloud.toolkit.util.*
import java.awt.BorderLayout
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.SwingConstants

class FunctionVersionTagsList(
    val version: CloudFunctionVersion?,
    val allVersions: List<CloudFunctionVersion>
) : YCPanel(BorderLayout()) {

    private val listModel = CollectionListModel<String>()
    private val list = JBList(listModel)

    private val tagRegex = "[a-z][-_0-9a-z]*".toRegex()

    var tags: Set<String>
        get() = listModel.items.toSet()
        set(value) = listModel.setItems(value.toList())

    fun addOnlyLegalTags(tags: Set<String>) {
        this.tags = tags.filter(tagRegex::matches).toSet()
    }

    init {
        list.setEmptyText("No Tags")
        list.cellRenderer = TagListRenderer()

        YCUI.separator("Version Tags") addAs BorderLayout.NORTH

        ToolbarDecorator.createDecorator(list).apply {
            setToolbarPosition(ActionToolbarPosition.RIGHT)
            setMoveDownAction(null)
            setMoveUpAction(null)
            setEditAction(null)
            setAddAction {
                showAddDialog()
            }

            setRemoveActionUpdater {
                val selectedValues = list.selectedValuesList
                selectedValues.isNotEmpty() && selectedValues.all(tagRegex::matches)
            }

            val copyAction = AnActionButton.fromAction(CopyToBufferAction { list.singleSelectedValue ?: "" })
            copyAction.addCustomUpdater { list.isSingleValueSelected }
            addExtraAction(copyAction)
        }.createPanel() addAs BorderLayout.CENTER
    }

    private fun showAddDialog() {
        DialogBuilder(this).apply {
            title("Add Version Tag")

            val valueField = JBTextField(name)
            val statusLabel = SimpleColoredComponent()

            valueField.withCaretListener {
                val text = it.text
                val isDuplicated = listModel.contains(text)
                val isValid = tagRegex.matches(text)
                val versionWithTag = allVersions.findWithTag(text)

                setOkActionEnabled(!isDuplicated && isValid)

                statusLabel.clear()
                statusLabel.border = JBUI.Borders.emptyTop(3)
                statusLabel.setTextAlign(SwingConstants.RIGHT)

                when {
                    isDuplicated -> statusLabel.append("Duplicated tag", SimpleTextAttributes.ERROR_ATTRIBUTES)
                    !isValid -> statusLabel.append("Invalid format", SimpleTextAttributes.ERROR_ATTRIBUTES)
                    versionWithTag != null -> statusLabel.append(
                        "Currently on '${versionWithTag.id}'",
                        SimpleTextAttributes.GRAY_ATTRIBUTES
                    )
                }
            }

            addOkAction()
            addCancelAction()
            okAction.setText("Add")
            setOkActionEnabled(false)

            centerPanel(
                YCUI.gridPanel {
                    YCUI.gridBag(horizontal = true) {
                        JLabel("Tag") addAs nextln(0.2)
                        valueField addAs next(0.8)
                        statusLabel addAs fullLine()
                    }
                }.withPreferredWidth(350)
            )

            setOkOperation {
                listModel.add(valueField.text)
                dialogWrapper.close(DialogWrapper.OK_EXIT_CODE)
            }

            setCancelOperation {
                dialogWrapper.close(DialogWrapper.CANCEL_EXIT_CODE)
            }
        }.show()
    }

    private inner class TagListRenderer : ColoredListCellRenderer<String>() {
        override fun customizeCellRenderer(
            list: JList<out String>,
            value: String,
            index: Int,
            selected: Boolean,
            hasFocus: Boolean
        ) {
            icon = CloudIcons.Nodes.Label
            val color = when {
                tagRegex.matches(value) -> SimpleTextAttributes.REGULAR_ATTRIBUTES
                else -> SimpleTextAttributes.GRAY_ATTRIBUTES
            }
            append(value, color)

            val versionWithTag = allVersions.findWithTag(value)
            if (versionWithTag != null && versionWithTag != version) {
                append(" (Currently on '${versionWithTag.id}')", SimpleTextAttributes.GRAY_ATTRIBUTES)
            }
        }
    }
}