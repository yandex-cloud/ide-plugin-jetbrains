package yandex.cloud.toolkit.ui.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import yandex.cloud.toolkit.api.explorer.CloudExplorer
import yandex.cloud.toolkit.api.resource.impl.HierarchicalCloudResource

class PinResourceAction(
    val explorer: CloudExplorer,
    val resource: HierarchicalCloudResource,
    val alreadyPinned: Boolean
) : DumbAwareAction(
    { if (alreadyPinned) "Already Pinned" else "Pin" },
    { "" },
    if (alreadyPinned) null else AllIcons.General.Pin_tab
) {

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = !alreadyPinned
    }

    override fun actionPerformed(e: AnActionEvent) {
        if (!alreadyPinned) explorer.pinResource(resource)
    }
}