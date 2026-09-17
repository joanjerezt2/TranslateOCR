package es.tobeit.jjerez.translateocr.runtime

import android.content.Context
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.jar.JarFile


class Asset {

    /**
     * Aquesta funció copia el diccionari a la memòria
     */

    // https://stackoverflow.com/a/56455963
    @Throws(IOException::class)
    fun copyDictionaryToCache(context: Context, fileName: String): File = File(context.cacheDir, fileName)
        .also {
            if (!it.exists()) {
                it.outputStream().use { cache ->
                    context.assets.open("apertium/$fileName").use { inputStream ->
                        inputStream.copyTo(cache)
                    }
                }
            }
        }

    /**
    * Aquesta funció copia un fitxer a la memòria
    */

    @Throws(IOException::class)
    fun copyAssetToCache(context: Context, fileName: String): File = File(context.cacheDir, fileName)
        .also {
            if (!it.exists()) {
                it.outputStream().use { cache ->
                    context.assets.open(fileName).use { inputStream ->
                        inputStream.copyTo(cache)
                    }
                }
            }
        }

    /**
    * Aquesta funció copia un fitxer a l'emmagatzematge persistent
    */

    fun copyAssetToStorage(context: Context, dir: File, fileName: String): File = File(dir.absolutePath, fileName)
        .also {
            if (!it.exists()) {
                it.outputStream().use { storage ->
                    context.assets.open("tessdata/$fileName").use { inputStream ->
                        inputStream.copyTo(storage)
                    }
                }
            }
        }

    /**
    * Aquesta funció extreu els continguts del paquet del diccionari.
    * Imprescindible per fer servir el traductor
    */

    // https://stackoverflow.com/a/13084442
    // https://stackoverflow.com/questions/4504291/how-to-speed-up-unzipping-time-in-java-android
    fun extractJarFile(jar: File, destdir: File){
        try {
            val jarfile = JarFile(jar)
            val enu = jarfile.entries()
            while (enu.hasMoreElements()) {
                destdir.mkdir()
                val je = enu.nextElement()
                println(je)
                var fl = File(destdir, je.name)
                if (!fl.exists()) {
                    fl.parentFile?.mkdir()
                    fl = File(destdir, je.name)
                }
                if (je.isDirectory) {
                    continue
                }
                val inputStream = jarfile.getInputStream(je)
                val fo = FileOutputStream(fl)
                writeFile(inputStream, fo)
                fo.close()
                inputStream.close()
                println(enu.hasMoreElements())
            }
        }
        catch(e: Exception){
            e.printStackTrace()
        }

    }

    private fun writeFile(inputStream: InputStream, fo: FileOutputStream){
            val inBuff = BufferedInputStream(inputStream)
            val outBuff = BufferedOutputStream(fo)
            val b = ByteArray(2048)
            while (true) {
                val nBytes: Int = inBuff.read(b)
                if (nBytes <= 0) {
                    break
                }
                outBuff.write(b, 0, nBytes)
            }
            outBuff.flush()
    }
}



