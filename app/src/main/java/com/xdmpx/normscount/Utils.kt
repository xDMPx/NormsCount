package com.xdmpx.normscount

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.xdmpx.normscount.database.CounterDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

object Utils {

    suspend fun exportToJSON(context: Context, uri: Uri): Boolean {
        val database = CounterDatabase.getInstance(context).counterDatabase

        val repositories = database.getAll().map {
            val jsonObject = JSONObject()
            jsonObject.put("id", it.id)
            jsonObject.put("name", if (it.name == "Counter #") "${it.name}${it.id}" else it.name)
            jsonObject.put("value", it.value)
        }
        val json = JSONArray(repositories)
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(json.toString().toByteArray())
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun importFromJSON(
        context: Context, uri: Uri, addCounter: (String, Long) -> Unit
    ): Boolean {
        val database = CounterDatabase.getInstance(context).counterDatabase

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bufferedReader = BufferedReader(InputStreamReader(inputStream))
                val importedJson = JSONArray(bufferedReader.readText())
                inputStream.close()

                val counters = database.getAll()
                val names =
                    counters.map { if (it.name == "Counter #") "${it.name}${it.id}" else it.name }
                val values = counters.map { it.value }

                val toImport =
                    (0 until importedJson.length()).map { importedJson.getJSONObject(it) }
                        .filter { jsonObject ->
                            val name = jsonObject.getString("name")
                            if (name !in names) {
                                true
                            } else {
                                values[names.indexOf(name)] != jsonObject.getLong("value")
                            }
                        }.toList()

                toImport.forEach {
                    addCounter(it.getString("name"), it.getLong("value"))
                }
            }
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun ShortToast(context: Context, text: CharSequence) {
        Toast.makeText(
            context, text, Toast.LENGTH_SHORT
        ).show()
    }

}