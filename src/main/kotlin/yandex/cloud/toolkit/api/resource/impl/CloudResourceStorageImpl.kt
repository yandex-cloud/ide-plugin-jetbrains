package yandex.cloud.toolkit.api.resource.impl

import com.intellij.openapi.project.Project
import yandex.cloud.toolkit.api.resource.*
import yandex.cloud.toolkit.api.resource.impl.model.*
import yandex.cloud.toolkit.util.Maybe
import yandex.cloud.toolkit.util.remote.resource.LoadingResourceState
import yandex.cloud.toolkit.util.remote.resource.RemoteResource
import yandex.cloud.toolkit.util.remote.resource.asLoading
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future
import javax.swing.event.EventListenerList

class CloudResourceStorageImpl : CloudResourceStorage {

    private val storage = DependentResourceStorage()
    private val taskManager = CloudResourceTaskManager(5)

    private val listeners = EventListenerList()

    private val descriptors = mutableMapOf<String, CloudResourceDescriptor>()

    init {
        addResourceDescriptor(CloudUser.Descriptor)
        addResourceDescriptor(Cloud.Descriptor)
        addResourceDescriptor(CloudFolder.Descriptor)
        addResourceDescriptor(CloudFunction.Descriptor)
        addResourceDescriptor(CloudFunctionVersion.Descriptor)
        addResourceDescriptor(CloudServiceAccount.Descriptor)
        addResourceDescriptor(CloudApiGateway.Descriptor)
        addResourceDescriptor(CloudTrigger.Descriptor)
        addResourceDescriptor(CloudFunctionGroup.Descriptor)
        addResourceDescriptor(CloudTriggerGroup.Descriptor)
        addResourceDescriptor(CloudGatewayGroup.Descriptor)
        addResourceDescriptor(CloudServiceAccountGroup.Descriptor)
    }

    private fun addResourceDescriptor(descriptor: CloudResourceDescriptor) {
        descriptors[descriptor.type] = descriptor
    }

    override fun getDescriptor(type: String): CloudResourceDescriptor? = descriptors[type]

    override fun <P : CloudResource, R> getResource(depres: DependentResource<P, R>): RemoteResource<R>? =
        storage[depres]

    override fun updateResources(result: CloudDependencyLoadingResult) {
        val updatedResources = result.entries.filterTo(mutableSetOf()) { updateResourceFromResultEntry(it) }
        val events = updatedResources.map { CloudResourceUpdateEvent(it.parent, true) }
        listeners.getListeners(CloudResourceListener::class.java).forEach { it.onCloudResourcesUpdate(events) }
    }

    private fun <P : CloudResource, R> updateResourceFromResultEntry(entry: CloudDependencyLoadingResult.Entry<P, R>): Boolean {
        val resource = storage.getOrInit(entry.dependency.of(entry.parent))
        resource.updateState { it.fromMaybe(entry.result) }
        return entry.parent.notifyListenersOnDependencyUpdate(entry.dependency)
    }

    private fun <P : CloudResource, R> tryLoadDependency(
        parent: P,
        dependency: CloudDependency<P, R>,
        startTask: () -> Future<Maybe<R>>
    ): Future<Maybe<R>> {
        val depres = dependency.of(parent)
        val resource = storage.getOrInit(depres)

        var loadingStarted = false
        var future: Future<Maybe<R>>

        resource.updateState { state ->
            when (state) {
                is LoadingResourceState -> {
                    future = state.future
                    state
                }
                else -> {
                    loadingStarted = true
                    future = startTask()
                    state.asLoading(future)
                }
            }
        }

        if (loadingStarted && parent.notifyListenersOnDependencyUpdate(dependency)) {
            val event = listOf(CloudResourceUpdateEvent(parent, false))
            listeners.getListeners(CloudResourceListener::class.java).forEach { it.onCloudResourcesUpdate(event) }
        }

        return future
    }

    override fun <P : CloudResource, R> loadDependencyAsync(
        context: CloudDependencyLoadingContext,
        parent: P,
        dependency: CloudDependency<P, R>,
        loader: CloudDependencyLoader<P, R>
    ): Future<Maybe<R>> {
        if (parent.isVirtual) throw IllegalArgumentException("Can not load dependency async for virtual resource")
        return tryLoadDependency(parent, dependency) {
            taskManager.handleAsync(context.project, parent, dependency, loader, context.onSuccess)
        }
    }

    override fun <P : CloudResource, R> loadDependencySync(
        project: Project,
        parent: P,
        dependency: CloudDependency<P, R>,
        loader: CloudDependencyLoader<P, R>,
        showErrorNotifications: Boolean
    ): Maybe<R> {
        if (parent.isVirtual) {
            val depres = dependency.of(parent)
            val (result, _) = taskManager.performLoad(project, parent, dependency, loader, showErrorNotifications)
            return result[depres] ?: throw IllegalStateException("Missing loaded dependency")
        }

        return tryLoadDependency(parent, dependency) {
            taskManager.handleSync(project, parent, dependency, loader, showErrorNotifications)
        }.get()
    }

    override fun registerListener(listener: CloudResourceListener) {
        listeners.add(CloudResourceListener::class.java, listener)
    }

    override fun unregisterListener(listener: CloudResourceListener) {
        listeners.remove(CloudResourceListener::class.java, listener)
    }
}

class DependentResourceStorage {

    private val resources = ConcurrentHashMap<DependentResource<*, *>, RemoteResource<*>>()

    @Suppress("UNCHECKED_CAST")
    operator fun <P : CloudResource, R> get(depres: DependentResource<P, R>): RemoteResource<R>? =
        resources[depres] as RemoteResource<R>?

    @Suppress("UNCHECKED_CAST")
    fun <P : CloudResource, R> getOrInit(depres: DependentResource<P, R>): RemoteResource<R> =
        resources.getOrPut(depres) { RemoteResource<R>() } as RemoteResource<R>
}