package es.tobeit.jjerez.translateocr.ui.history

import android.graphics.Typeface
import android.util.ArrayMap
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.material.snackbar.Snackbar
import es.tobeit.jjerez.translateocr.R
import es.tobeit.jjerez.translateocr.databinding.FragmentHistoryBinding
import es.tobeit.jjerez.translateocr.runtime.db.AppDatabase
import es.tobeit.jjerez.translateocr.runtime.db.Entry
import es.tobeit.jjerez.translateocr.runtime.db.EntryRepository
import es.tobeit.jjerez.translateocr.runtime.db.EntryViewModel
import es.tobeit.jjerez.translateocr.runtime.dict.Language
import es.tobeit.jjerez.translateocr.ui.history.HistoryFragment.HistoryItem
import java.text.DateFormat
import java.util.Locale


/**
 * [RecyclerView.Adapter] that can display a [HistoryItem].
 */
class MyHistoryRecyclerViewAdapter(
    private val values: MutableList<HistoryItem>
) : RecyclerView.Adapter<MyHistoryRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            FragmentHistoryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    private val actualPosition: MutableMap<Int, Int> = ArrayMap()
    private fun calcActualPosition(operation: Int, item: HistoryItem){
        // OK
        if(operation == 0) {
            val mark = actualPosition[item.id]!!
            for (i in values.indices) {
                if (values[i].id == item.id) {
                    actualPosition[item.id] = 0
                } else if (mark < actualPosition[values[i].id]!!) {
                    actualPosition[values[i].id] = actualPosition[values[i].id]!!
                } else {
                    actualPosition[values[i].id] = actualPosition[values[i].id]!! + 1
                }
            }
        }
        else if(operation == 2){
            var number = 1
            for (i in actualPosition.entries){
                if(i.value > actualPosition[item.id]!!){
                    actualPosition.replace(i.key, i.value-1)
                    number += 1
                }
            }
            actualPosition.remove(item.id)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = values[position]

        /**
         * S'inicialitzen les variables per a cada registre de l'historial
         */
        holder.origText.text = item.origText
        val spanned = HtmlCompat.fromHtml(item.destText, HtmlCompat.FROM_HTML_MODE_COMPACT)
        holder.destText.text = spanned
        val df = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT, Locale.ENGLISH)
        val date = df.format(item.timestamp)
        holder.date.text = date
        holder.date.setTypeface(holder.date.typeface, Typeface.BOLD)

        /**
         * S'obté la bandera del país del idioma d'origen i de destí
         */
        val firstLanguage = item.mode.subSequence(0,2).toString()
        val secondLanguage = item.mode.subSequence(3,5).toString()

        val firstCountry = Language().getCountryByLanguageCode(firstLanguage)
        val secondCountry = Language().getCountryByLanguageCode(secondLanguage)

        var firstFlag = "🏳"
        if (firstLanguage != "gl"){
            firstFlag = Language().getFlagByCountryCode(firstCountry[0]) + Language().getFlagByCountryCode(firstCountry[1])
        }
        var secondFlag = "🏳"
        if(secondLanguage != "gl"){
            secondFlag = Language().getFlagByCountryCode(secondCountry[0]) + Language().getFlagByCountryCode(secondCountry[1])
        }

        holder.origFlag.text = firstFlag
        holder.destFlag.text = secondFlag

        /**
         * S'inicialitza l'estat de favorit
         */
        if(values[position].favorite){
            holder.favorite.setBackgroundResource(R.drawable.baseline_favorite_24)
        }
        else{
            holder.favorite.setBackgroundResource(R.drawable.baseline_favorite_border_24)
        }

        /**
         * S'inicialitzen les posicions originals dels registres
         */
        for ( i in values.indices){
            actualPosition[values[i].id] = i
        }

        /**
         * Si es prem el botó de favorit, es marcarà el registre com a favorit i es desplaçarà al capdamunt de la llista
         */
        holder.favorite.setOnClickListener {
            val db = Room.databaseBuilder(holder.itemView.context, AppDatabase::class.java, "translateocr").build()
            val entry = Entry(item.id, item.timestamp, item.origText, item.destText, item.mode, item.code, !item.favorite)
            EntryViewModel(entryRepository = EntryRepository()).edit(db, entry)
            var valuesIndex = 0
            for(index in values.indices){
                if(values[index].id == item.id){
                    valuesIndex = index
                    break
                }
            }
            // OK
            val favorite: Boolean = values[valuesIndex].favorite

            if(favorite){
                holder.favorite.setBackgroundResource(R.drawable.baseline_favorite_border_24)
                println("Unmark original position $position")
            }
            // OK
            else{
                holder.favorite.setBackgroundResource(R.drawable.baseline_favorite_24)
                println("Mark original position $position")
                notifyItemMoved(actualPosition[item.id]!!, 0)
                calcActualPosition(0, item)
            }

            println("********************")
            println("Original position: $position")
            println("Actual position: $valuesIndex")
            println("********************")

            values[valuesIndex].favorite = !values[valuesIndex].favorite
            EntryViewModel(entryRepository = EntryRepository()).close(db)
        }

        /**
         * Si es prem el botó d'esborrar, s'esborrarà el registre de la pantalla i la base de dades
         */

        holder.remove.setOnClickListener {
            // val db = Room.databaseBuilder(holder.itemView.context, AppDatabase::class.java, "translateocr").build()
            val entry = Entry(item.id, item.timestamp, item.origText, item.destText, item.mode, item.code, !item.favorite)
            // EntryViewModel(entryRepository = EntryRepository()).remove(db, entry)
            for(index in values.indices){
                if(values[index].id == item.id){
                        values.removeAt(index)
                        break
                }
            }
            notifyItemRemoved(actualPosition[item.id]!!)
            calcActualPosition( 2, item)
        }



        /**
         * Es mostrarà un missatge emergent amb el text complet del camp
         */

        holder.origText.setOnClickListener {
            val message = Snackbar.make(holder.itemView, holder.origText.text, 5000)
            message.show()
        }

        holder.destText.setOnClickListener {
            val message = Snackbar.make(holder.itemView, holder.destText.text, 5000)
            message.show()
        }
    }

    override fun getItemCount(): Int = values.size

    inner class ViewHolder(binding: FragmentHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        val origText: TextView = binding.originalText
        val destText: TextView = binding.translatedText
        val favorite: Button = binding.favourite
        val date : TextView = binding.dateEntry
        val remove: Button = binding.remove
        val origFlag: TextView = binding.flagOrig
        val destFlag: TextView = binding.flagDest

        override fun toString(): String {
            return super.toString() + " '" + destText.text + "'"
        }
    }

}