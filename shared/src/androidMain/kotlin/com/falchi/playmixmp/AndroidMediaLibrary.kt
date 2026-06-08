package com.falchi.playmixmp

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.documentfile.provider.DocumentFile

class AndroidMediaLibrary(private val context: Context) : MediaLibrary {
    override suspend fun getSongsFromDownloads(): List<Song> {
        return queryMediaStore("%${Environment.DIRECTORY_DOWNLOADS}%")
    }

    private fun queryMediaStore(pathPattern: String): List<Song> {
        val songsFound = mutableListOf<Song>()
        val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DISPLAY_NAME,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Audio.Media.RELATIVE_PATH else MediaStore.Audio.Media.DATA
        )

        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
        } else {
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DATA} LIKE ?"
        }
        val selectionArgs = arrayOf(pathPattern)

        try {
            context.contentResolver.query(
                collectionUri,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Audio.Media.TITLE} ASC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val displayNameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val duration = cursor.getLong(durCol)
                    if (duration < 1500) continue

                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    songsFound.add(
                        Song(
                            id = id,
                            title = cursor.getString(titleCol) ?: "Unknown Title",
                            artist = cursor.getString(artistCol) ?: "Unknown Artist",
                            album = cursor.getString(albumCol) ?: "Unknown Album",
                            duration = duration,
                            fileName = cursor.getString(displayNameCol),
                            contentUri = contentUri.toString()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("AndroidMediaLibrary", "Error querying MediaStore: ${e.message}")
        }
        return songsFound
    }

    override suspend fun getSongsFromFolder(folderPath: String): List<Song> {
        if (folderPath.startsWith("content://")) {
            val songsFound = mutableListOf<Song>()
            val uri = Uri.parse(folderPath)
            val directory = DocumentFile.fromTreeUri(context, uri)
            directory?.listFiles()?.forEach { file ->
                if (file.isFile && (file.name?.endsWith(".mp3", true) == true || file.name?.endsWith(".wav", true) == true)) {
                    songsFound.add(
                        Song(
                            id = file.name.hashCode().toLong(),
                            title = file.name?.substringBeforeLast(".") ?: "Unknown",
                            artist = "Unknown",
                            album = "Unknown",
                            duration = 0,
                            fileName = file.name,
                            contentUri = file.uri.toString()
                        )
                    )
                }
            }
            return songsFound
        }
        return queryMediaStore("%$folderPath%")
    }

    override suspend fun getSubfoldersWithAudio(): List<Pair<String, String>> {
        val folders = mutableSetOf<Pair<String, String>>()
        val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Audio.Media.RELATIVE_PATH else MediaStore.Audio.Media.DATA
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        try {
            context.contentResolver.query(collectionUri, projection, selection, null, null)?.use { cursor ->
                val pathCol = cursor.getColumnIndexOrThrow(projection[0])
                while (cursor.moveToNext()) {
                    val fullPath = cursor.getString(pathCol) ?: continue
                    if (fullPath.contains(Environment.DIRECTORY_DOWNLOADS)) {
                        val parts = fullPath.split("/")
                        val downloadIndex = parts.indexOf(Environment.DIRECTORY_DOWNLOADS)
                        if (downloadIndex != -1 && downloadIndex < parts.size - 1) {
                            val folderName = parts[downloadIndex + 1]
                            if (folderName.isNotEmpty()) {
                                // For Q+, relative path is something like "Download/MyFolder/"
                                // For older, it's absolute path.
                                val relativePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    "${Environment.DIRECTORY_DOWNLOADS}/$folderName/"
                                } else {
                                    folderName // Simplified for older versions
                                }
                                folders.add(folderName to relativePath)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AndroidMediaLibrary", "Error finding subfolders: ${e.message}")
        }
        return folders.toList().sortedBy { it.first }
    }
}
