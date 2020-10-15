package yandex.cloud.toolkit.ui.node

import com.intellij.ide.projectView.PresentationData
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.treeStructure.SimpleNode

open class DummySimpleNode(val displayName: String) : SimpleNode() {

    override fun getChildren(): Array<SimpleNode> = emptyArray()

    override fun update(presentation: PresentationData) {
        presentation.addText(displayName, SimpleTextAttributes.GRAYED_ATTRIBUTES)
    }
}

object EmptySimpleNode : DummySimpleNode("Empty")
