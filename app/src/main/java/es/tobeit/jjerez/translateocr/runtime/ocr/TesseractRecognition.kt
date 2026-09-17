package es.tobeit.jjerez.translateocr.runtime.ocr

import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI

class TesseractRecognition : Recognition{

    /**
     * Funció que inicialitza el Tesseract i envia una imatge per tal de reconèixer els caràcters
     */

    override fun recognize(imgTest: Bitmap, dataPath: String, language: String): String {
        val tess = TessBaseAPI()
        if (!tess.init(dataPath, language)){
            tess.recycle()
        }
        tess.setImage(imgTest);
        val text2 = tess.utF8Text
        tess.recycle()
        return text2
    }


}