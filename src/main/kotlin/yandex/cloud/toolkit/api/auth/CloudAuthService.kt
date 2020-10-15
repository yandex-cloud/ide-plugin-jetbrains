package yandex.cloud.toolkit.api.auth

import com.intellij.openapi.components.ServiceManager

interface CloudAuthService {

    fun getAvailableAuthMethods(): Iterable<CloudAuthMethodDescriptor>
    fun getAuthMethod(id: String): CloudAuthMethod?

    companion object {
        val instance: CloudAuthService get() = ServiceManager.getService(CloudAuthService::class.java)
    }
}