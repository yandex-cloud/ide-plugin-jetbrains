package yandex.cloud.toolkit.api.resource

import com.intellij.openapi.project.Project
import yandex.cloud.toolkit.util.*
import yandex.cloud.toolkit.util.remote.resource.ResourceLoadingError
import yandex.cloud.toolkit.util.task.LazyTask
import yandex.cloud.toolkit.util.task.doLazy

interface CloudDependencyLoader<P : CloudResource, R> {

    fun load(parent: P, result: CloudDependencyLoadingResult)
}

class CloudDependencyLoadingContext(
    val project: Project,
    val onSuccess: (() -> Unit)?
)

class CloudDependencyLoadingResult {

    private val results = mutableMapOf<DependentResource<*, *>, Entry<*, *>>()
    val entries: List<Entry<*, *>> get() = results.entries.map { it.value }

    @Suppress("UNCHECKED_CAST")
    operator fun <P : CloudResource, R> get(depres: DependentResource<P, R>): Maybe<R>? =
        results[depres]?.result as Maybe<R>?

    fun <P : CloudResource, R> put(parent: P, dependency: CloudDependency<P, R>, result: R) {
        results[dependency.of(parent)] = Entry(parent, dependency, JustValue(result))
    }

    fun <P : CloudResource, R> put(parent: P, dependency: CloudDependency<P, R>, error: ResourceLoadingError) {
        results[dependency.of(parent)] = Entry(parent, dependency, NoValue(error))
    }

    class Entry<P : CloudResource, R>(
        val parent: P,
        val dependency: CloudDependency<P, R>,
        val result: Maybe<R>
    )
}

inline fun <P : CloudResource, R : Any> P.getOrLoad(
    dependency: CloudDependency<P, R>,
    project: Project,
    title: String,
    forceLoad: Boolean = false,
    crossinline loader: () -> CloudDependencyLoader<P, R>
): LazyTask<R> = doLazy(title) {
    val resource: R? = if (!forceLoad) dependency.of(this).get() else null

    when {
        resource != null -> just(resource)

        else -> CloudResourceStorage.instance.loadDependencySync(
            project,
            this,
            dependency,
            loader(),
            showErrorNotifications = false
        )
    }
}