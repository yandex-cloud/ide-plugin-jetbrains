package yandex.cloud.toolkit.ui.explorer

import com.intellij.openapi.project.Project
import yandex.cloud.toolkit.api.explorer.CloudExplorer
import yandex.cloud.toolkit.api.explorer.CloudResourceNode
import yandex.cloud.toolkit.api.resource.impl.HierarchicalCloudResource

data class CloudExplorerPath(
    val explorer: CloudExplorer,
    val branch: String = "root",
    val pinnedId: String? = null
) {

    val project : Project get() = explorer.project

    fun addBranch(pinned: HierarchicalCloudResource? = null): CloudExplorerPath {
        val branch = when (pinned) {
            null -> branch
            else -> "${branch}_${pinned.id}"
        }
        return CloudExplorerPath(explorer, branch, pinned?.id ?: pinnedId)
    }

    fun getNode(resource: HierarchicalCloudResource): CloudResourceNode = explorer.nodeStorage.getNode(resource, this)
}