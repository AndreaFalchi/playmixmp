package com.falchi.playmixmp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import java.io.File

class MainActivity : ComponentActivity() {
    
    private lateinit var viewModel: MusicPlayerViewModel
    private lateinit var player1: AndroidAudioPlayer
    private lateinit var player2: AndroidAudioPlayer

    private val sharedPrefs by lazy { getSharedPreferences("playmixmp_settings", MODE_PRIVATE) }

    private val settingsRepository = object : SettingsRepository {
        override fun getBoolean(key: String, defaultValue: Boolean): Boolean = sharedPrefs.getBoolean(key, defaultValue)
        override fun putBoolean(key: String, value: Boolean) { sharedPrefs.edit { putBoolean(key, value) } }
        override fun getLong(key: String, defaultValue: Long): Long = sharedPrefs.getLong(key, defaultValue)
        override fun putLong(key: String, value: Long) { sharedPrefs.edit { putLong(key, value) } }
        override fun getString(key: String, defaultValue: String): String = sharedPrefs.getString(key, defaultValue) ?: defaultValue
        override fun putString(key: String, value: String) { sharedPrefs.edit { putString(key, value) } }
    }

    private val platformActions = object : PlatformActions {
        override fun setAlwaysOnTop(enabled: Boolean) {
            if (enabled) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        override fun isAlwaysOnTopPermissionGranted(): Boolean {
            return Settings.canDrawOverlays(this@MainActivity)
        }

        override fun requestAlwaysOnTopPermission() {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:$packageName".toUri()
            )
            startActivity(intent)
        }

        override fun shareLogFile(filePath: String) {
            val file = File(filePath)
            if (!file.exists()) {
                Toast.makeText(this@MainActivity, "Log file not found", Toast.LENGTH_SHORT).show()
                return
            }

            val uri = FileProvider.getUriForFile(
                this@MainActivity,
                "${packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share Logs"))
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (!isGranted) {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    private val pickFolderLauncher = 
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let {
                viewModel.loadFromFolder(it.toString())
            }
        }

    private val pickTraktorFileLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                try {
                    contentResolver.openInputStream(it)?.use { stream ->
                        val xmlData = stream.bufferedReader().readText()
                        android.util.Log.d("MainActivity", "Traktor file read successfully, size: ${xmlData.length}")
                        viewModel.loadTraktorFile(xmlData)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Error reading Traktor file", e)
                    Toast.makeText(this, "Error reading Traktor file: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Logger.i("MainActivity onCreate, savedInstanceState: ${savedInstanceState != null}")

        appContext = applicationContext

        player1 = AndroidAudioPlayer(this)
        player2 = AndroidAudioPlayer(this)
        val mediaLibrary = AndroidMediaLibrary(this)
        val nmlParser = AndroidNmlParser()
        viewModel = MusicPlayerViewModel(player1, player2, mediaLibrary, nmlParser, platformActions, settingsRepository, lifecycleScope)

        viewModel.onPickFolder = { pickFolder() }
        viewModel.onPickTraktorFile = { pickTraktorFile() }

        // AVVIO DEL SERVICE (Passo 3)
        val serviceIntent = Intent(this, MusicService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        checkPermissions()

        setContent {
            App(viewModel)
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.first())
        }
    }

    fun pickFolder() {
        pickFolderLauncher.launch(null)
    }

    fun pickTraktorFile() {
        pickTraktorFileLauncher.launch(arrayOf("application/octet-stream", "text/xml"))
    }

    override fun onResume() {
        super.onResume()
        Logger.i("MainActivity onResume. Playlist size: ${viewModel.songList.size}")
        // Update viewModel state if permission was granted while app was in background
        if (viewModel.isAlwaysOnTop && !platformActions.isAlwaysOnTopPermissionGranted()) {
            viewModel.isAlwaysOnTop = false
            platformActions.setAlwaysOnTop(false)
        }
    }

    override fun onPause() {
        super.onPause()
        Logger.i("MainActivity onPause")
    }

    override fun onStop() {
        super.onStop()
        Logger.i("MainActivity onStop")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Logger.i("MainActivity onSaveInstanceState")
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.i("MainActivity onDestroy")
        player1.release()
        player2.release()
    }
}
