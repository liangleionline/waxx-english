package com.waxx.englishcard

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/**
 * 卡片数据的导入 / 导出（JSON 备份）。
 * 导出文件默认落在 Download/waxx/waxx_english_backup_时间戳.json。
 */
object DataBackup {

    const val FILE_PREFIX = "waxx_english_backup_"
    private const val APP_TAG = "waxx-english"

    fun backupFileName(): String = "$FILE_PREFIX${StorageHelper.timestamp()}.json"

    /** 生成备份 JSON 文本。 */
    fun buildJson(cards: List<CardItem>): String {
        val root = JSONObject()
        root.put("app", APP_TAG)
        root.put("formatVersion", 1)
        root.put("exportedAt", StorageHelper.timestampReadable())
        root.put("count", cards.size)
        val arr = JSONArray()
        cards.forEach { c ->
            arr.put(JSONObject().put("en", c.english).put("zh", c.chinese))
        }
        root.put("cards", arr)
        return root.toString(2)
    }

    /**
     * 解析备份文件。
     * 兼容两种格式：
     * 1) {"app":"waxx-english","cards":[{"en":"..","zh":".."}]}
     * 2) 纯数组 [{"en":"..","zh":".."}]
     */
    fun parseJson(text: String): List<CardItem> {
        val trimmed = text.trim().removePrefix("\uFEFF").trim()
        if (trimmed.isEmpty()) throw IllegalArgumentException("文件内容为空")
        val array: JSONArray = try {
            if (trimmed.startsWith("[")) {
                JSONArray(trimmed)
            } else {
                val obj = JSONObject(trimmed)
                obj.optJSONArray("cards")
                    ?: throw IllegalArgumentException("文件里没有 cards 字段，可能不是本 App 导出的备份")
            }
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            throw IllegalArgumentException("不是合法的 JSON 备份文件")
        }

        val result = ArrayList<CardItem>(array.length())
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            val en = o.optString("en", o.optString("english", "")).trim()
            val zh = o.optString("zh", o.optString("chinese", "")).trim()
            if (en.isNotEmpty()) result.add(CardItem(en, zh))
        }
        if (result.isEmpty()) throw IllegalArgumentException("备份文件里没有有效卡片")
        return result
    }

    /** 从用户选择的文件读取并解析。 */
    fun readFromUri(context: Context, uri: Uri): List<CardItem> {
        val text = context.contentResolver.openInputStream(uri)?.use { input ->
            input.bufferedReader(Charsets.UTF_8).readText()
        } ?: throw IllegalArgumentException("无法读取所选文件")
        return parseJson(text)
    }

    /** 导出结果：downloadPath 为 Download/waxx 下的展示路径；shareFile 为可直接分享的缓存文件。 */
    data class ExportResult(val downloadPath: String?, val shareFile: java.io.File)

    /** 导出到 Download/waxx，同时保留一份缓存文件用于系统分享。 */
    fun export(context: Context, cards: List<CardItem>): ExportResult {
        val fileName = backupFileName()
        val shareDir = java.io.File(context.cacheDir, "export").apply { mkdirs() }
        val shareFile = java.io.File(shareDir, fileName)
        shareFile.writeText(buildJson(cards))
        val path = StorageHelper.copyToDownload(
            context, shareFile, fileName, "application/json", relativeDir = "waxx"
        )
        return ExportResult(path, shareFile)
    }

    /**
     * 合并导入：跳过已存在的英文（忽略大小写），返回 (实际加入条数, 跳过重复条数)。
     */
    fun mergeInto(existing: MutableList<CardItem>, incoming: List<CardItem>): Pair<Int, Int> {
        var added = 0
        var skipped = 0
        incoming.forEach { item ->
            if (CardStore.isEnglishDuplicate(existing, item.english)) {
                skipped++
            } else {
                existing.add(item)
                added++
            }
        }
        return added to skipped
    }
}
