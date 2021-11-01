package yandex.cloud.toolkit.util.remote.list

import yandex.cloud.toolkit.util.*

class RemoteListController<E>(
    val pageSize: Int,
    private val loader: (RemoteListPointer) -> Maybe<RemoteList<E>>
) {

    var list: Maybe<RemoteList<E>> = just(RemoteList.empty())
        private set

    val canLoadPrev: Boolean get() = loadedPagesCount == 0 || !list.value?.state?.prevPageToken.isNullOrEmpty()
    val canLoadNext: Boolean get() = loadedPagesCount == 0 || !list.value?.state?.nextPageToken.isNullOrEmpty()

    var loadedPagesCount: Int = 0
        private set

    val isFailedToLoad: Boolean get() = list.hasError

    private fun loadPage(next: Boolean) {
        val currentList = list.value ?: return

        val rawPageToken = when {
            next -> currentList.state.nextPageToken
            else -> currentList.state.prevPageToken
        }
        if (loadedPagesCount != 0 && rawPageToken == null) return

        val pageToken = if (rawPageToken.isNullOrEmpty()) null else rawPageToken
        val page = loader(RemoteListPointer(pageSize, pageToken))

        list = page.mapValue {
            when {
                next -> currentList + it
                else -> it + currentList
            }
        }
        loadedPagesCount++
    }

    fun loadPrev() = loadPage(next = true)
    fun loadNext() = loadPage(next = false)

    fun loadAllPages(onlyNext: Boolean = true): Maybe<RemoteList<E>> {
        if (!onlyNext) while (canLoadPrev) loadPrev()
        while (canLoadNext) loadNext()
        return list
    }
}


fun <E> loadRemoteList(
    pageSize: Int = 100,
    onlyNext: Boolean = true,
    loader: (RemoteListPointer) -> RemoteList<E>
): Maybe<RemoteList<E>> {
    return RemoteListController(pageSize) {
        doMaybe {
            loader(it)
        }
    }.loadAllPages(onlyNext)
}