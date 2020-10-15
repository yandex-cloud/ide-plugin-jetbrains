package yandex.cloud.toolkit.ui.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.intellij.util.ui.UIUtil
import yandex.cloud.toolkit.api.explorer.CloudExplorer

class UnpinAllResourcesAction(val explorer: CloudExplorer) : DumbAwareAction("Unpin All") {

    override fun actionPerformed(e: AnActionEvent) {
        val message = Messages.showOkCancelDialog(
            explorer.project,
            "Unpin all resources from Yandex.Cloud explorer?",
            "Unpin All",
            Messages.getOkButton(),
            Messages.getCancelButton(),
            UIUtil.getQuestionIcon()
        )

        if (message != Messages.OK) return
        explorer.unpinAll()
    }
}