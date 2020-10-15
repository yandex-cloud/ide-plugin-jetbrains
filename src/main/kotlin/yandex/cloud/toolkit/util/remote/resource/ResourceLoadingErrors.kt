package yandex.cloud.toolkit.util.remote.resource

import com.intellij.openapi.actionSystem.AnAction
import yandex.cloud.toolkit.ui.action.ManageProfilesAction
import yandex.cloud.toolkit.util.ActionsBundle

open class ResourceLoadingError(
    override val message: String,
    val status: PresentableResourceStatus
) : RuntimeException(message), ActionsBundle

class UnexpectedResourceLoadingError(error: Throwable) : ResourceLoadingError(
    error.message ?: "Unknown error: ${error::class.java}",
    PresentableResourceStatus.FailedToLoad
)

object MissingResourceLoadingError : ResourceLoadingError(
    "Resource is missing",
    PresentableResourceStatus.FailedToLoad
)

class UnauthenticatedException :
    ResourceLoadingError("Profile is not authenticated", PresentableResourceStatus.Unauthenticated) {

    override fun getActions(actions: MutableList<AnAction>) {
        actions += ManageProfilesAction()
    }
}