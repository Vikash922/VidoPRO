package com.example

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL


object AppUpdater {
    private const val UPDATE_URL = "https://raw.githubusercontent.com/Vikash922/VidoPRO/main/update.json"
    
    fun checkForUpdates(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(UPDATE_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val jsonResponse = connection.inputStream.bufferedReader().readText()
                    val jsonObject = JSONObject(jsonResponse)
                    
                    val latestVersionCode = jsonObject.getInt("versionCode")
                    val latestVersionName = jsonObject.getString("versionName")
                    val apkUrl = jsonObject.getString("apkUrl")
                    val releaseNotes = jsonObject.optString("releaseNotes", "New update available!")
                    
                    val currentInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        currentInfo.longVersionCode.toInt()
                    } else {
                        currentInfo.versionCode
                    }
                    
                    if (latestVersionCode > currentVersionCode) {
                        withContext(Dispatchers.Main) {
                            showUpdateDialog(context, latestVersionName, releaseNotes, apkUrl)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AppUpdater", "Error checking for updates", e)
            }
        }
    }

    private fun showUpdateDialog(context: Context, versionName: String, releaseNotes: String, apkUrl: String) {
        AlertDialog.Builder(context)
            .setTitle("Update Available: v$versionName")
            .setMessage(releaseNotes)
            .setPositiveButton("Update") { _, _ ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Log.e("AppUpdater", "Failed to open update URL", e)
                }
            }
            .setNegativeButton("Later", null)
            .show()
    }
}

