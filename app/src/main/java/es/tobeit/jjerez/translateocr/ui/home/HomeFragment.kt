package es.tobeit.jjerez.translateocr.ui.home

import android.content.ActivityNotFoundException
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.LocaleList
import android.os.ParcelFileDescriptor
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.google.android.material.snackbar.Snackbar
import dalvik.system.DexClassLoader
import es.tobeit.jjerez.translateocr.R
import es.tobeit.jjerez.translateocr.databinding.FragmentHomeBinding
import es.tobeit.jjerez.translateocr.runtime.Asset
import es.tobeit.jjerez.translateocr.runtime.DataStoreManager
import es.tobeit.jjerez.translateocr.runtime.OfflineServiceProvider
import es.tobeit.jjerez.translateocr.runtime.db.AppDatabase
import es.tobeit.jjerez.translateocr.runtime.db.Entry
import es.tobeit.jjerez.translateocr.runtime.db.EntryRepository
import es.tobeit.jjerez.translateocr.runtime.db.EntryViewModel
import es.tobeit.jjerez.translateocr.runtime.dict.Language
import es.tobeit.jjerez.translateocr.runtime.ocr.TesseractRecognition
import es.tobeit.jjerez.translateocr.runtime.ocr.Training
import es.tobeit.jjerez.translateocr.runtime.text.ApertiumTranslator
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.IOException
import java.util.Date
import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern


