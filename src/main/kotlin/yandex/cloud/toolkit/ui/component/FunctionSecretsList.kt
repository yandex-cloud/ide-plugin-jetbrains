package yandex.cloud.toolkit.ui.component

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.*
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBTextField
import icons.CloudIcons
import yandex.cloud.toolkit.api.profile.impl.profileStorage
import yandex.cloud.toolkit.api.service.CloudRepository
import yandex.cloud.toolkit.configuration.function.deploy.FunctionSecret
import yandex.cloud.toolkit.util.*
import yandex.cloud.toolkit.util.task.modalTask
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.awt.event.MouseEvent
import javax.swing.*

class FunctionSecretsList(val project: Project) : YCPanel(BorderLayout()) {

    private val listModel = CollectionListModel<FunctionSecret>()
    private val list = JBList(listModel)

    var secrets: List<FunctionSecret>
        get() = listModel.items
        set(value) {
            listModel.setItems(value)
        }

    init {
        list.setEmptyText("No Secrets")
        list.cellRenderer = SecretListRenderer()
        withPreferredHeight(110)

        YCUI.separator("Lockbox Secrets").withIcon(CloudIcons.Resources.Lockbox) addAs BorderLayout.NORTH

        object : DoubleClickListener() {
            override fun onDoubleClick(event: MouseEvent?): Boolean {
                val selectedValue = list.singleSelectedValue ?: return true
                listModel.remove(selectedValue)
                SecretEditDialog(project, selectedValue).show()
                return true
            }
        }.installOn(list)

        ToolbarDecorator.createDecorator(list).apply {
            setToolbarPosition(ActionToolbarPosition.RIGHT)
            setMoveDownAction(null)
            setMoveUpAction(null)
            setAddAction {
                SecretEditDialog(project, FunctionSecret()).show()
            }
            setEditAction {
                val selectedValue = list.singleSelectedValue ?: return@setEditAction
                listModel.remove(selectedValue)
                SecretEditDialog(project, selectedValue).show()
            }
            setEditActionUpdater { list.isSingleValueSelected }

            val copyAction = AnActionButton.fromAction(CopyAction())
            copyAction.addCustomUpdater { list.isSingleValueSelected }
            addExtraAction(copyAction)
        }.createPanel() addAs BorderLayout.CENTER
    }

    fun clear() {
        listModel.removeAll()
    }

    fun addSecret(secret: FunctionSecret) {
        listModel.replaceAll(listModel.items.filter { it.envVariable != secret.envVariable })
        listModel.add(secret)
    }

    private inner class CopyAction : DumbAwareAction("Copy", null, AllIcons.Actions.Copy) {

        override fun actionPerformed(e: AnActionEvent) {
            val selectedValue = list.singleSelectedValue ?: return
            SecretEditDialog(project,
                FunctionSecret().apply {
                    copyFrom(selectedValue)
                    envVariable = null
                }
            ).show()
        }
    }

    private class SecretListRenderer : ColoredListCellRenderer<FunctionSecret>() {
        override fun customizeCellRenderer(
            list: JList<out FunctionSecret>,
            value: FunctionSecret,
            index: Int,
            selected: Boolean,
            hasFocus: Boolean
        ) {
            icon = AllIcons.Diff.Lock
            append(value.toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES)
        }
    }

    private inner class SecretEditDialog(project: Project, val secret: FunctionSecret) : DialogWrapper(project, true) {

        val envVarField = JBTextField(secret.envVariable)
        val idField = JBTextField(secret.id)
        val versionIdField = JBTextField(secret.versionId)
        val keyField = JBTextField(secret.key)

        private val testSecretAction = TestSecretAction()

        private val restrictions = restrictions<String>("") {
            textIsNotEmpty()
            textHasNoSpaces()
        }

        private val envVarRestrictions = restrictions<String>("Env Variable") {
            include(restrictions)
            textPattern("[a-zA-Z][a-zA-Z0-9_]*")
        }

        private val keyRestrictions = restrictions<String>("Key") {
            include(restrictions)
            textMaxLength(256)
            textPattern("[-_./\\@0-9a-zA-Z]+")
        }

        init {
            title = "Add Lockbox Secret"
            setResizable(false)
            testSecretAction.copyEnableStateOf(okAction)
            init()
        }

        override fun doValidate(): ValidationInfo? =
            envVarRestrictions.validate(envVarField.text, envVarField)
                ?: restrictions.rename("Secret ID").validate(idField.text, idField)
                ?: restrictions.rename("Version ID").validate(versionIdField.text, versionIdField)
                ?: keyRestrictions.validate(keyField.text, keyField)

        override fun createCenterPanel(): JComponent = YCUI.gridPanel {
            YCUI.gridBag(horizontal = true) {
                JLabel("Target Environment Variable") addAs fullLine()
                envVarField addAs fullLine()
                YCUI.separator("Lockbox Secret").withIcon(CloudIcons.Resources.Lockbox) addAs fullLine()
                JLabel("Secret ID ") addAs nextln(0.0)
                idField addAs next(1.0)
                JLabel("Version ID ") addAs nextln(0.0)
                versionIdField addAs next(1.0)
                JLabel("Key ") addAs nextln(0.0)
                keyField addAs next(1.0)
            }
        }.withPreferredWidth(400)

        override fun createLeftSideActions(): Array<Action> = arrayOf(testSecretAction)

        override fun doOKAction() {
            if (!okAction.isEnabled) return
            addSecret(FunctionSecret().apply {
                id = idField.text
                versionId = versionIdField.text
                key = keyField.text
                envVariable = envVarField.text
            })
            super.doOKAction()
        }

        override fun doCancelAction() {
            if (!cancelAction.isEnabled) return
            if (!secret.envVariable.isNullOrEmpty()) addSecret(secret)
            super.doCancelAction()
        }

        private inner class TestSecretAction : DialogWrapperAction("Test") {
            override fun doAction(e: ActionEvent?) {
                if (!okAction.isEnabled || doValidate() != null) {
                    return
                }

                val secretId = idField.text
                val versionId = versionIdField.text
                val key = keyField.text

                modalTask(project, "Testing Lockbox Secret", true) {
                    val authData = project.profileStorage.profile?.getAuthData(toUse = true)
                    if (authData == null) {
                        project.showAuthenticationMessage()
                        return@modalTask
                    }
                    text = "Connecting to Lockbox..."

                    val secret = tryDo {
                        CloudRepository.instance.getLockboxSecret(authData, secretId, versionId)
                    } onFail { error ->
                        if (!isCanceled) {
                            invokeLater {
                                Messages.showErrorDialog(project, error.message, "Failed to fetch Lockbox secret")
                            }
                        }
                    }

                    if (isCanceled) return@modalTask

                    invokeLater {
                        val entry = secret.entriesList.find { it.key == key }
                        if (entry == null) {
                            Messages.showWarningDialog(
                                project,
                                "Secret version '$versionId' has no key '$key'",
                                "Key Not Found"
                            )
                        } else {
                            val result = Messages.showDialog(
                                project,
                                "Please make sure that function's service account has access to it",
                                "Secret Exists!",
                                arrayOf(Messages.getOkButton(), "View Secret"),
                                0,
                                CloudIcons.Status.Success
                            )

                            if (result == 1) {
                                Messages.showTextAreaDialog(
                                    JTextField(if (entry.hasTextValue()) entry.textValue else "** Binary Value **"),
                                    "Lockbox Secret",
                                    "YandexCloudLockboxSecret"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
