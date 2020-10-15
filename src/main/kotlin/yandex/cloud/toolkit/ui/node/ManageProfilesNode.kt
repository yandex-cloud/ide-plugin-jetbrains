package yandex.cloud.toolkit.ui.node

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.treeStructure.SimpleNode
import yandex.cloud.toolkit.ui.dialog.ManageProfilesDialog
import java.awt.Component

class ManageProfilesNode : SimpleNode(), DynamicTreeNode {
    override fun getChildren(): Array<SimpleNode> = emptyArray()

    override fun update(presentation: PresentationData) {
        presentation.setIcon(AllIcons.General.Settings)
        presentation.addText("Manage Profiles", SimpleTextAttributes.GRAY_ATTRIBUTES)
        presentation.addText(" (Click)", SimpleTextAttributes.GRAYED_ATTRIBUTES)
    }

    override fun isAlwaysLeaf(): Boolean = true

    override fun onClick(project: Project, owner: Component, x: Int, y: Int) {
        ManageProfilesDialog(project).show()
    }
}