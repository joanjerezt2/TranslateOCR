package es.tobeit.jjerez.translateocr.runtime.db

class EntryRepository {

    /**
     * Aquestes funcions asíncrones, serveixen per fer operacions amb la base de dades sobre els registres de l'historial
     */

    fun addEntry(db: AppDatabase, entry: Entry) {
        db.entryDao().insertAll(entry)
    }

    fun getEntries(db: AppDatabase): List<Entry> {
        return db.entryDao().getAll()
    }

    fun closeDatabase(db: AppDatabase){
        db.close()
    }

    fun editEntry(db: AppDatabase, entry: Entry){
        db.entryDao().edit(entry)
    }

    fun removeEntry(db: AppDatabase, entry: Entry){
        db.entryDao().delete(entry)
    }
}