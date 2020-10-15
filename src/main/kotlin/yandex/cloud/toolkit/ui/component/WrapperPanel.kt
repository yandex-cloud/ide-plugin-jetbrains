package yandex.cloud.toolkit.ui.component

import yandex.cloud.toolkit.util.YCPanel
import java.awt.BorderLayout
import javax.swing.JComponent

class WrapperPanel(content: JComponent? = null) : YCPanel(BorderLayout()) {

    var content: JComponent?
        set(value) {
            removeAll()
            value?.addAs(BorderLayout.CENTER)
        }
        get() = if (componentCount > 0) getComponent(0) as? JComponent? else null

    init {
        border = null
        if (content != null) this.content = content
    }
}