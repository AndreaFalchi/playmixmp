package com.falchi.playmixmp

import android.os.Build

// We will initialize this from the Android App side
lateinit var appContext: android.content.Context

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val version: String by lazy {
        try {
            val packageInfo = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
            val buildNumber = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            "${packageInfo.versionName} (Build $buildNumber)"
        } catch (_: Exception) {
            "Unknown"
        }
    }
}

actual fun getPlatform(): Platform = AndroidPlatform()