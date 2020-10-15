package yandex.cloud.toolkit.api.profile

import com.intellij.openapi.components.ServiceManager
import java.util.*

interface CloudProfileService {

    fun getProfiles(): List<CloudProfile>
    fun getProfile(profileId: UUID): CloudProfile?
    fun addProfile(profile: CloudProfile)
    fun deleteProfile(profileId: UUID)

    companion object {
        val instance: CloudProfileService get() = ServiceManager.getService(CloudProfileService::class.java)
    }
}