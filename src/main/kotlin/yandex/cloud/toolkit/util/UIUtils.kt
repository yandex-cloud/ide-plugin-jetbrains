package yandex.cloud.toolkit.util

import com.intellij.ide.IdeBundle
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.Disposer
import com.intellij.ui.*
import com.intellij.ui.components.JBBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.fields.IntegerField
import com.intellij.util.ui.GridBag
import com.intellij.util.ui.UIUtil
import java.awt.*
import java.awt.event.ItemEvent
import javax.swing.*
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import javax.swing.text.JTextComponent
import javax.swing.tree.TreePath

val IntegerField.isValueValid: Boolean
    get() {
        val rawValue = valueEditor.valueText.toIntOrNull()
        return rawValue != null && valueEditor.isValid(rawValue)
    }

fun <C : JComponent> C.withPreferredSize(width: Int, height: Int) = apply {
    preferredSize = Dimension(width, height)
}

fun <C : JComponent> C.withPreferredWidth(width: Int) = withPreferredSize(width, preferredSize.height)
fun <C : JComponent> C.withPreferredHeight(height: Int) = withPreferredSize(preferredSize.width, height)

fun GridBag.nextln(): GridBag = nextLine().next()
fun GridBag.nextln(weight: Double): GridBag = nextLine().next(weight)
fun GridBag.fullLine(): GridBag = nextln().coverLine()

fun <C : JTextComponent> C.asEditable(editable: Boolean): C = apply {
    isEditable = editable
}

fun <C : JTextComponent> C.asFocusable(focusable: Boolean): C = apply {
    isFocusable = focusable
}

fun GridBag.next(weight: Double): GridBag = next().apply {
    when (defaultFill) {
        GridBag.HORIZONTAL -> weightx(weight)
        GridBag.VERTICAL -> weighty(weight)
    }
}

val <T> JComboBox<T>.item: T get() = selectedItem as T

fun <C : JTextComponent> C.withCaretListener(listener: (C) -> Unit): C {
    addCaretListener { listener(this) }
    return this
}

fun <E> CollectionListModel<E>.setItems(items: List<E>) {
    removeAll()
    addAll(0, items)
}

fun <C : JComponent> C.withNoBorder(): C {
    border = null
    return this
}

val <E> JList<E>.singleSelectedValue: E? get() = if (isSingleValueSelected) selectedValue else null
val JList<*>.isSingleValueSelected: Boolean get() = selectionModel.selectedItemsCount == 1

enum class ScrollBar(val horizontalConstraint: Int, val verticalConstraint: Int) {
    ALWAYS(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS),
    NEVER(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER),
    AS_NEEDED(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED),
}

operator fun Container.plus(view: Component): Container {
    add(view)
    return this
}

fun colorEditorCustomization(): EditorCustomization {
    return EditorCustomization { editor: EditorEx ->
        editor.setBackgroundColor(null) // to use background from set color scheme
        editor.colorsScheme = YCUI.editorColorScheme
    }
}

var Action.text: String?
    get() = getValue(Action.NAME) as? String
    set(value) = putValue(Action.NAME, value)

var Action.icon: Icon?
    get() = getValue(Action.SMALL_ICON) as? Icon
    set(value) = putValue(Action.SMALL_ICON, value)

fun ToolbarDecorator.clearActions() {
    setAddAction(null)
    setRemoveAction(null)
    setMoveUpAction(null)
    setMoveDownAction(null)
    setEditAction(null)
}

fun <C : JComponent> C.labeled(label: String, location: String = BorderLayout.WEST): LabeledComponent<C> =
    LabeledComponent.create(this, label, location)

fun JTree.getTreePath(x: Int, y: Int): TreePath? {
    val path: TreePath = getClosestPathForLocation(x, y) ?: return null
    val bounds: Rectangle? = getPathBounds(path)

    return if (bounds == null || bounds.y > y || y >= bounds.y + bounds.height) null else path
}

fun ComboBox<*>.reopenPopup() {
    val popup = this.popup
    if (popup != null) {
        if (popup.isVisible) popup.hide()
        popup.show()
    }
}

fun <E> ComboBox<E>.doOnFirstExpansion(action: () -> Unit) {
    this.addPopupMenuListener(object : PopupMenuListener {
        override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
            action()
            this@doOnFirstExpansion.removePopupMenuListener(this)
        }

        override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {}
        override fun popupMenuCanceled(e: PopupMenuEvent) {}
    })
}

fun <E> ComboBox<E>.doOnItemChange(action: (ItemEvent) -> Unit) {
    this.addItemListener {
        if (it.stateChange == ItemEvent.SELECTED) action(it)
    }
}

object YCUI {
    val editorColorScheme: EditorColorsScheme
        get() = when (ColorUtil.isDark(UIUtil.getPanelBackground())) {
            EditorColorsManager.getInstance().isDarkEditor -> EditorColorsManager.getInstance().globalScheme
            else -> EditorColorsManager.getInstance().schemeForCurrentUITheme
        }

    fun separator(text: String, labelFor: JBLabel? = null): TitledSeparator =
        SeparatorFactory.createSeparator(text, labelFor)

    inline fun hbox(block: JBBox.() -> Unit = {}) = JBBox(BoxLayout.X_AXIS).apply(block)
    inline fun vbox(block: JBBox.() -> Unit = {}) = JBBox(BoxLayout.Y_AXIS).apply(block)

    inline fun gridPanel(block: YCPanel.() -> Unit = {}): YCPanel = YCPanel(GridBagLayout()).apply(block)

    inline fun borderPanel(block: YCPanel.() -> Unit = {}): YCPanel = YCPanel(BorderLayout()).apply(block)
    inline fun borderPanel(hgap: Int, vgap: Int, block: YCPanel.() -> Unit = {}): YCPanel =
        YCPanel(BorderLayout(hgap, vgap)).apply(block)

    inline fun gridBag(horizontal: Boolean, defaultWeight: Double = 1.0, block: GridBag.() -> Unit = {}): GridBag =
        GridBag().apply {
            defaultFill = if (horizontal) GridBag.HORIZONTAL else GridBag.VERTICAL
            if (horizontal) defaultWeightX = defaultWeight else defaultWeightY = defaultWeight
            block()
        }

    fun scrollPane(
        view: Component,
        vertical: ScrollBar = ScrollBar.AS_NEEDED,
        horizontal: ScrollBar = ScrollBar.AS_NEEDED
    ): JScrollPane = ScrollPaneFactory.createScrollPane(
        view,
        vertical.verticalConstraint, horizontal.horizontalConstraint,
    )

    inline fun panel(layout: LayoutManager? = null, block: YCPanel.() -> Unit = {}): YCPanel =
        YCPanel(layout).apply(block)

    object Messages {
        val Delete: String get() = IdeBundle.message("title.delete")
    }
}

fun Disposable.disposeWith(parent: Disposable) {
    Disposer.register(parent, this)
}

open class YCPanel(layout: LayoutManager? = null) : JPanel(layout) {

    infix fun Component.addAs(constraints: Any) {
        add(this@addAs, constraints)
    }
}