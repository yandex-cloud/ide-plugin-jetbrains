package yandex.cloud.toolkit.util.remote.list

class RemoteList<E>(
    private val source: List<E>,
    val state: RemoteListState,
) : List<E> {

    override val size: Int get() = source.size

    val isFull: Boolean get() = state.prevPageToken == null && state.nextPageToken == null

    override fun contains(element: E): Boolean = source.contains(element)
    override fun containsAll(elements: Collection<E>): Boolean = source.containsAll(elements)
    override fun get(index: Int): E = source[index]
    override fun indexOf(element: E): Int = source.indexOf(element)
    override fun isEmpty(): Boolean = source.isEmpty()
    override fun iterator(): Iterator<E> = source.iterator()
    override fun lastIndexOf(element: E): Int = source.lastIndexOf(element)
    override fun listIterator(): ListIterator<E> = source.listIterator()
    override fun listIterator(index: Int): ListIterator<E> = source.listIterator(index)
    override fun subList(fromIndex: Int, toIndex: Int): List<E> = source.subList(fromIndex, toIndex)

    operator fun plus(other: RemoteList<E>): RemoteList<E> {
        val prevPageToken = if (isEmpty()) null else state.prevPageToken
        val nextPageToken = if (other.isEmpty()) null else other.state.nextPageToken

        return RemoteList(
            source + other.source,
            RemoteListState(prevPageToken, nextPageToken)
        )
    }

    fun <T> map(function: (E) -> T): RemoteList<T> =
        RemoteList(source.map(function), state)

    companion object {
        private val EMPTY = RemoteList<Any>(emptyList(), RemoteListState.DEFAULT)

        @Suppress("UNCHECKED_CAST")
        fun <E> empty(): RemoteList<E> = EMPTY as RemoteList<E>
    }
}

class RemoteListPointer(val pageSize: Int, val pageToken: String?)


data class RemoteListState(val prevPageToken: String?, val nextPageToken: String?) {

    constructor(nextPageToken: String?) : this(null, nextPageToken)

    companion object {
        val DEFAULT = RemoteListState(null, null)
    }
}
