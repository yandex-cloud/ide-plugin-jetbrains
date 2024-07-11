package yandex.cloud.toolkit.api.util

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class LogLine(val text: String, val time: Date, val savedTime: Date, val ingestedTime: Date) {

    companion object {
        private var DATE_FORMAT: DateFormat = SimpleDateFormat("HH:mm:ss dd.MM.yyyy")
    }

//    constructor(log: LogEventOuterClass.LogEvent) : this(
//        log.message,
//        log.createdAt.asDate(),
//        log.savedAt.asDate(),
//        log.ingestedAt.asDate()
//    )

    override fun toString(): String = "${DATE_FORMAT.format(time)} | $text"
}