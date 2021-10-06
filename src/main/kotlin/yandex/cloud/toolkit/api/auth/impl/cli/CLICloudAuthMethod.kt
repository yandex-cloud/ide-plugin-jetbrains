package yandex.cloud.toolkit.api.auth.impl.cli

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import icons.CloudIcons
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.SystemUtils
import yandex.cloud.toolkit.api.auth.CloudAuthData
import yandex.cloud.toolkit.api.auth.CloudAuthMethod
import yandex.cloud.toolkit.api.auth.CloudAuthMethodDescriptor
import yandex.cloud.toolkit.ui.dialog.CLIAuthConfigDialog
import yandex.cloud.toolkit.util.*
import yandex.cloud.toolkit.util.remote.resource.PresentableResourceStatus
import java.time.Duration
import java.util.concurrent.CancellationException

class CLICloudAuthMethod : CloudAuthMethod {

    object Descriptor : CloudAuthMethodDescriptor(
        "cli",
        "CLI Yandex.Cloud",
        CloudIcons.Other.Terminal,
        ::CLICloudAuthMethod
    )

    companion object {
        private const val DEFAULT_YC_LOCATION = "yandex-cloud/bin/yc"
    }

    override val descriptor: CloudAuthMethodDescriptor get() = Descriptor
    private var config: Config? = null

    private val defaultCliLocation: String
        get() = SystemUtils.getUserHome().resolve(DEFAULT_YC_LOCATION).absolutePath

    override fun authenticate(
        project: Project,
        currentAuthData: CloudAuthData?,
        callback: (Maybe<CloudAuthData>) -> Unit
    ) {
        invokeLater {
            val dialog = CLIAuthConfigDialog(config ?: Config(defaultCliLocation, null))
            val success = dialog.showAndGet()

            if (success) {
                config = Config(dialog.cliLocation, dialog.profile)
                callback(createAuthData())
            } else {
                callback(NoValue(CancellationException()))
            }
        }
    }

    override fun getAuthenticatedName(): String? {
        return if (config != null) {
            val profile = config?.profile
            if (profile != null) "CLI/$profile" else "CLI"
        } else null
    }

    override fun saveData(target: MutableMap<String, String>) {
        config?.save(target)
    }

    override fun loadData(target: MutableMap<String, String>) {
        config = Config.load(target)
    }

    override fun createAuthData(): Maybe<CloudAuthData> {
        val config = this.config ?: return noResource(
            "Missing CLI location", PresentableResourceStatus.ProfileCorrupted
        )
        return resolveEndpoint(config).tryMap { endpoint ->
            CloudAuthData.byCLI(
                this,
                config,
                endpoint ?: CloudAuthData.DEFAULT_ENDPOINT
            )
        }
    }

    private fun resolveEndpoint(config: Config): Maybe<String?> {
        val cliLocation = config.cliLocation
        if (cliLocation.isEmpty()) return just(null)

        val args = mutableListOf(cliLocation, "config", "get", "endpoint")
        if (config.profile != null) {
            args += "--profile"
            args += config.profile
        }

        val process = try {
            Runtime.getRuntime().exec(args.toTypedArray())
        } catch (e: Exception) {
            return noResource("Failed to extract API endpoint from profile: " + e.message)
        }

        val cliOutput = process.withTimeout(Duration.ofSeconds(3)) { IOUtils.toString(inputStream, Charsets.UTF_8) }

        return when {
            cliOutput.isNullOrEmpty() -> {
                val error = IOUtils.toString(process.errorStream, Charsets.UTF_8)
                when {
                    error.isNullOrEmpty() -> just(null)
                    else -> noResource(
                        "Failed to extract API endpoint from profile\n$cliOutput",
                        PresentableResourceStatus.Unresolved
                    )
                }
            }
            else -> just(cliOutput.trim())
        }
    }

    data class Config(val cliLocation: String, val profile: String?) {
        fun save(target: MutableMap<String, String>) {
            target["cliLocation"] = cliLocation
            if (profile != null) target["profile"] = profile
        }

        companion object {
            fun load(target: MutableMap<String, String>): Config? {
                val cliLocation = target["cliLocation"] ?: return null
                val profile = target["profile"]
                return Config(cliLocation, profile)
            }
        }
    }
}