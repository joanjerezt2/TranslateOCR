package es.tobeit.jjerez.translateocr.runtime.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface DictionaryDao {
    @Query("SELECT * FROM dictionary")
    fun getAll(): List<Dictionary>

    @Query("SELECT * FROM dictionary WHERE uid IN (:dictionaryIds)")
    fun loadAllByIds(dictionaryIds: IntArray): List<Dictionary>

    @Query("SELECT * FROM dictionary WHERE copied LIKE :copied LIMIT 1")
    fun findByState(copied: Boolean): Dictionary

    @Query("SELECT * FROM dictionary WHERE orig_dest_lang LIKE :code LIMIT 1")
    fun findByCode(code: String): Dictionary

    @Insert
    fun insertAll(vararg dictionaries: Dictionary)

    @Delete
    fun delete(dictionary: Dictionary)
}
