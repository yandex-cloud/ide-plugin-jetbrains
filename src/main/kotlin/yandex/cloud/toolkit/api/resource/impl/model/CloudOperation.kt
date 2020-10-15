package yandex.cloud.toolkit.api.resource.impl.model

import com.intellij.openapi.project.Project
import yandex.cloud.api.operation.OperationOuterClass
import yandex.cloud.toolkit.ui.action.ShowOperationsStatusAction
import yandex.cloud.toolkit.util.*

class CloudOperation(
    val name: String,
    val data: Maybe<OperationOuterClass.Operation>
)

class CloudOperationResult(
    val operation: CloudOperation,
    val result: Maybe<ActionsBundle>,
    val logsUrl: String?
)

val CloudOperationResult.isSuccess: Boolean get() = result.hasValue

fun CloudOperationResult.notifySuccess(project: Project, message: String) {
    listOf(this).notifySuccess(project, message)
}

fun CloudOperationResult.notifyError(project: Project, message: String) {
    listOf(this).notifyError(project, message)
}

fun List<CloudOperationResult>.notifySuccess(project: Project, message: String) {
    val notification = infoNotification(
        "Yandex.Cloud Operations",
        "Yandex.Cloud Operations",
        message
    )
    notification.withAction(ShowOperationsStatusAction(this))
    notification.showAt(project)
}

fun List<CloudOperationResult>.notifyError(project: Project, message: String) {
    val notification = errorNotification(
        "Yandex.Cloud Operations",
        "Yandex.Cloud Operations",
        message
    )
    notification.withAction(ShowOperationsStatusAction(this))
    notification.showAt(project)
}
