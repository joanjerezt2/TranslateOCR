package es.tobeit.jjerez.translateocr.runtime.db

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EntryViewModel(private val entryRepository: EntryRepository): ViewModel() {

    /**
     * Crides per crear una "coroutine" per fer consultes a la base de dades
     */
    fun insert(db: AppDatabase, entry: Entry) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                entryRepository.addEntry(db, entry)
            }
            catch(ex: Exception){
                println(ex)
            }
        }
    }

    fun getAll(db: AppDatabase): List<Entry> {
        var entries : List<Entry> = ArrayList()
        viewModelScope.launch(Dispatchers.IO) {
            entries = entryRepository.getEntries(db)
        }
        return entries
    }

    fun close(database: AppDatabase){
        viewModelScope.launch(Dispatchers.IO) {
            try{
                entryRepository.closeDatabase(database)
            }
            catch(ex: Exception){
                println(ex)
            }

        }
    }

    fun edit(db: AppDatabase, entry: Entry) {
        viewModelScope.launch(Dispatchers.IO) {
            try{
                entryRepository.editEntry(db, entry)
            }
            catch(ex: Exception){
                println(ex)
            }

        }
    }

    fun remove(db: AppDatabase, entry: Entry){
        viewModelScope.launch(Dispatchers.IO) {
            try{
                entryRepository.removeEntry(db, entry)
            }
            catch(ex: Exception){
                println(ex)
            }

        }
    }
}