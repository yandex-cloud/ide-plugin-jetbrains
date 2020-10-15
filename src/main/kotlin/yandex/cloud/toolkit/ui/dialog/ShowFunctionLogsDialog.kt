package yandex.cloud.toolkit.ui.dialog

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.SimpleTextAttributes.REGULAR_ATTRIBUTES
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.michaelbaranov.microba.calendar.DatePicker
import yandex.cloud.toolkit.api.auth.CloudAuthData
import yandex.cloud.toolkit.api.resource.impl.model.CloudFunctionVersion
import yandex.cloud.toolkit.api.service.CloudOperationService
import yandex.cloud.toolkit.api.util.LogLine
import yandex.cloud.toolkit.util.*
import yandex.cloud.toolkit.util.remote.list.RemoteListLine
import yandex.cloud.toolkit.util.remote.list.RemoteListListener
import yandex.cloud.toolkit.util.remote.list.RemoteListUI
import java.awt.BorderLayout
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Instant
import java.util.*
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.ListSelectionModel

class ShowFunctionLogsDialog(
    project: Project,
    private val authData: CloudAuthData,
    private val version: CloudFunctionVersion
) : DialogWrapper(true) {

    companion object {
        private val PAGE_SIZES = arrayOf(100, 250, 500)

        private var DATE_FORMAT: DateFormat = SimpleDateFormat("HH:mm:ss dd.MM.yyyy")
    }

    private val root = YCUI.panel(BorderLayout())

    val logsList = RemoteListUI(project, PAGE_SIZES[0]) { pointer ->
        CloudOperationService.instance.fetchFunctionLogs(
            authData,
            version.function.data.logGroupId,
            version.id,
            sinceTimeField.date.time / 1000,
            untilTimeField.date.time / 1000 + 1,
            pointer
        )
    }

    private val sinceTimeField = DatePicker(
        Date.from(Instant.now() - Duration.ofHours(1)),
        DATE_FORMAT
    )

    private val untilTimeField = DatePicker(
        Date.from(Instant.now() + Duration.ofHours(1)),
        DATE_FORMAT
    )

    private val pageSizeBox = ComboBox<Int>()

    init {
        myOKAction.text = "Load Logs"
        myOKAction.icon = AllIcons.Vcs.History
        myCancelAction.text = "Close"

        pageSizeBox.model = DefaultComboBoxModel(PAGE_SIZES)

        pageSizeBox.addActionListener {
            logsList.setPageSize(pageSizeBox.item)
            loadLogs()
        }

        logsList.cellRenderer = LogsListRenderer()
        logsList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        logsList.background = YCUI.editorColorScheme.defaultBackground

        logsList.addListener(object : RemoteListListener {
            override fun onStartLoading() {
                logsList.clearSelection()
                okAction.isEnabled = false
                pageSizeBox.isEditable = false
                untilTimeField.isFieldEditable = false
                sinceTimeField.isFieldEditable = false
                myOKAction.text = "Loading Logs..."
            }

            override fun onFinishLoading() {
                logsList.clearSelection()
                okAction.isEnabled = true
                pageSizeBox.isEditable = true
                untilTimeField.isFieldEditable = true
                sinceTimeField.isFieldEditable = true
                myOKAction.text = "Load Logs"
            }
        })

        untilTimeField.addActionListener {
            if (untilTimeField.date < sinceTimeField.date) {
                sinceTimeField.date = untilTimeField.date
            }
            loadLogs()
        }

        sinceTimeField.addActionListener {
            if (untilTimeField.date < sinceTimeField.date) {
                untilTimeField.date = sinceTimeField.date
            }
            loadLogs()
        }

        title = "Yandex.Cloud Function Logs"
        init()
    }

    private fun loadLogs() {
        logsList.loadMain("Loading logs...") { "No logs for this period" }
    }

    override fun doOKAction() {
        if (!okAction.isEnabled) return
        loadLogs()
    }

    override fun createCenterPanel(): JComponent = root.apply {
        YCUI.gridPanel {
            YCUI.gridBag(horizontal = true) {
                JBTextField("Function: ${version.fullName}").apply {
                    asEditable(false)
                } addAs fullLine()

                JBLabel("Since") addAs nextln(0.2)
                sinceTimeField addAs next(0.8)
                JBLabel("Until") addAs nextln(0.2)
                untilTimeField addAs next(0.8)
                JBLabel("Show") addAs nextln(0.2)
                pageSizeBox addAs next(0.8)
                YCUI.separator("Logs") addAs fullLine()
            }
        } addAs BorderLayout.NORTH

        ToolbarDecorator.createDecorator(logsList).apply {
            setToolbarPosition(ActionToolbarPosition.RIGHT)
            setRemoveAction(null)
            setMoveDownAction(null)
            setMoveUpAction(null)
            setAddAction(null)
            setEditAction(null)
        }.createPanel().apply {
            withPreferredSize(800, 700)
        } addAs BorderLayout.CENTER
    }

    override fun dispose() {
        logsList.dispose()
        super.dispose()
    }

    private inner class LogsListRenderer : RemoteListUI.CellRenderer<LogLine>() {
        override fun customizeElementCell(
            list: JList<out RemoteListLine<LogLine>>, value: LogLine, index: Int, selected: Boolean, hasFocus: Boolean
        ) {
            append("${value.time.time} ", SimpleTextAttributes.GRAY_ATTRIBUTES)
            //append("${DATE_FORMAT.format(value.time)} ", SimpleTextAttributes.GRAY_ATTRIBUTES)
            append(value.text, REGULAR_ATTRIBUTES)
        }
    }
}