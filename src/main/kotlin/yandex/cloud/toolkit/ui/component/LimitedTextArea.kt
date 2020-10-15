package yandex.cloud.toolkit.ui.component

import com.intellij.ui.components.JBTextArea
import javax.swing.JTextArea

class LimitedTextArea(
    labelText: String,
    maxLength: Int,
    area: JTextArea = JBTextArea()
) : LimitedTextComponent<JTextArea>(labelText, maxLength, area, true)