package yandex.cloud.toolkit.ui.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import icons.CloudIcons
import yandex.cloud.api.access.Access
import yandex.cloud.api.iam.v1.RoleOuterClass
import yandex.cloud.toolkit.api.auth.CloudAuthData
import yandex.cloud.toolkit.api.resource.impl.model.CloudFolder
import yandex.cloud.toolkit.api.resource.impl.model.CloudServiceAccount
import yandex.cloud.toolkit.api.resource.impl.model.CloudServiceAccountSpec
import yandex.cloud.toolkit.api.resource.impl.model.hasSubject
import yandex.cloud.toolkit.api.service.CloudOperationService
import yandex.cloud.toolkit.ui.component.LimitedTextArea
import yandex.cloud.toolkit.ui.component.ServiceAccountRolesList
import yandex.cloud.toolkit.util.*
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.SwingConstants

class CreateOrEditServiceAccountDialog(
    val project: Project,
    val authData: CloudAuthData,
    val folder: CloudFolder,
    val serviceAccount: CloudServiceAccount?,
    val folderAccessBindings: List<Access.AccessBinding>,
    val availableRoles: List<RoleOuterClass.Role>,
    val serviceAccounts: List<CloudServiceAccount>
) : DialogWrapper(true) {

    companion object {
        private const val MAX_DESCRIPTION_LENGTH = 256
    }

    private val nameRestrictions = restrictions<String>("Name") {
        textLength(3..63)
        textPattern("[a-z]([-a-z0-9]{0,61}[a-z0-9])?")
        requireNot({ hasServiceAccountWithName(it) }) { "must be unique in folder" }
    }

    private val nameField = JBTextField()
    private val descriptionArea = LimitedTextArea("Description", MAX_DESCRIPTION_LENGTH)
    private val rolesList = ServiceAccountRolesList(availableRoles)

    private val serviceAccountNames = serviceAccounts.mapTo(mutableSetOf()) { it.data.name }

    init {
        if (serviceAccount != null) {
            rolesList.roles = folderAccessBindings.filter {
                it.hasSubject(serviceAccount)
            }.mapTo(mutableSetOf(), Access.AccessBinding::getRoleId)

            nameField.text = serviceAccount.name
            descriptionArea.text = serviceAccount.data.description
        }

        title = when (serviceAccount) {
            null -> "Create Yandex.Cloud Service Account"
            else -> "Edit Yandex.Cloud Service Account"
        }

        init()
    }

    override fun doValidate(): ValidationInfo? {
        return nameRestrictions.validate(nameField.text, nameField)
            ?: descriptionArea.checkRestrictions()
    }

    private fun hasServiceAccountWithName(name: String): Boolean =
        name != serviceAccount?.name && name in serviceAccountNames

    override fun createCenterPanel(): JComponent {
        return YCUI.scrollPane(
            YCUI.panel(BorderLayout()) {
                withPreferredSize(450, 250)

                YCUI.gridPanel {
                    YCUI.gridBag(horizontal = true) {
                        JBLabel(
                            "Name: ",
                            CloudIcons.Resources.ServiceAccount,
                            SwingConstants.LEFT
                        ) addAs nextln(0.0)

                        nameField addAs next(1.0)

                        descriptionArea.withPreferredHeight(90) addAs fullLine()
                    }
                } addAs BorderLayout.NORTH

                rolesList addAs BorderLayout.CENTER
            }
        ).withNoBorder()
    }

    override fun doOKAction() {
        if (!okAction.isEnabled) return

        val spec = CloudServiceAccountSpec(
            name = nameField.text,
            description = descriptionArea.text,
            roles = rolesList.roles
        )

        if (serviceAccount != null) {
            CloudOperationService.instance.updateServiceAccount(
                project,
                authData,
                folderAccessBindings,
                serviceAccount,
                spec
            )
        } else {
            CloudOperationService.instance.createServiceAccount(
                project,
                authData,
                folder,
                spec
            )
        }

        super.doOKAction()
    }
}