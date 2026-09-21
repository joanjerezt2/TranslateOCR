package es.tobeit.jjerez.translateocr.ui.settings

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.snackbar.Snackbar
import es.tobeit.jjerez.translateocr.R
import es.tobeit.jjerez.translateocr.runtime.DataStoreManager
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

class SettingsFragment : PreferenceFragmentCompat() {

    /**
     * Aquesta funció activarà l'opció de marcar paraules desconegudes (a les preferències)
     */
    private suspend fun markUnknown() {
        val mark = true
        DataStoreManager().saveValue(requireContext(), DataStoreManager.markUnknown, mark)
    }

    /**
     * Aquesta funció activarà l'opció de mostrar ambiguitats (a les preferències)
     */

    private suspend fun markAmbiguity() {
        println("We're going to mark ambiguity")
        val ambiguity = true
        DataStoreManager().saveValue(requireContext(), DataStoreManager.markAmbiguity, ambiguity)
    }

    /**
     * Aquesta funció desactivarà l'opció de mostrar ambiguitats (a les preferències)
     */

    private suspend fun unmarkAmbiguity() {
        println("We're going to unmark ambiguity")
        val ambiguity = false
        DataStoreManager().saveValue(requireContext(), DataStoreManager.markAmbiguity, ambiguity)
    }

    /**
     * Aquesta funció desactivarà l'opció de marcar paraules desconegudes (a les preferències)
     */

    private suspend fun unmarkUnknown() {
        println("We're going to unmark unknown words")
        val mark = false
        DataStoreManager().saveValue(requireContext(), DataStoreManager.markUnknown, mark)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        // https://developer.android.com/guide/topics/ui/settings/customize-your-settings

        /**
         * Al prémer la casella corresponent, activarem o desactivarem les opcions
         * TODO Fer la crida asíncrona
         */

        val unknownWords: Preference? = findPreference("words") as Preference?
        unknownWords?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener { preference ->
                val sharedPreferences = preference.sharedPreferences
                val unknownWordsValue = sharedPreferences?.getBoolean("words", false)
                if (unknownWordsValue == true){
                    runBlocking {
                        withTimeoutOrNull(2000.milliseconds) { // UI can freeze for 2 seconds
                            markUnknown()
                        }
                    }
                }
                else{
                    runBlocking {
                        withTimeoutOrNull(2000.milliseconds) { // UI can freeze for 2 seconds
                            unmarkUnknown()
                        }
                    }
                }
                true
            }

        val ambiguityWords : Preference? = findPreference("ambiguity") as Preference?
        ambiguityWords!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener { preference ->
                val sharedPreferences = preference.sharedPreferences
                val unknownWordsValue = sharedPreferences?.getBoolean("ambiguity", false)
                if (unknownWordsValue == true){
                    runBlocking {
                        withTimeoutOrNull(2000.milliseconds) { // UI can freeze for 2 seconds
                            markAmbiguity()
                        }
                    }
                }
                else{
                    runBlocking {
                        withTimeoutOrNull(2000.milliseconds) { // UI can freeze for 2 seconds
                            unmarkAmbiguity()
                        }
                    }
                }
                true
            }

        /**
         * Si cliquem a "License", es mostrarà la llicència de l'aplicació
         * Si cliquem a "About", es mostrarà els crèdits de l'aplicació
         */

        val license : Preference? = findPreference("license") as Preference?
        license!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val mySnackbar = view?.let { it1 -> Snackbar.make(it1, "General Public License 3 (GPL 3)", 5000) }
            mySnackbar?.show()
            true
        }

        val about : Preference? = findPreference("about") as Preference?
        about!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val mySnackbar = view?.let { it1 -> Snackbar.make(it1,
                "TranslateOCR\n" +
                    "(c) 2023-2024 Joan Jerez\n" +
                        "Master's Degree Final Project", 5000).setTextMaxLines(3) }
            mySnackbar?.show()
            true
        }
    }
}