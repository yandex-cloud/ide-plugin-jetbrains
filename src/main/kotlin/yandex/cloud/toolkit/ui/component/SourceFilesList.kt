package yandex.cloud.toolkit.ui.component

import com.intellij.ide.presentation.VirtualFilePresentation
import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import yandex.cloud.toolkit.util.*
import java.awt.GridBagLayout
import javax.swing.JList

class SourceFilesList(val project: Project) : YCPanel(GridBagLayout()) {

    private val listModel = CollectionListModel<SourceFile>()
    private val list = JBList(listModel)

    var files: List<String>
        get() = listModel.items.map { it.path }
        set(value) {
            val fileSystem = LocalFileSystem.getInstance()
            listModel.setItems(
                value.map { SourceFile(it, fileSystem.findFileByPath(it)) }
            )
        }

    init {
        list.setEmptyText("No source files selected")
        list.cellRenderer = ListRenderer()

        YCUI.gridBag(horizontal = true) {
            YCUI.separator("Source Files") addAs fullLine()

            ToolbarDecorator.createDecorator(list).apply {
                setToolbarPosition(ActionToolbarPosition.BOTTOM)
                setMoveDownAction(null)
                setMoveUpAction(null)
                setAddAction {
                    val descriptor = FileChooserDescriptor(
                        true,
                        true,
                        false,
                        false,
                        false,
                        true
                    )

                    FileChooser.chooseFiles(
                        descriptor, project, null
                    ) { files ->
                        listModel.add(files.map { SourceFile(it.path, it) })
                    }
                }
            }.createPanel().withPreferredHeight(110) addAs fullLine()
        }
    }

    private class ListRenderer : ColoredListCellRenderer<SourceFile>() {
        override fun customizeCellRenderer(
            list: JList<out SourceFile>,
            file: SourceFile,
            index: Int,
            selected: Boolean,
            hasFocus: Boolean
        ) {
            val virtualFile = file.virtualFile
            if (virtualFile == null) {
                append(file.path, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                return
            }

            icon = VirtualFilePresentation.getIcon(virtualFile)
            append(virtualFile.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            val parent = virtualFile.parent
            if (parent != null) {
                append(" (" + FileUtil.toSystemDependentName(parent.path) + ")", SimpleTextAttributes.GRAY_ATTRIBUTES)
            }
        }
    }

    private data class SourceFile(val path: String, val virtualFile: VirtualFile?)
}