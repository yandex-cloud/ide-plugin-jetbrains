package yandex.cloud.toolkit.util.remote.list

import com.intellij.openapi.project.Project
import yandex.cloud.toolkit.util.Maybe
import yandex.cloud.toolkit.util.task.backgroundTask
import yandex.cloud.toolkit.util.mapValue
import yandex.cloud.toolkit.util.remote.resource.*
import yandex.cloud.toolkit.util.task.LazyTask
import java.util.concurrent.CompletableFuture

class RemoteListAsyncController<E>(
    pageSize: Int,
    private val loader: (RemoteListPointer) -> LazyTask<RemoteList<E>>
) {

    val list = RemoteResource<RemoteList<E>>()

    var pageSize = pageSize
        private set

    val isLoading: Boolean get() = list.isLoading
    val isLoaded: Boolean get() = list.state is FinalResourceState
    val isFailed: Boolean get() = list.state is FailedResourceState
    val isEmpty: Boolean get() = list.state.lastValue()?.isNotEmpty() != true

    val canLoadNext: Boolean get() = list.loadedValue?.state?.nextPageToken != null
    val canLoadPrev: Boolean get() = list.loadedValue?.state?.prevPageToken != null

    fun setList(value: RemoteList<E>) {
        list.updateState {
            when (it) {
                is LoadingResourceState -> throw IllegalStateException("Can not overwrite list in loading state")
                else -> LoadedResourceState(value)
            }
        }
    }

    fun clear() {
        list.updateState {
            when (it) {
                is LoadingResourceState -> throw IllegalStateException("Can not clear list in loading state")
                else -> EmptyResourceState()
            }
        }
    }

    fun resizePage(pageSize: Int) {
        clear()
        this.pageSize = pageSize
    }

    fun loadMain(project: Project, callback: (Maybe<RemoteList<E>>) -> Unit) {
        val future = CompletableFuture<Maybe<RemoteList<E>>>()

        list.updateState {
            when (it) {
                is EmptyResourceState -> it.asLoading(future)
                is FailedResourceState -> it.asLoading(future)
                is LoadingResourceState -> throw IllegalStateException("List already in loading state")
                is LoadedResourceState -> throw IllegalStateException("Main part of list already loaded")
            }
        }

        backgroundTask(project, "Yandex.Cloud") {
            val maybeList = loader(RemoteListPointer(pageSize, null)).perform()

            list.updateState { it.fromMaybe(maybeList) }
            callback(maybeList)
        }
    }

    fun loadNext(project: Project, callback: (Maybe<RemoteList<E>>) -> Unit) =
        loadPart(project, true, callback)

    fun loadPrev(project: Project, callback: (Maybe<RemoteList<E>>) -> Unit) =
        loadPart(project, false, callback)

    private fun loadPart(
        project: Project,
        next: Boolean,
        callback: (Maybe<RemoteList<E>>) -> Unit
    ) {
        val future = CompletableFuture<Maybe<RemoteList<E>>>()

        val loadingState = list.updateState { state ->
            when (state) {
                is EmptyResourceState, is FailedResourceState ->
                    throw IllegalStateException("Main part of list is not loaded (use loadMain)")
                is LoadingResourceState ->
                    throw IllegalStateException("List already in loading state")
                is LoadedResourceState -> {
                    when (if (next) state.value.state.nextPageToken else state.value.state.prevPageToken) {
                        null -> throw IllegalStateException("List has no ${if (next) "next" else "prev"} page")
                        else -> state.asLoading(future)
                    }
                }
            }
        }

        val oldList: RemoteList<E> = loadingState.value

        backgroundTask(project, "Yandex.Cloud") {
            val pageToken = if (next) oldList.state.nextPageToken else oldList.state.prevPageToken
            val maybeNewPart = loader(RemoteListPointer(pageSize, pageToken)).perform()

            val maybeNewState =
                maybeNewPart.mapValue { newList -> if (next) (oldList + newList) else (newList + oldList) }
            list.updateState { it.fromMaybe(maybeNewState) }

            callback(maybeNewPart)
        }
    }
}