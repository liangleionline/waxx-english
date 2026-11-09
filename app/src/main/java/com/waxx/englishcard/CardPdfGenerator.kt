package com.waxx.englishcard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.File

/**
 * 生成 A4 纸 4×4 共 16 格英文学习卡片 PDF。
 * 规则：
 * - A4 纸张，无页边距；
 * - 内部 3 条横线 + 3 条竖线，全部顶到纸张边缘，整体无外边框；
 * - 单元格内容水平居中 + 垂直居中，长文本自动换行并按需缩小字号；
 * - 每 16 格一页，自动分页。
 */
object CardPdfGenerator {

    // A4 @150dpi
    private const val PAGE_W = 1240
    private const val PAGE_H = 1754
    private const val COLS = 4
    private const val ROWS = 4
    private const val CELLS_PER_PAGE = COLS * ROWS

    private const val LINE_WIDTH = 3f
    private const val CELL_PADDING = 26f

    fun generate(context: Context, cards: List<CardItem>, outFile: File): File {
        val doc = PdfDocument()
        val baseTypeface = loadTypeface(context)
        val linePaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = LINE_WIDTH
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        val textPaint = TextPaint().apply {
            color = Color.BLACK
            this.typeface = baseTypeface
            isAntiAlias = true
        }

        val pages = if (cards.isEmpty()) 0 else (cards.size + CELLS_PER_PAGE - 1) / CELLS_PER_PAGE
        val cellW = PAGE_W.toFloat() / COLS
        val cellH = PAGE_H.toFloat() / ROWS

        for (p in 0 until pages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, p + 1).create()
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas

            // 内部井字分割线：只在内部，线条延伸到纸张四边，不画外框
            for (i in 1 until ROWS) {
                val y = cellH * i
                canvas.drawLine(0f, y, PAGE_W.toFloat(), y, linePaint)
            }
            for (i in 1 until COLS) {
                val x = cellW * i
                canvas.drawLine(x, 0f, x, PAGE_H.toFloat(), linePaint)
            }

            // 填内容：从上到下、从左到右
            for (idx in 0 until CELLS_PER_PAGE) {
                val globalIndex = p * CELLS_PER_PAGE + idx
                if (globalIndex >= cards.size) break
                val card = cards[globalIndex]
                val col = idx % COLS
                val row = idx / COLS
                val left = col * cellW
                val top = row * cellH
                // 英文占单元格上半部分，中文占下半部分，各自水平+垂直居中
                drawCellText(
                    canvas, card.english, textPaint,
                    left + CELL_PADDING, top + CELL_PADDING,
                    cellW - CELL_PADDING * 2, cellH / 2f - CELL_PADDING * 2,
                    left + cellW / 2f, top + cellH / 4f
                )
                drawCellText(
                    canvas, card.chinese, textPaint,
                    left + CELL_PADDING, top + cellH / 2f,
                    cellW - CELL_PADDING * 2, cellH / 2f - CELL_PADDING * 2,
                    left + cellW / 2f, top + cellH * 3f / 4f
                )
            }

            doc.finishPage(page)
        }

        // 空数据时给一张空白页，避免生成 0 页非法 PDF
        if (pages == 0) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create()
            val page = doc.startPage(pageInfo)
            for (i in 1 until ROWS) {
                page.canvas.drawLine(0f, cellH * i, PAGE_W.toFloat(), cellH * i, linePaint)
            }
            for (i in 1 until COLS) {
                page.canvas.drawLine(cellW * i, 0f, cellW * i, PAGE_H.toFloat(), linePaint)
            }
            doc.finishPage(page)
        }

        outFile.parentFile?.mkdirs()
        outFile.outputStream().use { doc.writeTo(it) }
        doc.close()
        return outFile
    }

    /**
     * 在指定单元格里绘制居中文字（英文在上、中文在下）。
     * 若两侧都有内容，各自占单元格上下两半；只有一侧时占满整个单元格。
     */
    private fun drawCellText(
        canvas: Canvas,
        text: String,
        basePaint: TextPaint,
        areaLeft: Float,
        areaTop: Float,
        areaW: Float,
        areaH: Float,
        centerX: Float,
        centerY: Float
    ) {
        if (text.isBlank()) return
        // 外层已经把单元格一分为二来画英文和中文，这里用整个单元格宽度的一半高度区域
        val usableH = areaH
        var size = 72f
        var layout: StaticLayout? = null
        while (size > 12f) {
            basePaint.textSize = size
            val l = buildLayout(text, basePaint, areaW.toInt())
            if (l.height <= usableH && l.width <= areaW + 1) {
                layout = l
                break
            }
            size -= 2f
        }
        val l = layout ?: buildLayout(text, basePaint.apply { textSize = 12f }, areaW.toInt())
        canvas.save()
        val translateY = centerY - l.height / 2f
        canvas.translate(areaLeft, translateY)
        l.draw(canvas)
        canvas.restore()
    }

    private fun buildLayout(text: String, paint: TextPaint, width: Int): StaticLayout {
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(1))
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setIncludePad(false)
            .setLineSpacing(0f, 1.05f)
            .setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE)
            .build()
    }

    private fun loadTypeface(context: Context): Typeface {
        return try {
            Typeface.createFromAsset(context.assets, "NotoSansSC-Regular.otf")
        } catch (e: Exception) {
            CrashLogger.logException(context, "loadTypeface failed", e)
            Typeface.SANS_SERIF
        }
    }

    fun pdfFileName(): String = "英语学习卡片_${StorageHelper.timestamp()}.pdf"
}
