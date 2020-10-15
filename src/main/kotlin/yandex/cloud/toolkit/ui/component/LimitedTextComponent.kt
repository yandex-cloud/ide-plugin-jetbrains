package yandex.cloud.toolkit.ui.component

import com.intellij.ui.JBColor
import com.intellij.ui.TitledSeparator
import com.intellij.util.ui.JBUI
import yandex.cloud.toolkit.util.YCPanel
import yandex.cloud.toolkit.util.YCUI
import yandex.cloud.toolkit.util.restrictions
import yandex.cloud.toolkit.util.textMaxLength
import java.awt.BorderLayout
import javax.swing.text.JTextComponent

open class LimitedTextComponent<C : JTextComponent>(
    val labelText: String,
    val maxLength: Int,
    val component: C,
    wrapToScroll: Boolean
) : YCPanel(BorderLayout()) {

    private val label = TitledSeparator("")

    var text: String
        get() = component.text
        set(value) {
            component.text = value
        }

    val restrictions = restrictions<String>(labelText) {
        textMaxLength(maxLength)
    }

    val isLimitExceeded: Boolean get() = text.length > maxLength

    init {
        updateLimit()
        component.addCaretListener { updateLimit() }

        label addAs BorderLayout.NORTH
        (if (wrapToScroll) YCUI.scrollPane(component) else component) addAs BorderLayout.CENTER
    }

    private fun updateLimit() {
        val textLength = text.length
        label.text = "$labelText ($textLength/$maxLength)"

        component.border = when (textLength > maxLength) {
            true -> JBUI.Borders.customLine(JBColor.red)
            false -> null
        }
    }

    fun checkRestrictions() = restrictions.validate(text, this)
}