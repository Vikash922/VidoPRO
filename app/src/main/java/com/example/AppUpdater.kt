package com.example

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
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
                downloadAndInstall(context, apkUrl)
            }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun downloadAndInstall(context: Context, apkUrl: String) {
        val fileName = "app-update.apk"
        val destination = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (destination.exists()) destination.delete()

        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("Downloading Update")
            .setDescription("Please wait...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(destination))

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = manager.enqueue(request)

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctxt: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    installApk(context, destination)
                    context.unregisterReceiver(this)
                }
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
    }

    private fun installApk(context: Context, file: File) {
        if (!file.exists()) return
        
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(
            FileProvider.getUriForFile(context, "${context.packageName}.provider", file),
            "application/vnd.android.package-archive"
        )
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
