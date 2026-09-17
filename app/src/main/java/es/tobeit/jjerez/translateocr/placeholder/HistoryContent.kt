package es.tobeit.jjerez.translateocr.placeholder

import java.util.ArrayList
import java.util.Date
import java.util.HashMap

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 */
object HistoryContent {

    /**
     * An array of (placeholder) items.
     */
    val ITEMS: MutableList<HistoryItem> = ArrayList()

    /**
     * A map of (placeholder) items, by ID.
     */
    val ITEM_MAP: MutableMap<String, HistoryItem> = HashMap()

    private const val COUNT = 1

    init {
        // Add some sample items.
        for (i in 1..COUNT) {
            addItem(createPlaceholderItem(i))
        }
    }

    private fun addItem(item: HistoryItem) {
        ITEMS.add(item)
        ITEM_MAP[item.id] = item
    }

    /**
     * Es crea la llista de entrades de l'historial
     */

    private fun createPlaceholderItem(position: Int): HistoryItem {

        // val list = db.entryDao().getAll()
        return HistoryItem("1", "Hola", "Hello", Date(), true)
        // return HistoryItem(position.toString(), list[position].origText, list[position].destText,
        //    list[position].timestamp, list[position].favorite)

    }

    /**
     * A placeholder item representing a piece of content.
     */

    data class HistoryItem(
        val id: String,
        val content: String,
        val details: String,
        val timestamp: Date,
        val favorite: Boolean
    ) {
        override fun toString(): String = content
    }
}