package yandex.cloud.toolkit.api.auth.impl.oauth

import com.intellij.openapi.project.Project
import icons.CloudIcons
import yandex.cloud.toolkit.api.auth.CloudAuthData
import yandex.cloud.toolkit.api.auth.CloudAuthMethod
import yandex.cloud.toolkit.api.auth.CloudAuthMethodDescriptor
import yandex.cloud.toolkit.api.auth.CloudOAuthService
import yandex.cloud.toolkit.util.*
import yandex.cloud.toolkit.util.remote.resource.PresentableResourceStatus

class OAuthCloudAuthMethod : CloudAuthMethod {

    object Descriptor : CloudAuthMethodDescriptor(
        "ouath",
        "OAuth",
        CloudIcons.Resources.CloudUser,
        ::OAuthCloudAuthMethod
    )

    override val descriptor: CloudAuthMethodDescriptor get() = Descriptor

    private var token: Maybe<String> = noValue()
    private var login: Maybe<String> = noValue()

    override fun authenticate(
        project: Project,
        currentAuthData: CloudAuthData?,
        callback: (Maybe<CloudAuthData>) -> Unit
    ) {
        CloudOAuthService.instance.requestToken(project) {
            token = it
            login = it.map(CloudOAuthService.instance::requestLogin)
            callback(createAuthData())
        }
    }

    override fun getAuthenticatedName(): String? = login.getOrNull()

    override fun saveData(target: MutableMap<String, String>) {
        token.value?.also { target["token"] = CloudOAuthService.instance.encryptToken(it) }
        login.value?.also { target["login"] = it }
    }

    override fun loadData(target: MutableMap<String, String>) {
        token = target["token"]?.let(CloudOAuthService.instance::decryptToken) ?: noResource(
            "Missing OAuth token",
            PresentableResourceStatus.ProfileCorrupted
        )
        login = target["login"]?.let(::just) ?: noValue()
    }

    override fun createAuthData(): Maybe<CloudAuthData> = token.map {
        doMaybe {
            CloudAuthData.byOAuthToken(this, it)
        }
    }
}