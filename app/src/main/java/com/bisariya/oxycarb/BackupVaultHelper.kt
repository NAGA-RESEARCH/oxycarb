package com.bisariya.oxycarb

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class LibraryBackupModel(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val favorites: List<TrackInfo> = emptyList(),
    val playlists: List<PlaylistBackupModel> = emptyList()
)

data class PlaylistBackupModel(
    val name: String,
    val tracks: List<TrackInfo>
)

object BackupVaultHelper {
    private const val BACKUP_DIR = "Documents/Oxycarb"
    private const val FILE_NAME = "oxycarb_library_backup.json"
    private val gson = Gson()

    suspend fun saveBackupToDocuments(
        context: Context,
        favorites: List<TrackInfo>,
        playlists: List<PlaylistBackupModel>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val backupData = LibraryBackupModel(
                favorites = favorites,
                playlists = playlists
            )
            val jsonString = gson.toJson(backupData)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, FILE_NAME)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, BACKUP_DIR)
                }

                val projection = arrayOf(MediaStore.MediaColumns._ID)
                val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? AND ${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
                val selectionArgs = arrayOf("%Documents/Oxycarb%", FILE_NAME)

                var existingUri: Uri? = null
                resolver.query(
                    MediaStore.Files.getContentUri("external"),
                    projection,
                    selection,
                    selectionArgs,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                        existingUri = Uri.withAppendedPath(MediaStore.Files.getContentUri("external"), id.toString())
                    }
                }

                val targetUri = existingUri ?: resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                if (targetUri != null) {
                    resolver.openOutputStream(targetUri, "rwt")?.use { output ->
                        output.write(jsonString.toByteArray(Charsets.UTF_8))
                    }
                    return@withContext true
                }
            } else {
                val docsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Oxycarb")
                if (!docsDir.exists()) docsDir.mkdirs()
                val targetFile = File(docsDir, FILE_NAME)
                targetFile.writeText(jsonString, Charsets.UTF_8)
                return@withContext true
            }
        } catch (e: Exception) {
            Log.e("BackupVault", "Failed to save backup", e)
        }
        return@withContext false
    }

    suspend fun readBackupFromDocuments(context: Context): LibraryBackupModel? = withContext(Dispatchers.IO) {
        try {
            var jsonString: String? = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val projection = arrayOf(MediaStore.MediaColumns._ID)
                val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? AND ${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
                val selectionArgs = arrayOf("%Documents/Oxycarb%", FILE_NAME)

                resolver.query(
                    MediaStore.Files.getContentUri("external"),
                    projection,
                    selection,
                    selectionArgs,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                        val uri = Uri.withAppendedPath(MediaStore.Files.getContentUri("external"), id.toString())
                        resolver.openInputStream(uri)?.use { input ->
                            jsonString = input.bufferedReader().readText()
                        }
                    }
                }
            } else {
                val docsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Oxycarb")
                val targetFile = File(docsDir, FILE_NAME)
                if (targetFile.exists()) {
                    jsonString = targetFile.readText(Charsets.UTF_8)
                }
            }

            if (!jsonString.isNullOrEmpty()) {
                return@withContext gson.fromJson(jsonString, LibraryBackupModel::class.java)
            }
        } catch (e: Exception) {
            Log.e("BackupVault", "Failed to read backup", e)
        }
        return@withContext null
    }
}
