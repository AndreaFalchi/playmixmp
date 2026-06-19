package com.falchi.playmixmp

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

class AndroidNmlParser : NmlParser {
    override suspend fun parse(xmlData: String): Pair<Map<String, TraktorCollectionTrack>, List<String>> {
        val parser = Xml.newPullParser()
        parser.setInput(StringReader(xmlData))
        
        val collection = mutableMapOf<String, TraktorCollectionTrack>()
        val playlist = mutableListOf<String>()
        
        var currentTrackKey: String? = null
        var currentTitle: String? = null
        var currentArtist: String? = null
        var currentComment: String? = null
        var currentTraktorKey: String? = null
        var currentFile: String? = null
        var currentBpm: Float? = null
        val currentCuePoints = mutableListOf<TraktorCuePoint>()

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "ENTRY" -> {
                        currentTitle = parser.getAttributeValue(null, "TITLE")
                        currentArtist = parser.getAttributeValue(null, "ARTIST")
                        currentComment = null
                        currentCuePoints.clear()
                        currentBpm = null
                    }
                    "INFO" -> {
                        currentComment = parser.getAttributeValue(null, "COMMENT")
                        currentTraktorKey = parser.getAttributeValue(null, "KEY")
                    }
                    "LOCATION" -> {
                        val file = parser.getAttributeValue(null, "FILE")
                        currentFile = file
                        currentTrackKey = file ?: parser.getAttributeValue(null, "VOLUME")
                    }
                    "TEMPO" -> {
                        currentBpm = parser.getAttributeValue(null, "BPM")?.toFloatOrNull()
                    }
                    "CUE_V2" -> {
                        val name = parser.getAttributeValue(null, "NAME")
                        val start = parser.getAttributeValue(null, "START")?.toDoubleOrNull()
                        val hotcue = parser.getAttributeValue(null, "HOTCUE")?.toIntOrNull() ?: -1
                        val type = parser.getAttributeValue(null, "TYPE")?.toIntOrNull() ?: 0
                        if (start != null) {
                            currentCuePoints.add(TraktorCuePoint(name ?: "Cue", start.toLong(), hotcue, type))
                        }
                    }
                    "PRIMARYKEY" -> {
                        val key = parser.getAttributeValue(null, "KEY")
                        if (key != null) playlist.add(key)
                    }
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.name == "ENTRY" && currentTrackKey != null) {
                    collection[currentTrackKey] = TraktorCollectionTrack(
                        key = currentTrackKey,
                        title = currentTitle ?: "",
                        artist = currentArtist ?: "",
                        fileName = currentFile,
                        comment = currentComment,
                        bpm = currentBpm,
                        traktorKey = currentTraktorKey,
                        cuePoints = currentCuePoints.toList()
                    )
                    currentTrackKey = null
                }
            }
            eventType = parser.next()
        }
        return collection to playlist
    }
}
