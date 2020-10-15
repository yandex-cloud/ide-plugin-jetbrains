package yandex.cloud.toolkit.util

import com.google.protobuf.Timestamp
import java.time.Instant
import java.util.*

fun Timestamp.asInstant(): Instant = Instant.ofEpochSecond(seconds, nanos.toLong())
fun Timestamp.asDate(): Date = Date.from(asInstant())