package com.waxx.englishcard

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 统一写入手机 Download 目录（PDF 写入 Download/waxx，崩溃日志写入 Download 根目录）。
 * - Android 10+：走 MediaStore，不需要存储权限；
 * - Android 9 及以下：需 WRITE_EXTERNAL_STORAGE，直接 File 写入。
 * 所有失败都会兜底写到 App 私有外部目录，保证不丢失。
 */
object StorageHelper {

    private const val TAG = "StorageHelper"

    private fun hasLegacyPermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
    }

    /** 把文件保存到 Download/<relativeDir>/<fileName>，返回可展示的真实路径文案；失败返回 null。 */
    fun copyToDownload(
        context: Context,
        source: File,
        fileName: String,
        mimeType: String,
        relativeDir: String = "waxx"
    ): String? {
        // 1) Android 10+ / Q 及以上
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(android.provider.MediaStore.Downloads.RELATIVE_PATH,
                        if (relativeDir.isBlank()) Environment.DIRECTORY_DOWNLOADS
                        else "${Environment.DIRECTORY_DOWNLOADS}/$relativeDir")
                    put(android.provider.MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalException("MediaStore insert 返回 null")
                resolver.openOutputStream(uri)?.use { out ->
                    source.inputStream().use { it.copyTo(out) }
                }
                values.clear()
                values.put(android.provider.MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                return if (relativeDir.isBlank())
                    "${Environment.DIRECTORY_DOWNLOADS}/$fileName"
                else "${Environment.DIRECTORY_DOWNLOADS}/$relativeDir/$fileName"
            } catch (e: Exception) {
                Log.w(TAG, "MediaStore 写入失败，尝试 File 方式", e)
            }
        }

        // 2) 老系统（或 MediaStore 失败）直接写公共目录
        try {
            val base = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val dir = if (relativeDir.isBlank()) base else File(base, relativeDir)
            if (!dir.exists()) dir.mkdirs()
            val target = File(dir, fileName)
            source.inputStream().use { input ->
                target.outputStream().use { out -> input.copyTo(out) }
            }
            // 通知相册 / 文件扫描，方便在系统「文件管理」里看到
            return target.absolutePath
        } catch (e: Exception) {
            Log.w(TAG, "File 方式写入 Download 失败", e)
        }

        // 3) 兜底：App 私有外部目录
        return try {
            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            val target = File(dir, fileName)
            source.inputStream().use { input ->
                target.outputStream().use { out -> input.copyTo(out) }
            }
            target.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "兜底写入失败", e)
            null
        }
    }

    private class IllegalException(msg: String) : Exception(msg)

    fun timestamp(): String =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

    fun timestampReadable(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
}
