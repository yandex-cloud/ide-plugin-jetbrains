package yandex.cloud.toolkit.ui.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import yandex.cloud.toolkit.ui.dialog.ManageProfilesDialog

class ChangeProfileAction(val project: Project) : DumbAwareAction(
    "Change Profile"
) {
    override fun actionPerformed(e: AnActionEvent) {
        ManageProfilesDialog(project).show()
    }
}