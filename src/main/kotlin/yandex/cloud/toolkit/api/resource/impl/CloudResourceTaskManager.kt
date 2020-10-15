package yandex.cloud.toolkit.api.resource.impl

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import io.grpc.StatusRuntimeException
import yandex.cloud.toolkit.api.auth.impl.CloudAuthServiceImpl
import yandex.cloud.toolkit.api.resource.*
import yandex.cloud.toolkit.util.*
import yandex.cloud.toolkit.util.remote.resource.MissingResourceLoadingError
import yandex.cloud.toolkit.util.remote.resource.PresentableResourceStatus
import yandex.cloud.toolkit.util.remote.resource.ResourceLoadingError
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.Future

class CloudResourceTaskManager(threads: Int) {

    private val log = logger<CloudAuthServiceImpl>()

    private val executor = Executors.newFixedThreadPool(threads)
    private val tasks = mutableMapOf<DependentResource<*, *>, Task<*, *>>()

    @Suppress("UNCHECKED_CAST")
    private fun <P : CloudResource, R> tryAddTask(
        depres: DependentResource<P, R>,
        taskFactory: () -> CloudResourceTaskManager.Task<P, R>
    ): Future<Maybe<R>> {
        val task = synchronized(tasks) {
            tasks.getOrPut(depres, taskFactory)
        } as Task<P, R>

        return task.future
    }

    fun <P : CloudResource, R> handleAsync(
        project: Project,
        parent: P,
        dependency: CloudDependency<P, R>,
        loader: CloudDependencyLoader<P, R>,
        onSuccess: (() -> Unit)?
    ): Future<Maybe<R>> {
        val depres = dependency.of(parent)

        return tryAddTask(depres) {
            AsyncTask(project, parent, depres, loader, onSuccess).also(executor::execute)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <P : CloudResource, R> handleSync(
        project: Project,
        parent: P,
        dependency: CloudDependency<P, R>,
        loader: CloudDependencyLoader<P, R>,
        showErrorNotifications: Boolean
    ): Future<Maybe<R>> {
        val depres = dependency.of(parent)

        return tryAddTask(depres) {
            SyncTask(project, parent, depres, loader, {}, showErrorNotifications)
        }
    }

    fun <P : CloudResource, R> performLoad(
        project: Project,
        parent: P,
        dependency: CloudDependency<P, R>,
        loader: CloudDependencyLoader<P, R>,
        showErrorNotifications: Boolean
    ): Pair<CloudDependencyLoadingResult, Boolean> {
        val result = CloudDependencyLoadingResult()

        try {
            loader.load(parent, result)
            return result to true
        } catch (e: StatusRuntimeException) {
            result.put(
                parent,
                dependency,
                ResourceLoadingError(
                    e.status.description ?: "",
                    PresentableResourceStatus.byStatusRE(e)
                )
            )

            if (showErrorNotifications) {
                errorNotification(
                    PROCESS_ID,
                    "Yandex Cloud Resource Loading Failed",
                    e.status.description ?: "",
                ).showAt(project)
            }
        } catch (e: ResourceLoadingError) {
            result.put(parent, dependency, e)

            if (showErrorNotifications) {
                val notification = errorNotification(
                    PROCESS_ID,
                    "Yandex Cloud Resource Loading Failed",
                    e.message
                ).withActions(e)

                notification.showAt(project)
            }
        } catch (e: Exception) {
            result.put(
                parent,
                dependency,
                ResourceLoadingError(e.message ?: "", PresentableResourceStatus.FailedToLoad)
            )

            if (showErrorNotifications) {
                errorNotification(
                    PROCESS_ID,
                    "Yandex Cloud Resource Loading Failed",
                    "Error - " + e.message
                ).showAt(project)
            }

            log.error("[Yandex Cloud] Unexpected resource loading error", e)
        }

        return result to false
    }

    private inner class AsyncTask<P : CloudResource, R>(
        project: Project,
        parent: P,
        depres: DependentResource<P, R>,
        loader: CloudDependencyLoader<P, R>,
        onSuccess: (() -> Unit)?,
    ) : Task<P, R>(project, parent, depres, loader, onSuccess, true), Runnable {

        override val future = CompletableFuture<Maybe<R>>()

        override fun run() {
            future.complete(doWork())
        }
    }

    private inner class SyncTask<P : CloudResource, R>(
        project: Project,
        parent: P,
        depres: DependentResource<P, R>,
        loader: CloudDependencyLoader<P, R>,
        onSuccess: (() -> Unit)?,
        showErrorNotifications: Boolean
    ) : Task<P, R>(project, parent, depres, loader, onSuccess, showErrorNotifications) {

        override val future = LazyFuture(this::doWork)
    }

    private abstract inner class Task<P : CloudResource, R>(
        val project: Project,
        val parent: P,
        val depres: DependentResource<P, R>,
        val loader: CloudDependencyLoader<P, R>,
        val onSuccess: (() -> Unit)?,
        val showErrorNotifications: Boolean
    ) {

        abstract val future: Future<Maybe<R>>

        private var isRemoved = false

        fun doWork(): Maybe<R> {
            val (result, success) = performLoad(project, parent, depres.dependency, loader, showErrorNotifications)

            CloudResourceStorage.instance.updateResources(result)
            remove()
            if (success) onSuccess?.invoke()

            return result[depres] ?: NoValue(MissingResourceLoadingError)
        }

        private fun remove() {
            if (!isRemoved) {
                synchronized(tasks) {
                    tasks.remove(depres, this)
                }
                isRemoved = true
            }
        }
    }


    companion object {
        private val PROCESS_ID = "yandex-cloud-resource-loading"
    }
}