package yandex.cloud.toolkit.ui.dialog

import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.execution.ExecutionBundle
import com.intellij.icons.AllIcons
import com.intellij.json.JsonLanguage
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.SpellCheckingEditorCustomizationProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiDocumentManager
import com.intellij.ui.*
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.TimerUtil
import org.apache.commons.lang.time.DurationFormatUtils
import yandex.cloud.toolkit.api.auth.CloudAuthData
import yandex.cloud.toolkit.api.resource.impl.model.CloudFunction
import yandex.cloud.toolkit.api.resource.impl.model.CloudFunctionVersion
import yandex.cloud.toolkit.api.resource.impl.model.findWithTag
import yandex.cloud.toolkit.api.service.CloudOperationService
import yandex.cloud.toolkit.ui.component.*
import yandex.cloud.toolkit.process.FunctionRunRequest
import yandex.cloud.toolkit.process.StatedProcessController
import yandex.cloud.toolkit.util.*
import yandex.cloud.toolkit.util.listener.SplitterSpoilerConnectivity
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ActionEvent
import javax.swing.*

class RunFunctionDialog(
    private val project: Project,
    private val authData: CloudAuthData,
    private val function: CloudFunction,
    private val versions: List<CloudFunctionVersion>,
    selectedTag: String?
) : DialogWrapper(true) {

    companion object {
        private const val AWAIT_DELAY = 200

        private val TEMPLATES = arrayOf(
            "No Template" to null,
            "HTTP Request" to "http-request.json",
            "Message Queue Trigger" to "message-queue-trigger.json",
            "Alice Skill" to "alice-skill.json"
        )
    }

    private val root = YCUI.panel(BorderLayout())

    private val inputField = createEditor(viewer = false)
    private val resultField = createEditor(viewer = true)

    private val templateBox = TemplateComboBox(project, inputField, "function-requests", TEMPLATES)
    private val tagBox = FunctionTagField(versions, canBeEmpty = false)
    private val statusField = JBTextField()
    private val statusBar = JProgressBar()
    private lateinit var logsSpoiler: SpoilerPanel<WrapperPanel>

    @Volatile
    private var task: RunTask? = null
    private val awaitTimer = TimerUtil.createNamedTimer("YC Function run awaiting", AWAIT_DELAY, this::onAwaitTimer)

    init {
        myOKAction.text = "Run"
        myOKAction.icon = AllIcons.Toolwindows.ToolWindowRun
        myCancelAction.text = "Close"

        if (selectedTag != null) tagBox.selectTag(selectedTag)

        awaitTimer.isRepeats = true

        myPreferredFocusedComponent = inputField
        title = "Run Yandex.Cloud Function"
        init()
    }

    private fun onAwaitTimer(event: ActionEvent) {
        val task = this.task

        if (task != null && !this@RunFunctionDialog.isDisposed) {
            if (task.request.isDone) {
                awaitTimer.stop()

                invokeLaterAt(this@RunFunctionDialog.root) {
                    try {
                        val response = task.request.response.get()
                        onFunctionRunResponse(response, task.startTime)
                    } catch (e: Exception) {
                        onRunProcessFail(e)
                    }

                    onRunProcessEnd(false)
                }
            } else {
                onRunProcessIdle(task)
            }
        } else {
            awaitTimer.stop()
        }
    }

    private fun onRunProcessStart() {
        logsSpoiler.tryOpenAutomatically()
        myOKAction.text = "Stop"
        myOKAction.icon = AllIcons.Actions.Suspend
        statusBar.isIndeterminate = true
        resultField.text = ""
        statusField.text = ""
        awaitTimer.start()

        val input = inputField.text
        val tag = tagBox.selectedTag!!

        val runRequest = FunctionRunRequest(authData, function.data.httpInvokeUrl, tag, input)
        val httpRequest = CloudOperationService.instance.runFunction(project, function.id, runRequest)
        task = RunTask(httpRequest, System.currentTimeMillis())
    }

    private fun onRunProcessEnd(suspend: Boolean) {
        myOKAction.text = "Run"
        myOKAction.icon = AllIcons.Toolwindows.ToolWindowRun
        statusBar.isIndeterminate = false
        if (suspend) {
            statusField.text = "Suspended"
            task?.request?.abort()
            awaitTimer.stop()
        }
        task = null
    }

    private fun onRunProcessSuspend() = onRunProcessEnd(true)

    private fun onRunProcessIdle(task: RunTask) {
        val currentTime = System.currentTimeMillis()
        val passedTime = currentTime - task.startTime

        val formattedTime = DurationFormatUtils.formatDuration(passedTime, "mm:ss", true)
        statusField.text = "Time passed - $formattedTime"
    }

    private fun onRunProcessFail(e: Throwable) {
        resultField.text = e.message ?: ""
        statusField.text = "Failed to fetch result"
    }

    private fun onFunctionRunResponse(response: SimpleHttpResponse, startTime: Long) {
        val statusCode = response.status.statusCode
        val statusPhrase = response.status.reasonPhrase
        val result = response.text ?: ""
        val passedTime = DurationFormatUtils.formatDuration(response.endTime - startTime, "mm:ss:SSS", true);
        val status = "$statusCode ($statusPhrase); Content: ${response.contentLength}B; Time $passedTime"

        resultField.text = result
        statusField.text = status

        PsiDocumentManager.getInstance(project).commitDocument(resultField.document)
        val resultFile = PsiDocumentManager.getInstance(project).getPsiFile(resultField.document)

        if (resultFile != null) {
            ReformatCodeProcessor(
                project,
                resultFile,
                null,
                false
            ).apply {
                setPostRunnable {
                    resultField.text = resultFile.text
                }
            }.run()
        }
    }

    private fun createEditor(viewer: Boolean): EditorTextField {
        return EditorTextFieldProvider.getInstance().getEditorField(
            JsonLanguage.INSTANCE,
            project,
            listOf(
                colorEditorCustomization(),
                ErrorStripeEditorCustomization.DISABLED,
                SpellCheckingEditorCustomizationProvider.getInstance().disabledCustomization,
                EditorCustomization {
                    it.isViewer = viewer
                    it.settings.apply {
                        isLineNumbersShown = true
                        isLineMarkerAreaShown = true
                        additionalLinesCount = 0
                        additionalColumnsCount = 0
                        isAdditionalPageAtBottom = false
                        isShowIntentionBulb = false
                    }
                }
            )
        )
    }

    override fun doOKAction() {
        if (!okAction.isEnabled) return
        when (task) {
            null -> onRunProcessStart()
            else -> onRunProcessSuspend()
        }
    }

    override fun createCenterPanel(): JComponent = root.apply {
        YCUI.gridPanel {
            YCUI.gridBag(horizontal = true) {
                JBTextField("Function: ${function.fullName}").apply {
                    asEditable(false)
                } addAs nextln(0.75)

                tagBox addAs next(0.25)
                statusBar addAs fullLine().insetBottom(4)
            }
        } addAs BorderLayout.NORTH

        JBSplitter(true).apply {
            splitterProportionKey = "YCUI.function_run_dialog.fields/logs"

            firstComponent = JBSplitter().apply {
                splitterProportionKey = "YCUI.function_run_dialog.input/output"

                firstComponent = YCUI.panel(BorderLayout()) {
                    minimumSize = Dimension(100, minimumSize.height)
                    withPreferredSize(500, 600)

                    inputField addAs BorderLayout.CENTER
                    YCUI.gridPanel {
                        YCUI.gridBag(horizontal = true) {
                            JLabel("Template") addAs nextln(0.2)
                            templateBox addAs next(0.8)
                        }
                    } addAs BorderLayout.SOUTH
                }

                secondComponent = YCUI.panel(BorderLayout()) {
                    minimumSize = Dimension(100, minimumSize.height)
                    withPreferredSize(500, 600)

                    resultField addAs BorderLayout.CENTER
                    statusField.asEditable(false) addAs BorderLayout.SOUTH
                }
            }

            if (versions.isNotEmpty()) {
                logsSpoiler = SpoilerPanel("Logs", WrapperPanel(), autoOpenKey = "run_function.open_logs")
                SplitterSpoilerConnectivity(this, logsSpoiler).install()
                setupLogsTrackingSpoiler()

                secondComponent = logsSpoiler
            }
        } addAs BorderLayout.CENTER
    }

    private fun destroyLogsTrackingPanel() {
        val wrapper = logsSpoiler.content
        val logsPanel = (wrapper.content as? FunctionLogsTrackingPanel<*>)
        wrapper.content = null
        if (logsPanel != null) Disposer.dispose(logsPanel)
    }

    private fun setupLogsTrackingPanel() {
        if (!logsSpoiler.isOpened) return
        val selectedTag = tagBox.item.first
        val version = versions.findWithTag(selectedTag) ?: return

        val logsPanel = FunctionLogsTrackingPanel(
            project,
            version,
            StatedProcessController()
        )

        logsPanel.setupActions {
            add(CloseLogsTrackingAction(logsSpoiler))
        }

        logsPanel.disposeWith(myDisposable)
        logsPanel.startTracking()
        logsSpoiler.content.content = logsPanel
    }

    private fun setupLogsTrackingSpoiler() {
        tagBox.doOnItemChange {
            destroyLogsTrackingPanel()
            setupLogsTrackingPanel()
        }

        logsSpoiler.addSpoilerListener(object : SpoilerListener {
            override fun onOpened() {
                setupLogsTrackingPanel()
            }

            override fun onClosed() {
                destroyLogsTrackingPanel()
            }
        })
    }


    override fun dispose() {
        super.dispose()
        awaitTimer.stop()
    }

    private class RunTask(val request: AsyncHttpRequest<SimpleHttpResponse>, val startTime: Long)

    private class CloseLogsTrackingAction(val logsSpoiler: SpoilerPanel<WrapperPanel>) :
        AnAction(ExecutionBundle.messagePointer("close.tab.action.name"), AllIcons.Actions.Cancel) {

        override fun actionPerformed(e: AnActionEvent) {
            logsSpoiler.close()
        }
    }
}