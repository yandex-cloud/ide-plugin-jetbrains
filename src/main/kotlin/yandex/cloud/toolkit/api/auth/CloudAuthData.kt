package yandex.cloud.toolkit.api.auth

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.util.PlatformUtils
import yandex.cloud.sdk.ChannelFactory
import yandex.cloud.sdk.ServiceFactory
import yandex.cloud.sdk.auth.Auth
import yandex.cloud.sdk.auth.provider.CredentialProvider
import yandex.cloud.toolkit.api.auth.impl.cli.CLICloudAuthMethod
import yandex.cloud.toolkit.api.auth.impl.cli.cliAuthBuilder
import yandex.cloud.toolkit.api.auth.impl.oauth.OAuthCloudAuthMethod
import java.time.Duration

data class CloudAuthData(
    val usedAuthMethod: CloudAuthMethod,
    val credentials: CredentialProvider,
    val serviceFactory: ServiceFactory,
    val channelFactory: ChannelFactory
) {

    val iamToken: String get() = credentials.get().token

    companion object {

        val USER_AGENT = "YandexCloudIntelliJPlugin/" +
                PluginManagerCore.getPlugin(PluginId.getId("yandex.cloud-toolkit"))!!.version +
                " IntelliJ." + PlatformUtils.getPlatformPrefix() + "/" +
                PluginManagerCore.getPlugin(PluginManagerCore.CORE_ID)?.version

        private fun byCredentials(authMethod: CloudAuthMethod, credentials: CredentialProvider): CloudAuthData {
            val channelFactory = ChannelFactory(ChannelFactory.DEFAULT_ENDPOINT, USER_AGENT)
            val serviceFactory = ServiceFactory.builder()
                .credentialProvider(credentials)
                .channelFactory(channelFactory)
                .requestTimeout(Duration.ofMinutes(1))
                .build()

            return CloudAuthData(authMethod, credentials, serviceFactory, channelFactory)
        }

        fun byOAuthToken(authMethod: OAuthCloudAuthMethod, token: String): CloudAuthData {
            val credentials = Auth.oauthTokenBuilder()
                .oauth(token)
                .enableCache()
                .build()

            return byCredentials(authMethod, credentials)
        }

        fun byCLI(authMethod: CLICloudAuthMethod, config: CLICloudAuthMethod.Config): CloudAuthData {
            val credentials = cliAuthBuilder()
                .cliLocation(config.cliLocation)
                .profile(config.profile)
                .enableCache()
                .build()

            return byCredentials(authMethod, credentials)
        }
    }
}