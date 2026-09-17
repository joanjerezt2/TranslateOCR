package es.tobeit.jjerez.translateocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.net.toUri
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import dalvik.system.DexClassLoader
import es.tobeit.jjerez.translateocr.runtime.Asset
import es.tobeit.jjerez.translateocr.runtime.OfflineServiceProvider
import es.tobeit.jjerez.translateocr.runtime.ocr.TesseractRecognition
import es.tobeit.jjerez.translateocr.runtime.ocr.Training
import es.tobeit.jjerez.translateocr.runtime.text.ApertiumTranslator

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import java.io.File
import java.io.IOException

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    /**
     * Es prova que la traducció de l'anglès al català, a partir del text anglès "Hello!", retorna el resultat esperat
     */

    @Test
    fun textTranslation(){

        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val mode = "en-ca"
        val code = "en-ca"
        val text = "Hello!"
        val markUnknown = true
        val markAmbiguity = false

        val file = Asset().copyDictionaryToCache(appContext, "apertium-$code.jar")
        val subdir = File(file.parentFile?.absolutePath, code)
        try{
            Asset().extractJarFile(file, subdir)
        } catch(e: Exception){
            print(e.localizedMessage)
        }

        val offline = OfflineServiceProvider(
            code = mode,
            dir = File(appContext.cacheDir.absolutePath, code)
        )
        val jar = File(offline.dir, "classes.dex")
        val safeCacheDir = File(File(appContext.cacheDir, "packages"), "safe")
        fun getSafeCacheDir(): File {
            safeCacheDir.mkdirs()
            return safeCacheDir
        }

        val classLoader: ClassLoader = DexClassLoader(
            jar.absolutePath,
            getSafeCacheDir().absolutePath,
            null,
            javaClass.classLoader
        )

        val internalCacheDir = File(File(appContext.cacheDir, "packages"), "cache")

        val txtResult = ApertiumTranslator(
            offline.code,
            offline.dir,
            internalCacheDir,
            classLoader
        ).translate(text, markUnknown, markAmbiguity)
        assertEquals("Hola!", txtResult.trim())
    }

    /**
     * Es prova que la traducció de l'anglès al català, a partir de la imatge de prova, retorna el resultat esperat
     * S'ha fet servir el reconeixement òptic per obtenir el text "Hello"
     */

    @Throws(IOException::class)
    fun getBitmapFromUri(uri: Uri, appContext: Context): Bitmap? {
        val parcelFileDescriptor: ParcelFileDescriptor =
            appContext.contentResolver?.openFileDescriptor(uri, "r")!!
        val fileDescriptor = parcelFileDescriptor.fileDescriptor
        val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
        parcelFileDescriptor.close()
        return image
    }

    @Test
    fun ocrTranslation(){
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val tessLanguage = "eng"
        val mode = "en-ca"
        val code = "en-ca"
        val mediaFile = Asset().copyAssetToCache(appContext, "hello.png")
        val bitmap = getBitmapFromUri(mediaFile.toUri(), appContext)!!
        val dataPath = Training().copyLanguage("eng", appContext)
        val text = TesseractRecognition().recognize(bitmap, dataPath, tessLanguage)
        val markUnknown = true
        val markAmbiguity = false

        val file = Asset().copyDictionaryToCache(appContext, "apertium-$code.jar")
        val subdir = File(file.parentFile?.absolutePath, code)
        try{
            Asset().extractJarFile(file, subdir)
        } catch(e: Exception){
            print(e.localizedMessage)
        }

        val offline = OfflineServiceProvider(
            code = mode,
            dir = File(appContext.cacheDir.absolutePath, code)
        )
        val jar = File(offline.dir, "classes.dex")
        val safeCacheDir = File(File(appContext.cacheDir, "packages"), "safe")
        fun getSafeCacheDir(): File {
            safeCacheDir.mkdirs()
            return safeCacheDir
        }

        val classLoader: ClassLoader = DexClassLoader(
            jar.absolutePath,
            getSafeCacheDir().absolutePath,
            null,
            javaClass.classLoader
        )

        val internalCacheDir = File(File(appContext.cacheDir, "packages"), "cache")

        val txtResult = ApertiumTranslator(
            offline.code,
            offline.dir,
            internalCacheDir,
            classLoader
        ).translate(text, markUnknown, markAmbiguity)
        assertEquals("Hola", txtResult)
    }
}