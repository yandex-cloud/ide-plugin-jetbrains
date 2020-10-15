package yandex.cloud.toolkit.ui.dialog

import com.intellij.icons.AllIcons
import com.intellij.ide.util.BrowseFilesListener
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.TextFieldWithHistory
import com.intellij.ui.components.JBLabel
import com.intellij.util.containers.addIfNotNull
import com.intellij.util.ui.UIUtil
import icons.CloudIcons
import yandex.cloud.toolkit.api.auth.impl.cli.CLICloudAuthMethod
import yandex.cloud.toolkit.util.*
import java.time.Duration
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.SwingConstants
import kotlin.concurrent.thread

class CLIAuthConfigDialog(
    lastConfig: CLICloudAuthMethod.Config?
) : DialogWrapper(true) {

    private val cliLocationField = TextFieldWithBrowseButton()
    private val profileField = TextFieldWithHistory()
    private val cliCheckButton = JButton("Test CLI")
    private val loadProfilesButton = JButton("Load Profiles")
    private val cliStatus = JBLabel("", SwingConstants.LEFT)

    @Volatile
    private var cliProcess: Process? = null

    var cliLocation: String = ""
        private set
    var profile: String? = null
        private set

    init {
        cliLocationField.text = lastConfig?.cliLocation ?: ""

        profileField.renderer = ProfilesListRenderer()
        val profiles = mutableListOf("")
        profiles.addIfNotNull(lastConfig?.profile)
        setProfilesHistory(profiles)

        cliLocationField.addBrowseFolderListener(
            "Select CLI Yandex.Cloud executable", "", null,
            BrowseFilesListener.SINGLE_FILE_DESCRIPTOR
        )

        cliStatus.font = UIUtil.getLabelFont(UIUtil.FontSize.SMALL)

        updateToolStatus()
        updateProcessButtons()

        cliLocationField.textField.addCaretListener {
            updateToolStatus()
            updateProcessButtons()
        }

        cliCheckButton.addActionListener { runCLICheckProcess() }
        loadProfilesButton.addActionListener { runLoadProfilesProcess() }

        title = "CLI Yandex.Cloud Authentication"
        init()
    }

    private fun updateToolStatus() {
        if (cliLocationField.text.isEmpty()) {
            cliStatus.icon = null
            cliStatus.text = null
        }
    }

    private fun updateProcessButtons() {
        val canStartProcess = cliLocationField.text.isNotEmpty() && cliProcess == null
        cliCheckButton.isEnabled = canStartProcess
        loadProfilesButton.isEnabled = canStartProcess
    }

    private fun runCLICheckProcess() {
        val cliLocation = cliLocationField.text
        if (cliProcess != null || cliLocation.isEmpty()) return

        val process = try {
            Runtime.getRuntime().exec(
                arrayOf(cliLocation, "version")
            )
        } catch (e: Exception) {
            updateStatus(null)
            null
        }

        cliProcess = process ?: return
        updateProcessButtons()
        val timeoutTask = process.setupTimeout(Duration.ofSeconds(3))

        thread(name = "yc-cli-check", isDaemon = true) {
            val ycToolVersion: String? = try {
                process.inputStream.bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                null
            }

            cliProcess = null
            timeoutTask.cancel()

            invokeLaterAt(this.contentPanel) {
                updateStatus(ycToolVersion)
                updateProcessButtons()
            }
        }
    }

    private fun runLoadProfilesProcess() {
        val cliLocation = cliLocationField.text
        if (cliProcess != null || cliLocation.isEmpty()) return

        val process = try {
            Runtime.getRuntime().exec(
                arrayOf(cliLocation, "config", "profile", "list")
            )
        } catch (e: Exception) {
            null
        }

        cliProcess = process ?: return
        updateProcessButtons()
        profileField.history = emptyList()
        val timeoutTask = process.setupTimeout(Duration.ofSeconds(3))

        thread(name = "yc-profiles-load", isDaemon = true) {
            val rawProfilesList: List<String>? = try {
                process.inputStream.bufferedReader().use { it.readLines() }
            } catch (e: Exception) {
                null
            }

            cliProcess = null
            timeoutTask.cancel()

            invokeLaterAt(this.contentPanel) {
                updateProcessButtons()

                if (!rawProfilesList.isNullOrEmpty()) {
                    val profiles = mutableListOf("")
                    rawProfilesList.mapNotNullTo(profiles) { it.split(" ").firstOrNull() }
                    setProfilesHistory(profiles)
                }
            }
        }
    }

    private fun setProfilesHistory(profiles: List<String>) {
        profileField.history = profiles
        if (profiles.size > 1) {
            profileField.selectedIndex = 1
        }
    }

    private fun updateStatus(ycToolVersion: String?) = invokeLaterAt(this.contentPanel) {
        if (ycToolVersion != null && ycToolVersion.startsWith("Yandex.Cloud")) {
            cliStatus.text = ycToolVersion
            cliStatus.icon = CloudIcons.Status.Success
        } else {
            cliStatus.text = "Invalid CLI Yandex.Cloud selected"
            cliStatus.icon = AllIcons.General.Error
        }
    }

    override fun doValidate(): ValidationInfo? {
        return when {
            cliLocationField.text.isEmpty() -> ValidationInfo("Please select CLI Yandex.Cloud location")
            else -> null
        }
    }

    override fun doOKAction() {
        if (!myOKAction.isEnabled) return

        cliLocation = cliLocationField.text
        val rawProfile = profileField.text
        profile = if (rawProfile.isNullOrEmpty()) null else rawProfile

        super.doOKAction()
    }

    override fun createCenterPanel(): JComponent = YCUI.gridPanel {
        YCUI.gridBag(horizontal = true) {
            withPreferredSize(650, 70)

            JBLabel("CLI Location:") addAs nextln(0.0)
            cliLocationField addAs next(1.0)
            cliCheckButton addAs next(0.0)

            JBLabel("Profile:") addAs nextln(0.0)
            profileField addAs next(1.0)
            loadProfilesButton addAs next(0.0)

            cliStatus addAs fullLine()
        }
    }

    private class ProfilesListRenderer : ColoredListCellRenderer<String>() {
        override fun customizeCellRenderer(
            list: JList<out String>,
            value: String,
            index: Int,
            selected: Boolean,
            hasFocus: Boolean
        ) {
            if (value.isEmpty()) {
                append("(No profile)", SimpleTextAttributes.GRAYED_ATTRIBUTES)
            } else {
                append(value, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            }
        }
    }
}