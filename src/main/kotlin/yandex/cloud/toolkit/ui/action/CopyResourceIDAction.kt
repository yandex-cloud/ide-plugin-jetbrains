package yandex.cloud.toolkit.ui.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareAction
import yandex.cloud.toolkit.api.resource.CloudResource
import java.awt.datatransfer.StringSelection

class CopyResourceIDAction(val resource: CloudResource) :
    DumbAwareAction("Copy ID", null, AllIcons.Actions.Copy) {

    override fun actionPerformed(e: AnActionEvent) {
        CopyPasteManager.getInstance().setContents(StringSelection(resource.id))
    }
}