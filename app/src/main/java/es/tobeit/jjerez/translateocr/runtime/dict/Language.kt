package es.tobeit.jjerez.translateocr.runtime.dict

import android.widget.ArrayAdapter
import android.widget.SpinnerAdapter
import android.widget.TextView


class Language {

    /**
     * Aquesta funció retorna el codi del diccionari segons el idioma d'origen i de destí
     */

    // https://svn.code.sf.net/p/apertium/svn/builds/language-pairs
    fun getDictionaryCode(origLanguage: String, destLanguage: String): String {
        return if ((origLanguage == "Afrikaans" && destLanguage == "Dutch") || (destLanguage == "Afrikaans" && origLanguage == "Dutch")) {
            "af-nl"
        } else if ((origLanguage == "Catalan" && destLanguage == "Italian") || (destLanguage == "Catalan" && origLanguage == "Italian")) {
            "ca-it"
        } else if ((origLanguage == "English" && destLanguage == "Catalan") || (destLanguage == "English" && origLanguage == "Catalan")) {
            "en-ca"
        } else if ((origLanguage == "English" && destLanguage == "Spanish") || (destLanguage == "English" && origLanguage == "Spanish")) {
            "en-es"
        } else if ((origLanguage == "English" && destLanguage == "Galician") || (destLanguage == "English" && origLanguage == "Galician")) {
            "en-gl"
        } else if ((destLanguage == "Esperanto" && origLanguage == "Catalan")) {
            "eo-ca"
        } else if ((origLanguage == "Esperanto" && destLanguage == "English") || (destLanguage == "Esperanto" && origLanguage == "English")) {
            "eo-en"
        } else if ((destLanguage == "Esperanto" && origLanguage == "Spanish")) {
            "eo-es"
        } else if ((destLanguage == "Esperanto" && origLanguage == "French")) {
            "eo-fr"
        } else if ((origLanguage == "Spanish" && destLanguage == "Catalan") || (destLanguage == "Spanish" && origLanguage == "Catalan")) {
            "es-ca"
        } else if ((origLanguage == "Spanish" && destLanguage == "Galician") || (destLanguage == "Spanish" && origLanguage == "Galician")) {
            "es-gl"
        } else if ((origLanguage == "Spanish" && destLanguage == "Portuguese") || (destLanguage == "Spanish" && origLanguage == "Portuguese")) {
            "es-pt"
        } else if ((origLanguage == "Romanian" && destLanguage == "Spanish")) {
            "es-ro"
        } else if ((origLanguage == "French" && destLanguage == "Catalan") || (destLanguage == "French" && origLanguage == "Catalan")) {
            "fr-ca"
        } else if ((origLanguage == "French" && destLanguage == "Spanish") || (destLanguage == "French" && origLanguage == "Spanish")) {
            "fr-es"
        } else if (origLanguage == "Haitian" && destLanguage == "English") {
            "ht-en"
        } else if ((origLanguage == "Portuguese" && destLanguage == "Catalan") || (destLanguage == "Portuguese" && origLanguage == "Catalan")) {
            "pt-ca"
        } else if ((origLanguage == "Portuguese" && destLanguage == "Galician") || (destLanguage == "Portuguese" && origLanguage == "Galician")) {
            "pt-gl"
        } else if ((origLanguage == "Swedish" && destLanguage == "Danish")) {
            "sv-da"
        } else {
            "un-un"
        }
    }

    /**
     * Aquesta funció retorna el país segons l'idioma
     */

    fun getCountryByLanguageCode(languageCode: String): String {

        when (languageCode) {
            "da" -> {
                return "DK"
            }
            "en" -> {
                return "GB"
            }
            "gl" -> {
                return "ES"
            }
            "es" -> {
                return "ES"
            }
            "eo" -> {
                return "UN"
            }
            "ca" -> {
                return "AD"
            }
            "fr" -> {
                return "FR"
            }
            "af" -> {
                return "ZA"
            }
            "nl" -> {
                return "NL"
            }
            "ro" -> {
                return "RO"
            }
            "it" -> {
                return "IT"
            }
            "pt" -> {
                return "PT"
            }
            "sv" -> {
                return "SE"
            }
            "ht" -> {
                return "HT"
            }
        }
        return "UN"

    }

    /**
     * Aquesta funció retorna la bandera segons el país d'origen
     */

    fun getFlagByCountryCode(character: Char): String {

        val code: Int = when (character.uppercaseChar()) {
            'A' -> 0x1F1E6
            'B' -> 0x1F1E7
            'C' -> 0x1F1E8
            'D' -> 0x1F1E9
            'E' -> 0x1F1EA
            'F' -> 0x1F1EB
            'G' -> 0x1F1EC
            'H' -> 0x1F1ED
            'I' -> 0x1F1EE
            'J' -> 0x1F1EF
            'K' -> 0x1F1F0
            'L' -> 0x1F1F1
            'M' -> 0x1F1F2
            'N' -> 0x1F1F3
            'O' -> 0x1F1F4
            'P' -> 0x1F1F5
            'Q' -> 0x1F1F6
            'R' -> 0x1F1F7
            'S' -> 0x1F1F8
            'T' -> 0x1F1F9
            'U' -> 0x1F1FA
            'V' -> 0x1F1FB
            'W' -> 0x1F1FC
            'X' -> 0x1F1FD
            'Y' -> 0x1F1FE
            'Z' -> 0x1F1FF
            else -> 0
        }

        return String(Character.toChars(code))
    }

