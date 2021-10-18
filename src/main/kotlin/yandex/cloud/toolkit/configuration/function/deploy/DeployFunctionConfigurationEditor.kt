package yandex.cloud.toolkit.configuration.function.deploy

import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.ClickListener
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.components.fields.IntegerField
import com.intellij.util.text.nullize
import com.intellij.util.ui.JBUI
import yandex.cloud.toolkit.api.resource.impl.model.CloudFunction
import yandex.cloud.toolkit.api.resource.impl.model.CloudFunctionVersion
import yandex.cloud.toolkit.api.resource.impl.model.CloudServiceAccount
import yandex.cloud.toolkit.api.resource.impl.model.VPCNetwork
import yandex.cloud.toolkit.ui.component.*
import yandex.cloud.toolkit.util.*
import java.awt.BorderLayout
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JTextField

class DeployFunctionConfigurationEditor(
    val project: Project,
    target: CloudFunction?,
    versions: List<CloudFunctionVersion>?,
    serviceAccounts: List<CloudServiceAccount>?,
    networks: List<VPCNetwork>?,
    runtimes: List<String>?,
) : SettingsEditor<DeployFunctionConfiguration>() {

    companion object {
        private const val MAX_DESCRIPTION_LENGTH = 256

        private const val MIN_MEMORY = 128
        private const val MAX_MEMORY = 2048
        private const val DEFAULT_MEMORY = 128
        private const val MEMORY_PART = 128

        private val KEY_MEMORY_VALUES = intArrayOf(MIN_MEMORY, 512, 1024, 1536, MAX_MEMORY)
    }

    private val functionField = CloudResourceLabel(project, target, CloudFunction.Descriptor.icon)

    private val runtimeField = FunctionRuntimeField(project, runtimes)
    private val entryPointField = JTextField()
    private val sourceFilesList = SourceFilesList(project)
    private val sourceFolderPolicyBox = SourceFolderPolicyBox()
    private val descriptionArea = LimitedTextArea("Description", MAX_DESCRIPTION_LENGTH)

    private val serviceAccountField = ServiceAccountField(project, target?.group?.folder?.id, serviceAccounts)
    private val timeoutField = IntegerField(null, 0, Integer.MAX_VALUE)
    private val memoryField = MemoryField(MIN_MEMORY, MAX_MEMORY, MEMORY_PART, DEFAULT_MEMORY, KEY_MEMORY_VALUES)

    private val envVariablesList = EnvironmentVariablesList()
    private val tagsList = FunctionVersionTagsList(null, versions ?: emptyList())

    private val networkField = VPCNetworkField(project, target?.group?.folder?.id, networks)
    private val subnetsList = SpoilerPanel("Enter Subnets", VPCSubnetsList(project, target?.group?.folder?.id))

    override fun resetEditorFrom(s: DeployFunctionConfiguration) {
        functionField.value = s.state.functionId ?: ""
        runtimeField.text = s.state.runtime
        entryPointField.text = s.state.entryPoint
        sourceFilesList.files = s.state.sourceFiles
        sourceFolderPolicyBox.value = SourceFolderPolicy.byId(s.state.sourceFolderPolicy)
        descriptionArea.text = s.state.description ?: ""
        memoryField.valueBytes = s.state.memoryBytes
        timeoutField.value = s.state.timeoutSeconds.toInt()
        serviceAccountField.value = s.state.serviceAccountId ?: ""
        envVariablesList.entries = s.state.envVariables
        tagsList.tags = s.state.tags.toSet()
        networkField.value = s.state.networkId ?: ""
        subnetsList.content.subnets = s.state.subnets
        subnetsList.isOpened = s.state.useSubnets
    }

    override fun applyEditorTo(s: DeployFunctionConfiguration) {
        s.loadState(FunctionDeploySpec().apply {
            functionId = functionField.value
            runtime = runtimeField.text
            entryPoint = entryPointField.text
            sourceFiles = sourceFilesList.files.toMutableList()
            sourceFolderPolicy = sourceFolderPolicyBox.value.id
            description = descriptionArea.text
            memoryBytes = memoryField.valueBytes
            timeoutSeconds = timeoutField.value.toLong()
            serviceAccountId = serviceAccountField.value.nullize()
            envVariables = envVariablesList.entries.toMutableMap()
            tags = tagsList.tags.toMutableList()
            networkId = networkField.value.nullize()
            subnets = subnetsList.content.subnets.toMutableList()
            useSubnets = subnetsList.isOpened
        })
    }

    fun doValidate(): ValidationInfo? = when {
        functionField.value.isEmpty() -> ValidationInfo("No function ID defined", functionField)
        entryPointField.text.isNullOrEmpty() -> ValidationInfo("No entry point defined", entryPointField)
        sourceFilesList.files.isEmpty() -> ValidationInfo("No source files selected", sourceFilesList)
        !timeoutField.isValueValid -> ValidationInfo("Invalid timeout value", timeoutField)
        runtimeField.text.isBlank() -> ValidationInfo("No runtime defined", runtimeField)
        else -> descriptionArea.checkRestrictions()
    }

    private fun updateNetworkField() {
        networkField.isEnabled = !subnetsList.isOpened
        networkField.isFocusable = networkField.isEnabled
        if (networkField.isEnabled) {
            networkField.field.grabFocus()
            networkField.field.setTextToTriggerEmptyTextStatus("")
            networkField.field.emptyText.text = ""
        } else {
            subnetsList.content.grabFocus()
            networkField.field.setTextToTriggerEmptyTextStatus(networkField.field.text)
            networkField.field.emptyText.text = "Subnets mode selected (click to change)"
        }
    }

    override fun createEditor(): JComponent = YCUI.borderPanel {
        subnetsList.addSpoilerListener(SpoilerToggleListener(this@DeployFunctionConfigurationEditor::updateNetworkField))
        object: ClickListener() {
            override fun onClick(event: MouseEvent, clickCount: Int): Boolean {
                if (event.button == MouseEvent.BUTTON1) {
                    if (!networkField.isEnabled) subnetsList.close()
                    return true
                }
                return false
            }
        }.installOn(networkField.field)
        updateNetworkField()

        functionField.labeled("Function ID") addAs BorderLayout.NORTH

        JBTabbedPane().apply {
            border = JBUI.Borders.emptyTop(5)

            addTab("Main", YCUI.borderPanel {
                YCUI.gridPanel {
                    YCUI.gridBag(horizontal = true) {
                        JBLabel("Runtime: ") addAs nextln(0.0)
                        runtimeField addAs next(1.0)
                        JBLabel("Entry Point: ") addAs nextln(0.0)
                        entryPointField addAs next(1.0)
                    }
                } addAs BorderLayout.NORTH

                sourceFilesList addAs BorderLayout.CENTER
                sourceFolderPolicyBox.labeled("Source Folders") addAs BorderLayout.SOUTH
            })

            addTab("Parameters", YCUI.borderPanel {
                YCUI.gridPanel {
                    YCUI.gridBag(horizontal = true) {
                        JBLabel("Service Account ID: ") addAs nextln(0.0)
                        serviceAccountField addAs next(1.0)
                        JBLabel("Timeout (sec): ") addAs nextln(0.0)
                        timeoutField addAs next(1.0)
                        JBLabel("Memory: ") addAs nextln(0.0).insetTop(3)
                        memoryField addAs next().coverLine()
                    }
                } addAs BorderLayout.NORTH
                envVariablesList addAs BorderLayout.CENTER
            })

            addTab("Misc", YCUI.borderPanel {
                descriptionArea.withPreferredHeight(100) addAs BorderLayout.NORTH
                tagsList.withPreferredHeight(100) addAs BorderLayout.CENTER
            })

            addTab("VPC", YCUI.borderPanel {
                networkField.labeled("Network ID") addAs BorderLayout.NORTH
                subnetsList.withPreferredHeight(120) addAs BorderLayout.CENTER
            })
        } addAs BorderLayout.CENTER
    }
}