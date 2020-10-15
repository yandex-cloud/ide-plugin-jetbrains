package yandex.cloud.toolkit.ui.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

class StubAction(val text: String) : DumbAwareAction(text) {

    init {
        templatePresentation.isEnabled = false
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = false
    }

    override fun actionPerformed(e: AnActionEvent) {}
}