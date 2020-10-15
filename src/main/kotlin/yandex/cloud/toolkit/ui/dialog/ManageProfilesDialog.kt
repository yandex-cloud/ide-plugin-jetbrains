package yandex.cloud.toolkit.ui.dialog

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.AnActionButton
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import yandex.cloud.toolkit.api.auth.CloudAuthData
import yandex.cloud.toolkit.api.auth.CloudAuthMethodDescriptor
import yandex.cloud.toolkit.api.auth.CloudAuthService
import yandex.cloud.toolkit.api.profile.CloudProfile
import yandex.cloud.toolkit.api.profile.CloudProfileService
import yandex.cloud.toolkit.api.profile.drawProfile
import yandex.cloud.toolkit.api.profile.impl.profileStorage
import yandex.cloud.toolkit.util.*
import yandex.cloud.toolkit.util.remote.resource.PresentableResourceStatus
import yandex.cloud.toolkit.util.remote.resource.ResourceLoadingError
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.util.*
import java.util.concurrent.CancellationException
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JList

class ManageProfilesDialog(val project: Project) : DialogWrapper(true) {

    private val profilesListModel = CollectionListModel<CloudProfile>()
    private val profilesList = JBList(profilesListModel)

    private val fixProfileAction = FixProfileAction()
    private val recreateProfileAction = RecreateProfileAction()
    private val reAuthProfileAction = ReAuthProfileAction()

    init {
        profilesList.setEmptyText("No Profiles")
        profilesList.cellRenderer = ProfileListRenderer()
        title = "Select Yandex.Cloud Profile"
        profilesListModel.add(CloudProfileService.instance.getProfiles())
        okAction.text = "Select"
        cancelAction.text = "Close"
        init()

        updateButtons()

        profilesList.addListSelectionListener {
            updateButtons()
        }
    }

    override fun createCenterPanel(): JComponent = YCUI.borderPanel {
        ToolbarDecorator.createDecorator(profilesList).apply {
            setToolbarPosition(ActionToolbarPosition.BOTTOM)
            setMoveDownAction(null)
            setMoveUpAction(null)

            setAddAction {
                val actions = CloudAuthService.instance.getAvailableAuthMethods().map(::SelectAuthMethodAction)
                val actionGroup = DefaultActionGroup(actions)

                JBPopupFactory.getInstance().createActionGroupPopup(
                    "Select authentication method",
                    actionGroup,
                    it.dataContext,
                    JBPopupFactory.ActionSelectionAid.MNEMONICS, false
                ).show(it.preferredPopupPoint!!)
            }

            setEditAction {
                val selectedProfile = profilesList.singleSelectedValue ?: return@setEditAction
                NameProfileDialog(selectedProfile, true, selectedProfile::rename).showAndGet()
            }

            setRemoveAction {
                val selectedProfile = profilesList.singleSelectedValue ?: return@setRemoveAction
                profilesListModel.remove(selectedProfile)
                onProfileRemoved(selectedProfile)
                updateButtons()
            }

            addExtraAction(reAuthProfileAction)

            setRemoveActionUpdater { profilesList.isSingleValueSelected }
            setEditActionUpdater { profilesList.isSingleValueSelected }
        }.createPanel().withPreferredSize(400, 150) addAs BorderLayout.CENTER
    }

    private fun updateButtons() {
        val profile = profilesList.singleSelectedValue
        val profileSelected = profile != null
        okAction.isEnabled = profileSelected
        fixProfileAction.isEnabled = profileSelected && profile!!.getAuthDataStatus() != null
        recreateProfileAction.isEnabled = profileSelected
        reAuthProfileAction.isEnabled = profileSelected
    }

    private fun onProfileRemoved(profile: CloudProfile) {
        CloudProfileService.instance.deleteProfile(profile.id)
    }

    private fun onProfileAuthEnd(profile: CloudProfile, authData: Maybe<CloudAuthData>) {
        when (authData) {
            is NoValue -> {
                if (authData.error is CancellationException) return
                Messages.showErrorDialog(
                    authData.error.message,
                    (authData.error as? ResourceLoadingError)?.status?.toString() ?: "Authentication Failed"
                )
            }
            is JustValue -> {
                val isNamed = NameProfileDialog(profile, false, profile::rename).showAndGet()
                if (isNamed) {
                    profilesListModel.add(profile)
                    val profileIndex = profilesListModel.size - 1
                    profilesList.selectionModel.setSelectionInterval(profileIndex, profileIndex)
                    CloudProfileService.instance.addProfile(profile)
                } else {
                    profile.dispose()
                }
            }
        }
    }

    override fun doOKAction() {
        val selectedProfile = profilesList.singleSelectedValue
        if (okAction.isEnabled && selectedProfile != null) {
            project.profileStorage.selectProfile(selectedProfile)
        }
        super.doOKAction()
    }

    private class ProfileListRenderer : ColoredListCellRenderer<CloudProfile>() {
        override fun customizeCellRenderer(
            list: JList<out CloudProfile>,
            profile: CloudProfile,
            index: Int,
            selected: Boolean,
            hasFocus: Boolean
        ) {
            drawProfile(profile)
        }
    }

    private inner class SelectAuthMethodAction(
        private val authMethodDescriptor: CloudAuthMethodDescriptor
    ) : DumbAwareAction(authMethodDescriptor.name) {

        override fun actionPerformed(e: AnActionEvent) {
            val profileId = UUID.randomUUID()
            val authMethod = authMethodDescriptor.createAuthMethod()
            val profile = CloudProfile(profileId, "Unauthenticated Profile", authMethod)

            val authStarted = profile.tryAuthenticate(project) { authData ->
                if (this@ManageProfilesDialog.isDisposed) return@tryAuthenticate
                invokeLaterAt(this@ManageProfilesDialog.rootPane) {
                    onProfileAuthEnd(profile, authData)
                }
            }

            if (!authStarted) {
                onProfileAuthEnd(
                    profile,
                    noResource(
                        "This profile is already in authentication state",
                        PresentableResourceStatus.ConcurrentAction
                    )
                )
            }
        }
    }

    override fun createLeftSideActions(): Array<Action> = arrayOf(fixProfileAction, recreateProfileAction)

    private inner class FixProfileAction : DialogWrapperAction("Fix") {
        override fun doAction(e: ActionEvent?) {
            val selectedProfile = profilesList.singleSelectedValue
            val error = selectedProfile?.getAuthDataError() ?: return

            close(CANCEL_EXIT_CODE)
            invokeLater {
                FixProfileDialog(project, selectedProfile, error, false).show()
            }
        }
    }

    private inner class RecreateProfileAction : DialogWrapperAction("Recreate") {
        override fun doAction(e: ActionEvent?) {
            val profile = profilesList.singleSelectedValue ?: return
            profile.tryAuthenticate(project) {
                invokeLaterAt(this@ManageProfilesDialog.rootPane) {
                    close(CANCEL_EXIT_CODE)
                }
            }
        }
    }

    private inner class ReAuthProfileAction : AnActionButton("Reauthenticate", AllIcons.Actions.Refresh) {
        override fun actionPerformed(e: AnActionEvent) {
            val profile = profilesList.singleSelectedValue ?: return
            profile.tryRecreateAuthData()
        }
    }
}