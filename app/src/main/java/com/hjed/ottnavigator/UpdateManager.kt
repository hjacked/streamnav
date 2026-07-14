package com.hjed.ottnavigator

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String
)

object UpdateManager {

    suspend fun getLatestVersion(): UpdateInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val connection = (URL("https://kinsfolktv.vercel.app/update.json").openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5_000
                    readTimeout = 5_000
                }
                val jsonText = try {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } finally {
                    connection.disconnect()
                }
                val json = JSONObject(jsonText)

                UpdateInfo(
                    versionCode = json.getInt("versionCode"),
                    versionName = json.getString("versionName"),
                    downloadUrl = json.getString("downloadUrl")
                )
            } catch (e: Exception) {
                Log.e("UpdateManager", "Error fetching update info", e)
                null
            }
        }
    }

    fun downloadAndInstall(context: Context, url: String) {
        Toast.makeText(context, "Downloading update in background...", Toast.LENGTH_LONG).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val destinationFile = File(context.cacheDir, "StreamNav-Latest.apk")
                if (destinationFile.exists()) {
                    destinationFile.delete()
                }

                val downloadUrl = URL(url)
                val connection = downloadUrl.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val outputStream = FileOutputStream(destinationFile)

                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }

                    outputStream.flush()
                    outputStream.close()
                    inputStream.close()
                    connection.disconnect()

                    withContext(Dispatchers.Main) {
                        triggerPromptInstall(context, destinationFile)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Server download error code: ${connection.responseCode}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("UpdateManager", "Direct download execution failed", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Update failed to download. Check network connections.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun triggerPromptInstall(context: Context, file: File) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION

                val apkUri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )

                setDataAndType(apkUri, "application/vnd.android.package-archive")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("UpdateManager", "Failed to launch package installer window context", e)
            Toast.makeText(context, "Unable to launch system package updater window.", Toast.LENGTH_LONG).show()
        }
    }
}