class HomeFragment : Fragment(), AdapterView.OnItemSelectedListener {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    @Throws(IOException::class)
    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        val parcelFileDescriptor: ParcelFileDescriptor =
            context?.contentResolver?.openFileDescriptor(uri, "r")!!
        val fileDescriptor = parcelFileDescriptor.fileDescriptor
        val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
        parcelFileDescriptor.close()
        return image
    }

    /**
     * La funció ens retorna el camí a la imatge escollida a la galeria
     */
    // https://developer.android.com/training/basics/intents/result
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        // Callback is invoked after the user selects a media item or closes the
        // photo picker.
        if (uri != null) {
            Log.d("PhotoPicker", "Selected URI: $uri")
            val language = Training().getCode(origLanguage.selectedItem.toString())
            val dataPath = Training().copyLanguage(language, view?.context!!)
            val imgTest = getBitmapFromUri(uri)!!
            val result = TesseractRecognition().recognize(imgTest, dataPath, language)
            origText.setText(result)
        } else {
            Log.d("PhotoPicker", "No media selected")
            val mediaFile = Asset().copyAssetToCache(view?.context!!, "hello.png")
            val language = "eng"
            val dataPath = Training().copyLanguage(language, view?.context!!)
            val imgTest = getBitmapFromUri(mediaFile.toUri())!!
            val result = TesseractRecognition().recognize(imgTest, dataPath, language)
            origText.setText(result)
        }
    }

    /**
     * La funció ens retorna si ha pogut desar la imatge capturada per la càmera
     */
    // https://developer.android.com/training/camera/camera-intents
    // https://stackoverflow.com/questions/5991319/capture-image-from-camera-and-display-in-activity
    // https://stackoverflow.com/questions/61941959/activityresultcontracts-takepicture
    // https://stackoverflow.com/questions/38200282/android-os-fileuriexposedexception-file-storage-emulated-0-test-txt-exposed
    // https://stackoverflow.com/questions/42516126/fileprovider-illegalargumentexception-failed-to-find-configured-root
    // https://inthecheesefactory.com/blog/how-to-share-access-to-file-with-fileprovider-on-android-nougat/en
    private var cameraResultLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()) { success ->
        if(success){
            Log.d("TakePicture", "Selected URI: $photoUri")
            val language = Training().getCode(origLanguage.selectedItem.toString())
            val dataPath = Training().copyLanguage(language, view?.context!!)
            val imgTest = getBitmapFromUri(photoUri)!!
            val result = TesseractRecognition().recognize(imgTest, dataPath, language)
            origText.setText(result)
        }
        else{
            Log.d("TakePicture", "No picture taken")
            val mediaFile = Asset().copyAssetToCache(view?.context!!, "hello.png")
            val language = "eng"
            val dataPath = Training().copyLanguage(language, view?.context!!)
            val imgTest = getBitmapFromUri(mediaFile.toUri())!!
            val result = TesseractRecognition().recognize(imgTest, dataPath, language)
            origText.setText(result)

        }
    }

    // https://stackoverflow.com/questions/2390102/how-to-set-selected-item-of-spinner-by-value-not-by-position
    private fun getIndex(spinner: Spinner, myString: String): Int {
        for (i in 0 until spinner.count) {
            if (spinner.getItemAtPosition(i).toString().equals(myString, ignoreCase = true)) {
                return i
            }
        }
        return 0
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this)[HomeViewModel::class.java]

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val db = Room.databaseBuilder(root.context, AppDatabase::class.java, "translateocr").build()
        val translateButton: Button = root.findViewById(R.id.button)
        origText = root.findViewById(R.id.editTextTextMultiLine)
        val destText: EditText = root.findViewById(R.id.editTextTextMultiLine2)
        destText.showSoftInputOnFocus = false;

        /**
         * Definim el selector dels idiomes d'origen
         */
        origLanguage = root.findViewById(R.id.spinner)
        // https://developer.android.com/develop/ui/views/components/spinner
        // Create an ArrayAdapter using the string array and a default spinner layout.
        ArrayAdapter.createFromResource(
            root.context,
            R.array.orig_languages_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears.
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner.
            origLanguage.adapter = adapter
        }

        /**
         * Definim el selector dels idiomes de destí, dinàmicament, segons l'idioma d'origen
         */
        origLanguage.onItemSelectedListener = this
        destLanguage = root.findViewById(R.id.spinner2)
        // https://stackoverflow.com/questions/14569296/setting-the-value-of-the-spinner-dynamically
        // Create an ArrayAdapter using the string array and a default spinner layout.
        val dataAdapter = ArrayAdapter<String>(root.context, android.R.layout.simple_spinner_dropdown_item)
        val itemNames: Array<String> = root.context.resources.getStringArray(R.array.dest_languages_array)
        destLanguage.adapter = Language().getAdapterBasedOnOrig(dataAdapter, itemNames, origLanguage.selectedItem.toString())

        /**
         * Sincronitzem l’idioma del diccionari predictiu del teclat amb l’idioma d’origen seleccionat
         */
        // https://stackoverflow.com/questions/71157667/how-to-change-default-input-language-in-textinputedittext-java-android
        // https://stackoverflow.com/questions/56387603/how-we-can-specify-input-language-for-specific-edittextnot-for-whole-app-any-h
        // https://stackoverflow.com/questions/5715072/change-android-keyboard-language
        // https://developer.android.com/reference/android/widget/TextView#setImeHintLocales(android.os.LocaleList)
        val locale = Language().getLocale(origLanguage.selectedItem.toString())
        origText.imeHintLocales = LocaleList(Locale(locale[0], locale[1]))
        val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.restartInput(origText)

        /**
         * Al prémer el botó, iniciem la traducció i mostrem la sortida
         */
        translateButton.setOnClickListener {
            val inputManager = requireView().context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputManager.hideSoftInputFromWindow(origText.applicationWindowToken, 0)
            val textSortida = translate(root.rootView, origLanguage.selectedItem.toString(), destLanguage.selectedItem.toString(), origText.text.toString())
            // https://stackoverflow.com/questions/2116162/how-to-display-html-in-textview
            val spanned = HtmlCompat.fromHtml(textSortida, HtmlCompat.FROM_HTML_MODE_COMPACT)
            destText.setText(spanned)
            if(!textSortida.startsWith("<b>")){
                val entry = Entry(0, timestamp = Date(), origText = origText.text.toString(),
                    destText = textSortida, dictCode = Language().getDictionaryCode(origLanguage.selectedItem.toString(), destLanguage.selectedItem.toString()),
                    favorite = false, mode = Language().getModeCode(origLanguage.selectedItem.toString(), destLanguage.selectedItem.toString()))
                EntryViewModel(entryRepository = EntryRepository()).insert(db, entry)
                EntryViewModel(entryRepository = EntryRepository()).close(db)
            }
        }

        /**
         * Al prémer el botó IMG, s'obre la galeria i s'obté el text a partir de l'imatge seleccionada
         * Si no se selecciona cap imatge, agafarà una imatge de prova
         */
        val img: Button = root.findViewById(R.id.image)
        img.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            pickMedia.launch(PickVisualMediaRequest())
        }

        /**
         * Al prémer el botó C, s'obre la càmera i s'obté el text a partir de l'imatge capturada
         * Si no es captura cap imatge, agafarà una imatge de prova
         */
        val camera: Button = root.findViewById(R.id.camera)
        camera.setOnClickListener {
            val newFile = File(root.context.cacheDir, "snap.jpg")
            photoUri = FileProvider.getUriForFile(
                root.context,
                root.context.packageName + ".provider",
                newFile
            )
            try {
                cameraResultLauncher.launch(photoUri)
            } catch (e: ActivityNotFoundException) {
                e.printStackTrace()
            }
        }

        /**
         * Al prémer el botó E, s'intercanvien les llengües d'origen i de destí
         */
        val exchange: Button = root.findViewById(R.id.exchange)
        exchange.setOnClickListener {

            val origOld = origLanguage.selectedItem.toString()
            val destOld = destLanguage.selectedItem.toString()
            if(destOld == "Esperanto" && origOld != "English"){
                val snack = Snackbar.make(requireView(),
                    "It's not possible to exchange $destOld and $origOld", 5000)
                snack.show()
            }
            else if(origOld == "Haitian" || origOld == "Romanian" || origOld == "Swedish"){
                val snack = Snackbar.make(requireView(),
                    "It's not possible to put $origOld as a destination language", 5000)
                snack.show()
            }
            else{
                buttonE = 1
                origLanguage.setSelection(getIndex(origLanguage, destOld))
                val dataAdapter2 = ArrayAdapter<String>(root.context, android.R.layout.simple_spinner_dropdown_item)
                val itemNames2: Array<String> = root.context.resources.getStringArray(R.array.dest_languages_array)
                destLanguage.adapter = Language().getAdapterBasedOnOrig(dataAdapter2, itemNames2, destOld)
                destLanguage.setSelection(getIndex(destLanguage, origOld))
            }
        }

        return root
    }

    private lateinit var photoUri: Uri
    private lateinit var destLanguage: Spinner
    private lateinit var origLanguage: Spinner
    private lateinit var origText: EditText
    private var buttonE = 0

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }



    private fun translate(
        view: View,
        origLanguage: String,
        destLanguage: String,
        text: String
    ): String {
        var markUnknown = false
        var markAmbiguity = false

        /**
         * En primer lloc, es llegeixen les preferències de manera síncrona sobre:
         * les paraules desconegudes i l'ambiguitat
         */

        runBlocking {
            withTimeoutOrNull(2000) {
                markUnknown = DataStoreManager().readValue(
                    view.context,
                    DataStoreManager.markUnknown
                ) ?: false
            }
        }

        runBlocking {
            withTimeoutOrNull(2000) {
                markAmbiguity = DataStoreManager().readValue(
                    view.context,
                    DataStoreManager.markAmbiguity
                ) ?: false
            }
        }

        /**
         * Obté el codi del diccionari i el mode de traducció a partir de l'idioma d'origen i de destí
         */

        val code = Language().getDictionaryCode(origLanguage, destLanguage)
        val mode = Language().getModeCode(origLanguage, destLanguage)
        val copiedDictionary = File(view.context.cacheDir, code).exists()
        if (!copiedDictionary) {
            return "<b>Not available, go to Dictionaries section and copy appropriate dictionary</b>"
        }
        if (code == "un-un") {
            return "<b>Not available</b>"
        }
        val offline = OfflineServiceProvider(
            code = mode,
            dir = File(view.context.cacheDir.absolutePath, code)
        )

        /**
         * La variable "jar" apunta al fitxer classes.dex dins de la carpeta del diccionari
         * La variable "classLoader" defineix el carregador de classes Java
         * S'obté el resultat de la traducció en text pla i es retorna formatejat en HTML
         */

        val jar = File(offline.dir, "classes.dex")
        println("JAR:" + jar.absolutePath + "\n")
        println("Directori" + offline.dir + "\n")

        val safeCacheDir = File(File(view.context.cacheDir, "packages"), "safe")
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

        val internalCacheDir = File(File(view.context.cacheDir, "packages"), "cache")

        val txtResult = ApertiumTranslator(
            offline.code,
            offline.dir,
            internalCacheDir,
            classLoader
        ).translate(text, markUnknown, markAmbiguity)
        return format(txtResult, markUnknown)
    }

    /*** Mitzuli (c) 2014 - 2016 Mikel Artetxe ***
     *** (c) 2023 - 2024 Joan Jerez            ***/

    /**
     * Es formateja la sortida en HTML
     */

    private val unknownPattern = Pattern.compile("\\B\\*((\\p{L}||\\p{N})+)\\b")
    private fun escape(s: String?): String {
        return TextUtils.htmlEncode(s).replace("\n".toRegex(), "<br/>")
    }

    private fun format(s: String, markUnknown: Boolean): String {
        val htmlOutput = true
        val matcher: Matcher = unknownPattern.matcher(s)
        val sb = StringBuilder()

        if (htmlOutput) sb.append("<html>")
        var prevEnd = 0
        while (matcher.find()) {
            if (htmlOutput) sb.append(escape(s.substring(prevEnd, matcher.start()))) else sb.append(s.substring(prevEnd, matcher.start()))
            if (markUnknown) sb.append(if (htmlOutput) "<font color='#EE0000'>" else "*")
            if (htmlOutput) sb.append(escape(matcher.group(1))) else sb.append(matcher.group(1))
            if (markUnknown && htmlOutput) sb.append("</font>")
            prevEnd = matcher.end()
        }
        if (htmlOutput) sb.append(escape(s.substring(prevEnd))) else sb.append(s.substring(prevEnd))
        if (htmlOutput) sb.append("</html>")
        return sb.toString()
    }

    /*** end ***/

    /**
     * Si canviem l'idioma d'origen, es recalcularan els idiomes de destí disponibles
     * S'ajustarà l'idioma del teclat al nou idioma d'origen seleccionat
     */

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val item = parent?.getItemAtPosition(position)
        println("Origin Language: $item")
        val locale = Language().getLocale(origLanguage.selectedItem.toString())
        origText.imeHintLocales = LocaleList(Locale(locale[0], locale[1]))
        val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.restartInput(origText)
        if(buttonE == 0){
            val context = requireView().context
            // Create an ArrayAdapter using the string array and a default spinner layout.
            val dataAdapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item)
            val itemNames: Array<String> = context.resources.getStringArray(R.array.dest_languages_array)
            destLanguage.adapter = Language().getAdapterBasedOnOrig(dataAdapter, itemNames, origLanguage.selectedItem.toString())
        }
        else{
            buttonE = 0
        }

    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        println("No és possible no seleccionar cap idioma")
    }
}