package yandex.cloud.toolkit.api.profile.impl

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import yandex.cloud.toolkit.api.auth.CloudAuthData
import yandex.cloud.toolkit.api.profile.CloudProfile
import yandex.cloud.toolkit.api.profile.CloudProfileService
import yandex.cloud.toolkit.api.profile.ProfileLifecycleListener
import yandex.cloud.toolkit.api.profile.ProfileStorageListener
import yandex.cloud.toolkit.util.DataUtils
import javax.swing.event.EventListenerList

@Service
class CloudProfileStorage(private val project: Project) : ProfileLifecycleListener {

    companion object {
        private const val PROFILE_KEY = "yandex.cloud.toolkit.cloud-profile"
    }

    var profile: CloudProfile? = null
        private set

    private val listeners = EventListenerList()

    init {
        val rawProfileId = PropertiesComponent.getInstance().getValue(PROFILE_KEY)
        val profileId = DataUtils.asUUID(rawProfileId)
        if (profileId != null) {
            selectProfile(CloudProfileService.instance.getProfile(profileId))
        }
    }

    override fun onProfileRemoved(profile: CloudProfile) {
        selectProfile(null)
    }

    override fun onProfileRenamed(profile: CloudProfile, oldName: String) {
        listeners.getListeners(ProfileStorageListener::class.java).forEach {
            it.onProfileRenamed(profile)
        }
    }

    override fun onProfileAuthDataUpdated(profile: CloudProfile, oldAuthData: CloudAuthData?) {
        listeners.getListeners(ProfileStorageListener::class.java).forEach {
            it.onProfileAuthDataUpdated(profile)
        }
    }

    fun selectProfile(profile: CloudProfile?) {
        if (profile === this.profile) return
        this.profile?.removeListener(this)
        this.profile = profile
        this.profile?.addListener(this)

        PropertiesComponent.getInstance().setValue(PROFILE_KEY, profile?.id?.toString())
        listeners.getListeners(ProfileStorageListener::class.java).forEach {
            it.onProfileSelected(profile)
        }
    }

    fun registerListener(listener: ProfileStorageListener) {
        listeners.add(ProfileStorageListener::class.java, listener)
    }

    fun unregisterListener(listener: ProfileStorageListener) {
        listeners.remove(ProfileStorageListener::class.java, listener)
    }
}

val Project.profileStorage: CloudProfileStorage get() = getService(CloudProfileStorage::class.java)