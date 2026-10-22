package com.exodidio.mimi.widgets

import android.content.Context

/**
 * The song a widget is showing.
 *
 * The shape is the widget's own rather than the library's: a widget draws a cover, a
 * name and an artist, and it needs to know whether the music is playing so that the
 * button shows the right one.
 */
data class WidgetSong(
    /** How the player knows the song, which is the media store uri. */
    val mediaId: String,
    val title: String,
    val artist: String?,
    val album: String?,
    /** How long the song is, in milliseconds. */
    val durationMs: Long,
    /** True while the song is playing rather than paused. */
    val playing: Boolean,
)

/**
 * What was playing, kept where the widgets can find it.
 *
 * A widget is drawn by the system rather than by a screen, so it cannot ask the player
 * for anything: it asks this instead. The service writes the song down as it changes,
 * and the widget reads it back when it is drawn, which is what lets the widget be right
 * when the app is not running at all.
 */
class NowPlayingStore(context: Context) {

    private val preferences = context.applicationContext
        .getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** Writes down the song that is playing. */
    fun save(song: WidgetSong) {
        preferences.edit()
            .putString(KEY_MEDIA_ID, song.mediaId)
            .putString(KEY_TITLE, song.title)
            .putString(KEY_ARTIST, song.artist)
            .putString(KEY_ALBUM, song.album)
            .putLong(KEY_DURATION, song.durationMs)
            .putBoolean(KEY_PLAYING, song.playing)
            .apply()
    }

    /** The song that was playing, or null when nothing has been written down yet. */
    fun read(): WidgetSong? {
        val mediaId = preferences.getString(KEY_MEDIA_ID, null)?.takeIf { it.isNotBlank() }
            ?: return null
        return WidgetSong(
            mediaId = mediaId,
            title = preferences.getString(KEY_TITLE, null).orEmpty(),
            artist = preferences.getString(KEY_ARTIST, null),
            album = preferences.getString(KEY_ALBUM, null),
            durationMs = preferences.getLong(KEY_DURATION, 0L),
            playing = preferences.getBoolean(KEY_PLAYING, false),
        )
    }

    private companion object {
        /** One file for one thing: the widget and the service both know this name. */
        const val FILE = "mimi_widget_now_playing"

        const val KEY_MEDIA_ID = "media_id"
        const val KEY_TITLE = "title"
        const val KEY_ARTIST = "artist"
        const val KEY_ALBUM = "album"
        const val KEY_DURATION = "duration_ms"
        const val KEY_PLAYING = "playing"
    }
}
