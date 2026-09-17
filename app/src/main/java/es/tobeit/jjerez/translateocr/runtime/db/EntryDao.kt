package es.tobeit.jjerez.translateocr.runtime.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface EntryDao {
    @Query("SELECT * FROM entry")
    fun getAll(): List<Entry>

    @Query("SELECT * FROM entry WHERE uid IN (:entryIds)")
    fun loadAllByIds(entryIds: IntArray): List<Entry>

    @Query("SELECT * FROM entry WHERE orig_text LIKE :first AND " +
            "dest_text LIKE :last LIMIT 1")
    fun findByName(first: String, last: String): Entry

    @Update
    fun edit(entry: Entry)

    @Insert
    fun insertAll(vararg entries: Entry)

    @Delete
    fun delete(entry: Entry)
}
