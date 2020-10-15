package yandex.cloud.toolkit.ui.node

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.treeStructure.SimpleNode
import icons.CloudIcons
import yandex.cloud.toolkit.api.resource.impl.model.CloudFunctionScalingPolicy

class ScalingPolicyNode(val parentNode: SimpleNode, val scalingPolicy: CloudFunctionScalingPolicy) : SimpleNode() {

    override fun getParentDescriptor(): NodeDescriptor<*> = parentNode

    override fun getChildren(): Array<SimpleNode> = arrayOf(
        ScalingPolicyPropertyNode(
            this, "Zone Instances Limit",
            { scalingPolicy.zoneInstancesLimit }, { scalingPolicy.zoneInstancesLimit = it },
        ),
        ScalingPolicyPropertyNode(
            this, "Zone Requests Limit",
            { scalingPolicy.zoneRequestsLimit }, { scalingPolicy.zoneRequestsLimit = it },
        )
    )

    override fun update(presentation: PresentationData) {
        presentation.setIcon(CloudIcons.Nodes.Label)
        presentation.clearText()
        presentation.addText(scalingPolicy.tag, SimpleTextAttributes.REGULAR_ATTRIBUTES)
    }
}

class ScalingPolicyPropertyNode(
    val policyNode: ScalingPolicyNode,
    val propertyName: String,
    val propertyGetter: () -> Long,
    val propertySetter: (Long) -> Unit
) : SimpleNode() {

    override fun getParentDescriptor(): NodeDescriptor<*> = policyNode
    override fun getChildren(): Array<SimpleNode> = emptyArray()
    override fun isAlwaysLeaf(): Boolean = true

    override fun update(presentation: PresentationData) {
        presentation.setIcon(AllIcons.Nodes.Property)
        presentation.clearText()
        presentation.addText("$propertyName: ", SimpleTextAttributes.REGULAR_ATTRIBUTES)
        val value = propertyGetter()
        presentation.addText(
            value.toString(),
            if (value == 0L) SimpleTextAttributes.GRAY_ATTRIBUTES else SimpleTextAttributes.SYNTHETIC_ATTRIBUTES
        )
    }
}