package yandex.cloud.toolkit.ui.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

class IgnoreAction(val action: () -> Unit) : DumbAwareAction("Do it anyway!") {

    override fun actionPerformed(e: AnActionEvent) {
        action()
    }
}