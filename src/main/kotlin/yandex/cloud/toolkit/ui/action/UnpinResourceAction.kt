package yandex.cloud.toolkit.ui.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import yandex.cloud.toolkit.api.explorer.CloudExplorer
import yandex.cloud.toolkit.api.explorer.PinnedResource

class UnpinResourceAction(val explorer: CloudExplorer, val resource: PinnedResource) : DumbAwareAction(
    { "Unpin" },
    AllIcons.General.Remove
) {
    override fun actionPerformed(e: AnActionEvent) {
        explorer.unpinResource(resource)
    }
}