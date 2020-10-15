package yandex.cloud.toolkit.api.auth

import com.intellij.openapi.project.Project
import yandex.cloud.toolkit.util.Maybe
import javax.swing.Icon

interface CloudAuthMethod {

    val descriptor: CloudAuthMethodDescriptor

    fun authenticate(project: Project, currentAuthData: CloudAuthData?, callback: (Maybe<CloudAuthData>) -> Unit)
    fun createAuthData(): Maybe<CloudAuthData>
    fun getAuthenticatedName(): String?

    fun saveData(target: MutableMap<String, String>)
    fun loadData(target: MutableMap<String, String>)
}

open class CloudAuthMethodDescriptor(
    val id: String,
    val name: String,
    val icon: Icon,
    private val factory: () -> CloudAuthMethod
) {

    fun createAuthMethod(): CloudAuthMethod = factory()
}

val CloudAuthMethod.id: String get() = descriptor.id