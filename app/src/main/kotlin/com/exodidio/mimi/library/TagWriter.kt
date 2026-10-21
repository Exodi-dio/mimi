package com.exodidio.mimi.library

import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Writing tags back into the file.
 *
 * Since Android 11 the system will not let an app change the tags of a file it does not
 * own unless the listener agrees to it, and the agreement is asked for through the
 * system's own dialog. The app asks for that agreement, writes the tags once it has
 * been given, and then reads the row back to see whether the file really holds them.
 *
 * Reading the row back matters because a write can be accepted and change nothing: a
 * format the provider cannot tag, or a file on a volume it cannot write to. The values
 * the platform reports are what the app believes from then on.
 */
object TagWriter {

    /**
     * The system's request for permission to write to the songs named.
     *
     * Null when the platform cannot ask, which is every version before Android 11: those
     * versions will not let an app write into a file it does not own at all, so there is
     * nothing to ask about.
     */
    fun writeRequest(context: Context, uris: List<Uri>): IntentSender? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        if (uris.isEmpty()) return null
        return runCatching {
            MediaStore.createWriteRequest(context.contentResolver, uris).intentSender
        }.getOrNull()
    }

    /**
     * Writes the tags into the files named.
     *
     * Only the fields that changed are written, because rewriting a tag that is already
     * right asks the platform for nothing and can cost a tag the file had. A field the
     * listener emptied is written as an empty string, which is how the platform is told
     * that there is no value there any more.
     *
     * False when the platform refused the write or nothing came of it, which is the
     * signal to keep the correction in the app's own library instead.
     */
    suspend fun applyTags(
        context: Context,
        uris: List<Uri>,
        values: TagValues,
        changed: Set<TagField>,
    ): Boolean = withContext(Dispatchers.IO) {
        if (uris.isEmpty() || changed.isEmpty()) return@withContext false

        val content = ContentValues()
        if (TagField.TITLE in changed) content.put(MediaStore.Audio.Media.TITLE, values.title)
        if (TagField.ARTIST in changed) {
            content.put(MediaStore.Audio.Media.ARTIST, values.artist.orEmpty())
        }
        if (TagField.ALBUM in changed) {
            content.put(MediaStore.Audio.Media.ALBUM, values.album.orEmpty())
        }
        if (TagField.ALBUM_ARTIST in changed) {
            content.put(MediaStore.Audio.Media.ALBUM_ARTIST, values.albumArtist.orEmpty())
        }
        if (TagField.YEAR in changed) {
            content.put(MediaStore.Audio.Media.YEAR, values.year ?: 0)
        }
        if (TagField.TRACK_NUMBER in changed) {
            content.put(MediaStore.Audio.Media.TRACK, values.trackNumber ?: 0)
        }
        if (TagField.GENRE in changed) {
            content.put(MediaStore.Audio.Media.GENRE, values.genre.orEmpty())
        }

        uris.all { uri ->
            runCatching {
                context.contentResolver.update(uri, content, null, null) > 0
            }.getOrDefault(false)
        }
    }

    /**
     * The tags the platform reports for a song now.
     *
     * Null when the row could not be read, which leaves the app believing what it
     * believed before rather than inventing an answer.
     */
    suspend fun readTags(context: Context, uri: Uri): TagValues? = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver
                .query(uri, tagColumns, null, null, null)
                ?.use { cursor ->
                    if (!cursor.moveToFirst()) return@use null
                    val raw = HashMap<String, String?>()
                    raw["title"] = cursor.stringOrNull(MediaStore.Audio.Media.TITLE)
                    raw["artist"] = cursor.stringOrNull(MediaStore.Audio.Media.ARTIST)
                    raw["album"] = cursor.stringOrNull(MediaStore.Audio.Media.ALBUM)
                    raw["album_artist"] = cursor.stringOrNull(MediaStore.Audio.Media.ALBUM_ARTIST)
                    raw["year"] = cursor.stringOrNull(MediaStore.Audio.Media.YEAR)
                    raw["track"] = cursor.stringOrNull(MediaStore.Audio.Media.TRACK)
                    raw["genre"] = cursor.stringOrNull(MediaStore.Audio.Media.GENRE)
                    tagValuesFrom(raw)
                }
        }.getOrNull()
    }

    /** The columns the tags are read from, in the order they are read. */
    private val tagColumns = arrayOf(
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.ALBUM_ARTIST,
        MediaStore.Audio.Media.YEAR,
        MediaStore.Audio.Media.TRACK,
        MediaStore.Audio.Media.GENRE,
    )

    /** The value of a column, or null when the provider has no such column. */
    private fun android.database.Cursor.stringOrNull(column: String): String? {
        val index = getColumnIndex(column)
        if (index < 0) return null
        return getString(index)
    }
}
