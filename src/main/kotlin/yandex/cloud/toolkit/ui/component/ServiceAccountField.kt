package yandex.cloud.toolkit.ui.component

import com.intellij.ide.ui.laf.darcula.ui.DarculaTextBorder
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBTextField
import yandex.cloud.api.iam.v1.ServiceAccountOuterClass
import yandex.cloud.toolkit.api.profile.impl.profileStorage
import yandex.cloud.toolkit.api.resource.findById
import yandex.cloud.toolkit.api.resource.impl.model.CloudServiceAccount
import yandex.cloud.toolkit.api.resource.impl.model.VirtualCloudFolder
import yandex.cloud.toolkit.api.service.CloudOperationService
import yandex.cloud.toolkit.util.*
import yandex.cloud.toolkit.util.task.backgroundTask
import java.awt.BorderLayout
import java.awt.event.ItemEvent
import javax.swing.DefaultComboBoxModel
import javax.swing.JList

class ServiceAccountField(val project: Project, val folderId: String?, serviceAccounts: List<CloudServiceAccount>?) :
    YCPanel(BorderLayout()), Disposable {

    @Volatile
    private var disposed = false

    @Volatile
    private var serviceAccounts: Maybe<List<CloudServiceAccount>>? = serviceAccounts?.asJust()

    private val field = JBTextField()
    private val box = ComboBox<ServiceAccountOuterClass.ServiceAccount?>()

    var value: String
        get() = this.field.text
        set(value) {
            this.field.text = value
            updateBox()
        }

    init {
        box.border = DarculaTextBorder()
        box.background = null
        box.isOpaque = false

        setBoxElements(serviceAccounts)
        box.renderer = ListRenderer()

        box.addItemListener {
            if (it.stateChange != ItemEvent.SELECTED) return@addItemListener
            val serviceAccount = box.selectedItem as? ServiceAccountOuterClass.ServiceAccount ?: return@addItemListener
            invokeLaterAt(this@ServiceAccountField) {
                if (field.text != serviceAccount.id) field.text = serviceAccount.id
            }
        }

        field.addCaretListener {
            updateBox()
        }

        if (serviceAccounts == null && !folderId.isNullOrEmpty()) {
            box.doOnFirstExpansion(this::loadServiceAccounts)
        }

        field addAs BorderLayout.CENTER
        box addAs BorderLayout.EAST
    }

    private fun updateBox() {
        val serviceAccountId = field.text
        val serviceAccount = box.selectedItem as? ServiceAccountOuterClass.ServiceAccount
        if (serviceAccount != null && serviceAccount.id == serviceAccountId) return
        box.selectedItem = serviceAccounts?.value?.findById(serviceAccountId)?.data
    }

    private fun loadServiceAccounts() {
        val user = project.profileStorage.profile?.resourceUser ?: return
        setBoxElements(listOf(null))

        backgroundTask(project, "Yandex.Cloud") {
            val folder = VirtualCloudFolder(user, folderId ?: "")
            val serviceAccounts = CloudOperationService.instance.fetchServiceAccounts(project, folder).handle()

            invokeLaterAt(this@ServiceAccountField) {
                if (disposed) return@invokeLaterAt
                this@ServiceAccountField.serviceAccounts = serviceAccounts
                if (serviceAccounts is JustValue) setBoxElements(serviceAccounts.value)
                box.reopenPopup()
                updateBox()
            }
        }
    }

    private fun setBoxElements(serviceAccounts: List<CloudServiceAccount?>?) {
        box.model = DefaultComboBoxModel(
            mutableListOf<ServiceAccountOuterClass.ServiceAccount?>().apply {
                add(null)
                serviceAccounts?.forEach { add(it?.data) }
            }.toTypedArray()
        )
        box.selectedIndex = 0
    }

    override fun dispose() {
        disposed = true
    }

    private inner class ListRenderer : ColoredListCellRenderer<ServiceAccountOuterClass.ServiceAccount?>() {
        override fun customizeCellRenderer(
            list: JList<out ServiceAccountOuterClass.ServiceAccount?>,
            value: ServiceAccountOuterClass.ServiceAccount?,
            index: Int,
            selected: Boolean,
            hasFocus: Boolean
        ) {
            when {
                value != null -> {
                    icon = CloudServiceAccount.Descriptor.icon
                    append(value.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                }
                index < 1 -> append("Custom", SimpleTextAttributes.GRAY_ATTRIBUTES)

                this@ServiceAccountField.serviceAccounts == null -> append(
                    "Loading...",
                    SimpleTextAttributes.GRAY_ATTRIBUTES
                )
                this@ServiceAccountField.serviceAccounts is NoValue -> append(
                    "Failed to load",
                    SimpleTextAttributes.ERROR_ATTRIBUTES
                )
            }
        }
    }
}