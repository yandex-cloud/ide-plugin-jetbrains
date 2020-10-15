package yandex.cloud.toolkit.util

class StateLatch(closed: Boolean = false) {

    var isClosed: Boolean = closed
        private set

    fun tryClose(): Boolean {
        if (isClosed) return false
        isClosed = true
        return true
    }

    fun tryOpen(): Boolean {
        if (!isClosed) return false
        isClosed = false
        return true
    }

    fun close(errorMessage: () -> String) {
        if (!tryClose()) throw IllegalStateException(errorMessage())
    }

    fun open(errorMessage: () -> String) {
        if (!tryOpen()) throw IllegalStateException(errorMessage())
    }
}