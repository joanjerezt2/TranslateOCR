package es.tobeit.jjerez.translateocr.runtime.ocr

import android.graphics.Bitmap

interface Recognition {

    @Throws(Exception::class)
    fun recognize(imgTest: Bitmap, dataPath: String, language: String): String

}