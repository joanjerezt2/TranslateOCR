package es.tobeit.jjerez.translateocr.runtime.text

import java.io.File

class ApertiumTranslator(
    private val code: String,
    private val base: File,
    private val cacheDir: File,
    private val classLoader: ClassLoader
) : Translator {

    /***
     * Aquesta funció configura els paràmetres d'Apertium i retorna el text traduït
     */

    /*** Mitzuli (c) 2014 - 2016 Mikel Artetxe ***
     *** (c) 2023 - 2024 Joan Jerez            ***/
    @Throws(Exception::class)
    override fun translate(text: String?, displayMarks: Boolean, displayAmbiguity: Boolean): String {

        // https://wiki.apertium.org/wiki/User:Mikel/Embeddable_lttoolbox-java:_Progress

        try{
            synchronized(org.apertium.Translator::class.java) {
                org.apertium.Translator.setDisplayMarks(displayMarks)
                org.apertium.Translator.setDisplayAmbiguity(displayAmbiguity)
                org.apertium.Translator.setBase(base.absolutePath, classLoader)
                org.apertium.Translator.setMode(code)
                org.apertium.utils.IOUtils.cacheDir = cacheDir
                return org.apertium.Translator.translate(text)
            }
        }
        catch(e: Exception){
            return "Error en la traducció amb Apertium"
        }

    }

    /*** end ***/
}
