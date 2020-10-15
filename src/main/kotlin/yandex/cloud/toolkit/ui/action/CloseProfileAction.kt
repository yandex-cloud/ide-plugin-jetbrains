package yandex.cloud.toolkit.ui.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import yandex.cloud.toolkit.api.profile.impl.profileStorage

class CloseProfileAction(val project: Project) : DumbAwareAction(
    "Exit"
) {
    override fun actionPerformed(e: AnActionEvent) {
        project.profileStorage.selectProfile(null)
    }
}