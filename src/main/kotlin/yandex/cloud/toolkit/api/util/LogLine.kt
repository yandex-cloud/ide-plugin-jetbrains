package yandex.cloud.toolkit.api.util

import yandex.cloud.api.logging.v1.LogEntryOuterClass
import yandex.cloud.toolkit.util.asDate
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class LogLine(val text: String, val time: Date, val savedTime: Date, val ingestedTime: Date) {

    companion object {
        private var DATE_FORMAT: DateFormat = SimpleDateFormat("HH:mm:ss dd.MM.yyyy")
    }

    constructor(log: LogEntryOuterClass.LogEntry) : this(
        log.message,
        log.timestamp.asDate(),
        log.savedAt.asDate(),
        log.ingestedAt.asDate()
    )

    override fun toString(): String = "${DATE_FORMAT.format(time)} | $text"
}
