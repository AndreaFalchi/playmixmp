package com.falchi.playmixmp

import platform.UIKit.UIDevice
import platform.Foundation.NSBundle
import platform.Foundation.NSUserDefaults

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override val version: String = NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String ?: "Unknown"
}

class IosSettingsRepository : SettingsRepository {
    private val defaults = NSUserDefaults.standardUserDefaults
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = 
        if (defaults.objectForKey(key) != null) defaults.boolForKey(key) else defaultValue
    override fun putBoolean(key: String, value: Boolean) = defaults.setBool(value, forKey = key)
    override fun getLong(key: String, defaultValue: Long): Long = 
        if (defaults.objectForKey(key) != null) defaults.integerForKey(key) else defaultValue
    override fun putLong(key: String, value: Long) = defaults.setInteger(value, forKey = key)
    override fun getString(key: String, defaultValue: String): String = 
        defaults.stringForKey(key) ?: defaultValue
    override fun putString(key: String, value: String) = defaults.setObject(value, forKey = key)
}

actual fun getPlatform(): Platform = IOSPlatform()
