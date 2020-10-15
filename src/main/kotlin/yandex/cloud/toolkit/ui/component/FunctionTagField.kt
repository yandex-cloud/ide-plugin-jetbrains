package yandex.cloud.toolkit.ui.component

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import yandex.cloud.toolkit.api.resource.impl.model.CloudFunctionVersion
import yandex.cloud.toolkit.util.item
import javax.swing.DefaultComboBoxModel
import javax.swing.JList

class FunctionTagField(
    val versions: List<CloudFunctionVersion>,
    canBeEmpty: Boolean,
    filter: (String) -> Boolean = { true }
) : ComboBox<Pair<String, String>>() {

    val tags: List<Pair<String, String>>

    val selectedTag: String?
        get() = this.item?.first

    var noTagsMessage = "No Tags"

    init {
        val tags = mutableListOf<Pair<String, String>>()
        this.tags = tags

        for (version in versions) {
            version.data.tagsList.forEach {
                if (filter(it)) tags += it to version.id
            }
        }

        if (tags.isEmpty() && !canBeEmpty) {
            tags += CloudFunctionVersion.LATEST_TAG to ""
        }

        model = when (tags.isNotEmpty()) {
            true -> DefaultComboBoxModel(tags.toTypedArray())
            false -> DefaultComboBoxModel(arrayOf("" to ""))
        }

        renderer = ListRenderer()
    }

    fun selectTag(selectedTag: String) {
        val selectedTagIndex = tags.indexOfFirst { it.first == selectedTag }
        if (selectedTagIndex != -1) selectedIndex = selectedTagIndex
    }

    private inner class ListRenderer : ColoredListCellRenderer<Pair<String, String>>() {
        override fun customizeCellRenderer(
            list: JList<out Pair<String, String>>,
            value: Pair<String, String>,
            index: Int,
            selected: Boolean,
            hasFocus: Boolean
        ) {
            if (tags.isEmpty()) {
                append(noTagsMessage, SimpleTextAttributes.ERROR_ATTRIBUTES)
                return
            }

            append(value.first, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            if (value.second.isNotEmpty()) {
                append(" (${value.second})", SimpleTextAttributes.GRAY_ATTRIBUTES)
            }
        }
    }
}