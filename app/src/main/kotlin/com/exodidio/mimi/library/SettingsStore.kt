package com.exodidio.mimi.library

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The listener's choices, read and written.
 *
 * Kept in the same table as everything else the app must not lose, under a prefix of its
 * own. A second storage mechanism would have meant a second thing to get wrong on upgrade,
 * and this data is no more precious than the playlists sitting next to it.
 *
 * Writes always store the whole set. That is eight small rows, which is cheaper to think
 * about than tracking which one changed, and it means a setting that is removed later
 * simply stops being written rather than needing a migration.
 */
class SettingsStore(private val metaDao: MetaDao) {

    val settings: Flow<Settings> = metaDao
        .observeWithPrefix(Settings.PREFIX)
        .map(Settings::from)

    suspend fun update(settings: Settings) = metaDao.putAll(Settings.rows(settings))
}
