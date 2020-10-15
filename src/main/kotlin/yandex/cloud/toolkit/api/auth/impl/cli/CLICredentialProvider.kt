package yandex.cloud.toolkit.api.auth.impl.cli

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.diagnostic.logger
import org.apache.commons.io.IOUtils
import yandex.cloud.sdk.auth.Auth
import yandex.cloud.sdk.auth.IamToken
import yandex.cloud.sdk.auth.provider.AbstractCredentialProviderBuilder
import yandex.cloud.sdk.auth.provider.CredentialProvider
import yandex.cloud.toolkit.util.withTimeout
import java.io.IOException
import java.time.Duration

class CLICredentialProvider(
    private val cliLocation: String,
    private val profile: String?,
    private val tokenRequestTimeout: Duration
) : CredentialProvider {

    companion object {
        private val om = ObjectMapper()
        private val log = logger<CLICredentialProvider>()
    }

    override fun get(): IamToken {
        log.info("Token from CLI requested: $cliLocation --profile $profile")

        val args = mutableListOf(cliLocation, "iam", "create-token", "--format", "json")
        if (profile != null) {
            args += "--profile"
            args += profile
        }

        val process = try {
            Runtime.getRuntime().exec(args.toTypedArray())
        } catch (e: Exception) {
            log.error("Can not start CLI: ${e.message}")
            throw IOException("Failed to authenticate account - can not start CLI Yandex.Cloud:\n${e.message}")
        }

        val token = try {
            val cliOutput = process.withTimeout(tokenRequestTimeout) { IOUtils.toString(inputStream, Charsets.UTF_8) }
            val result = om.readTree(cliOutput)
            val rawToken = when {
                result == null -> throw IOException(IOUtils.toString(process.errorStream, Charsets.UTF_8))
                !result.has("iam_token") -> throw IOException("CLI did not return token")
                else -> result["iam_token"].textValue()
            }

            if (!rawToken.isNullOrEmpty()) rawToken else throw IOException("Invalid fetched token")
        } catch (e: Exception) {
            log.error("Can not get token from CLI: ${e.message}")
            throw IOException("Failed to authenticate account - can not get token from CLI Yandex.Cloud:\n${e.message}")
        }

        return Auth.iamTokenBuilder().token(token).build().get()
    }

    class Builder : AbstractCredentialProviderBuilder<Builder>() {

        private var cliLocation: String = "yc"
        private var profile: String? = null
        private var tokenRequestTimeout: Duration = Duration.ofSeconds(10)

        fun cliLocation(path: String): Builder {
            cliLocation = path
            return this
        }

        fun profile(profile: String?): Builder {
            this.profile = profile
            return this
        }

        fun tokenRequestTimeout(timeout: Duration): Builder {
            tokenRequestTimeout = timeout
            return this
        }

        override fun providerBuild(): CredentialProvider =
            CLICredentialProvider(cliLocation, profile, tokenRequestTimeout)
    }
}

fun cliAuthBuilder(): CLICredentialProvider.Builder = CLICredentialProvider.Builder()