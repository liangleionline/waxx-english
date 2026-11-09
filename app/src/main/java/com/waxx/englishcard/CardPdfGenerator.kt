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
 *
 * 通用规则：
 * - A4 纸张，无页边距；
 * - 内部 3 条横线 + 3 条竖线，全部顶到纸张边缘，整体无外边框；
 * - 单元格内容水平居中 + 垂直居中，长文本自动换行并按需缩小字号；
 * - 每页 16 格，自动分页；填充顺序为从上到下、从左到右。
 *
 * 两种排版模式（设置页可切换）：
 * - [PdfLayoutMode.SPLIT]：英文、中文交替占用独立单元格。第 1 格英文、第 2 格中文、
 *   第 3 格英文、第 4 格中文……每页 16 格 = 8 张卡片（默认）。
 * - [PdfLayoutMode.COMBINED]：每格上半部分英文、下半部分中文，一格一张卡片，
 *   每页 16 格 = 16 张卡片。
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

    enum class PdfLayoutMode {
        /** 英文/中文交替独立占格（默认） */
        SPLIT,
        /** 每格英文+中文合在一起 */
        COMBINED
    }

    /** 一个单元格要绘制的内容：secondary 为空表示整格只放一段文字。 */
    private data class CellContent(val primary: String, val secondary: String? = null)

    fun cellsPerPage(mode: PdfLayoutMode): Int =
        if (mode == PdfLayoutMode.SPLIT) CELLS_PER_PAGE / 2 else CELLS_PER_PAGE

    fun pageCount(cardCount: Int, mode: PdfLayoutMode): Int {
        if (cardCount <= 0) return 0
        val per = cellsPerPage(mode)
        return (cardCount + per - 1) / per
    }

    private fun buildCells(cards: List<CardItem>, mode: PdfLayoutMode): List<CellContent> {
        return if (mode == PdfLayoutMode.SPLIT) {
            val list = ArrayList<CellContent>(cards.size * 2)
            cards.forEach {
                list.add(CellContent(formatCellText(it.english)))
                list.add(CellContent(formatCellText(it.chinese)))
            }
            list
        } else {
            cards.map {
                CellContent(formatCellText(it.english), formatCellText(it.chinese))
            }
        }
    }

    /**
     * PDF 排版用的智能处理：把内容里的中英文逗号（, 与 ，）当作分隔符，
     * 每个片段单独成行，并且不再显示逗号本身。
     * 例："a, an"  ->  "a\nan"；"你好，世界"  ->  "你好\n世界"。
     * 仅影响 PDF 输出，App 界面里仍显示原始文本。
     */
    fun formatCellText(text: String): String {
        if (!text.contains(',') && !text.contains('，')) return text
        val parts = text.split(',', '，').map { it.trim() }.filter { it.isNotEmpty() }
        return when {
            parts.isEmpty() -> ""
            parts.size == 1 -> parts[0]
            else -> parts.joinToString("\n")
        }
    }

    fun generate(
        context: Context,
        cards: List<CardItem>,
        outFile: File,
        mode: PdfLayoutMode = PdfLayoutMode.SPLIT
    ): File {
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

        val cells = buildCells(cards, mode)
        val pages = if (cells.isEmpty()) 1 else (cells.size + CELLS_PER_PAGE - 1) / CELLS_PER_PAGE
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

            for (idx in 0 until CELLS_PER_PAGE) {
                val globalIndex = p * CELLS_PER_PAGE + idx
                if (globalIndex >= cells.size) break
                val cell = cells[globalIndex]
                val col = idx % COLS
                val row = idx / COLS
                val left = col * cellW
                val top = row * cellH

                if (cell.secondary == null) {
                    // 整格单段文字（英文或中文单占一格）：铺满整个单元格居中
                    drawCellText(
                        canvas, cell.primary, textPaint,
                        left + CELL_PADDING, top + CELL_PADDING,
                        cellW - CELL_PADDING * 2, cellH - CELL_PADDING * 2,
                        left + cellW / 2f, top + cellH / 2f
                    )
                } else {
                    // 合排模式：英文占上半格、中文占下半格，各自居中
                    drawCellText(
                        canvas, cell.primary, textPaint,
                        left + CELL_PADDING, top + CELL_PADDING,
                        cellW - CELL_PADDING * 2, cellH / 2f - CELL_PADDING * 2,
                        left + cellW / 2f, top + cellH / 4f
                    )
                    drawCellText(
                        canvas, cell.secondary, textPaint,
                        left + CELL_PADDING, top + cellH / 2f,
                        cellW - CELL_PADDING * 2, cellH / 2f - CELL_PADDING * 2,
                        left + cellW / 2f, top + cellH * 3f / 4f
                    )
                }
            }

            doc.finishPage(page)
        }

        outFile.parentFile?.mkdirs()
        outFile.outputStream().use { doc.writeTo(it) }
        doc.close()
        return outFile
    }

    /**
     * 在指定区域内绘制居中文字。
     * 自动从 54px 逐级缩小字号，直到文本完整落在区域内。
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
        var size = 54f
        var layout: StaticLayout? = null
        while (size > 10f) {
            basePaint.textSize = size
            val l = buildLayout(text, basePaint, areaW.toInt())
            if (l.height <= areaH && l.width <= areaW + 1) {
                layout = l
                break
            }
            size -= 2f
        }
        val l = layout ?: buildLayout(text, basePaint.apply { textSize = 10f }, areaW.toInt())
        canvas.save()
        canvas.translate(areaLeft, centerY - l.height / 2f)
        l.draw(canvas)
        canvas.restore()
    }

    private fun buildLayout(text: String, paint: TextPaint, width: Int): StaticLayout {
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(1))
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setIncludePad(false)
            .setLineSpacing(0f, 1.05f)
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
