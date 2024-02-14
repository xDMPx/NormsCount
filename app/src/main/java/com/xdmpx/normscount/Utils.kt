package com.xdmpx.normscount

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.xdmpx.normscount.database.CounterDatabase
import org.json.JSONArray
import org.json.JSONObject

object Utils {

    suspend fun exportToJson(context: Context, uri: Uri): Boolean {
        val database = CounterDatabase.getInstance(context).counterDatabase

        val repositories = database.getAll().map {
            val jsonObject = JSONObject()
            jsonObject.put("id", it.id)
            jsonObject.put("name", it.name)
            jsonObject.put("value", it.value)
        }
        val json = JSONArray(repositories)
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(json.toString().toByteArray())
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