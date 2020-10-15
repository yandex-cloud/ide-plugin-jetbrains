package yandex.cloud.toolkit.ui.explorer

import com.intellij.openapi.project.Project
import yandex.cloud.toolkit.api.explorer.CloudExplorer

class CloudExplorerContext(val explorer: CloudExplorer, val path: CloudExplorerPath) {

    val pinnedId: String? get() = path.pinnedId
    val project: Project get() = explorer.project
}