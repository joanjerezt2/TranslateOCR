package es.tobeit.jjerez.translateocr

import es.tobeit.jjerez.translateocr.runtime.dict.Language
import org.junit.Test
import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    /**
     * Prova unitària senzilla per tal d'obtenir el codi del diccionari a partir de:
     * l'idioma d'origen i de destí
     */
    @Test
    fun getCodeDictionary(){
        val origLanguage = "Catalan"
        val destLanguage = "English"
        val code = Language().getDictionaryCode(origLanguage, destLanguage)
        assertEquals(code, "en-ca")
    }
}