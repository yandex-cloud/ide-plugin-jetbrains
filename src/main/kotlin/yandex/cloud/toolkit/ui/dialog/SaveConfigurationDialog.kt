package yandex.cloud.toolkit.ui.dialog

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.impl.RunConfigurationLevel
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import yandex.cloud.toolkit.util.*
import javax.swing.JLabel

class SaveConfigurationDialog(val project: Project, val configuration: RunConfiguration) : DialogBuilder(project) {
    init {
        title("Save Configuration")

        val nameField = JBTextField(configuration.name).withCaretListener {
            setOkActionEnabled(it.text.isNotEmpty())
        }
        val projectLevelCheckbox = JBCheckBox("Store as project file")

        setOkActionEnabled(nameField.text.isNotEmpty())

        centerPanel(
            YCUI.gridPanel {
                YCUI.gridBag(horizontal = true) {
                    JLabel("Name ") addAs nextln(0.0)
                    nameField addAs next(1.0)
                    projectLevelCheckbox addAs fullLine()
                }
            }.withPreferredWidth(400)
        )

        setOkOperation {
            val runManager = RunManagerImpl.getInstanceImpl(project)
            configuration.name = nameField.text
            val settings = RunnerAndConfigurationSettingsImpl(
                runManager,
                configuration,
                isTemplate = false,
                if (projectLevelCheckbox.isSelected) RunConfigurationLevel.PROJECT else RunConfigurationLevel.WORKSPACE
            )
            runManager.addConfiguration(settings)
            runManager.selectedConfiguration = settings
            dialogWrapper.close(DialogWrapper.OK_EXIT_CODE)
        }
    }
}