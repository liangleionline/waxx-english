package com.waxx.englishcard

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class CardItem(val english: String, val chinese: String)

/**
 * 卡片数据本地持久化：SharedPreferences + JSON。
 * 每次打开 App 自动读取，界面上所有增删改都会同步落盘。
 */
object CardStore {

    private const val PREF_NAME = "waxx_english_store"
    private const val KEY_CARDS = "cards_json"

    @Volatile
    private var cached: MutableList<CardItem>? = null

    fun load(context: Context): MutableList<CardItem> {
        cached?.let { return it }
        val list = mutableListOf<CardItem>()
        val raw = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CARDS, null)
        if (!raw.isNullOrBlank()) {
            try {
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val en = o.optString("en", "").trim()
                    val zh = o.optString("zh", "").trim()
                    if (en.isNotEmpty()) list.add(CardItem(en, zh))
                }
            } catch (e: Exception) {
                CrashLogger.logException(context, "CardStore.load parse error", e)
            }
        }
        cached = list
        return list
    }

    /** 保存整套列表（覆盖）。返回是否成功。 */
    fun saveAll(context: Context, list: List<CardItem>): Boolean {
        return try {
            val arr = JSONArray()
            list.forEach {
                arr.put(JSONObject().put("en", it.english).put("zh", it.chinese))
            }
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_CARDS, arr.toString()).apply()
            cached = list.toMutableList()
            true
        } catch (e: Exception) {
            CrashLogger.logException(context, "CardStore.saveAll error", e)
            false
        }
    }

    /** 英文是否已存在（忽略大小写与首尾空格）。excludeIndex 用于编辑场景排除自身。 */
    fun isEnglishDuplicate(list: List<CardItem>, english: String, excludeIndex: Int = -1): Boolean {
        val key = english.trim().lowercase()
        return list.withIndex().any { (i, c) -> i != excludeIndex && c.english.trim().lowercase() == key }
    }

    fun findDuplicateEnglish(list: List<CardItem>): String? {
        val seen = HashMap<String, Int>()
        for (c in list) {
            val key = c.english.trim().lowercase()
            if (key.isEmpty()) continue
            seen[key] = (seen[key] ?: 0) + 1
            if (seen[key]!! > 1) return c.english.trim()
        }
        return null
    }
}
