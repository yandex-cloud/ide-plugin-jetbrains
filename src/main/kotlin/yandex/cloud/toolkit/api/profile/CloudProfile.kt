package yandex.cloud.toolkit.api.profile

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import yandex.cloud.toolkit.api.auth.*
import yandex.cloud.toolkit.api.resource.impl.model.CloudUser
import yandex.cloud.toolkit.api.service.CloudRepository
import yandex.cloud.toolkit.util.*
import yandex.cloud.toolkit.util.remote.resource.*
import java.util.*
import javax.swing.Icon
import javax.swing.event.EventListenerList

class CloudProfile(
    val id: UUID,
    displayName: String,
    val authMethod: CloudAuthMethod
) : Disposable {

    private val listeners = EventListenerList()
    private val authData = RemoteResource<CloudAuthData>()

    var displayName: String = displayName
        private set

    val resourceUser = CloudUser(this)

    val icon: Icon get() = authMethod.descriptor.icon

    fun tryAuthenticate(project: Project, callback: (Maybe<CloudAuthData>) -> Unit = {}): Boolean {
        val currentAuthData = authData.value
        return authData.tryLoad({ authMethod.authenticate(project, currentAuthData, it) }) {
            handleAuthDataUpdate(it, currentAuthData, callback)
        }
    }

    fun tryRecreateAuthData(callback: (Maybe<CloudAuthData>) -> Unit = {}): Boolean {
        val currentAuthData = authData.value
        return authData.tryLoad({ authMethod.createAuthData().apply(it) }) {
            handleAuthDataUpdate(it, currentAuthData, callback)
        }
    }

    private fun handleAuthDataUpdate(
        authData: Maybe<CloudAuthData>,
        oldAuthData: CloudAuthData?,
        callback: (Maybe<CloudAuthData>) -> Unit
    ) {
        rename(authMethod.getAuthenticatedName() ?: "Unknown Profile")
        callback(authData)
        listeners.getListeners(ProfileLifecycleListener::class.java).forEach {
            it.onProfileAuthDataUpdated(this, oldAuthData)
        }
        if (oldAuthData != null && authData.value !== oldAuthData) {
            CloudRepository.instance.invalidateAuthData(oldAuthData)
        }
    }

    override fun dispose() {
        authData.value?.also(CloudRepository.instance::invalidateAuthData)
        listeners.getListeners(ProfileLifecycleListener::class.java).forEach {
            it.onProfileRemoved(this)
        }
    }

    fun rename(name: String) {
        if (name == displayName) return
        val oldName = displayName
        displayName = name
        listeners.getListeners(ProfileLifecycleListener::class.java).forEach {
            it.onProfileRenamed(this, oldName)
        }
    }

    fun addListener(listener: ProfileLifecycleListener) {
        listeners.add(ProfileLifecycleListener::class.java, listener)
    }

    fun removeListener(listener: ProfileLifecycleListener) {
        listeners.remove(ProfileLifecycleListener::class.java, listener)
    }

    fun getAuthData(toUse: Boolean): CloudAuthData? {
        if (toUse) {
            authData.updateState {
                if (it is EmptyResourceState<*>) {
                    it.fromMaybe(authMethod.createAuthData())
                } else it
            }
        }
        return authData.loadedValue
    }

    fun getAuthDataStatus(): PresentableResourceStatus? = authData.asPresentableStatus()
    fun getAuthDataError(): ResourceLoadingError? = authData.error

    fun getSaveData(): CloudProfileData = CloudProfileData().apply {
        this.displayName = this@CloudProfile.displayName
        this.authMethodId = this@CloudProfile.authMethod.id
        this.authData = mutableMapOf<String, String>().apply(authMethod::saveData)
    }

    companion object {
        fun fromData(rawId: String, data: CloudProfileData): Maybe<CloudProfile> {
            val profileId = DataUtils.asUUID(rawId)
                ?: return noResource("Invalid profile id")
            val authMethod = CloudAuthService.instance.getAuthMethod(data.authMethodId ?: "")
                ?: return noResource("Unknown auth method '${data.authMethodId}'")
            val displayName = data.displayName
                ?: return noResource("Missing display name")

            authMethod.loadData(data.authData)
            return just(CloudProfile(profileId, displayName, authMethod))
        }
    }
}

class CloudProfileData : BaseState() {
    var displayName by string()
    var authMethodId by string()
    var authData by map<String, String>()
}


fun SimpleColoredComponent.drawProfile(profile: CloudProfile) {
    append(profile.displayName, SimpleTextAttributes.REGULAR_ATTRIBUTES)
    append(" (${profile.authMethod.descriptor.name})", SimpleTextAttributes.GRAY_ATTRIBUTES)

    val status = profile.getAuthDataStatus()
    if (status != null) {
        append(" ")
        status.display(this)
    }

    val error = profile.getAuthDataError()
    if (error != null) {
        toolTipText = error.message
    }

    icon = if (status == null) profile.icon else AllIcons.General.Error
}