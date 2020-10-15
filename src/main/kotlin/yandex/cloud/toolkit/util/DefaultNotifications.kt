package yandex.cloud.toolkit.util

import com.intellij.openapi.project.Project
import yandex.cloud.toolkit.api.resource.impl.model.CloudFunction
import yandex.cloud.toolkit.ui.action.DeployFunctionAction
import yandex.cloud.toolkit.ui.action.IgnoreAction
import yandex.cloud.toolkit.ui.action.ManageProfilesAction
import java.time.Duration

fun Project.showAuthenticationNotification() {
    errorNotification(
        "Yandex Cloud",
        "Yandex Cloud",
        "You are not authenticated in Yandex Cloud"
    ).withAction(ManageProfilesAction()).showAt(this, Duration.ofSeconds(15))
}

fun Project.showNoFunctionVersionsNotification(function: CloudFunction, ignoreAction: (() -> Unit)?) {
    val notification = warningNotification(
        "Yandex Cloud",
        "Yandex Cloud Functions",
        "Function '$function' has no versions to run"
    ).withAction(DeployFunctionAction(this, function))
    if (ignoreAction != null) notification.withAction(IgnoreAction(ignoreAction))
    notification.showAt(this, Duration.ofSeconds(10))
}

fun Project.showNoFunctionVersionWithTagNotification(function: CloudFunction, versionTag: String) {
    warningNotification(
        "Yandex Cloud",
        "Yandex Cloud Functions",
        "Function '$function' has no version with tag '$versionTag'"
    ).showAt(this, Duration.ofSeconds(10))
}