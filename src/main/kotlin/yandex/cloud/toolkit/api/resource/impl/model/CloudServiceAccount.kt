package yandex.cloud.toolkit.api.resource.impl.model

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.ui.treeStructure.SimpleNode
import icons.CloudIcons
import yandex.cloud.api.iam.v1.ServiceAccountOuterClass
import yandex.cloud.toolkit.api.resource.*
import yandex.cloud.toolkit.api.resource.impl.*
import yandex.cloud.toolkit.ui.action.CreateOrEditServiceAccountAction
import yandex.cloud.toolkit.ui.action.DeleteServiceAccountAction
import yandex.cloud.toolkit.ui.explorer.CloudExplorerContext
import yandex.cloud.toolkit.ui.node.DummySimpleNode
import java.util.*

class CloudServiceAccountGroup(val folder: CloudFolder) : SimpleHierarchicalCloudResource<CloudServiceAccountGroup, CloudServiceAccount>(), FolderGroup {
    override val id: String get() = "${folder.id}-service-accounts"
    override val name: String = "Service Accounts"
    override val parent: HierarchicalCloudResource get() = folder
    override val firstExpansionDepth: Int get() = 1
    override val descriptor: CloudResourceDescriptor get() = Descriptor
    override val isTrueCloudResource: Boolean get() = false

    override val mainDependency get() = ServiceAccounts
    override val mainDependencyName: String get() = descriptor.type
    override val mainDependencyLoader get() = CloudServiceAccountsLoader

    override val emptyNode: SimpleNode get() = DummySimpleNode("No Service Accounts")

    val accounts: List<CloudServiceAccount>? get() = ServiceAccounts.of(this).get()

    override fun getPath(innerPath: CloudResourcePath?): CloudResourcePath = folder.getGroupPath(this, innerPath)
    override fun getLocation(joiner: StringJoiner) = folder.getLocation(joiner)

    object ServiceAccounts : CloudDependency<CloudServiceAccountGroup, List<CloudServiceAccount>>()

    override fun getExplorerPopupActions(context: CloudExplorerContext, actions: MutableList<AnAction>) {
        actions += CreateOrEditServiceAccountAction(folder, null)
    }

    object Descriptor : CloudResourceDescriptor(
        "service-accounts",
        "Service Accounts",
        icon = null,
        isPinnable = true
    )
}

class CloudServiceAccount(
    val data: ServiceAccountOuterClass.ServiceAccount,
    val group: CloudServiceAccountGroup
) : HierarchicalCloudResource() {
    override val id: String get() = data.id
    override val name: String get() = data.name
    override val parent: HierarchicalCloudResource get() = group
    override val descriptor: CloudResourceDescriptor get() = Descriptor

    override fun getExplorerPopupActions(context: CloudExplorerContext, actions: MutableList<AnAction>) {
        actions += CreateOrEditServiceAccountAction(group.folder, this)
        actions += DeleteServiceAccountAction(this)
    }

    override fun getPath(innerPath: CloudResourcePath?): CloudResourcePath = group.getChildPath(this, innerPath)

    object Descriptor : CloudResourceDescriptor(
        "service-account",
        "Service Account",
        icon = CloudIcons.Resources.ServiceAccount,
        isPinnable = true
    )
}

class CloudServiceAccountSpec(
    val name: String,
    val description: String?,
    val roles: Set<String>
)

class CloudServiceAccountUpdate(
    val name: String?,
    val description: String?
) {
    val isEmpty: Boolean get() = (name ?: description) == null
}