package es.tobeit.jjerez.translateocr.runtime.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Dictionary(
    @PrimaryKey val uid: Int,
    @ColumnInfo(name = "stage") val stage: Int,
    @ColumnInfo(name = "orig_dest_lang") val code: String,
    @ColumnInfo(name = "copied") val copied: Boolean
)
