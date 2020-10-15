package yandex.cloud.toolkit.api.resource.impl.model

import com.intellij.ui.treeStructure.SimpleNode
import icons.CloudIcons
import yandex.cloud.api.serverless.triggers.v1.TriggerOuterClass
import yandex.cloud.toolkit.api.resource.*
import yandex.cloud.toolkit.api.resource.impl.*
import yandex.cloud.toolkit.ui.node.DummySimpleNode
import java.util.*

class CloudTriggerGroup(val folder: CloudFolder) : SimpleHierarchicalCloudResource<CloudTriggerGroup, CloudTrigger>(), FolderGroup {
    override val id: String get() = "${folder.id}-triggers"
    override val name: String = "Triggers"
    override val parent: HierarchicalCloudResource get() = folder
    override val descriptor: CloudResourceDescriptor get() = Descriptor
    override val isTrueCloudResource: Boolean get() = false

    override val mainDependency get() = Triggers
    override val mainDependencyName: String get() = descriptor.name
    override val mainDependencyLoader get() = CloudTriggersLoader

    override val emptyNode: SimpleNode get() = DummySimpleNode("No Triggers")

    val triggers: List<CloudTrigger>? get() = Triggers.of(this).get()

    override fun getPath(innerPath: CloudResourcePath?): CloudResourcePath = folder.getGroupPath(this, innerPath)
    override fun getLocation(joiner: StringJoiner) = folder.getLocation(joiner)

    object Triggers : CloudDependency<CloudTriggerGroup, List<CloudTrigger>>()

    object Descriptor : CloudResourceDescriptor(
        "triggers",
        "Triggers",
        icon = null,
        isPinnable = true
    )
}

class CloudTrigger(
    val data: TriggerOuterClass.Trigger,
    val group: CloudTriggerGroup
) : HierarchicalCloudResource() {
    override val id: String get() = data.id
    override val name: String get() = data.name
    override val parent: HierarchicalCloudResource get() = group
    override val descriptor: CloudResourceDescriptor get() = Descriptor

    override fun getPath(innerPath: CloudResourcePath?): CloudResourcePath = group.getChildPath(this, innerPath)

    object Descriptor : CloudResourceDescriptor(
        "trigger",
        "Trigger",
        icon = CloudIcons.Resources.Trigger,
        isPinnable = true
    )
}