package yandex.cloud.toolkit.ui.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import icons.CloudIcons
import yandex.cloud.toolkit.api.auth.CloudAuthData
import yandex.cloud.toolkit.api.resource.impl.model.CloudFolder
import yandex.cloud.toolkit.api.resource.impl.model.CloudFunction
import yandex.cloud.toolkit.api.resource.impl.model.CloudFunctionSpec
import yandex.cloud.toolkit.api.service.CloudOperationService
import yandex.cloud.toolkit.ui.component.LimitedTextArea
import yandex.cloud.toolkit.util.*
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.SwingConstants

class CreateOrEditFunctionDialog(
    val project: Project,
    val authData: CloudAuthData,
    val folder: CloudFolder,
    val function: CloudFunction?,
    val folderFunctions: List<CloudFunction>
) : DialogWrapper(true) {

    companion object {
        private const val MAX_DESCRIPTION_LENGTH = 256
    }

    private val nameRestrictions = restrictions<String>("Name") {
        textLength(3..63)
        textPattern("[a-z][-a-z0-9]{1,61}[a-z0-9]")
        requireNot({ hasFunctionWithName(it) }) { "must be unique in folder" }
    }

    private val nameField = JBTextField()
    private val descriptionArea = LimitedTextArea("Description", MAX_DESCRIPTION_LENGTH)

    private val functionNames = folderFunctions.mapTo(mutableSetOf()) { it.data.name }

    init {
        if (function != null) {
            nameField.text = function.name
            descriptionArea.text = function.data.description
        }

        title = when (function) {
            null -> "Create Yandex.Cloud Function"
            else -> "Edit Yandex.Cloud Function"
        }

        init()
    }

    override fun doValidate(): ValidationInfo? {
        return nameRestrictions.validate(nameField.text, nameField)
            ?: descriptionArea.checkRestrictions()
    }

    private fun hasFunctionWithName(name: String): Boolean =
        name != function?.name && name in functionNames

    override fun createCenterPanel(): JComponent = YCUI.borderPanel {
        withPreferredSize(450, 150)

        YCUI.gridPanel {
            YCUI.gridBag(horizontal = true) {
                JBLabel(
                    "Name: ",
                    CloudIcons.Resources.Function,
                    SwingConstants.LEFT
                ) addAs nextln(0.0)

                nameField addAs next(1.0)
            }
        } addAs BorderLayout.NORTH

        descriptionArea addAs BorderLayout.CENTER
    }

    override fun doOKAction() {
        if (!okAction.isEnabled) return

        val spec = CloudFunctionSpec(
            name = nameField.text,
            description = descriptionArea.text
        )

        if (function != null) {
            CloudOperationService.instance.updateFunction(
                project,
                authData,
                function,
                spec
            )
        } else {
            CloudOperationService.instance.createFunction(
                project,
                authData,
                folder,
                spec
            )
        }

        super.doOKAction()
    }
}