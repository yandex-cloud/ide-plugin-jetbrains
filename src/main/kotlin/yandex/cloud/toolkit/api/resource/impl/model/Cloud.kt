package yandex.cloud.toolkit.api.resource.impl.model

import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.ui.treeStructure.SimpleNode
import icons.CloudIcons
import yandex.cloud.api.iam.v1.RoleOuterClass
import yandex.cloud.api.resourcemanager.v1.CloudOuterClass
import yandex.cloud.toolkit.api.auth.CloudAuthData
import yandex.cloud.toolkit.api.profile.CloudProfile
import yandex.cloud.toolkit.api.resource.*
import yandex.cloud.toolkit.api.resource.impl.*
import yandex.cloud.toolkit.ui.action.ChangeProfileAction
import yandex.cloud.toolkit.ui.action.CloseProfileAction
import yandex.cloud.toolkit.ui.explorer.CloudExplorerContext
import yandex.cloud.toolkit.ui.node.DummySimpleNode
import yandex.cloud.toolkit.ui.node.FixProfileNode
import yandex.cloud.toolkit.ui.node.ManageProfilesNode
import yandex.cloud.toolkit.util.remote.resource.PresentableResourceStatus
import java.util.*
import javax.swing.Icon

class CloudUser(val profile: CloudProfile) : SimpleHierarchicalCloudResource<CloudUser, Cloud>() {
    override val id = profile.id.toString()
    override val name get() = profile.displayName
    override val parent: HierarchicalCloudResource get() = this
    override val isRoot: Boolean get() = true
    override val descriptor: CloudResourceDescriptor get() = Descriptor
    override val isTrueCloudResource: Boolean get() = false

    override val mainDependency get() = Clouds
    override val mainDependencyName: String get() = "clouds"
    override val mainDependencyLoader get() = CloudsLoader

    val authData: CloudAuthData? get() = profile.getAuthData(toUse = true)
    override val status: PresentableResourceStatus? get() = profile.getAuthDataStatus() ?: super.status
    override val emptyNode: SimpleNode get() = DummySimpleNode("No Clouds")

    val clouds: List<Cloud>? get() = Clouds.of(this).get()
    val dummyFolder: CloudFolder = VirtualCloudFolder(this, "${id}_dummy_folder")

    override fun getNodeIcon(context: CloudExplorerContext): Icon = profile.icon

    override fun getChildNodes(context: CloudExplorerContext): List<SimpleNode>? {
        if (authData == null) {
            return listOf(ManageProfilesNode())
        }

        val error = Clouds.of(this).instance?.error
        if (error != null && error.status === PresentableResourceStatus.Unauthenticated) {
            return listOf(FixProfileNode(profile, error), ManageProfilesNode())
        }
        return super.getChildNodes(context)
    }

    override fun getExplorerPopupActions(context: CloudExplorerContext, actions: MutableList<AnAction>) {
        actions += ChangeProfileAction(context.project)
        actions += CloseProfileAction(context.project)
    }

    override fun getParentNode(context: CloudExplorerContext): NodeDescriptor<*> = context.explorer.rootNode
    override fun getPath(innerPath: CloudResourcePath?): CloudResourcePath = innerPath ?: CloudResourcePath("", id, innerPath)
    override fun getLocation(joiner: StringJoiner) {}

    object Clouds : CloudDependency<CloudUser, List<Cloud>>()

    object AvailableRuntimes : CloudDependency<CloudUser, List<String>>()
    object AvailableRoles : CloudDependency<CloudUser, List<RoleOuterClass.Role>>()

    object Descriptor : CloudResourceDescriptor(
        "user",
        "User",
         CloudIcons.Resources.CloudUser
    )
}

class Cloud(
    val data: CloudOuterClass.Cloud,
    val user: CloudUser
) : SimpleHierarchicalCloudResource<Cloud, CloudFolder>() {
    override val id: String get() = data.id
    override val name: String get() = data.name
    override val parent: HierarchicalCloudResource get() = user
    override val descriptor: CloudResourceDescriptor get() = Descriptor

    override val mainDependency get() = Folders
    override val mainDependencyName: String get() = "folders"
    override val mainDependencyLoader get() = CloudFoldersLoader

    override val emptyNode: SimpleNode get() = DummySimpleNode("No Folders")

    val folders: List<CloudFolder>? get() = Folders.of(this).get()

    override fun getPath(innerPath: CloudResourcePath?): CloudResourcePath = user.getChildPath(this, innerPath)

    object CloudData : CloudDependency<Cloud, CloudOuterClass.Cloud>()
    object Folders : CloudDependency<Cloud, List<CloudFolder>>()

    object Descriptor : CloudResourceDescriptor(
        "cloud",
        "Cloud",
        icon = CloudIcons.Resources.Cloud,
        isPinnable = true
    )
}