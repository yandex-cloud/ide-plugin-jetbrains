package yandex.cloud.toolkit.util.remote.list

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.ListUtil
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBList
import com.intellij.util.ui.JBUI
import yandex.cloud.toolkit.util.JustValue
import yandex.cloud.toolkit.util.NoValue
import yandex.cloud.toolkit.util.remote.resource.ResourceLoadingError
import yandex.cloud.toolkit.util.task.LazyTask
import java.util.*
import javax.swing.DefaultListModel
import javax.swing.JList
import javax.swing.event.EventListenerList

class RemoteListUI<E>(
    val project: Project,
    pageSize: Int,
    loader: (RemoteListPointer) -> LazyTask<RemoteList<E>>
) : JBList<RemoteListLine<E>>(), Disposable {

    private val listeners = EventListenerList()

    val controller = RemoteListAsyncController(pageSize, loader)
    val model = DefaultListModel<RemoteListLine<E>>()

    var isDisposed = false
        private set

    var isLoading: Boolean = false
        private set

    val canLoadPrev: Boolean get() = !isLoading && controller.canLoadPrev
    val canLoadNext: Boolean get() = !isLoading && controller.canLoadNext

    fun setPageSize(newSize: Int) {
        controller.resizePage(newSize)
        clear()
        setEmptyText("")
    }

    init {
        setEmptyText("")
        setModel(model)

        addListSelectionListener {
            val selectedIndex = selectedIndex
            if (selectedIndex != -1) {
                (model[selectedIndex] as? RemoteListBorder)?.onClick()
            }
        }
    }

    fun addListener(listener: RemoteListListener) {
        listeners.add(RemoteListListener::class.java, listener)
    }

    fun removeListener(listener: RemoteListListener) {
        listeners.remove(RemoteListListener::class.java, listener)
    }

    fun clear() {
        controller.clear()
        ListUtil.removeAllItems(model)
        updateUI()
    }

    private fun onLoadingError(error: Throwable) {
        clear()
        setEmptyText((error as? ResourceLoadingError)?.message ?: "Failed to load records")
    }

    private fun insertToStart(list: RemoteList<E>) {
        model.addAll(1, list.map(::RemoteListElement))
        if (list.isEmpty() || list.state.prevPageToken == null) model.remove(0)
    }

    private fun insertToEnd(list: RemoteList<E>) {
        model.addAll(model.size() - 1, list.map(::RemoteListElement))
        if (list.isEmpty() || list.state.nextPageToken == null) model.remove(model.size() - 1)
    }

    private fun invokeInContext(block: () -> Unit) {
        invokeLater(ModalityState.stateForComponent(this)) {
            if (!isDisposed) block()
        }
    }

    private fun onStartLoading() {
        isLoading = true
        listeners.getListeners(RemoteListListener::class.java).forEach(RemoteListListener::onStartLoading)
    }

    private fun onFinishLoading() {
        listeners.getListeners(RemoteListListener::class.java).forEach(RemoteListListener::onFinishLoading)
        isLoading = false
    }

    fun loadMain(processName: String, emptyText: () -> String) {
        if (isLoading) return
        if (controller.isLoaded) clear()
        setEmptyText(processName)
        onStartLoading()

        controller.loadMain(project) { mainPart ->
            invokeInContext {
                when (mainPart) {
                    is NoValue -> onLoadingError(mainPart.error)
                    is JustValue -> {
                        if (mainPart.value.isEmpty()) {
                            setEmptyText(emptyText())
                        } else {
                            val list = mainPart.value
                            if (list.state.prevPageToken != null) model.addElement(RemoteListBorder(this, false))
                            model.addAll(list.map(::RemoteListElement))
                            if (list.state.nextPageToken != null) model.addElement(RemoteListBorder(this, true))
                        }
                    }
                }
                onFinishLoading()
            }
        }
    }

    fun loadPrevious() {
        if (!canLoadPrev) return
        onStartLoading()

        controller.loadPrev(project) { prevPart ->
            invokeInContext {
                when (prevPart) {
                    is NoValue -> onLoadingError(prevPart.error)
                    is JustValue -> insertToStart(prevPart.value)
                }
                onFinishLoading()
            }
        }
    }

    fun loadNext() {
        if (!canLoadNext) return
        onStartLoading()

        controller.loadNext(project) { nextPart ->
            invokeInContext {
                when (nextPart) {
                    is NoValue -> onLoadingError(nextPart.error)
                    is JustValue -> insertToEnd(nextPart.value)
                }
                onFinishLoading()
            }
        }
    }

    abstract class CellRenderer<E> : ColoredListCellRenderer<RemoteListLine<E>>() {
        override fun customizeCellRenderer(
            list: JList<out RemoteListLine<E>>,
            value: RemoteListLine<E>,
            index: Int,
            selected: Boolean,
            hasFocus: Boolean
        ) {
            value.render(this)
            if (value is RemoteListElement) customizeElementCell(list, value.element, index, selected, hasFocus)
        }

        abstract fun customizeElementCell(
            list: JList<out RemoteListLine<E>>,
            value: E,
            index: Int,
            selected: Boolean,
            hasFocus: Boolean
        )
    }

    override fun dispose() {
        isDisposed = true
    }
}


sealed class RemoteListLine<E> {
    open fun render(renderer: ColoredListCellRenderer<RemoteListLine<E>>) {}
}

class RemoteListElement<E>(val element: E) : RemoteListLine<E>() {
    override fun render(renderer: ColoredListCellRenderer<RemoteListLine<E>>) {
        renderer.border = null
    }
}

class RemoteListBorder<E>(private val ui: RemoteListUI<E>, val isNext: Boolean) : RemoteListLine<E>() {
    override fun render(renderer: ColoredListCellRenderer<RemoteListLine<E>>) {
        renderer.icon = if (isNext) AllIcons.Actions.MoveDown else AllIcons.Actions.MoveUp
        renderer.border = JBUI.Borders.customLine(
            JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground(),
            1, 0, 1, 0
        )

        if (ui.isLoading) {
            renderer.append(
                "Loading...",
                SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES
            )
        } else {
            renderer.append(
                "Load ${if (isNext) "next" else "previous"} ${ui.controller.pageSize} records",
                SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES
            )
        }
    }

    fun onClick() {
        if (isNext) ui.loadNext() else ui.loadPrevious()
    }
}

interface RemoteListListener : EventListener {

    fun onStartLoading()
    fun onFinishLoading()
}