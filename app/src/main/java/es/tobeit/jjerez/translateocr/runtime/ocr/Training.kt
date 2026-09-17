package es.tobeit.jjerez.translateocr.runtime.ocr

import android.content.Context
import es.tobeit.jjerez.translateocr.runtime.Asset
import java.io.File

class Training {

    /**
     * Aquesta funció retorna el codi del fitxer d'entrenament d'OCR per cada idioma (només s'han inclòs els 6 idiomes amb més idiomes de destí)
     */

    fun getCode(language: String): String {
        return when (language) {
            "Catalan" -> {
                "cat"
            }
            "English" -> {
                "eng"
            }
            "French" -> {
                "fra"
            }
            "Galician" -> {
                "glg"
            }
            "Portuguese" -> {
                "por"
            }
            "Spanish" -> {
                "spa"
            }
            else -> "eng"
        }
    }

    /**
     * Aquesta funció copia el fitxer d'entrenament a la subcarpeta "tessdata"
     */

    // https://developer.android.com/training/data-storage/app-specific#kotlin
    // https://stackoverflow.com/questions/11324348/why-app-appears-while-creating-directory-using-contextwraper-getdir
    fun copyLanguage(language: String, context: Context): String {
        val dir = context.getDir("tesseract", Context.MODE_PRIVATE)
        val createDir = File("$dir/tessdata")
        if (!createDir.exists()) {
            createDir.mkdir()
        }
        val trained = Asset().copyAssetToStorage(context, createDir, "$language.traineddata")
        println(trained.absolutePath)
        return File(context.filesDir.parent, "app_tesseract").absolutePath
    }
}