package yandex.cloud.toolkit.ui.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import icons.CloudIcons
import yandex.cloud.toolkit.ui.dialog.ManageProfilesDialog

class ManageProfilesAction() : DumbAwareAction(
    "Manage Profiles",
    null,
    CloudIcons.Nodes.Profile
) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ManageProfilesDialog(project).show()
    }
}