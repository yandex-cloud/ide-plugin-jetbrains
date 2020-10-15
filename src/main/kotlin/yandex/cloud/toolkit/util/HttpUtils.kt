package yandex.cloud.toolkit.util

import org.apache.http.HttpResponse
import org.apache.http.StatusLine
import java.util.concurrent.Future

class AsyncHttpRequest<R>(val response: Future<R>, private val onAbort: () -> Unit) {

    val isDone: Boolean get() = response.isDone

    fun abort() = onAbort()
}

class SimpleHttpResponse(val text: String?, val contentLength: Long, val status: StatusLine, val endTime: Long)

fun HttpResponse.asSimple(): SimpleHttpResponse {
    val time = System.currentTimeMillis()
    val bodyReader = this.entity?.content?.bufferedReader()
    val text = bodyReader?.readText()
    bodyReader?.close()

    return SimpleHttpResponse(text, entity?.contentLength ?: 0, statusLine, time)
}