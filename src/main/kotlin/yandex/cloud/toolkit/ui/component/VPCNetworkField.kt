package yandex.cloud.toolkit.ui.component

import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import yandex.cloud.toolkit.api.resource.impl.model.CloudUser
import yandex.cloud.toolkit.api.resource.impl.model.VPCNetwork
import yandex.cloud.toolkit.api.resource.impl.model.VirtualCloudFolder
import yandex.cloud.toolkit.api.service.CloudOperationService
import yandex.cloud.toolkit.util.task.LazyTask
import java.awt.BorderLayout

class VPCNetworkField(
    project: Project,
    val folderId: String?,
    networks: List<VPCNetwork>?,
    boxPosition: String = BorderLayout.EAST
) : TextFieldWithLazyOptions<VPCNetwork>(project, networks, boxPosition) {

    override fun getOptionValue(option: VPCNetwork): String = option.id

    override fun decorateOption(option: VPCNetwork, cell: SimpleColoredComponent) {
        cell.icon = option.descriptor.icon
        cell.append(option.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
    }

    override fun canLoadOptions(): Boolean = folderId != null

    override fun loadOptions(user: CloudUser): LazyTask<List<VPCNetwork>> {
        val folder = VirtualCloudFolder(user, folderId ?: "")
        return CloudOperationService.instance.fetchVPCNetworks(project, folder)
    }
}