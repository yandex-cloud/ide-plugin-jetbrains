package yandex.cloud.toolkit.api.profile.impl

import com.intellij.openapi.components.*
import com.intellij.openapi.diagnostic.logger
import yandex.cloud.toolkit.api.profile.CloudProfile
import yandex.cloud.toolkit.api.profile.CloudProfileData
import yandex.cloud.toolkit.api.profile.CloudProfileService
import yandex.cloud.toolkit.util.JustValue
import yandex.cloud.toolkit.util.NoValue
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayList

@State(name = "Yandex.Cloud Toolkit Profiles", storages = [(Storage("yandexCloudProfiles.xml"))])
class CloudProfileServiceImpl : CloudProfileService, PersistentStateComponent<CloudProfileServiceImpl.State> {

    companion object {
        private val log = logger<CloudProfileServiceImpl>()
    }

    val profiles = ConcurrentHashMap<UUID, CloudProfile>()

    override fun getProfiles(): List<CloudProfile> = ArrayList(profiles.values)

    override fun getProfile(profileId: UUID): CloudProfile? {
        return profiles[profileId]
    }

    override fun addProfile(profile: CloudProfile) {
        profiles.putIfAbsent(profile.id, profile)
        log.info("Added profile ${profile.id} (${profile.displayName}) authenticated by ${profile.authMethod.descriptor.name}")
    }

    override fun deleteProfile(profileId: UUID) {
        val profile = profiles.remove(profileId) ?: return
        profile.dispose()
        log.info("Profile ${profile.id} (${profile.displayName}) deleted")
    }

    override fun getState(): State = State().apply {
        for ((profileId, profile) in this@CloudProfileServiceImpl.profiles) {
            this.profiles[profileId.toString()] = profile.getSaveData()
        }
    }

    override fun loadState(state: State) {
        profiles.clear()
        for ((rawId, profileData) in state.profiles) {
            when (val profile = CloudProfile.fromData(rawId, profileData)) {
                is JustValue -> addProfile(profile.value)
                is NoValue -> log.error("Failed to load profile '$rawId': ${profile.error.message}")
            }
        }
    }

    class State : BaseState() {
        var profiles by map<String, CloudProfileData>()
    }
}