    /**
     * Aquesta funció retorna la localització segons l'idioma d'origen
     */

    fun getLocale(origLanguage: String): ArrayList<String> {
        var lang = "en"
        var country = "GB"
        when (origLanguage) {
            "English" -> {
                lang = "en"
                country = "GB"
            }
            "Spanish" -> {
                lang = "es"
                country = "ES"
            }
            "Esperanto" -> {
                lang = "eo"
                country = "UN"
            }
            "Catalan" -> {
                lang = "ca"
                country = "AD"
            }
            "Basque" -> {
                lang = "eu"
                country = "ES"
            }
            "French" -> {
                lang = "fr"
                country = "FR"
            }
            "Afrikaans" -> {
                lang = "af"
                country = "ZA"
            }
            "Dutch" -> {
                lang = "nl"
                country = "NL"
            }
            "Romanian" -> {
                lang = "ro"
                country = "RO"
            }
            "Italian" -> {
                lang = "it"
                country = "IT"
            }
            "Galician" -> {
                lang = "gl"
                country = "ES"
            }
            "Portuguese" -> {
                lang = "pt"
                country = "PT"
            }
            "Swedish" -> {
                lang = "sv"
                country = "SE"
            }
            "Haitian" -> {
                lang = "ht"
                country = "HT"
            }
        }

        val result = ArrayList<String>()
        result.add(lang)
        result.add(country)
        return result
    }

    /**
     * Aquesta funció retorna el mode segons l'idioma d'origen i de destí
     */

    fun getModeCode(origLanguage: String, destLanguage: String): String {
        when (origLanguage) {
            "Afrikaans" -> {
                when (destLanguage) {
                    "Dutch" -> {
                        return "af-nl"
                    }
                }
            }

            "Catalan" -> {
                when (destLanguage) {
                    "English" -> {
                        return "ca-en"
                    }
                    "Italian" -> {
                        return "ca-it"
                    }
                    "Esperanto" -> {
                        return "ca-eo"
                    }
                    "French" -> {
                        return "ca-fr"
                    }
                    "Spanish" -> {
                        return "ca-es"
                    }
                    "Portuguese" -> {
                        return "ca-pt"
                    }
                }
            }

            "English" -> {
                when (destLanguage) {
                    "Catalan" -> {
                        return "en-ca"
                    }

                    "Spanish" -> {
                        return "en-es"
                    }

                    "French" -> {
                        return "en-fr"
                    }

                    "Galician" -> {
                        return "en-gl"
                    }

                    "Esperanto" -> {
                        return "eo-en"
                    }
                }
            }

            "Italian" -> {
                when (destLanguage) {
                    "Catalan" -> {
                        return "it-ca"
                    }
                }
            }
            "French" -> {
                when (destLanguage) {
                    "Catalan" -> {
                        return "fr-ca"
                    }
                    "Spanish" -> {
                        return "fr-es"
                    }
                    "Esperanto" -> {
                        return "fr-eo"
                    }
                }
            }

            "Galician" -> {
                when (destLanguage) {
                    "English" -> {
                        return "gl-en"
                    }

                    "Spanish" -> {
                        return "gl-es"
                    }

                    "Portuguese" -> {
                        return "gl-pt"
                    }
                }
            }

            "Dutch" -> {
                when (destLanguage) {
                    "Afrikaans" -> {
                        return "nl-af"
                    }
                }
            }

            "Romanian" -> {
                when (destLanguage) {
                    "Spanish" -> {
                        return "ro-es"
                    }
                }
            }

            "Portuguese" -> {
                when (destLanguage) {
                    "Galician" -> {
                        return "pt-gl"
                    }
                    "Spanish" -> {
                        return "pt-es"
                    }
                    "Catalan" -> {
                        return "pt-ca"
                    }
                }
            }

            "Spanish" -> {
                when (destLanguage) {
                    "English" -> {
                        return "es-en"
                    }
                    "Esperanto" -> {
                        return "es-eo"
                    }
                    "French" -> {
                        return "es-fr"
                    }
                    "Catalan" -> {
                        return "es-ca"
                    }
                    "Galician" -> {
                        return "es-gl"
                    }
                    "Portuguese" -> {
                        return "es-pt"
                    }
                }
            }

            "Esperanto" -> {
                when (destLanguage) {
                    "English" -> {
                        return "eo-en"
                    }
                }
            }

            "Swedish" -> {
                when (destLanguage) {
                    "Danish" -> {
                        return "sv-da"
                    }
                }
            }

            "Haitian" -> {
                when (destLanguage) {
                    "English" -> {
                        return "ht-en"
                    }
                }
            }
        }
        return "un-un"
    }

