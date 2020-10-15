package yandex.cloud.toolkit.ui.component

import com.intellij.ide.ui.laf.darcula.ui.DarculaTextBorder
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import yandex.cloud.toolkit.api.resource.findById
import yandex.cloud.toolkit.api.resource.impl.HierarchicalCloudResource
import yandex.cloud.toolkit.api.resource.impl.icon
import yandex.cloud.toolkit.util.*
import java.awt.BorderLayout
import javax.swing.Icon

class CloudResourceField(
    val project: Project,
    val resources: List<HierarchicalCloudResource>?,
    val defaultIcon: Icon?,
    val locked: Boolean
) : YCPanel(BorderLayout()) {

    constructor(project: Project, resource: HierarchicalCloudResource?, defaultIcon: Icon? = null) : this(
        project,
        listOfNotNull(resource),
        resource?.icon ?: defaultIcon,
        resource != null
    ) {
        this.field.text = resource?.id
    }

    private val field = JBTextField()
    private val icon = JBLabel()
    private val label = JBTextField()
    private val panel: YCPanel

    var value: String
        get() = this.field.text
        set(value) {
            if (locked) return
            this.field.text = value
            updateLabel()
        }

    init {
        isOpaque = false
        label.border = JBUI.Borders.empty()
        label.isOpaque = false
        icon.isOpaque = false
        field.isEditable = !locked

        field.addCaretListener {
            updateLabel()
        }
        label.isEditable = false

        field addAs BorderLayout.CENTER
        panel = YCUI.borderPanel {
            icon addAs BorderLayout.WEST
            label addAs BorderLayout.CENTER
        }
        panel addAs BorderLayout.EAST

        updateLabel()
    }

    private fun updateLabel() {
        val resourceId = field.text
        val resource = resources?.findById(resourceId)
        panel.updateUI()
        panel.isOpaque = false
        panel.border = DarculaTextBorder()
        label.text = resource?.name
        label.isVisible = resource != null
        icon.border = if (resource != null) JBUI.Borders.emptyLeft(4) else JBUI.Borders.empty(0, 4)
        icon.icon = resource?.icon ?: defaultIcon
    }
}