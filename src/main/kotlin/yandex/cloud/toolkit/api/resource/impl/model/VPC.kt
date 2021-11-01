package yandex.cloud.toolkit.api.resource.impl.model

import com.intellij.ui.treeStructure.SimpleNode
import icons.CloudIcons
import yandex.cloud.api.vpc.v1.NetworkOuterClass
import yandex.cloud.api.vpc.v1.SubnetOuterClass
import yandex.cloud.toolkit.api.resource.CloudDependency
import yandex.cloud.toolkit.api.resource.CloudResourcePath
import yandex.cloud.toolkit.api.resource.get
import yandex.cloud.toolkit.api.resource.impl.*
import yandex.cloud.toolkit.api.resource.of
import yandex.cloud.toolkit.ui.node.DummySimpleNode
import yandex.cloud.toolkit.util.remote.resource.CloudResourceStatusZone
import java.util.*

class VPCNetworkGroup(val folder: CloudFolder) :
    SimpleHierarchicalCloudResource<VPCNetworkGroup, VPCNetwork>(), FolderGroup {
    override val id: String get() = "${folder.id}-vpc-networks"
    override val name: String = "VPC"
    override val parent: HierarchicalCloudResource get() = folder
    override val descriptor: CloudResourceDescriptor get() = Descriptor
    override val isTrueCloudResource: Boolean get() = false

    override val mainDependency get() = Networks
    override val mainDependencyName: String get() = descriptor.type
    override val mainDependencyLoader get() = VPCNetworksLoader

    override val emptyNode: SimpleNode get() = DummySimpleNode("No Networks")

    val networks: List<VPCNetwork>? get() = Networks.of(this).get()

    override fun getPath(innerPath: CloudResourcePath?): CloudResourcePath = folder.getGroupPath(this, innerPath)
    override fun getLocation(joiner: StringJoiner) = folder.getLocation(joiner)

    object Networks : CloudDependency<VPCNetworkGroup, List<VPCNetwork>>()

    object Descriptor : CloudResourceDescriptor(
        "vpc-networks",
        "VPC",
        icon = null,
        isPinnable = true
    )
}

class VPCNetwork(
    val data: NetworkOuterClass.Network,
    val group: VPCNetworkGroup
) : SimpleHierarchicalCloudResource<VPCNetwork, VPCSubnet>() {

    override val id: String get() = data.id
    override val name: String get() = data.name
    override val parent: HierarchicalCloudResource get() = group
    override val descriptor: CloudResourceDescriptor get() = Descriptor

    override val mainDependency get() = Subnets
    override val mainDependencyName: String get() = "subnets"
    override val mainDependencyLoader get() = VPCSubnetsLoader

    override val emptyNode: SimpleNode get() = DummySimpleNode("No Subnets")

    val fullName: String get() = if (group.folder.isVirtual) name else "${group.folder.fullName}/$name"

    val subnets: List<VPCSubnet>? get() = Subnets.of(this).get()

    override fun getPath(innerPath: CloudResourcePath?): CloudResourcePath = group.getChildPath(this, innerPath)

    object Subnets : CloudDependency<VPCNetwork, List<VPCSubnet>>()

    object Descriptor : CloudResourceDescriptor(
        "vpc-network",
        "VPC Network",
        icon = CloudIcons.Resources.Network,
        isPinnable = true
    )
}

class VPCSubnet(
    val data: SubnetOuterClass.Subnet,
    val network: VPCNetwork
) : HierarchicalCloudResource() {

    override val id: String get() = data.id
    override val name: String get() = data.name
    override val parent: HierarchicalCloudResource get() = network
    override val descriptor: CloudResourceDescriptor get() = Descriptor

    override val status = if (!name.contains(data.zoneId)) CloudResourceStatusZone(data.zoneId) else null

    val fullName: String get() = "${network.fullName}/$id"

    override fun getPath(innerPath: CloudResourcePath?): CloudResourcePath = network.getChildPath(this, innerPath)

    object Descriptor : CloudResourceDescriptor(
        "vpc-subnet",
        "VPC Subnet",
        CloudIcons.Resources.Subnet
    )
}