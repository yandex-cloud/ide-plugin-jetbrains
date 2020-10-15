package yandex.cloud.toolkit.api.auth

import java.util.*

class CloudAuthDataUpdatedEvent(val authData: CloudAuthData?)

interface CloudAuthListener : EventListener {

    fun onAuthDataUpdate(event: CloudAuthDataUpdatedEvent)
}