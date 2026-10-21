package com.exodidio.mimi.library

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore

/**
 * What the device can say about the file behind a song.
 *
 * Every field is allowed to be missing. The provider does not know the bitrate of every
 * format and the headers of a file are not always readable, so "not known" is a normal
 * answer here rather than a failure: the player says what it can and leaves the rest out.
 */
data class SongFormat(
    /** The file's own type, as the provider names it, e.g. `audio/flac`. */
    val mimeType: String? = null,
    /** The bitrate in bits per second, or null when nothing knows it. */
    val bitrateBps: Int? = null,
    /** The sample rate in hertz, or null when nothing knows it. */
    val sampleRateHz: Int? = null,
)

/**
 * Reads what the device knows about the file behind [mediaId].
 *
 * The provider is asked first, because it already holds the answer and asking costs
 * nothing. The file itself is only opened when the provider came back without a bitrate
 * or a sample rate: reading a file's headers costs real time on a phone, and most
 * devices can answer without it.
 */
fun readSongFormat(context: Context, mediaId: String?): SongFormat {
    val id = mediaId?.substringAfterLast('/')?.toLongOrNull()
    val stored = if (id != null) readFromProvider(context, id) else SongFormat()
    if (stored.bitrateBps != null && stored.sampleRateHz != null) return stored

    val read = readFromFile(context, mediaId) ?: return stored
    return SongFormat(
        mimeType = stored.mimeType ?: read.mimeType,
        bitrateBps = stored.bitrateBps ?: read.bitrateBps,
        sampleRateHz = stored.sampleRateHz ?: read.sampleRateHz,
    )
}

/**
 * Asks the provider about one row of the audio collection.
 *
 * Each column is asked for on its own. A projection naming a column the provider does
 * not have fails the whole query, and a file's bitrate is asked for by fewer providers
 * than its type is, so one query for everything would lose the type along with it.
 */
private fun readFromProvider(context: Context, id: Long): SongFormat = SongFormat(
    mimeType = queryColumn(context, id, MediaStore.MediaColumns.MIME_TYPE),
    bitrateBps = queryColumn(context, id, MediaStore.MediaColumns.BITRATE)?.toIntOrNull(),
    // Only newer devices have a column for the sample rate, so this one often comes
    // back empty and the file's own headers are read instead.
    sampleRateHz = queryColumn(context, id, MediaStore.Audio.AudioColumns.SAMPLERATE)?.toIntOrNull(),
)

/**
 * One column of one row, as text, or null when the provider cannot answer.
 *
 * The value comes back as a string because the columns are all read the same way and
 * the numbers are converted by the caller that knows what they mean.
 */
private fun queryColumn(context: Context, id: Long, column: String): String? = runCatching {
    context.contentResolver
        .query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(column),
            "${MediaStore.MediaColumns._ID} = ?",
            arrayOf(id.toString()),
            null,
        )
        ?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val index = cursor.getColumnIndex(column)
            if (index < 0 || cursor.isNull(index)) null else cursor.getString(index)
        }
}.getOrNull()

/**
 * Reads the headers of the file itself.
 *
 * This is the fallback for the formats the provider has no numbers for, and it is also
 * the only way to learn the sample rate of a file whose type the provider does know.
 * A file the platform cannot open simply yields nothing.
 */
private fun readFromFile(context: Context, mediaId: String?): SongFormat? {
    val uri = mediaId?.let { runCatching { Uri.parse(it) }.getOrNull() } ?: return null

    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        SongFormat(
            mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE),
            bitrateBps = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                ?.toIntOrNull(),
            sampleRateHz = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
                ?.toIntOrNull(),
        )
    } catch (throwable: Throwable) {
        null
    } finally {
        runCatching { retriever.release() }
    }
}
