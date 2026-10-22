package com.exodidio.mimi.widgets

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Where the widgets are drawn.
 *
 * A widget is drawn by the system rather than by a screen, so there is no scope to
 * borrow from one. One is kept here for as long as the app is running, and every
 * drawing is put on it.
 */
internal val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
