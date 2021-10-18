package yandex.cloud.toolkit.configuration.function.run

import com.intellij.json.JsonLanguage
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.spellchecker.ui.SpellCheckingEditorCustomization
import com.intellij.ui.EditorCustomization
import com.intellij.ui.EditorTextFieldProvider
import com.intellij.ui.ErrorStripeEditorCustomization
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.text.nullize
import yandex.cloud.toolkit.api.resource.impl.model.CloudFunction
import yandex.cloud.toolkit.api.resource.impl.model.CloudFunctionVersion
import yandex.cloud.toolkit.ui.component.CloudResourceLabel
import yandex.cloud.toolkit.util.*
import javax.swing.JComponent

class RunFunctionConfigurationEditor(
    val project: Project,
    target: CloudFunction?
) : SettingsEditor<RunFunctionConfiguration>() {

    private val functionField = CloudResourceLabel(project, target, CloudFunction.Descriptor.icon)
    private val versionTagField = JBTextField()
    private val authorizeRequestCheckbox = JBCheckBox("Authorize Request")
    private val showRequestCheckbox = JBCheckBox("Show Request")
    private val formatResponseCheckbox = JBCheckBox("Format Response")

    private val requestField = EditorTextFieldProvider.getInstance().getEditorField(
        JsonLanguage.INSTANCE,
        project,
        listOf(
            colorEditorCustomization(),
            ErrorStripeEditorCustomization.DISABLED,
            SpellCheckingEditorCustomization.getInstance(false),
            EditorCustomization {
                it.isViewer = false
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
        requestField.setFontInheritedFromLAF(false)
        versionTagField.emptyText.text = CloudFunctionVersion.LATEST_TAG
    }

    override fun resetEditorFrom(s: RunFunctionConfiguration) {
        functionField.value = s.state.functionId ?: ""
        versionTagField.text = s.state.versionTag ?: ""
        requestField.text = s.state.request ?: ""
        authorizeRequestCheckbox.isSelected = s.state.authorizeRequest
        showRequestCheckbox.isSelected = s.state.showRequest
        formatResponseCheckbox.isSelected = s.state.formatResponse
    }

    override fun applyEditorTo(s: RunFunctionConfiguration) {
        s.loadState(FunctionRunSpec().apply {
            functionId = functionField.value
            versionTag = versionTagField.text.nullize()
            request = requestField.text.nullize()
            authorizeRequest = authorizeRequestCheckbox.isSelected
            showRequest = showRequestCheckbox.isSelected
            formatResponse = formatResponseCheckbox.isSelected
        })
    }

    override fun createEditor(): JComponent = YCUI.gridPanel {
        YCUI.gridBag(horizontal = true) {
            JBLabel("Function ID ") addAs nextln(0.0)
            functionField addAs next(1.0)
            JBLabel("Version Tag ") addAs nextln(0.0)
            versionTagField addAs next(1.0)
            authorizeRequestCheckbox addAs fullLine()
            showRequestCheckbox addAs fullLine()
            formatResponseCheckbox addAs fullLine()
            YCUI.separator("Request") addAs fullLine()
            requestField addAs fullLine()
        }
    }
}