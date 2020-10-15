package yandex.cloud.toolkit.ui.dialog

import com.intellij.icons.AllIcons
import com.intellij.json.JsonLanguage
import com.intellij.lang.Language
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.EditorCustomization
import com.intellij.ui.EditorTextField
import com.intellij.ui.EditorTextFieldProvider
import com.intellij.ui.ErrorStripeEditorCustomization
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import yandex.cloud.toolkit.api.auth.CloudAuthData
import yandex.cloud.toolkit.api.resource.impl.model.CloudApiGateway
import yandex.cloud.toolkit.ui.action.CreateOrEditApiGatewayAction
import yandex.cloud.toolkit.util.*
import java.awt.BorderLayout
import javax.swing.JComponent

class ApiGatewaySpecificationDialog(
    val project: Project,
    val authData: CloudAuthData,
    val apiGateway: CloudApiGateway,
    val apiGatewaySpec: String
) : DialogWrapper(project, null, true, IdeModalityType.PROJECT) {

    private val specField: EditorTextField = EditorTextFieldProvider.getInstance().getEditorField(
        Language.findLanguageByID("yaml") ?: JsonLanguage.INSTANCE,
        project,
        listOf(
            colorEditorCustomization(),
            ErrorStripeEditorCustomization.ENABLED,
            EditorCustomization {
                it.isViewer = true
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

    init {
        specField.text = apiGatewaySpec

        myOKAction.text = "Edit"
        myOKAction.icon = AllIcons.Actions.Edit

        myCancelAction.text = "Close"

        title = "Yandex.Cloud API Gateway Specification"
        init()
    }

    override fun doOKAction() {
        super.doOKAction()
        invokeLater {
            CreateOrEditApiGatewayAction.openDialog(project, authData, apiGateway.group.folder, apiGateway)
        }
    }

    override fun createCenterPanel(): JComponent = YCUI.borderPanel {
        YCUI.gridPanel {
            YCUI.gridBag(horizontal = true) {
                JBLabel("API Gateway ") addAs nextln(0.0)
                JBTextField(apiGateway.fullName).apply {
                    asEditable(false)
                } addAs next(1.0)

                JBLabel("Domain ") addAs nextln(0.0)
                JBTextField(apiGateway.data.domain).apply {
                    asEditable(false)
                } addAs next(1.0)

                YCUI.separator("Specification") addAs fullLine()
            }
        } addAs BorderLayout.NORTH

        specField addAs BorderLayout.CENTER
    }.withPreferredSize(600, 600)
}