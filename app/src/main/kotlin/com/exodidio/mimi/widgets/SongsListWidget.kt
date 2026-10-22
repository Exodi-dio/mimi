package com.exodidio.mimi.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.exodidio.mimi.MimiApplication
import com.exodidio.mimi.R
import com.exodidio.mimi.browse.BrowseSong
import com.exodidio.mimi.browse.toBrowseSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * The songs list widget.
 *
 * A scrolling list of the library, with the cover, the name and the artist on each row.
 * The rows are drawn by the launcher rather than by the app, so the songs come from a
 * factory the system binds to and the app is asked for one row at a time.
 */
class SongsListWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_songs_list)
        views.setPendingIntentTemplate(R.id.widget_song_list, widgetActionIntent(context, ACTION_PLAY))
        appWidgetManager.updateAppWidget(appWidgetIds, views)
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_song_list)
    }
}

/**
 * The service the launcher binds to for the rows of the songs list.
 *
 * The system binds to it rather than the app being started, so the songs are read when
 * the list is first asked for rather than when the widget is placed.
 */
class SongsListService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        SongsFactory(applicationContext)
}

/**
 * The rows of the songs list.
 *
 * The songs are read from the cache when the list is first asked for and again whenever
 * the library changes, so a rescan shows up without the widget being placed again.
 *
 * What is handed over is a description of a row rather than the row itself, because the
 * row is drawn by the launcher in the launcher's own process. The cover is the one
 * thing that cannot be described, so it is read here and handed over as a picture.
 */
class SongsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    /** The songs the rows are drawn from. */
    @Volatile
    private var songs: List<BrowseSong> = emptyList()

    override fun onCreate() {
        songs = readSongs()
    }

    override fun onDataSetChanged() {
        songs = readSongs()
    }

    override fun onDestroy() {
        songs = emptyList()
    }

    override fun getCount(): Int = songs.size

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_song_row)
        val song = songs.getOrNull(position) ?: return views

        views.setTextViewText(R.id.widget_song_title, song.title)
        views.setTextViewText(R.id.widget_song_artist, song.artist.orEmpty())
        views.setOnClickFillInIntent(R.id.widget_song_row, playFillIn(song.mediaId))

        val cover = artworkFor(context, song.mediaId, song.albumId)
        if (cover != null) {
            views.setImageViewBitmap(R.id.widget_song_art, cover)
        } else {
            views.setImageViewResource(R.id.widget_song_art, R.drawable.ic_widget_music)
        }
        return views
    }

    override fun getLoadingView(): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_song_row)

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    /**
     * The songs, read from the cache.
     *
     * Read here rather than waited for, because the launcher asks for the rows straight
     * after asking for the data, and a row that was not ready would be an empty one.
     * The read is a question asked of the cache the app already keeps, so it is quick.
     */
    private fun readSongs(): List<BrowseSong> = runBlocking {
        withContext(Dispatchers.IO) {
            (context.applicationContext as MimiApplication).library.tracks.first()
                .map { it.toBrowseSong() }
        }
    }
}

/**
 * Tells the songs list that the library has changed.
 *
 * The rows are drawn by the launcher, so the only way to change them is to say so and
 * let it ask again.
 */
fun refreshSongsListWidget(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val ids = manager.getAppWidgetIds(ComponentName(context, SongsListWidget::class.java))
    if (ids.isEmpty()) return
    manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_song_list)
}

/**
 * The intent that says which song a row is for.
 *
 * Filled into the template the widget carries, so one intent serves every row and each
 * row says which song it means.
 */
private fun playFillIn(mediaId: String): Intent = Intent(ACTION_PLAY).apply {
    putExtra(EXTRA_MEDIA_ID, mediaId)
}
