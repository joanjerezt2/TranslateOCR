package es.tobeit.jjerez.translateocr.runtime.dict

import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import es.tobeit.jjerez.translateocr.R
import es.tobeit.jjerez.translateocr.runtime.Asset
import java.io.File


class CopyRepository {

        /**
         * En aquesta funció asíncrona, s'esborrarà el diccionari de la memòria
         */

        // https://www.baeldung.com/java-delete-directory
        private fun deleteDirectory(directoryToBeDeleted: File): Boolean {
                val allContents = directoryToBeDeleted.listFiles()
                if (allContents != null) {
                        for (file in allContents) {
                                deleteDirectory(file)
                        }
                }
                return directoryToBeDeleted.delete()
        }

        fun removeDictionary(dictionary: TextView, holder: ViewHolder, download: Button) {
                val code: String = Language().getCode(dictionary)
                val context = holder.itemView.context
                val subdir = File(context.cacheDir.absolutePath, code)
                if(subdir.exists()){
                        val deleted: Boolean = deleteDirectory(subdir)
                        if(deleted){
                                download.setText(R.string.download)
                                val color = context.resources.getColor(R.color.green, context.theme)
                                download.setBackgroundColor(color)
                        }
                }
                else{
                        download.setText(R.string.download)
                        val color = context.resources.getColor(R.color.green, context.theme)
                        download.setBackgroundColor(color)
                }

        }

        /**
         * En aquesta funció asíncrona, es copiarà el diccionari a la memòria
         */

        fun selectDictionary(dictionary: TextView, holder: ViewHolder, download: Button) {
                val code: String = Language().getCode(dictionary)
                copyDictionary(code, holder)
                download.setText(R.string.cache)
                val context = holder.itemView.context
                val red = context.resources.getColor(R.color.red, context.theme)
                download.setBackgroundColor(red)
        }

        private fun copyDictionary(code: String, holder: ViewHolder){
                val file = Asset().copyDictionaryToCache(holder.itemView.context, "apertium-$code.jar")
                val subdir = File(file.parentFile?.absolutePath, code)
                try{
                        Asset().extractJarFile(file, subdir)
                } catch(e: Exception){
                        print(e.localizedMessage)
                }
        }

}