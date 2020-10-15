package yandex.cloud.toolkit.ui.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import yandex.cloud.toolkit.api.auth.CloudAuthData
import yandex.cloud.toolkit.api.resource.impl.model.CloudFunctionVersion
import yandex.cloud.toolkit.api.service.CloudOperationService
import yandex.cloud.toolkit.ui.component.FunctionVersionTagsList
import yandex.cloud.toolkit.util.withPreferredSize
import javax.swing.JComponent

class EditFunctionVersionTagsDialog(
    val project: Project,
    val authData: CloudAuthData,
    val versions: List<CloudFunctionVersion>,
    val version: CloudFunctionVersion
) : DialogWrapper(true) {

    private val list = FunctionVersionTagsList(version, versions)

    init {
        list.tags = version.data.tagsList.toSet()

        title = "Edit Yandex.Cloud Function Version Tags"
        init()
    }

    override fun createCenterPanel(): JComponent {
        return list.withPreferredSize(300, 140)
    }

    override fun doOKAction() {
        if (!okAction.isEnabled) return
        CloudOperationService.instance.setFunctionVersionTags(project, authData, version, list.tags)
        super.doOKAction()
    }
}