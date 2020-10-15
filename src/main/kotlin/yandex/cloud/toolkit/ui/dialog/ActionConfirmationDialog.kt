package yandex.cloud.toolkit.ui.dialog

import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBTextField
import yandex.cloud.toolkit.util.YCUI
import yandex.cloud.toolkit.util.text
import yandex.cloud.toolkit.util.withPreferredWidth
import java.awt.BorderLayout
import java.awt.Container
import javax.swing.*

class ActionConfirmationDialog(title: String, val message: String, val confirmationKey: String) : DialogWrapper(true) {

    private val confirmationField = JBTextField()

    init {
        this.title = title
        this.okAction.text = "Confirm"
        init()
    }

    override fun createCenterPanel(): JComponent = YCUI.borderPanel(8, 12) {
        withPreferredWidth(400)
        createIconPanel() addAs BorderLayout.WEST

        Messages.wrapToScrollPaneIfNeeded(
            createMessageComponent(message),
            100, 10
        ) addAs BorderLayout.CENTER

        YCUI.borderPanel(0, 4) {
            JSeparator() addAs BorderLayout.NORTH
            createMessageComponent("Please type '$confirmationKey' to confirm action.") addAs BorderLayout.CENTER
            confirmationField addAs BorderLayout.SOUTH
        } addAs BorderLayout.SOUTH
    }

    override fun doValidate(): ValidationInfo? = when {
        confirmationField.text.trim() != confirmationKey -> ValidationInfo(
            "Invalid confirmation key",
            confirmationField
        )
        else -> null
    }

    private fun createIconPanel(): Container {
        val iconLabel = JLabel(AllIcons.General.Warning)
        val container = Container()
        container.layout = BorderLayout()
        container.add(iconLabel, BorderLayout.NORTH)
        return container
    }

    private fun createMessageComponent(message: String): JTextPane {
        val messageComponent = JTextPane()
        return Messages.configureMessagePaneUi(messageComponent, message)
    }
}