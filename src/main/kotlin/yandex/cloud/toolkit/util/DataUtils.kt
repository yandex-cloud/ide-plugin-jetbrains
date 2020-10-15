package yandex.cloud.toolkit.util

import java.util.*

object DataUtils {

    fun asUUID(name: String?): UUID? {
        if (name == null) return null
        return try {
            UUID.fromString(name)
        } catch (e: IllegalArgumentException) {
            return null
        }
    }
}