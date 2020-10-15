package yandex.cloud.toolkit.api.resource.impl.model

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.ui.treeStructure.SimpleNode
import icons.CloudIcons
import yandex.cloud.api.serverless.apigateway.v1.Apigateway
import yandex.cloud.toolkit.api.resource.*
import yandex.cloud.toolkit.api.resource.impl.*
import yandex.cloud.toolkit.ui.action.CreateOrEditApiGatewayAction
import yandex.cloud.toolkit.ui.action.DeleteApiGatewayAction
import yandex.cloud.toolkit.ui.action.ShowApiGatewaySpecificationAction
import yandex.cloud.toolkit.ui.explorer.CloudExplorerContext
import yandex.cloud.toolkit.ui.node.DummySimpleNode
import java.util.*

class CloudGatewayGroup(val folder: CloudFolder) :
    SimpleHierarchicalCloudResource<CloudGatewayGroup, CloudApiGateway>(), FolderGroup {
    override val id: String get() = "${folder.id}-gateways"
    override val name: String = "API Gateways"
    override val parent: HierarchicalCloudResource get() = folder
    override val descriptor: CloudResourceDescriptor get() = Descriptor
    override val isTrueCloudResource: Boolean get() = false

    override val mainDependency get() = ApiGateways
    override val mainDependencyName: String get() = descriptor.name
    override val mainDependencyLoader get() = CloudApiGatewaysLoader

    override val emptyNode: SimpleNode get() = DummySimpleNode("No API Gateways")

    val gateways: List<CloudApiGateway>? get() = ApiGateways.of(this).get()

    override fun getExplorerPopupActions(context: CloudExplorerContext, actions: MutableList<AnAction>) {
        actions += CreateOrEditApiGatewayAction(folder, null)
    }

    override fun getPath(innerPath: CloudResourcePath?): CloudResourcePath = folder.getGroupPath(this, innerPath)
    override fun getLocation(joiner: StringJoiner) = folder.getLocation(joiner)

    object ApiGateways : CloudDependency<CloudGatewayGroup, List<CloudApiGateway>>()

    object Descriptor : CloudResourceDescriptor(
        "api-gateways",
        "API Gateways",
        icon = null,
        isPinnable = true
    )
}

class CloudApiGateway(
    val data: Apigateway.ApiGateway,
    val group: CloudGatewayGroup
) : HierarchicalCloudResource() {
    override val id: String get() = data.id
    override val name: String get() = data.name
    override val parent: HierarchicalCloudResource get() = group
    override val descriptor: CloudResourceDescriptor get() = Descriptor

    val fullName: String get() = if (group.folder.isVirtual) name else "${group.folder.fullName}/$name"

    override fun getExplorerPopupActions(context: CloudExplorerContext, actions: MutableList<AnAction>) {
        actions += ShowApiGatewaySpecificationAction(this)
        actions += CreateOrEditApiGatewayAction(group.folder, this)
        actions += DeleteApiGatewayAction(this)
    }

    override fun getPath(innerPath: CloudResourcePath?): CloudResourcePath = group.getChildPath(this, innerPath)

    object Descriptor : CloudResourceDescriptor(
        "api-gateway",
        "API Gateway",
        icon = CloudIcons.Resources.ApiGateway,
        isPinnable = true
    )
}

class CloudApiGatewaySpec(
    val name: String,
    val description: String?,
    val labels: Map<String, String>,
    val openapiSpec: String
)

class CloudApiGatewayUpdate(
    val name: String? = null,
    val description: String? = null,
    val labels: Map<String, String>? = null,
    val openapiSpec: String? = null
) {
    val isEmpty: Boolean get() = (name ?: description ?: labels ?: openapiSpec) == null
}