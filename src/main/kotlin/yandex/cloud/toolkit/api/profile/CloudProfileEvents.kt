package yandex.cloud.toolkit.api.profile

import yandex.cloud.toolkit.api.auth.CloudAuthData
import java.util.*

interface ProfileLifecycleListener : EventListener {

    fun onProfileRemoved(profile: CloudProfile)
    fun onProfileRenamed(profile: CloudProfile, oldName: String)
    fun onProfileAuthDataUpdated(profile: CloudProfile, oldAuthData: CloudAuthData?)

}

interface ProfileStorageListener : EventListener {

    fun onProfileSelected(profile: CloudProfile?)
    fun onProfileRenamed(profile: CloudProfile)
    fun onProfileAuthDataUpdated(profile: CloudProfile)
}