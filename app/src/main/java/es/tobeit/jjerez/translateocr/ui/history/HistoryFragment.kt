package es.tobeit.jjerez.translateocr.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import es.tobeit.jjerez.translateocr.R
import es.tobeit.jjerez.translateocr.runtime.db.AppDatabase
import es.tobeit.jjerez.translateocr.runtime.db.Dictionary
import java.util.Date

/**
 * A fragment representing a list of Items.
 */
class HistoryFragment : Fragment() {

    private var columnCount = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            columnCount = it.getInt(ARG_COLUMN_COUNT)
        }
    }

    private fun initDictionaries(db: AppDatabase) {

        val dictionaries : MutableList<Dictionary> = ArrayList()
        dictionaries.add(Dictionary(1, 0, "af-nl", false))
        dictionaries.add(Dictionary(2, 0, "ca-it", false))
        dictionaries.add(Dictionary(3, 0, "en-ca", false))
        dictionaries.add(Dictionary(4,0,"en-es",false))
        dictionaries.add(Dictionary(5, 0, "en-gl", false))
        dictionaries.add(Dictionary(6, 0, "eo-ca", false))
        dictionaries.add(Dictionary(7, 0, "eo-en", false))
        dictionaries.add(Dictionary(8, 0, "eo-es", false))
        dictionaries.add(Dictionary(9, 0, "eo-fr", false))
        dictionaries.add(Dictionary(10, 0, "es-ca", false))
        dictionaries.add(Dictionary(11, 0, "es-gl", false))
        dictionaries.add(Dictionary(12, 0, "es-pt", false))
        dictionaries.add(Dictionary(13, 0, "es-ro", false))
        dictionaries.add(Dictionary(14, 0, "eu-en", false))
        dictionaries.add(Dictionary(15, 0, "eu-es", false))
        dictionaries.add(Dictionary(16, 0, "fr-ca", false))
        dictionaries.add(Dictionary(17, 0, "fr-es", false))
        dictionaries.add(Dictionary(18, 0, "ht-en", false))
        dictionaries.add(Dictionary(19, 0, "pt-ca", false))
        dictionaries.add(Dictionary(20, 0, "pt-gl", false))
        dictionaries.add(Dictionary(21, 0, "sv-da", false))

        for(i in dictionaries.indices){
            db.dictionaryDao().insertAll(dictionaries[i])
        }

    }

    data class HistoryItem(
        val id: Int,
        val origText: String,
        val destText: String,
        val timestamp: Date,
        var favorite: Boolean,
        val mode: String,
        val code: String
    ) {
        override fun toString(): String = origText
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history_list, container, false)
        val db = Room.databaseBuilder(view.context, AppDatabase::class.java, "translateocr").allowMainThreadQueries().build()
        // S'insereix la llista de diccionaris a la base de dades
        val dictionaries = db.dictionaryDao().getAll()
        if(dictionaries.isEmpty()){
            initDictionaries(db)
        }
        // Obtenim totes les entrades de la base de dades
        val items : MutableList<HistoryItem> = ArrayList()
        val entries = db.entryDao().getAll()
        db.close()

        /**
         * Si hi ha entrades, s'afegiran en una llista
         */
        if(entries.isNotEmpty()){
            for (i in entries.indices) {
                items.add(HistoryItem(entries[i].uid, entries[i].origText, entries[i].destText,
                    entries[i].timestamp, entries[i].favorite, entries[i].dictCode, entries[i].mode))
            }
        }

        /**
         * Si és favorit, es desplaçarà el registre al capdamunt de la llista
         */
        for (i in items.indices){
            if(items[i].favorite){
                items.add(0, items[i])
                items.removeAt(i+1)
            }
        }

        // Set the adapter
        if (view is RecyclerView) {
            with(view) {
                layoutManager = when {
                    columnCount <= 1 -> LinearLayoutManager(context)
                    else -> GridLayoutManager(context, columnCount)
                }

                adapter = MyHistoryRecyclerViewAdapter(items)
            }
        }
        return view
    }

    companion object {

        // TODO: Customize parameter argument names
        const val ARG_COLUMN_COUNT = "column-count"

        // TODO: Customize parameter initialization
        @JvmStatic
        fun newInstance(columnCount: Int) =
            HistoryFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_COLUMN_COUNT, columnCount)
                }
            }
    }
}