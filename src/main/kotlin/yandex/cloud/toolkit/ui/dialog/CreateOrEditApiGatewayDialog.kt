package yandex.cloud.toolkit.ui.dialog

import com.intellij.json.JsonLanguage
import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.*
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import icons.CloudIcons
import yandex.cloud.toolkit.api.auth.CloudAuthData
import yandex.cloud.toolkit.api.resource.impl.model.CloudApiGateway
import yandex.cloud.toolkit.api.resource.impl.model.CloudApiGatewaySpec
import yandex.cloud.toolkit.api.resource.impl.model.CloudFolder
import yandex.cloud.toolkit.api.service.CloudOperationService
import yandex.cloud.toolkit.ui.component.ApiGatewayLabelsList
import yandex.cloud.toolkit.ui.component.LimitedTextArea
import yandex.cloud.toolkit.ui.component.SpoilerPanel
import yandex.cloud.toolkit.ui.component.TemplateComboBox
import yandex.cloud.toolkit.util.*
import java.awt.BorderLayout
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.swing.JComponent
import javax.swing.SwingConstants

class CreateOrEditApiGatewayDialog(
    val project: Project,
    val authData: CloudAuthData,
    val folder: CloudFolder,
    val apiGateway: CloudApiGateway?,
    val apiGatewaySpec: String,
    val apiGateways: List<CloudApiGateway>
) : DialogWrapper(true) {

    companion object {
        private const val MAX_DESCRIPTION_LENGTH = 256
        private const val MAX_LABEL_COUNT = 64

        private val TEMPLATES = arrayOf(
            "No Template" to null,
            "Hello, World!" to "hello-world.yaml"
        )
    }

    private val nameField = JBTextField()
    private val descriptionArea = LimitedTextArea("Description", MAX_DESCRIPTION_LENGTH)
    private val labelList = SpoilerPanel("Labels", ApiGatewayLabelsList())

    private val specEditor: EditorTextField
    private val templateBox: TemplateComboBox

    private val yamlLanguageFound: Boolean
    private var languageWarningShown: Boolean = false

    private val apiGatewayNames = apiGateways.mapTo(mutableSetOf()) { it.data.name }

    private val nameRestrictions = restrictions<String>("Name") {
        textLength(3, 63)
        textPattern("[a-z][-a-z0-9]{1,61}[a-z0-9]")
        requireNot({ hasApiGatewayWithName(it) }) { "must be unique in folder" }
    }

    init {
        val yamlLanguage = Language.findLanguageByID("yaml")
        yamlLanguageFound = yamlLanguage != null
        specEditor = EditorTextFieldProvider.getInstance().getEditorField(
            yamlLanguage ?: JsonLanguage.INSTANCE,
            project,
            listOf(
                colorEditorCustomization(),
                ErrorStripeEditorCustomization.ENABLED,
                EditorCustomization {
                    it.settings.apply {
                        isLineNumbersShown = true
                        isLineMarkerAreaShown = true
                        additionalLinesCount = 0
                        additionalColumnsCount = 0
                        isAdditionalPageAtBottom = false
                        isShowIntentionBulb = false
                    }
                }
            )
        )

        specEditor.addFocusListener(object : FocusAdapter() {
            override fun focusGained(e: FocusEvent?) {
                onEditorFocus()
            }
        })

        templateBox = TemplateComboBox(project, specEditor, "api-gateways", TEMPLATES)

        nameField.text = apiGateway?.data?.name ?: ""
        specEditor.text = apiGatewaySpec
        descriptionArea.text = apiGateway?.data?.description ?: ""

        val labels = apiGateway?.data?.labels
        if (!labels.isNullOrEmpty()) {
            labels.forEach { labelList.content.addLabel(it.toPair()) }
            labelList.open()
        }

        title = when (apiGateway) {
            null -> "Create Yandex.Cloud API Gateway"
            else -> "Edit Yandex.Cloud API Gateway"
        }

        init()
    }

    override fun doValidate(): ValidationInfo? {
        return nameRestrictions.validate(nameField.text, nameField)
            ?: descriptionArea.checkRestrictions()
            ?: if (labelList.content.labelsCount > MAX_LABEL_COUNT) {
                ValidationInfo("Max labels count is $MAX_LABEL_COUNT", labelList.content)
            } else null
    }

    override fun createCenterPanel(): JComponent = JBSplitter(false, "YCUI.api_gateway_edit_dialog", 0.30f).apply {
        firstComponent = YCUI.panel(BorderLayout()) {
            YCUI.gridPanel {
                YCUI.gridBag(horizontal = true) {
                    JBLabel(
                        "Name ",
                        CloudIcons.Resources.ApiGateway,
                        SwingConstants.LEFT
                    ) addAs nextln(0.0)

                    nameField addAs next(1.0)

                    descriptionArea.withPreferredHeight(110) addAs fullLine()
                }
            } addAs BorderLayout.NORTH

            labelList addAs BorderLayout.CENTER
        }.withPreferredSize(480, 600)

        secondComponent = YCUI.borderPanel {
            YCUI.separator("Specification") addAs BorderLayout.NORTH
            specEditor addAs BorderLayout.CENTER
            LabeledComponent.create(templateBox, "Template", BorderLayout.WEST) addAs BorderLayout.SOUTH
        }.withPreferredWidth(480)
    }

    private fun onEditorFocus() {
        if (!yamlLanguageFound && !languageWarningShown) {
            languageWarningShown = true
            Messages.showWarningDialog(
                project,
                "Your IDE does not support YAML language.\n Please install YAML plugin for proper usage of editor.",
                "Oops!"
            )
        }
    }

    private fun hasApiGatewayWithName(name: String): Boolean =
        name != apiGateway?.name && name in apiGatewayNames

    override fun doOKAction() {
        if (!okAction.isEnabled) return

        val data = CloudApiGatewaySpec(
            name = nameField.text,
            description = descriptionArea.text,
            labels = labelList.content.entries,
            openapiSpec = specEditor.text
        )

        if (apiGateway != null) {
            CloudOperationService.instance.updateApiGateway(
                project,
                authData,
                apiGateway,
                apiGatewaySpec,
                data,
            )
        } else {
            CloudOperationService.instance.createApiGateway(
                project,
                authData,
                folder,
                data
            )
        }

        super.doOKAction()
    }
}