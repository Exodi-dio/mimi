package com.exodidio.mimi.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import com.exodidio.mimi.MimiApplication
import com.exodidio.mimi.R
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The now playing widget.
 *
 * The cover on the left, the name and the artist beside it, and the two buttons that
 * matter on a home screen. The body opens the app on the player; the buttons act at
 * once, because a listener tapping next on a home screen means next now.
 */
class NowPlayingWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        // The cover is read off the disk, so the widget is drawn in the background and
        // the receiver is held open until it is finished.
        val pending = goAsync()
        widgetScope.launch {
            try {
                appWidgetManager.updateAppWidget(appWidgetIds, buildNowPlayingViews(context))
            } finally {
                pending.finish()
            }
        }
    }
}

/**
 * The now playing widget as it should be drawn now.
 *
 * The song comes from what the service last wrote down and the cover from the file
 * itself, so the widget is right even when the app is not running at all.
 */
suspend fun buildNowPlayingViews(context: Context): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_now_playing)
    val song = currentWidgetSong(context)

    if (song == null || song.mediaId.isBlank()) {
        views.setTextViewText(
            R.id.widget_now_playing_title,
            context.getString(R.string.widget_nothing_playing),
        )
        views.setTextViewText(R.id.widget_now_playing_artist, "")
        views.setImageViewResource(R.id.widget_now_playing_art, R.drawable.ic_widget_music)
        views.setImageViewResource(R.id.widget_now_playing_play, R.drawable.ic_widget_play)
    } else {
        views.setTextViewText(R.id.widget_now_playing_title, song.title)
        views.setTextViewText(R.id.widget_now_playing_artist, song.artist.orEmpty())
        views.setImageViewResource(
            R.id.widget_now_playing_play,
            if (song.playing) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
        )

        val cover = loadArtwork(context, song.mediaId, albumIdFor(context, song.mediaId))
        if (cover != null) {
            views.setImageViewBitmap(R.id.widget_now_playing_art, cover)
        } else {
            views.setImageViewResource(R.id.widget_now_playing_art, R.drawable.ic_widget_music)
        }
    }

    views.setOnClickPendingIntent(
        R.id.widget_now_playing_play,
        widgetActionIntent(context, ACTION_TOGGLE),
    )
    views.setOnClickPendingIntent(
        R.id.widget_now_playing_next,
        widgetActionIntent(context, ACTION_NEXT),
    )
    views.setOnClickPendingIntent(R.id.widget_now_playing_body, openPlayerIntent(context))
    return views
}

/**
 * Draws the now playing widget again.
 *
 * Called when the song changes and when the library is rescanned. The drawing happens
 * in the background, because the cover is read off the disk.
 */
fun refreshNowPlayingWidget(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val ids = manager.getAppWidgetIds(ComponentName(context, NowPlayingWidget::class.java))
    if (ids.isEmpty()) return
    widgetScope.launch {
        manager.updateAppWidget(ids, buildNowPlayingViews(context))
    }
}

/**
 * The song the widget should be showing.
 *
 * What the service wrote down is the truth when there is any, because the service knows
 * what the player is doing. Failing that the session the library kept is used, which is
 * the song the app itself would pick up when it opens.
 */
private suspend fun currentWidgetSong(context: Context): WidgetSong? {
    NowPlayingStore(context).read()?.let { return it }

    val library = (context.applicationContext as MimiApplication).library
    val saved = library.loadPlayback() ?: return null
    val track = library.tracks.first().firstOrNull { it.contentUri == saved.mediaId } ?: return null
    return WidgetSong(
        mediaId = track.contentUri,
        title = track.title,
        artist = track.artist,
        album = track.album,
        durationMs = track.durationMs,
        playing = false,
    )
}

/**
 * The album a song is on, which is what the device's cover index is keyed by.
 */
private suspend fun albumIdFor(context: Context, mediaId: String): Long? {
    val library = (context.applicationContext as MimiApplication).library
    return library.tracks.first().firstOrNull { it.contentUri == mediaId }?.albumId
}
