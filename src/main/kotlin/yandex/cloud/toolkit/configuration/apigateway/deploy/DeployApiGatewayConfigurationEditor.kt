package yandex.cloud.toolkit.configuration.apigateway.deploy

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBLabel
import yandex.cloud.toolkit.api.resource.impl.model.CloudApiGateway
import yandex.cloud.toolkit.ui.component.*
import yandex.cloud.toolkit.util.*
import javax.swing.JComponent

class DeployApiGatewayConfigurationEditor(
    val project: Project,
    target: CloudApiGateway?
) : SettingsEditor<DeployApiGatewayConfiguration>() {

    private val apiGatewayField = CloudResourceField(project, target, CloudApiGateway.Descriptor.icon)
    private val specFileField = TextFieldWithBrowseButton()

    init {
        val fileDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
        specFileField.addBrowseFolderListener("API Gateway Spec File", null, project, fileDescriptor)
    }

    override fun resetEditorFrom(s: DeployApiGatewayConfiguration) {
        apiGatewayField.value = s.state.apiGatewayId ?: ""
        specFileField.text = s.state.specFile ?: ""
    }

    override fun applyEditorTo(s: DeployApiGatewayConfiguration) {
        s.loadState(ApiGatewayDeploySpec().apply {
            apiGatewayId = apiGatewayField.value
            specFile = specFileField.text
        })
    }

    override fun createEditor(): JComponent = YCUI.gridPanel {
        YCUI.gridBag(horizontal = true) {
            JBLabel("API Gateway ID ") addAs nextln(0.0)
            apiGatewayField addAs next(1.0)
            JBLabel("Spec File ") addAs nextln(0.0)
            specFileField addAs next(1.0)
        }
    }
}