package yandex.cloud.toolkit.ui.node

import com.intellij.openapi.project.Project

interface RefreshTreeNode {

    fun canBeRefreshed(): Boolean
    fun onRefresh(project: Project)
}