package yandex.cloud.toolkit.util

import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class LazyFuture<R>(private val task: Callable<R>) : Future<R> {

    private val isTaskRunning = AtomicBoolean(false)
    private val future = CompletableFuture<R>()

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean = future.cancel(mayInterruptIfRunning)
    override fun isCancelled(): Boolean = future.isCancelled
    override fun isDone(): Boolean = future.isDone

    override fun get(): R {
        return tryRunTask() ?: future.get()
    }

    override fun get(timeout: Long, unit: TimeUnit): R {
        return tryRunTask() ?: future.get(timeout, unit)
    }

    private fun tryRunTask(): R? {
        return if (isTaskRunning.compareAndSet(false, true)) {
            try {
                val result = task.call()
                future.complete(result)
                return result
            } catch (e: Exception) {
                future.completeExceptionally(e)
                throw e
            }
        } else null
    }
}