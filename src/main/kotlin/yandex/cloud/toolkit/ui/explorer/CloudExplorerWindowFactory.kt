package yandex.cloud.toolkit.ui.explorer

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import yandex.cloud.toolkit.api.explorer.cloudExplorer

internal class CloudExplorerWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentManager = toolWindow.contentManager
        val explorer = project.cloudExplorer
        val content = explorer.createToolWindowContent(toolWindow)
        contentManager.addContent(content)
    }
}