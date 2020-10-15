package yandex.cloud.toolkit.api.explorer

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.content.Content
import com.intellij.ui.treeStructure.SimpleNode
import yandex.cloud.toolkit.api.profile.CloudProfile
import yandex.cloud.toolkit.api.profile.ProfileStorageListener
import yandex.cloud.toolkit.api.profile.impl.profileStorage
import yandex.cloud.toolkit.api.resource.CloudResourceListener
import yandex.cloud.toolkit.api.resource.CloudResourcePath
import yandex.cloud.toolkit.api.resource.CloudResourceStorage
import yandex.cloud.toolkit.api.resource.CloudResourceUpdateEvent
import yandex.cloud.toolkit.api.resource.impl.HierarchicalCloudResource
import yandex.cloud.toolkit.api.resource.impl.model.CloudUser
import yandex.cloud.toolkit.api.resource.impl.user
import yandex.cloud.toolkit.ui.explorer.CloudExplorerPanel
import yandex.cloud.toolkit.ui.explorer.CloudExplorerPath
import yandex.cloud.toolkit.ui.explorer.CloudExplorerRootNode
import yandex.cloud.toolkit.ui.node.PinnedResourcesNode
import yandex.cloud.toolkit.util.Maybe
import yandex.cloud.toolkit.util.asJust
import yandex.cloud.toolkit.util.getOrNull
import yandex.cloud.toolkit.util.noResource
import yandex.cloud.toolkit.util.remote.resource.PresentableResourceStatus
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicReference

@Service
@State(name = "Yandex.Cloud Toolkit Explorer", storages = [(Storage("yandexCloudExplorer.xml"))])
class CloudExplorer(val project: Project) : CloudResourceListener, ProfileStorageListener, Disposable,
    PersistentStateComponent<CloudExplorer.State> {

    private val pins: ConcurrentMap<CloudResourcePath, PinnedResource> = ConcurrentHashMap()

    private val profileStorage = project.profileStorage
    private val panel = AtomicReference<CloudExplorerPanel>()

    val user: Maybe<CloudUser>
        get() = profileStorage.profile?.resourceUser?.asJust() ?: noResource(
            "No profile selected",
            PresentableResourceStatus.Unauthenticated
        )

    val rootPath = CloudExplorerPath(this)
    val userNode: CloudResourceNode? get() = user.getOrNull()?.getNode(rootPath)
    val rootNode = CloudExplorerRootNode(this)
    val pinnedGroupNode = PinnedResourcesNode(this)

    val nodeStorage = CloudExplorerNodeStorage(this)

    init {
        CloudResourceStorage.instance.registerListener(this)
        project.profileStorage.registerListener(this)
    }

    override fun onCloudResourcesUpdate(events: List<CloudResourceUpdateEvent>) {
        val user = user.getOrNull() ?: return
        val localUpdatedNodes = events.filter { event ->
            (event.resource as? HierarchicalCloudResource)?.user === user
        }

        if (localUpdatedNodes.isNotEmpty()) panel.get()?.onCloudResourcesUpdate(localUpdatedNodes)
    }

    override fun onProfileSelected(profile: CloudProfile?) {
        nodeStorage.clear()
        pins.values.forEach(PinnedResource::reset)
        panel.get()?.onProfileSelected(profile)
    }

    override fun onProfileAuthDataUpdated(profile: CloudProfile) {
        userNode?.onRefresh(project)
    }

    override fun onProfileRenamed(profile: CloudProfile) {
        panel.get()?.onProfileRenamed()
    }

    fun pinResource(resource: HierarchicalCloudResource) {
        val wasEmpty = pins.isEmpty()
        val pinned = PinnedResource(resource)
        pins[pinned.path] = pinned
        updatePinList(updateGroup = wasEmpty)
    }

    fun unpinResource(resource: PinnedResource) {
        val isSinglePinned = pins.size <= 1
        pins.remove(resource.path)
        updatePinList(updateGroup = isSinglePinned)
    }

    fun unpinAll(){
        pins.clear()
        updatePinList(updateGroup = true)
    }

    fun updatePinList(updateGroup: Boolean, newNode: SimpleNode? = null) {
        panel.get()?.onPinListChanged(updateGroup, newNode)
    }

    fun updatePinNode(pinNode: UnresolvedPinnedResourceNode) {
        panel.get()?.onPinNodeChanged(pinNode)
    }

    fun hasPinned() : Boolean = pins.isNotEmpty()

    fun getPinnedResources(): Collection<PinnedResource> = pins.values

    fun getPinned(path: CloudResourcePath): PinnedResource? = pins[path]

    fun createToolWindowContent(toolWindow: ToolWindow): Content {
        if (panel.get() == null && !panel.compareAndSet(null, CloudExplorerPanel(this))) {
            throw IllegalStateException("Can not initialize explorer twice at the same time")
        }
        return toolWindow.contentManager.factory.createContent(panel.get(), null, false)
    }

    override fun dispose() {
        CloudResourceStorage.instance.unregisterListener(this)
        project.profileStorage.unregisterListener(this)
        panel.get()?.dispose()
    }

    override fun getState(): State = State().apply {
        pinned = pins.values.map(PinnedResource::getSaveData).toMutableList()
    }

    override fun loadState(state: State) {
        pins.clear()
        for (pinned in state.pinned.mapNotNull(PinnedResource::fromData)) {
            pins[pinned.path] = pinned
        }
    }

    class State : BaseState() {
        var pinned by list<PinnedResourceData>()
    }
}

val Project.cloudExplorer: CloudExplorer get() = getService(CloudExplorer::class.java)