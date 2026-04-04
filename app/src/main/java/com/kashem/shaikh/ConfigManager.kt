package com.kashem.shaikh

import android.content.Context
import android.util.Log
import org.json.JSONObject

object ConfigManager {

    private const val CONFIG_FILE = "Server.json"

    fun getServerUrls(context: Context): Pair<String, String>? {
        return try {
            val jsonString = context.assets.open(CONFIG_FILE).bufferedReader().use { it.readText() }
            val json = JSONObject(jsonString)
            val host = json.getString("host")
            val socket = json.getString("socket")
            Pair(host, socket)
        } catch (e: Exception) {
            Log.e("ConfigManager", "Failed to load config", e)
            null
        }
    }
}