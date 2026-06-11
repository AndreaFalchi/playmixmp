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
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope

class MainActivity : ComponentActivity() {
    
    private lateinit var viewModel: MusicPlayerViewModel
    private lateinit var player1: AndroidAudioPlayer
    private lateinit var player2: AndroidAudioPlayer

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
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                viewModel.loadMusic()
            } else {
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

        appContext = applicationContext

        player1 = AndroidAudioPlayer(this)
        player2 = AndroidAudioPlayer(this)
        val mediaLibrary = AndroidMediaLibrary(this)
        val nmlParser = AndroidNmlParser()
        viewModel = MusicPlayerViewModel(player1, player2, mediaLibrary, nmlParser, platformActions, lifecycleScope)

        viewModel.onPickFolder = { pickFolder() }
        viewModel.onPickTraktorFile = { pickTraktorFile() }

        checkPermissions()

        setContent {
            App(viewModel)
        }
    }

    private fun checkPermissions() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            viewModel.loadMusic()
        } else {
            requestPermissionLauncher.launch(permission)
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
        // Update viewModel state if permission was granted while app was in background
        if (viewModel.isAlwaysOnTop && !platformActions.isAlwaysOnTopPermissionGranted()) {
            viewModel.isAlwaysOnTop = false
            platformActions.setAlwaysOnTop(false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player1.release()
        player2.release()
    }
}
