package yandex.cloud.toolkit.util.task

import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import yandex.cloud.toolkit.util.*
import java.io.File
import java.io.FileNotFoundException

class LazyTask<R>(private val taskName: String, action: () -> Maybe<R>) : TaskAction<R>(action) {

    private var errorMessage: (Throwable) -> String = { it.message ?: "" }

    fun tryPerform(context: TaskContext): R = context.run {
        text = taskName
        notifyOnFail(errorMessage)
    }

    infix fun performIn(context: TaskContext): Maybe<R> = context.run {
        text = taskName
        val result = handle()
        if (result is NoValue) {
            notifyError(result.error, errorMessage)
        }
        result
    }

    infix fun onError(errorMessage: (Throwable) -> String): LazyTask<R> {
        this.errorMessage = errorMessage
        return this
    }
}

fun <R> doLazy(taskText: String = "", action: () -> Maybe<R>) = LazyTask(taskText, action)
fun <R> tryLazy(taskText: String = "", action: () -> R) = LazyTask(taskText) { doMaybe(action) }

fun File.asVirtual(): LazyTask<VirtualFile> =
    doLazy {
        LocalFileSystem.getInstance().refreshAndFindFileByIoFile(this).orElse(::FileNotFoundException)
    } onError {
        "Local File System can not find file:\n$absolutePath"
    }