package es.tobeit.jjerez.translateocr.placeholder

import java.util.ArrayList
import java.util.HashMap

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 */
object DictionaryContent {

    /**
     * An array of (placeholder) items.
     */
    val ITEMS: MutableList<PlaceholderItem> = ArrayList()

    /**
     * A map of (placeholder) items, by ID.
     */
    val ITEM_MAP: MutableMap<String, PlaceholderItem> = HashMap()

    private val COUNT = 20

    init {
        // Add some sample items.
        for (i in 1..COUNT) {
            addItem(createPlaceholderItem(i))
        }
    }

    private fun addItem(item: PlaceholderItem) {
        ITEMS.add(item)
        ITEM_MAP[item.id] = item
    }

    /**
     * Es crea la llista de diccionaris precompilats
     */

    private fun createPlaceholderItem(position: Int): PlaceholderItem {
        // https://svn.code.sf.net/p/apertium/svn/builds/language-pairs
        when (position) {
            1 -> {
                return PlaceholderItem(
                    id = "af-nl",
                    content = "Afrikaans - Dutch",
                    details = "Dictionary"
                )
            }
            2 -> {
                return PlaceholderItem(
                    id = "ca-it",
                    content = "Catalan - Italian",
                    details = "Dictionary"
                )
            }
            3 -> {
                return PlaceholderItem(
                    id = "en-ca",
                    content = "English - Catalan",
                    details = "Dictionary"
                )
            }
            4 -> {
                    return PlaceholderItem(
                        id = "en-es",
                        content = "English - Spanish",
                        details = "Dictionary"
                    )
            }
            5 -> {
                return PlaceholderItem(
                    id = "en-gl",
                    content = "English - Galician",
                    details = "Dictionary"
                )
            }
            6 -> {
                return PlaceholderItem(
                    id = "eo-ca",
                    content = "Esperanto - Catalan",
                    details = "Dictionary"
                )
            }
            7 -> {
                return PlaceholderItem(
                    id = "eo-en",
                    content = "Esperanto - English",
                    details = "Dictionary"
                )
            }
            8 -> {
                return PlaceholderItem(
                    id = "eo-fr",
                    content = "Esperanto - French",
                    details = "Dictionary"
                )
            }
            /* 9 -> {
                return PlaceholderItem(
                    id = "es-ast",
                    content = "Spanish - Asturian",
                    details = "Dictionary"
                )
            }*/
            9 -> {
                return PlaceholderItem(
                    id = "es-ca",
                    content = "Spanish - Catalan",
                    details = "Dictionary"
                )
            }
            10 -> {
                return PlaceholderItem(
                    id = "es-gl",
                    content = "Spanish - Galician",
                    details = "Dictionary"
                )
            }
            11 -> {
                return PlaceholderItem(
                    id = "es-pt",
                    content = "Spanish - Portuguese",
                    details = "Dictionary"
                )
            }
            12 -> {
                return PlaceholderItem(
                    id = "es-ro",
                    content = "Spanish - Romanian",
                    details = "Dictionary"
                )
            }
            13 -> {
                return PlaceholderItem(
                    id = "eu-en",
                    content = "Basque - English",
                    details = "Dictionary"
                )
            }
            14 -> {
                return PlaceholderItem(
                    id = "eu-es",
                    content = "Basque - Spanish",
                    details = "Dictionary"
                )
            }
            15 -> {
                return PlaceholderItem(
                    id = "fr-ca",
                    content = "French - Catalan",
                    details = "Dictionary"
                )
            }
            16 -> {
                return PlaceholderItem(
                    id = "fr-es",
                    content = "French - Spanish",
                    details = "Dictionary"
                )
            }
            17 -> {
                return PlaceholderItem(
                    id = "ht-en",
                    content = "Haitian Creole - English",
                    details = "Dictionary"
                )
            }
            /* 19 -> {
                return PlaceholderItem(
                    id = "oc-ca",
                    content = "Occitan - Catalan",
                    details = "Dictionary"
                )
            }
            20 -> {
                return PlaceholderItem(
                    id = "oc-es",
                    content = "Occitan - Spanish",
                    details = "Dictionary"
                )
            }*/
            18 -> {
                return PlaceholderItem(
                    id = "pt-ca",
                    content = "Portuguese - Catalan",
                    details = "Dictionary"
                )
            }
            19 -> {
                return PlaceholderItem(
                    id = "pt-gl",
                    content = "Portuguese - Galician",
                    details = "Dictionary"
                )
            }
            20 -> {
                return PlaceholderItem(
                    id = "sv-da",
                    content = "Swedish - Danish",
                    details = "Dictionary"
                )
            }
        }
        return PlaceholderItem(position.toString(), "Item $position", makeDetails(position))
    }

    private fun makeDetails(position: Int): String {
        val builder = StringBuilder()
        builder.append("Details about Item: ").append(position)
        for (i in 0..<position) {
            builder.append("\nMore details information here.")
        }
        return builder.toString()
    }

    /**
     * A placeholder item representing a piece of content.
     */
    data class PlaceholderItem(val id: String, val content: String, val details: String) {
        override fun toString(): String = content
    }
}