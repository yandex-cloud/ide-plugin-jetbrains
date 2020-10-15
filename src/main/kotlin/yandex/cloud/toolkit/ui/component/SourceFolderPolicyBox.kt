package yandex.cloud.toolkit.ui.component

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import yandex.cloud.toolkit.util.SourceFolderPolicy
import javax.swing.DefaultComboBoxModel
import javax.swing.JList

class SourceFolderPolicyBox : ComboBox<SourceFolderPolicy>() {

    var value: SourceFolderPolicy
        get() = selectedItem as? SourceFolderPolicy ?: SourceFolderPolicy.KEEP
        set(policy) {
            selectedItem = policy
        }

    init {
        model = DefaultComboBoxModel(SourceFolderPolicy.values())
        renderer = ListRenderer()
    }

    private class ListRenderer : ColoredListCellRenderer<SourceFolderPolicy>() {
        override fun customizeCellRenderer(
            list: JList<out SourceFolderPolicy>,
            value: SourceFolderPolicy,
            index: Int,
            selected: Boolean,
            hasFocus: Boolean
        ) {
            append(value.displayName, SimpleTextAttributes.REGULAR_ATTRIBUTES)
        }
    }
}