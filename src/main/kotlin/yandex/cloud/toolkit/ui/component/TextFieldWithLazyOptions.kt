package yandex.cloud.toolkit.ui.component

import com.intellij.ide.ui.laf.darcula.ui.DarculaTextBorder
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBTextField
import yandex.cloud.toolkit.api.profile.impl.profileStorage
import yandex.cloud.toolkit.api.resource.impl.model.CloudUser
import yandex.cloud.toolkit.util.*
import yandex.cloud.toolkit.util.task.LazyTask
import yandex.cloud.toolkit.util.task.backgroundTask
import java.awt.BorderLayout
import java.awt.event.ItemEvent
import javax.swing.JList

abstract class TextFieldWithLazyOptions<R>(
    val project: Project,
    options: List<R>?,
    boxPosition: String = BorderLayout.EAST,
    label: String? = null
) : YCPanel(BorderLayout()), Disposable {

    @Volatile
    private var disposed = false

    @Volatile
    private var options: Maybe<Map<String, R>>? = options?.associateBy(::getOptionValue)?.asJust()

    val field = JBTextField()
    private val box = ComboBox<R?>()

    var value: String
        get() = this.field.text
        set(value) {
            this.field.text = value
            updateBox()
        }

    init {
        box.border = DarculaTextBorder()
        box.background = null
        box.isOpaque = false

        setBoxElements(options)
        box.renderer = ListRenderer()

        box.addItemListener {
            if (it.stateChange != ItemEvent.SELECTED) return@addItemListener
            val option = box.selectedItem ?: return@addItemListener

            @Suppress("UNCHECKED_CAST")
            val optionValue = getOptionValue(option as R)
            invokeLaterAt(this@TextFieldWithLazyOptions) {
                if (field.text != optionValue) field.text = optionValue
            }
        }

        field.addCaretListener {
            updateBox()
        }

        if (options == null) {
            box.doOnFirstExpansion(this::startLoadTask)
        }

        (if (label != null) field.labeled(label) else field) addAs BorderLayout.CENTER
        box addAs boxPosition

        this.addPropertyChangeListener {
            if (it.propertyName == "enabled") {
                box.isEnabled = it.newValue as Boolean
                field.isEditable = box.isEnabled
            }
            if (it.propertyName == "focusable") {
                box.isFocusable = it.newValue as Boolean
                field.isFocusable = box.isFocusable
            }
        }
    }

    abstract fun getOptionValue(option: R): String
    abstract fun decorateOption(option: R, cell: SimpleColoredComponent)
    abstract fun canLoadOptions(): Boolean
    abstract fun loadOptions(user: CloudUser): LazyTask<List<R>>

    fun getAvailableOptions(): List<R>? = options?.value?.values?.toList()

    private fun updateBox() {
        if (options == null) return
        val value = field.text
        val option = box.selectedItem
        @Suppress("UNCHECKED_CAST")
        if (option != null && getOptionValue(option as R) == value) return
        box.selectedItem = options?.value?.get(value)
    }

    fun getSelectedOption(): R? {
        @Suppress("UNCHECKED_CAST")
        return box.selectedItem as R?
    }

    private fun startLoadTask() {
        if (!canLoadOptions()) return
        val user = project.profileStorage.profile?.resourceUser ?: return
        setBoxElements(listOf(null))

        backgroundTask(project, "Yandex.Cloud") {
            val options = loadOptions(user).handle()

            invokeLaterAt(this@TextFieldWithLazyOptions) {
                if (disposed) return@invokeLaterAt
                this@TextFieldWithLazyOptions.options = options.mapValue { it.associateBy(::getOptionValue) }
                if (options is JustValue) setBoxElements(options.value)
                updateBox()
                box.reopenPopup()
            }
        }
    }

    private fun setBoxElements(options: List<R?>?) {
        box.model = CollectionComboBoxModel<R?>(
            mutableListOf<R?>().apply {
                add(null)
                options?.forEach { add(it) }
            }
        )
        box.selectedIndex = 0
    }

    override fun dispose() {
        disposed = true
    }

    private inner class ListRenderer : ColoredListCellRenderer<R?>() {
        override fun customizeCellRenderer(
            list: JList<out R?>,
            value: R?,
            index: Int,
            selected: Boolean,
            hasFocus: Boolean
        ) {
            when {
                value != null -> {
                    decorateOption(value, this)
                }
                index < 1 -> append("Custom", SimpleTextAttributes.GRAY_ATTRIBUTES)

                this@TextFieldWithLazyOptions.options == null -> append(
                    "Loading...",
                    SimpleTextAttributes.GRAY_ATTRIBUTES
                )
                this@TextFieldWithLazyOptions.options is NoValue -> append(
                    "Failed to load",
                    SimpleTextAttributes.ERROR_ATTRIBUTES
                )
            }
        }
    }
}