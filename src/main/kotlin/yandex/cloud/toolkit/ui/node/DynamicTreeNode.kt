package yandex.cloud.toolkit.ui.node

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import java.awt.Component

interface DynamicTreeNode {

    fun onDoubleClick(project: Project, owner: Component, x: Int, y: Int) {}
    fun onClick(project: Project, owner: Component, x: Int, y: Int) {}

    fun onCollapsed(project: Project) {}
    fun onExpanded(project: Project) {}

    fun getPopupActions(project: Project, actions: MutableList<AnAction>) {}
}