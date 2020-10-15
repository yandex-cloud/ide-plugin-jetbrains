package yandex.cloud.toolkit.ui.component

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.TextFieldWithHistory
import com.intellij.util.ui.SwingHelper
import yandex.cloud.toolkit.api.profile.impl.profileStorage
import yandex.cloud.toolkit.api.service.CloudOperationService
import yandex.cloud.toolkit.util.*
import yandex.cloud.toolkit.util.task.backgroundTask
import javax.swing.JList

class FunctionRuntimeField(val project: Project, runtimes: List<String>?) : TextFieldWithHistory(), Disposable {

    @Volatile
    private var disposed = false

    @Volatile
    private var failedToLoad = false

    init {
        setHistorySize(-1)
        renderer = ListRenderer()

        if (runtimes != null) {
            history = runtimes
        } else {
            SwingHelper.addHistoryOnExpansion(this, this::loadRuntimes)
        }
    }

    private fun loadRuntimes(): List<String> {
        val user = project.profileStorage.profile?.resourceUser ?: return emptyList()
        backgroundTask(project, "Yandex.Cloud") {
            val runtimes = CloudOperationService.instance.fetchRuntimes(project, user).handle()

            invokeLaterAt(this@FunctionRuntimeField) {
                if (disposed) return@invokeLaterAt

                when (runtimes) {
                    is JustValue -> {
                        history = runtimes.value
                    }
                    is NoValue -> failedToLoad = true
                }

                this@FunctionRuntimeField.reopenPopup()
            }
        }
        return listOf("")
    }

    override fun dispose() {
        disposed = true
    }

    private inner class ListRenderer : ColoredListCellRenderer<String>() {
        override fun customizeCellRenderer(
            list: JList<out String>,
            value: String,
            index: Int,
            selected: Boolean,
            hasFocus: Boolean
        ) {
            if (value.isEmpty()) {
                when (failedToLoad) {
                    false -> append("Loading...", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                    true -> append("Failed to load", SimpleTextAttributes.ERROR_ATTRIBUTES)
                }
            } else {
                append(value, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            }
        }
    }
}