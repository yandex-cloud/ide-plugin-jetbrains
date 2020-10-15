package yandex.cloud.toolkit.api.auth.impl

import yandex.cloud.toolkit.api.auth.CloudAuthMethod
import yandex.cloud.toolkit.api.auth.CloudAuthMethodDescriptor
import yandex.cloud.toolkit.api.auth.CloudAuthService
import yandex.cloud.toolkit.api.auth.impl.cli.CLICloudAuthMethod
import yandex.cloud.toolkit.api.auth.impl.oauth.OAuthCloudAuthMethod

class CloudAuthServiceImpl : CloudAuthService {

    private val authMethodDescriptors = mutableMapOf<String, CloudAuthMethodDescriptor>()

    init {
        addAuthMethod(OAuthCloudAuthMethod.Descriptor)
        addAuthMethod(CLICloudAuthMethod.Descriptor)
    }

    override fun getAuthMethod(id: String): CloudAuthMethod? {
        return authMethodDescriptors[id]?.createAuthMethod()
    }

    private fun addAuthMethod(descriptor: CloudAuthMethodDescriptor) {
        authMethodDescriptors[descriptor.id] = descriptor
    }

    override fun getAvailableAuthMethods(): Iterable<CloudAuthMethodDescriptor> = authMethodDescriptors.values
}