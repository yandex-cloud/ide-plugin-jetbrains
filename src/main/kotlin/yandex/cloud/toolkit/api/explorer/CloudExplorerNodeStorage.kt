package yandex.cloud.toolkit.api.explorer

import yandex.cloud.toolkit.api.resource.impl.HierarchicalCloudResource
import yandex.cloud.toolkit.ui.explorer.CloudExplorerPath
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

class CloudExplorerNodeStorage(val explorer: CloudExplorer) {

    private val resourceNodes = ConcurrentHashMap<String, BranchedNodes>()

    fun getNode(resource: HierarchicalCloudResource, path: CloudExplorerPath): CloudResourceNode =
        resourceNodes.getOrPut(resource.id) { BranchedNodes() }.getNodeAtPath(resource, path)

    fun getNodes(resource: HierarchicalCloudResource): Collection<CloudResourceNode> =
        resourceNodes.getOrPut(resource.id) { BranchedNodes() }.getAllNodes()

    private class BranchedNodes {

        private val nodes = ConcurrentHashMap<String, NodeContainer>()

        fun getNodeAtPath(resource: HierarchicalCloudResource, path: CloudExplorerPath): CloudResourceNode =
            nodes.getOrPut(path.branch) { NodeContainer(resource, path) }.getNodeForInstance(resource, path)

        fun getAllNodes(): Collection<CloudResourceNode> = nodes.values.map { it.getNode() }
    }

    private class NodeContainer(resource: HierarchicalCloudResource, path: CloudExplorerPath) {

        private val node = AtomicReference(CloudResourceNode(path.explorer, path, resource))

        fun getNodeForInstance(resource: HierarchicalCloudResource, path: CloudExplorerPath): CloudResourceNode {
            val node = node.get()
            while (node.resource.version < resource.version) {
                val newNode = CloudResourceNode(path.explorer, path, resource)
                if (this.node.compareAndSet(node, newNode)) {
                    return newNode
                }
            }
            return node
        }

        fun getNode(): CloudResourceNode = node.get()
    }

    fun clear() {
        resourceNodes.clear()
    }
}