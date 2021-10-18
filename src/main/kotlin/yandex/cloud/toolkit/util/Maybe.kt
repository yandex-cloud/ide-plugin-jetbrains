package yandex.cloud.toolkit.util

import yandex.cloud.toolkit.util.remote.resource.PresentableResourceStatus
import yandex.cloud.toolkit.util.remote.resource.ResourceLoadingError

sealed class Maybe<out R>

class JustValue<R>(val value: R) : Maybe<R>()
class NoValue<R>(val error: Throwable) : Maybe<R>()

val <R> Maybe<R>.value: R? get() = (this as? JustValue)?.value
val <R> Maybe<R>.error: Throwable? get() = (this as? NoValue)?.error

val <R> Maybe<R>.hasValue: Boolean get() = this is JustValue
val <R> Maybe<R>.hasError: Boolean get() = this is NoValue

fun <R> Maybe<R>.getOrThrow(): R = when (this) {
    is JustValue -> value
    is NoValue -> throw error
}

fun <R> Maybe<R>.getOrThrow(handle: (Throwable) -> Throwable): R = when (this) {
    is JustValue -> value
    is NoValue -> throw handle(error)
}

fun <R> Maybe<R>.getOrNull(): R? = when (this) {
    is JustValue -> value
    is NoValue -> null
}

inline fun <R : Any> (R?).orElse(error: () -> Throwable): Maybe<R> = when (this) {
    null -> NoValue(error())
    else -> just(this)
}

fun <R, T> Maybe<R>.tryMap(function: (R) -> T) = when (this) {
    is JustValue -> doMaybe { function(this.value) }
    is NoValue -> NoValue(error)
}

fun <R, T> Maybe<R>.map(function: (R) -> Maybe<T>) = when (this) {
    is JustValue -> function(this.value)
    is NoValue -> NoValue(error)
}

fun <R, T> Maybe<R>.mapValue(function: (R) -> T) = when (this) {
    is JustValue -> just(function(this.value))
    is NoValue -> NoValue(error)
}

fun <R> Maybe<R>.mapError(function: (Throwable) -> Throwable) = when (this) {
    is JustValue -> this
    is NoValue -> NoValue(function(error))
}

fun <R : Any> (R?).asJust(): Maybe<R>? = when (this) {
    null -> null
    else -> just(this)
}

fun <R> just(value: R): JustValue<R> = JustValue(value)
fun <R> justAsMaybe(value: R): Maybe<R> = JustValue(value)

fun <R> doMaybe(factory: () -> R): Maybe<R> =
    try {
        just(factory())
    } catch (e: Exception) {
        NoValue(e)
    }

fun <R> noResource(
    message: String,
    status: PresentableResourceStatus = PresentableResourceStatus.FailedToLoad
): NoValue<R> =
    NoValue(ResourceLoadingError(message, status))

fun <R> noValue(): NoValue<R> = NoValue(RuntimeException("Missing value"))

