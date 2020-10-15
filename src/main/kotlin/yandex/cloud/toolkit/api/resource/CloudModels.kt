package yandex.cloud.toolkit.api.resource

import yandex.cloud.toolkit.util.remote.resource.RemoteResource

interface CloudResource {

    val userId: String

    val id: String
    val name: String

    val isVirtual: Boolean
    val isTrueCloudResource : Boolean

    fun notifyListenersOnDependencyUpdate(dependency: CloudDependency<*, *>): Boolean
}

open class CloudDependency<P : CloudResource, R>

data class DependentResource<P : CloudResource, R>(
    val userId: String,
    val parentId: String,
    val dependency: CloudDependency<P, R>
)

typealias SelfReference<R> = DependentResource<R, R>

fun <P : CloudResource, R> CloudDependency<P, R>.of(parent: CloudResource): DependentResource<P, R> =
    DependentResource(parent.userId, parent.id, this)

fun <P : CloudResource, R> CloudDependency<P, R>.of(userId: String, parentId: String): DependentResource<P, R> =
    DependentResource(userId, parentId, this)

val <P : CloudResource, R> DependentResource<P, R>.instance: RemoteResource<R>?
    get() = CloudResourceStorage.instance.getResource(this)

fun <P : CloudResource, R> DependentResource<P, R>.get(ifSuccess: Boolean = false): R? = when {
    ifSuccess -> instance?.loadedValue
    else -> instance?.value
}

fun <R : CloudResource> List<R>.findById(id: String?): R? = when {
    id != null -> find { it.id == id }
    else -> null
}