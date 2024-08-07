package yandex.cloud.toolkit.ui.component


import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.TextFieldWithHistory
import com.intellij.util.ui.SwingHelper
import yandex.cloud.toolkit.api.profile.impl.profileStorage
import yandex.cloud.toolkit.api.resource.impl.model.CloudFolder
import yandex.cloud.toolkit.api.service.CloudOperationService
import yandex.cloud.toolkit.util.JustValue
import yandex.cloud.toolkit.util.NoValue
import yandex.cloud.toolkit.util.invokeLaterAt
import yandex.cloud.toolkit.util.reopenPopup
import yandex.cloud.toolkit.util.task.backgroundTask
import javax.swing.JList

class ObjectStorageBucketField(
    private val project: Project,
    private val folder: CloudFolder?
) : TextFieldWithHistory(), Disposable {

    @Volatile
    private var disposed = false

    @Volatile
    private var failedToLoad = false

    init {
        setHistorySize(-1)
        renderer = ListRenderer()

        if (folder is CloudFolder) {
            SwingHelper.addHistoryOnExpansion(this, this::loadBuckets)
        }
    }

    private fun loadBuckets(): List<String> {
        val user = project.profileStorage.profile?.resourceUser ?: return emptyList()
        backgroundTask(project, "Yandex.Cloud") {
            val buckets = CloudOperationService.instance.fetchBuckets(user, folder!!).handle()

            invokeLaterAt(this@ObjectStorageBucketField) {
                if (disposed) return@invokeLaterAt

                when (buckets) {
                    is JustValue -> {
                        history = buckets.value.map { it.name }
                    }
                    is NoValue -> failedToLoad = true
                }

                this@ObjectStorageBucketField.reopenPopup()
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
