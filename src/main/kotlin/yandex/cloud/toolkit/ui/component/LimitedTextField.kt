package yandex.cloud.toolkit.ui.component

import com.intellij.ui.components.JBTextField
import javax.swing.JTextField

class LimitedTextField(
    labelText: String,
    maxLength: Int,
    field: JTextField = JBTextField(),
) : LimitedTextComponent<JTextField>(labelText, maxLength, field, false)