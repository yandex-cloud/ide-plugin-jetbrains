package yandex.cloud.toolkit.ui.component

import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import yandex.cloud.api.vpc.v1.SubnetOuterClass
import yandex.cloud.toolkit.api.resource.impl.model.CloudUser
import yandex.cloud.toolkit.api.resource.impl.model.VPCSubnet
import yandex.cloud.toolkit.api.service.CloudOperationService
import yandex.cloud.toolkit.util.task.LazyTask
import java.awt.BorderLayout

class VPCSubnetField(
    project: Project,
    val folderId: String?,
    subnets: List<SubnetOuterClass.Subnet>?,
    boxPosition: String = BorderLayout.EAST,
    label: String? = null
) : TextFieldWithLazyOptions<SubnetOuterClass.Subnet>(project, subnets, boxPosition, label) {

    override fun getOptionValue(option: SubnetOuterClass.Subnet): String = option.id

    override fun decorateOption(option: SubnetOuterClass.Subnet, cell: SimpleColoredComponent) {
        cell.icon = VPCSubnet.Descriptor.icon
        cell.append(option.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
        if (!option.name.contains(option.zoneId)) {
            cell.append(" ${option.zoneId}", SimpleTextAttributes.GRAY_SMALL_ATTRIBUTES)
        }
    }

    override fun canLoadOptions(): Boolean = folderId != null

    override fun loadOptions(user: CloudUser): LazyTask<List<SubnetOuterClass.Subnet>> {
        return CloudOperationService.instance.fetchFolderSubnets(user, folderId!!)
    }
}