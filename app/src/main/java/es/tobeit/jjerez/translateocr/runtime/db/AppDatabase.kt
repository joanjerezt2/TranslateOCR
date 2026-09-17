package es.tobeit.jjerez.translateocr.runtime.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// https://developer.android.com/training/data-storage/room

@Database(entities = [Entry::class, Dictionary::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun dictionaryDao(): DictionaryDao
}
