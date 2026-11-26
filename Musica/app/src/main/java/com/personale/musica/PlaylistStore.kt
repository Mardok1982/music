package com.personale.musica

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Playlist(
    val id: Long,
    val name: String,
    val songIds: List<Long>,
)

/** Salva le playlist in un file JSON nella memoria privata dell'app. */
class PlaylistStore(context: Context) {

    private val file = File(context.filesDir, "playlists.json")

    fun load(): List<Playlist> {
        if (!file.exists()) return emptyList()
        return try {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val ids = o.getJSONArray("songs")
                Playlist(
                    id = o.getLong("id"),
                    name = o.getString("name"),
                    songIds = (0 until ids.length()).map { ids.getLong(it) },
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(playlists: List<Playlist>) {
        val arr = JSONArray()
        playlists.forEach { p ->
            arr.put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("songs", JSONArray(p.songIds))
            })
        }
        // scrittura sicura: prima su file temporaneo, poi rinomina
        val tmp = File(file.parentFile, "playlists.json.tmp")
        tmp.writeText(arr.toString())
        tmp.renameTo(file)
    }
}
