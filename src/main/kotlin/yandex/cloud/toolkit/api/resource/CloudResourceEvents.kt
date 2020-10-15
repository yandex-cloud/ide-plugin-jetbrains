package yandex.cloud.toolkit.api.resource

import java.util.*

class CloudResourceUpdateEvent(val resource: CloudResource, val isChildrenChanged: Boolean)

interface CloudResourceListener : EventListener {

    fun onCloudResourcesUpdate(events: List<CloudResourceUpdateEvent>)
}