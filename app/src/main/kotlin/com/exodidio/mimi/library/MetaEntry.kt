package com.exodidio.mimi.library

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Small key/value table for library bookkeeping (last scan time, etc.). */
@Entity(tableName = "meta")
data class MetaEntry(
    @PrimaryKey @ColumnInfo(name = "meta_key") val key: String,
    @ColumnInfo(name = "meta_value") val value: String,
)
