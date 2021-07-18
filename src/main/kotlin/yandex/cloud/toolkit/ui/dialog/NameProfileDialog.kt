package yandex.cloud.toolkit.ui.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBTextField
import yandex.cloud.toolkit.api.profile.CloudProfile
import yandex.cloud.toolkit.util.*
import javax.swing.JComponent

class NameProfileDialog(
    val project: Project,
    val profile: CloudProfile,
    val isRename: Boolean,
    val nameCallback: (String) -> Unit
) : DialogWrapper(project, true) {

    private val nameField = JBTextField()

    private val nameRestrictions = restrictions<String>("Name") {
        textIsNotEmpty()
    }

    init {
        nameField.text = profile.displayName
        title = when (isRename) {
            true -> "Rename Yandex.Cloud Profile"
            false -> "Name Yandex.Cloud Profile"
        }
        init()
    }

    override fun doValidate(): ValidationInfo? = nameRestrictions.validate(nameField.text, nameField)

    override fun createCenterPanel(): JComponent = YCUI.gridPanel {
        YCUI.gridBag(horizontal = true) {
            if (!isRename) {
                JBTextField("Authenticated by " + profile.authMethod.descriptor.name).apply {
                    asEditable(false)
                    asFocusable(false)
                } addAs fullLine()
            }
            nameField.withPreferredWidth(400) addAs fullLine()
        }
    }

    override fun doOKAction() {
        if (okAction.isEnabled) {
            nameCallback(nameField.text)
        }
        super.doOKAction()
    }
}