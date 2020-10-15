package yandex.cloud.toolkit.api.resource

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import yandex.cloud.toolkit.api.resource.impl.CloudResourceDescriptor
import yandex.cloud.toolkit.util.Maybe
import yandex.cloud.toolkit.util.remote.resource.RemoteResource
import java.util.concurrent.Future

interface CloudResourceStorage {

    fun getDescriptor(type: String): CloudResourceDescriptor?

    fun <P : CloudResource, R> getResource(depres: DependentResource<P, R>): RemoteResource<R>?

    fun updateResources(result: CloudDependencyLoadingResult)

    fun <P : CloudResource, R> loadDependencyAsync(
        context: CloudDependencyLoadingContext,
        parent: P,
        dependency: CloudDependency<P, R>,
        loader: CloudDependencyLoader<P, R>
    ): Future<Maybe<R>>

    fun <P : CloudResource, R> loadDependencySync(
        project: Project,
        parent: P,
        dependency: CloudDependency<P, R>,
        loader: CloudDependencyLoader<P, R>,
        showErrorNotifications: Boolean = true
    ): Maybe<R>

    fun registerListener(listener: CloudResourceListener)
    fun unregisterListener(listener: CloudResourceListener)

    companion object {
        val instance: CloudResourceStorage get() = ServiceManager.getService(CloudResourceStorage::class.java)
    }
}