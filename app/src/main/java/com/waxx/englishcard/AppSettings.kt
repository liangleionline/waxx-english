package com.waxx.englishcard

import android.content.Context

/**
 * 应用设置（本地持久化）。
 * 目前只有一项：PDF 卡片排版方式。
 */
object AppSettings {

    private const val PREF_NAME = "waxx_english_settings"
    private const val KEY_PDF_LAYOUT = "pdf_layout_mode"

    /** 英文/中文交替独立占格（默认，每页 8 张卡片） */
    const val MODE_SPLIT = 0
    /** 每格同时显示英文和中文（每页 16 张卡片） */
    const val MODE_COMBINED = 1

    fun getPdfLayoutMode(context: Context): Int =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_PDF_LAYOUT, MODE_SPLIT)

    fun setPdfLayoutMode(context: Context, mode: Int) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_PDF_LAYOUT, mode).apply()
    }

    fun pdfLayoutMode(context: Context): CardPdfGenerator.PdfLayoutMode =
        if (getPdfLayoutMode(context) == MODE_SPLIT)
            CardPdfGenerator.PdfLayoutMode.SPLIT
        else
            CardPdfGenerator.PdfLayoutMode.COMBINED
}
