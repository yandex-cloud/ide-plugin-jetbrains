package yandex.cloud.toolkit.ui.component

import com.intellij.icons.AllIcons
import com.intellij.ui.ClickListener
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import yandex.cloud.toolkit.util.YCPanel
import yandex.cloud.toolkit.util.YCUI
import yandex.cloud.toolkit.util.next
import java.awt.BorderLayout
import java.awt.Component
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.event.EventListenerList

class SpoilerPanel<C : Component>(
    val text: String,
    val content: C,
    closed: Boolean = true,
    private val autoOpenKey: String? = null
) : YCPanel(BorderLayout()) {

    var isOpened: Boolean = true
        private set

    private val icon = JBLabel(AllIcons.General.ArrowDown)

    private val listeners = EventListenerList()
    private var authOpenCheckBox: PropertyCheckBox? = null

    init {
        val clickListener = object : ClickListener() {
            override fun onClick(event: MouseEvent, clickCount: Int): Boolean {
                toggle()
                return true
            }
        }

        YCUI.gridPanel {
            YCUI.gridBag(horizontal = true) {
                icon.apply {
                    clickListener.installOn(this)
                } addAs next(0.0)

                YCUI.separator(text).apply {
                    clickListener.installOn(this)
                } addAs next(1.0)

                if (autoOpenKey != null) {
                    PropertyCheckBox("Open Automatically", autoOpenKey, true).apply {
                        authOpenCheckBox = this
                        border = JBUI.Borders.emptyLeft(4)
                    } addAs next(0.0)
                }
            }

        } addAs BorderLayout.NORTH

        content addAs BorderLayout.CENTER

        if (closed) closeInternal(false)
        isOpened = !closed
    }

    fun addSpoilerListener(listener: SpoilerListener) {
        listeners.add(SpoilerListener::class.java, listener)
    }

    fun removeSpoilerListener(listener: SpoilerListener) {
        listeners.remove(SpoilerListener::class.java, listener)
    }

    fun open() {
        if (isOpened) return
        isOpened = true
        content.isVisible = true
        listeners.getListeners(SpoilerListener::class.java).forEach { it.onOpened() }
        icon.icon = AllIcons.General.ArrowDown
    }

    fun tryOpenAutomatically() {
        if (authOpenCheckBox?.isSelected == true && !isOpened) open()
    }

    fun close() = closeInternal(true)

    private fun closeInternal(notifyListeners: Boolean) {
        if (!isOpened) return
        isOpened = false
        if (notifyListeners) listeners.getListeners(SpoilerListener::class.java).forEach { it.onClosed() }
        content.isVisible = false
        icon.icon = AllIcons.General.ArrowRight
    }

    fun toggle() = if (isOpened) close() else open()
}

interface SpoilerListener : EventListener {
    fun onOpened()
    fun onClosed()
}

class SpoilerToggleListener(val onToggle: () -> Unit) : SpoilerListener {
    override fun onOpened() {
        onToggle()
    }

    override fun onClosed() {
        onToggle()
    }
}