package yandex.cloud.toolkit.ui.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.components.JBTextArea
import yandex.cloud.toolkit.api.profile.CloudProfile
import yandex.cloud.toolkit.api.profile.drawProfile
import yandex.cloud.toolkit.util.YCUI
import yandex.cloud.toolkit.util.asEditable
import yandex.cloud.toolkit.util.invokeLaterAt
import yandex.cloud.toolkit.util.remote.resource.ResourceLoadingError
import yandex.cloud.toolkit.util.withPreferredSize
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import javax.swing.Action
import javax.swing.JComponent

class FixProfileDialog(
    val project: Project,
    val profile: CloudProfile,
    var error: ResourceLoadingError,
    val canReauth: Boolean
) :
    DialogWrapper(
        true
    ) {

    private val profileLabel = SimpleColoredComponent()
    private val errorArea = JBTextArea()

    init {
        profileLabel.drawProfile(profile)
        title = "Fix Yandex.Cloud Profile"
        init()

        errorArea.text = error.message
    }

    override fun createCenterPanel(): JComponent = YCUI.borderPanel {
        withPreferredSize(500, 200)

        YCUI.vbox {
            add(profileLabel)
            add(YCUI.separator("Error"))
        } addAs BorderLayout.NORTH

        YCUI.scrollPane(
            errorArea.asEditable(false)
        ) addAs BorderLayout.CENTER
    }

    override fun createLeftSideActions(): Array<Action> = when {
        canReauth -> arrayOf(ReAuthAction(), RecreateAction())
        else -> arrayOf(RecreateAction())
    }

    override fun createActions(): Array<Action> = arrayOf(okAction)

    private inner class ReAuthAction : DialogWrapperAction("Reauthenticate") {
        override fun doAction(e: ActionEvent?) {
            profile.tryRecreateAuthData {
                invokeLaterAt(this@FixProfileDialog.rootPane) {
                    close(CANCEL_EXIT_CODE)
                }
            }
        }
    }

    private inner class RecreateAction : DialogWrapperAction("Recreate") {
        override fun doAction(e: ActionEvent?) {
            profile.tryAuthenticate(project) {
                invokeLaterAt(this@FixProfileDialog.rootPane) {
                    close(CANCEL_EXIT_CODE)
                }
            }
        }
    }
}