    /**
     * Aquesta funció retorna l'identificador del diccionari.
     * TODO Funció possiblement redundant ja que el codi es pot obtenir de item.id des del RecyclerViewAdapter
     */

    fun getCode(dictionary: TextView): String {
        var code = "un-un"
        if(dictionary.text.equals("English - Catalan")){
            code = "en-ca"
        }
        else if(dictionary.text.equals("Afrikaans - Dutch")){
            code = "af-nl"
        }
        else if(dictionary.text.equals("Catalan - Italian")){
            code = "ca-it"
        }
        else if(dictionary.text.equals("English - Spanish")){
            code = "en-es"
        }
        else if(dictionary.text.equals("English - Galician")){
            code = "en-gl"
        }
        else if(dictionary.text.equals("Spanish - Catalan")){
            code = "es-ca"
        }
        else if(dictionary.text.equals("Spanish - Galician")){
            code = "es-gl"
        }
        else if(dictionary.text.equals("Spanish - Portuguese")){
            code = "es-pt"
        }
        else if(dictionary.text.equals("Spanish - Romanian")){
            code = "es-ro"
        }
        else if(dictionary.text.equals("Basque - English")){
            code = "eu-en"
        }
        else if(dictionary.text.equals("Basque - Spanish")){
            code = "eu-es"
        }
        else if(dictionary.text.equals("French - Catalan")){
            code = "fr-ca"
        }
        else if(dictionary.text.equals("French - Spanish")){
            code = "fr-es"
        }
        else if(dictionary.text.equals("Haitian Creole - English")){
            code = "ht-en"
        }
        else if(dictionary.text.equals("Portuguese - Catalan")){
            code = "pt-ca"
        }
        else if(dictionary.text.equals("Portuguese - Galician")){
            code = "pt-gl"
        }
        else if(dictionary.text.equals("Swedish - Danish")){
            code = "sv-da"
        }
        return code
    }

    /**
     * Funció per obtenir les possibles llengües de destí a partir de la llengua d'origen
     */

    fun getAdapterBasedOnOrig(
        dataAdapter: ArrayAdapter<String>,
        itemNames: Array<String>,
        origLanguage: String
    ): SpinnerAdapter {
        when (origLanguage) {
            "Afrikaans" -> {
                for (i in itemNames.indices)  // Maximum size of i upto --> Your Array Size
                {
                    if (i == 8) dataAdapter.add(itemNames[i])
                }
            }
            "Catalan" -> {
                for (i in itemNames.indices)  // Maximum size of i upto --> Your Array Size
                {
                    if (!(i == 0 || i == 1 || i == 2 || i == 6 || i == 8)) dataAdapter.add(itemNames[i])
                }
            }
            "English" -> {
                for (i in itemNames.indices)  // Maximum size of i upto --> Your Array Size
                {
                    if (i == 1 || i == 4 || i == 6 || i == 10) dataAdapter.add(itemNames[i])
                }
            }
            "Esperanto" -> {
                for (i in itemNames.indices)  // Maximum size of i upto --> Your Array Size
                {
                    if (i == 3) dataAdapter.add(itemNames[i])
                }
            }
            "French" -> {
                for (i in itemNames.indices)  // Maximum size of i upto --> Your Array Size
                {
                    if (i == 1 || i == 4 || i == 10) dataAdapter.add(itemNames[i])
                }
            }
            "Galician" -> {
                for (i in itemNames.indices)  // Maximum size of i upto --> Your Array Size
                {
                    if (i == 3 || i == 9 || i == 10) dataAdapter.add(itemNames[i])
                }
            }
            "Haitian" -> {
                for (i in itemNames.indices)  // Maximum size of i upto --> Your Array Size
                {
                    if (i == 3) dataAdapter.add(itemNames[i])
                }
            }
            "Italian" -> {
                for (i in itemNames.indices)  // Maximum size of i upto --> Your Array Size
                {
                    if (i == 1) dataAdapter.add(itemNames[i])
                }
            }
            "Dutch" -> {
                for (i in itemNames.indices)  // Maximum size of i upto --> Your Array Size
                {
                    if (i == 0) dataAdapter.add(itemNames[i])
                }
            }
            "Portuguese" -> {
                for (i in itemNames.indices)  // Maximum size of i upto --> Your Array Size
                {
                    if (i == 1 || i == 6 || i == 10) dataAdapter.add(itemNames[i])
                }
            }
            "Romanian" -> {
                for (i in itemNames.indices)  // Maximum size of i upto --> Your Array Size
                {
                    if (i == 10) dataAdapter.add(itemNames[i])
                }
            }
            "Spanish" -> {
                for (i in itemNames.indices)  // Maximum size of i upto --> Your Array Size
                {
                    if (!(i == 0 || i == 2 || i == 7 || i == 8 || i == 10)) dataAdapter.add(itemNames[i])
                }
            }
            "Swedish" -> {
                for (i in itemNames.indices)  // Maximum size of i upto --> Your Array Size
                {
                    if (i == 2) dataAdapter.add(itemNames[i])
                }
            }
        }

        return dataAdapter

    }
}