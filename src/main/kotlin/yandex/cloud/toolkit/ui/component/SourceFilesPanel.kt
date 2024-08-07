package yandex.cloud.toolkit.ui.component


import com.intellij.openapi.project.Project
import yandex.cloud.toolkit.util.YCPanel
import yandex.cloud.toolkit.util.YCUI
import yandex.cloud.toolkit.util.labeled
import java.awt.BorderLayout

class SourceFilesPanel(project: Project) : YCPanel(BorderLayout()) {

    val sourceFilesList = SourceFilesList(project)
    val sourceFolderPolicyBox = SourceFolderPolicyBox()

    init {
        sourceFilesList addAs BorderLayout.CENTER
        sourceFolderPolicyBox.labeled("Source Folders") addAs BorderLayout.SOUTH
    }
}
