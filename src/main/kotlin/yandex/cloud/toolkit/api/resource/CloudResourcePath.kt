package yandex.cloud.toolkit.api.resource

import java.util.*

data class CloudResourcePath(val dependency: String, val resourceId: String, val next: CloudResourcePath? = null) {

    fun toString(joiner: StringJoiner) {
        if (dependency.isNotEmpty()) joiner.add("$dependency($resourceId)")
        next?.toString(joiner)
    }

    override fun toString(): String = StringJoiner(".").apply(this::toString).toString()

    companion object {

        fun fromString(value: String): CloudResourcePath? {
            val paths = value.split(".")
            var path: CloudResourcePath? = null

            for (i in paths.lastIndex downTo 0) {
                val rawPath = paths[i]
                if (rawPath.last() != ')') return null
                val separator = rawPath.indexOf('(')
                if (separator !in 0 until rawPath.lastIndex) return null
                path = CloudResourcePath(
                    rawPath.substring(0, separator),
                    rawPath.substring(separator + 1, rawPath.lastIndex),
                    path
                )
            }

            return path
        }
    }
}