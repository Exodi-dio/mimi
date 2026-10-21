package com.exodidio.mimi.library

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri

/**
 * How many channels the file behind [mediaId] has, as far as the device can say.
 *
 * The provider's own column is not asked for, because it is missing on some devices and
 * empty on others, and a number that is wrong here would be a confident lie on the
 * details screen. The file's own header is read instead.
 *
 * Null means the device could not say, which is an ordinary answer rather than a
 * failure: a file the platform cannot open, or one whose header carries no channel
 * count, simply has nothing to report.
 *
 * Not pure, because it opens the file. It is meant to be called off the main thread.
 */
fun readAudioChannelCount(context: Context, mediaId: String?): Int? {
    val uri = mediaId?.let { runCatching { Uri.parse(it) }.getOrNull() } ?: return null

    val extractor = MediaExtractor()
    return try {
        extractor.setDataSource(context, uri, null)
        var channels: Int? = null
        for (index in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(index)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (!mime.startsWith("audio/")) continue
            val count = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT, 0)
            if (count > 0) {
                channels = count
                break
            }
        }
        channels
    } catch (throwable: Throwable) {
        null
    } finally {
        runCatching { extractor.release() }
    }
}

/**
 * What a channel count is called on the details screen.
 *
 * One channel is mono and two are stereo, which is what a listener would say about
 * them. A file with more channels than that is named by its count rather than called
 * something it is not, and a count the device did not give is called unknown rather
 * than guessed at.
 *
 * Pure, so the wording can be tested without a device.
 */
fun channelLabel(channels: Int?): String = when {
    channels == null -> "unknown"
    channels <= 0 -> "unknown"
    channels == 1 -> "mono"
    channels == 2 -> "stereo"
    else -> "$channels channels"
}
