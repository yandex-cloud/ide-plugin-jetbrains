package yandex.cloud.toolkit.api.auth

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import yandex.cloud.toolkit.util.Maybe

interface CloudOAuthService {

    fun requestToken(project: Project, tokenCallback: (Maybe<String>) -> Unit)
    fun requestLogin(token: String): Maybe<String>

    fun encryptToken(token: String): String
    fun decryptToken(data: String): Maybe<String>

    companion object {
        val instance: CloudOAuthService get() = ServiceManager.getService(CloudOAuthService::class.java)
    }
}