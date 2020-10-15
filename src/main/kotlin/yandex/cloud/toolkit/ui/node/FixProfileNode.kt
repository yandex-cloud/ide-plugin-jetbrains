package yandex.cloud.toolkit.ui.node

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.treeStructure.SimpleNode
import yandex.cloud.toolkit.api.profile.CloudProfile
import yandex.cloud.toolkit.ui.dialog.FixProfileDialog
import yandex.cloud.toolkit.util.remote.resource.ResourceLoadingError
import java.awt.Component

class FixProfileNode(val profile: CloudProfile, val error: ResourceLoadingError) : SimpleNode(), DynamicTreeNode {
    override fun getChildren(): Array<SimpleNode> = emptyArray()

    override fun update(presentation: PresentationData) {
        presentation.setIcon(AllIcons.General.Settings)
        presentation.addText("Fix Profile", SimpleTextAttributes.GRAY_ATTRIBUTES)
        presentation.addText(" (Click)", SimpleTextAttributes.GRAYED_ATTRIBUTES)
    }

    override fun isAlwaysLeaf(): Boolean = true

    override fun onClick(project: Project, owner: Component, x: Int, y: Int) {
        FixProfileDialog(project, profile, error, true).show()
    }
}