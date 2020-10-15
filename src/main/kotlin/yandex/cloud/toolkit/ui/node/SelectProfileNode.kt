package yandex.cloud.toolkit.ui.node

import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.treeStructure.SimpleNode
import icons.CloudIcons
import yandex.cloud.toolkit.ui.dialog.ManageProfilesDialog
import java.awt.Component

class SelectProfileNode : SimpleNode(), DynamicTreeNode {
    override fun getChildren(): Array<SimpleNode> = emptyArray()

    override fun update(presentation: PresentationData) {
        presentation.setIcon(CloudIcons.Nodes.Profile)
        presentation.addText("Select Profile", SimpleTextAttributes.REGULAR_ATTRIBUTES)
        presentation.addText(" (Click)", SimpleTextAttributes.GRAYED_ATTRIBUTES)
    }

    override fun isAlwaysLeaf(): Boolean = true

    override fun onClick(project: Project, owner: Component, x: Int, y: Int) {
        ManageProfilesDialog(project).show()
    }
}