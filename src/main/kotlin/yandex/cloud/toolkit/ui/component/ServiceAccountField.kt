package yandex.cloud.toolkit.ui.component

import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import yandex.cloud.toolkit.api.resource.impl.model.CloudServiceAccount
import yandex.cloud.toolkit.api.resource.impl.model.CloudUser
import yandex.cloud.toolkit.api.resource.impl.model.VirtualCloudFolder
import yandex.cloud.toolkit.api.service.CloudOperationService
import yandex.cloud.toolkit.util.task.LazyTask
import java.awt.BorderLayout

class ServiceAccountField(
    project: Project,
    val folderId: String?,
    accounts: List<CloudServiceAccount>?,
    boxPosition: String = BorderLayout.EAST
) : TextFieldWithLazyOptions<CloudServiceAccount>(project, accounts, boxPosition) {

    override fun getOptionValue(option: CloudServiceAccount): String = option.id

    override fun decorateOption(option: CloudServiceAccount, cell: SimpleColoredComponent) {
        cell.icon = option.descriptor.icon
        cell.append(option.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
    }

    override fun canLoadOptions(): Boolean = folderId != null

    override fun loadOptions(user: CloudUser): LazyTask<List<CloudServiceAccount>> {
        val folder = VirtualCloudFolder(user, folderId ?: "")
        return CloudOperationService.instance.fetchServiceAccounts(project, folder)
    }
}