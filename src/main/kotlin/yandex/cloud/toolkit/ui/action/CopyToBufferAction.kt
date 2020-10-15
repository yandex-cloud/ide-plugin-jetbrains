package yandex.cloud.toolkit.ui.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.text.StringUtil
import java.awt.datatransfer.StringSelection

class CopyToBufferAction(private val text: () -> String) :
    DumbAwareAction("Copy to Buffer", null, AllIcons.Actions.Copy) {

    override fun actionPerformed(e: AnActionEvent) {
        val str = StringUtil.convertLineSeparators(text())
        CopyPasteManager.getInstance().setContents(StringSelection(str))
    }
}