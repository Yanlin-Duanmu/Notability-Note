package com.noteability.mynote.util

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageStorageManager {
    
    private const val IMAGE_DIR = "note_images"
    
    suspend fun saveImageFromUri(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val imageDir = File(context.filesDir, IMAGE_DIR).apply { 
                if (!exists()) mkdirs() 
            }
            
            val extension = getFileExtension(context, uri)
            val fileName = "${UUID.randomUUID()}.$extension"
            val destFile = File(imageDir, fileName)
            
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("Failed to open input stream"))
            
            Result.success("$IMAGE_DIR/$fileName")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getAbsolutePath(context: Context, relativePath: String): String {
        return File(context.filesDir, relativePath).absolutePath
    }
    
    fun getFileUri(context: Context, relativePath: String): String {
        return "file://${getAbsolutePath(context, relativePath)}"
    }
    
    private fun getFileExtension(context: Context, uri: Uri): String {
        val mimeType = context.contentResolver.getType(uri)
        return when {
            mimeType?.contains("jpeg") == true || mimeType?.contains("jpg") == true -> "jpg"
            mimeType?.contains("png") == true -> "png"
            mimeType?.contains("gif") == true -> "gif"
            mimeType?.contains("webp") == true -> "webp"
            else -> "jpg"
        }
    }
}