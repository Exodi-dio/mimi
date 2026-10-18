package com.exodidio.mimi.library

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Track::class, MetaEntry::class, Favorite::class, Playlist::class, PlaylistEntry::class],
    version = 3,
    exportSchema = false,
)
abstract class MimiDatabase : RoomDatabase() {

    abstract fun trackDao(): TrackDao

    abstract fun metaDao(): MetaDao

    abstract fun favoriteDao(): FavoriteDao

    abstract fun playlistDao(): PlaylistDao

    companion object {
        private const val NAME = "mimi.db"

        /**
         * v1 -> v2 added the tables holding what the user makes: loved songs and playlists.
         *
         * Purely additive — no existing table is rewritten and there is nothing to carry
         * over, which is why this is plain `CREATE TABLE` and not a copy-and-swap.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `favorites` (
                        `track_id` INTEGER NOT NULL,
                        `added_at` INTEGER NOT NULL,
                        PRIMARY KEY(`track_id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `playlists` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `created_at` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `playlist_entries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `playlist_id` INTEGER NOT NULL,
                        `track_id` INTEGER NOT NULL,
                        `position` INTEGER NOT NULL,
                        FOREIGN KEY(`playlist_id`) REFERENCES `playlists`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_playlist_entries_playlist_id`
                    ON `playlist_entries` (`playlist_id`)
                    """.trimIndent()
                )
            }
        }

        /**
         * v2 -> v3 teaches the library about album artists, years, composers, genres, file
         * names and folders.
         *
         * Every added column is nullable, and that is the whole reason this is safe. A
         * column added by `ALTER TABLE` lands with no default value, and Room holds the
         * migration to the entity's description down to that detail. Declaring a default
         * here to match a non-null field would have to agree exactly with what the entity
         * promises, and getting it subtly wrong is a crash when the database opens — on a
         * phone, with the user's own library. Nullable columns cannot disagree, and none
         * of this data is invented: it is all filled in by the next scan.
         *
         * Additive again, so the tables holding loved songs and playlists are untouched.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `tracks` ADD COLUMN `album_artist` TEXT")
                db.execSQL("ALTER TABLE `tracks` ADD COLUMN `date_added` INTEGER")
                db.execSQL("ALTER TABLE `tracks` ADD COLUMN `year` INTEGER")
                db.execSQL("ALTER TABLE `tracks` ADD COLUMN `genre` TEXT")
                db.execSQL("ALTER TABLE `tracks` ADD COLUMN `composer` TEXT")
                db.execSQL("ALTER TABLE `tracks` ADD COLUMN `file_name` TEXT")
                db.execSQL("ALTER TABLE `tracks` ADD COLUMN `folder` TEXT")
            }
        }

        @Volatile
        private var instance: MimiDatabase? = null

        fun get(context: Context): MimiDatabase =
            instance ?: synchronized(this) {
                instance ?: Room
                    .databaseBuilder(context.applicationContext, MimiDatabase::class.java, NAME)
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    // Everything the app cannot rebuild lives in these tables, so the
                    // upgrade itself must not be the thing that loses it. The fallback is
                    // only there so a phone that somehow cannot walk the migration opens a
                    // working library instead of crashing on launch; a rebuild is a scan.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
    }
}
