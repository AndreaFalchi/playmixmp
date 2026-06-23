package com.falchi.playmixmp

import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

interface FileLogger {
    fun log(message: String)
    fun getLogFilePath(): String
    fun readLogs(): String
    fun clearLogs()
}

object Logger {
    private var fileLogger: FileLogger? = null
    var isEnabled: Boolean = true

    fun initialize(logger: FileLogger) {
        fileLogger = logger
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (!isEnabled) return
        val timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).toString()
        val logMessage = "[$timestamp] ERROR: $message" + (throwable?.let { "\n${it.stackTraceToString()}" } ?: "")
        println(logMessage) // For logcat/console
        fileLogger?.log(logMessage)
    }

    fun i(message: String) {
        if (!isEnabled) return
        val timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).toString()
        val logMessage = "[$timestamp] INFO: $message"
        println(logMessage)
        fileLogger?.log(logMessage)
    }

    fun getLogFilePath(): String = fileLogger?.getLogFilePath() ?: "Not Initialized"
    fun readLogs(): String = fileLogger?.readLogs() ?: ""
    fun clearLogs() = fileLogger?.clearLogs()
}
