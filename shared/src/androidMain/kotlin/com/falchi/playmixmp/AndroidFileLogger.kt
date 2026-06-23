package com.falchi.playmixmp

import android.content.Context
import java.io.File

class AndroidFileLogger(private val context: Context) : FileLogger {
    private val logFile = File(context.filesDir, "app_logs.txt")

    override fun log(message: String) {
        try {
            logFile.appendText("$message\n\n")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getLogFilePath(): String = logFile.absolutePath

    override fun readLogs(): String {
        return if (logFile.exists()) logFile.readText() else "No logs found."
    }

    override fun clearLogs() {
        if (logFile.exists()) logFile.delete()
    }
}
