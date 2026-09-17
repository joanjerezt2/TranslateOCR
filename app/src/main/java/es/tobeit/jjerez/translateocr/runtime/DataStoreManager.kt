package es.tobeit.jjerez.translateocr.runtime

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException


// https://medium.com/@akarenina25/the-simplest-way-to-use-preferences-datastore-1083d23fade2
// https://gist.github.com/shibbirweb/d4e028e47f960d6d2308229ec67459c8

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
class DataStoreManager {

    /**
     * Funcions per llegir i escriure valors de les preferències.
     * També fer persistent l'estat dels diccionaris (copiant, disponible o esborrable si està instal·lat)
     */

    suspend fun <T> saveValue(context: Context, key: Preferences.Key<T>, value: T )
    {
        context.dataStore.edit { setting ->
            setting[key] = value
        }
    }

    suspend fun <T> readValue(context: Context, key: Preferences.Key<T>) : T?
    {
        val result = context.dataStore.data.catch { exception ->
            if (exception is IOException)
            {
                emit(emptyPreferences())
            }
            else
            {
                throw  exception
            }
        }.first()[key]
        return result
    }

    companion object {
        val markUnknown = booleanPreferencesKey("mark")
        val markAmbiguity = booleanPreferencesKey("ambiguity")
        var dict: MutableMap<String, Preferences.Key<Int>> = HashMap()
    }


}