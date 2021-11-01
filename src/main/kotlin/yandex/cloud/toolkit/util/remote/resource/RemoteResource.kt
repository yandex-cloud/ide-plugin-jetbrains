package yandex.cloud.toolkit.util.remote.resource

import yandex.cloud.toolkit.util.JustValue
import yandex.cloud.toolkit.util.Maybe
import yandex.cloud.toolkit.util.NoValue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class RemoteResource<E>() {

    @Volatile
    var state: RemoteResourceState<E> = EmptyResourceState()
        private set

    constructor(value: E) : this() {
        state = LoadedResourceState(value)
    }

    constructor(error: ResourceLoadingError) : this() {
        state = FailedResourceState(null, error)
    }

    private val stateLock: Any = Unit

    val value: E? get() = (state as? NotEmptyResourceState)?.value
    val error: ResourceLoadingError? get() = (state as? FailedResourceState)?.error

    val loadedValue: E? get() = state.loadedValue
    val lastValue: E? get() = state.lastValue()

    @OptIn(ExperimentalContracts::class)
    fun <S : RemoteResourceState<E>> updateState(handler: (RemoteResourceState<E>) -> S): S {
        contract {
            callsInPlace(handler, InvocationKind.EXACTLY_ONCE)
        }

        val newState: S
        synchronized(stateLock) {
            newState = handler(state)
            state = newState
        }
        return newState
    }

    fun tryLoad(loader: ((Maybe<E>) -> Unit) -> Unit, callback: (Maybe<E>) -> Unit = {}): Boolean {
        val future = CompletableFuture<Maybe<E>>()

        synchronized(stateLock) {
            if (state is LoadingResourceState<*>) return false
            state = state.asLoading(future)
        }

        loader { value ->
            synchronized(stateLock) {
                state = (state as LoadingResourceState<E>).fromMaybe(value)
            }
            future.complete(value)
            callback(value)
        }
        return true
    }

    fun loadOrAwait(loader: ((Maybe<E>) -> Unit) -> Unit, callback: (Maybe<E>) -> Unit = {}): Boolean {
        val future = CompletableFuture<Maybe<E>>()
        var alreadyLoadingFuture: Future<Maybe<E>>? = null

        synchronized(stateLock) {
            if (state is LoadingResourceState<*>) {
                alreadyLoadingFuture = (state as LoadingResourceState<E>).future
            } else {
                state = state.asLoading(future)
            }
        }

        return if (alreadyLoadingFuture != null) {
            callback(alreadyLoadingFuture!!.get())
            false
        } else {
            loader { value ->
                synchronized(stateLock) {
                    state = (state as LoadingResourceState<E>).fromMaybe(value)
                }
                future.complete(value)
                callback(value)
            }
            true
        }
    }
}

sealed class RemoteResourceState<E> {
    fun asLoaded(value: E): LoadedResourceState<E> = LoadedResourceState(value)
    abstract fun asFailed(error: ResourceLoadingError): FailedResourceState<E>

    fun fromMaybe(maybeResource: Maybe<E>): FinalResourceState<E> = when (maybeResource) {
        is JustValue -> asLoaded(maybeResource.value)
        is NoValue -> {
            val error = maybeResource.error
            asFailed(if (error is ResourceLoadingError) error else UnexpectedResourceLoadingError(error))
        }
    }
}

fun <E> Maybe<E>.asResourceState(): FinalResourceState<E> = EmptyResourceState<E>().fromMaybe(this)

sealed class NotEmptyResourceState<E> : RemoteResourceState<E>() {
    abstract val value: E?
}

sealed class FinalResourceState<E> : NotEmptyResourceState<E>()

interface LoadableResourceState<E> {
    fun asLoading(future: Future<Maybe<E>>): LoadingResourceState<E>
}

@Suppress("UNCHECKED_CAST")
fun <E> RemoteResourceState<E>.asLoading(future: Future<Maybe<E>>): LoadingResourceState<E> =
    (this as LoadableResourceState<E>).asLoading(future)

class EmptyResourceState<E> : LoadableResourceState<E>, RemoteResourceState<E>() {
    override fun asLoading(future: Future<Maybe<E>>) = LoadingResourceState(null, future)
    override fun asFailed(error: ResourceLoadingError) = FailedResourceState<E>(null, error)
}

open class LoadingResourceState<E>(override val value: E?, val future: Future<Maybe<E>>) : NotEmptyResourceState<E>() {
    override fun asFailed(error: ResourceLoadingError) = FailedResourceState(value, error)
}

class ReloadingResourceState<E>(override val value: E, future: Future<Maybe<E>>) :
    LoadingResourceState<E>(value, future)

class LoadedResourceState<E>(override val value: E) : FinalResourceState<E>(), LoadableResourceState<E> {
    override fun asLoading(future: Future<Maybe<E>>) = ReloadingResourceState(value, future)
    override fun asFailed(error: ResourceLoadingError) = FailedResourceState(value, error)
}

class FailedResourceState<E>(override val value: E?, val error: ResourceLoadingError) : FinalResourceState<E>(),
    LoadableResourceState<E> {

    override fun asLoading(future: Future<Maybe<E>>) = LoadingResourceState(value, future)
    override fun asFailed(error: ResourceLoadingError): FailedResourceState<E> = FailedResourceState(value, error)
}

val <E> RemoteResourceState<E>.loadedValue: E? get() = (this as? LoadedResourceState)?.value
val <E> RemoteResourceState<E>.error: ResourceLoadingError? get() = (this as? FailedResourceState)?.error

@OptIn(ExperimentalContracts::class)
fun <E> RemoteResourceState<E>.lastValue(): E? {
    contract {
        returnsNotNull() implies (this@lastValue is NotEmptyResourceState<*>)
    }
    return (this as? NotEmptyResourceState)?.value
}


val RemoteResourceState<*>.isLoading: Boolean get() = this is LoadingResourceState
val RemoteResource<*>?.isLoading: Boolean get() = this?.state?.isLoading ?: false
val RemoteResource<*>?.wasLoaded: Boolean get() = this?.state is NotEmptyResourceState<*>