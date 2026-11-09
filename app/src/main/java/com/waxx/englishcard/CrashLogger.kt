package com.waxx.englishcard

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 崩溃日志：App 出现未捕获异常时，自动把完整堆栈 + 设备信息写入手机 Download 目录，
 * 文件名格式 waxx_crash_yyyyMMdd_HHmmss.log，用户可直接把这个文件发给开发者定位问题。
 */
object CrashLogger {

    private const val TAG = "CrashLogger"
    const val LAST_CRASH_FILE = "last_crash.log"

    private val handling = java.util.concurrent.atomic.AtomicBoolean(false)

    fun install(context: Context) {
        val appContext = context.applicationContext
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (handling.compareAndSet(false, true)) {
                try {
                    writeCrash(appContext, thread, throwable)
                } catch (t: Throwable) {
                    Log.e(TAG, "写入崩溃日志失败", t)
                }
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun deviceInfo(): String = buildString {
        append("App: ").append(BuildConfig.APPLICATION_ID).append(" v").append(BuildConfig.VERSION_NAME)
        append(" (").append(BuildConfig.VERSION_CODE).append(")\n")
        append("时间: ").append(StorageHelper.timestampReadable()).append("\n")
        append("设备: ").append(Build.BRAND).append(" ").append(Build.MODEL).append("\n")
        append("系统: Android ").append(Build.VERSION.RELEASE)
        append(" (API ").append(Build.VERSION.SDK_INT).append(")\n")
        append("ABI: ").append(Build.SUPPORTED_ABIS?.joinToString(",") ?: "-").append("\n")
        append("卡片数量: ").append("-\n\n")
    }

    private fun throwableToString(t: Throwable): String {
        val sw = StringWriter()
        PrintWriter(sw).use { pw -> t.printStackTrace(pw) }
        return sw.toString()
    }

    /** 记录一次异常（不崩溃场景，例如字体加载失败、解析失败）。 */
    fun logException(context: Context, tag: String, t: Throwable) {
        Log.e(tag, "异常", t)
        try {
            val file = File(context.filesDir, "error_${StorageHelper.timestamp()}.log")
            val content = deviceInfo() + "[$tag]\n" + throwableToString(t)
            file.writeText(content)
        } catch (e: Throwable) {
            Log.e(TAG, "logException 写文件失败", e)
        }
    }

    /** 崩溃时写入：先写 App 内部（保证一定成功），再往 Download 根目录导出一份。 */
    fun writeCrash(context: Context, thread: Thread, throwable: Throwable): String? {
        val content = buildString {
            append("==== waxx-english 崩溃日志 ====\n")
            append(deviceInfo())
            append("崩溃线程: ").append(thread.name).append("\n\n")
            append(throwableToString(throwable))
        }
        // 1) 内部存储（下次启动可以读到并提示用户）
        try {
            File(context.filesDir, LAST_CRASH_FILE).writeText(content)
        } catch (e: Throwable) {
            Log.e(TAG, "写内部崩溃日志失败", e)
        }
        // 2) Download 根目录，方便用户直接取出
        return try {
            val temp = File(context.cacheDir, "waxx_crash_${StorageHelper.timestamp()}.log")
            temp.writeText(content)
            val path = StorageHelper.copyToDownload(
                context, temp,
                "waxx_crash_${StorageHelper.timestamp()}.log",
                "text/plain",
                relativeDir = ""
            )
            temp.delete()
            Log.i(TAG, "崩溃日志已写入: $path")
            path
        } catch (e: Throwable) {
            Log.e(TAG, "导出崩溃日志到 Download 失败", e)
            null
        }
    }

    /** 读取上一次崩溃日志（用于启动时提示）。 */
    fun readLastCrash(context: Context): String? {
        return try {
            val f = File(context.filesDir, LAST_CRASH_FILE)
            if (!f.exists()) null else f.readText()
        } catch (e: Throwable) {
            null
        }
    }

    fun clearLastCrash(context: Context) {
        try {
            File(context.filesDir, LAST_CRASH_FILE).delete()
        } catch (_: Throwable) {
        }
    }
}
