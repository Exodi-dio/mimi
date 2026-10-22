package com.exodidio.mimi.widgets

import android.content.Context

/**
 * Draws the widgets again.
 *
 * The songs list shows the library and the now playing widget shows what is playing, so
 * both of them have to be told when the library has been rebuilt or the song has
 * changed.
 */
fun refreshWidgets(context: Context) {
    refreshNowPlayingWidget(context)
    refreshSongsListWidget(context)
}
