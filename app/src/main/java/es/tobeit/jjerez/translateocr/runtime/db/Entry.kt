package es.tobeit.jjerez.translateocr.runtime.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity
data class Entry(
    @PrimaryKey(autoGenerate = true) val uid: Int,
    @ColumnInfo(name = "time") val timestamp: Date,
    @ColumnInfo(name = "orig_text") val origText: String,
    @ColumnInfo(name = "dest_text") val destText: String,
    @ColumnInfo(name = "mode") val mode: String,
    @ColumnInfo(name = "dictCode") val dictCode: String,
    @ColumnInfo(name = "favorite") val favorite: Boolean
)
