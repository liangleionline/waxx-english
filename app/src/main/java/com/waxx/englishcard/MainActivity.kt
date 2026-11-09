package com.waxx.englishcard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.waxx.englishcard.databinding.ActivityMainBinding
import com.waxx.englishcard.databinding.DialogMultilineBinding
import com.waxx.englishcard.databinding.DialogResultBinding
import com.waxx.englishcard.databinding.ItemCardRowBinding
import java.io.File

/**
 * 主界面：打开即进入左右两栏卡片页（不再有「创建卡片」首页）。
 * 数据为空时页面没有内容，直接点底部【新增】即可开始录入。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val cards = mutableListOf<CardItem>()
    private var lastPdf: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cards.clear()
        cards.addAll(CardStore.load(this))
        renderList()

        binding.btnAdd.setOnClickListener { showAddDialog() }
        binding.btnSavePdf.setOnClickListener { doSaveAndGeneratePdf() }
        binding.btnClear.setOnClickListener { confirmClearAll() }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        requestLegacyStoragePermissionIfNeeded()
        checkLastCrash()
    }

    override fun onResume() {
        super.onResume()
        // 从设置页返回时可能发生过导入/覆盖，这里重新对齐一次数据；排版统计也随之刷新
        val latest = CardStore.load(this)
        if (latest != cards) {
            cards.clear()
            cards.addAll(latest)
            renderList()
        } else {
            updateCount()
        }
    }

    // ---------------- 列表渲染 ----------------

    private fun renderList() {
        val inflater = LayoutInflater.from(this)
        binding.listContainer.removeAllViews()
        cards.forEachIndexed { index, card ->
            val row = ItemCardRowBinding.inflate(inflater, binding.listContainer, false)
            row.tvEnglish.text = card.english
            row.tvChinese.text = card.chinese
            row.root.setOnClickListener { showEditDialog(index) }
            row.root.setOnLongClickListener {
                confirmDelete(index)
                true
            }
            binding.listContainer.addView(row.root)

            val divider = View(this)
            divider.layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
            )
            divider.setBackgroundColor(0xFFEEEEEE.toInt())
            binding.listContainer.addView(divider)
        }
        binding.tvEmptyHint.visibility = if (cards.isEmpty()) View.VISIBLE else View.GONE
        updateCount()
    }

    private fun updateCount() {
        val mode = AppSettings.pdfLayoutMode(this)
        val pages = CardPdfGenerator.pageCount(cards.size, mode)
        val perPage = CardPdfGenerator.cellsPerPage(mode)
        binding.tvCount.text = "共 ${cards.size} 张卡片 · 每页 $perPage 张 · ${pages} 页 PDF"
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    // ---------------- 新增 ----------------

    private fun showAddDialog() {
        val dialogBinding = DialogMultilineBinding.inflate(layoutInflater)
        dialogBinding.tvHint.text = getString(R.string.hint_multiline)
        val dialog = AlertDialog.Builder(this)
            .setTitle("新增卡片")
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnDialogSave.setOnClickListener {
            val text = dialogBinding.etInput.text?.toString().orEmpty()
            val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.isEmpty()) {
                toast("内容不能为空")
                return@setOnClickListener
            }
            var pairCount = 0
            var dupCount = 0
            var i = 0
            while (i + 1 < lines.size) {
                val en = lines[i]
                val zh = lines[i + 1]
                if (CardStore.isEnglishDuplicate(cards, en)) {
                    dupCount++
                } else {
                    cards.add(CardItem(en, zh))
                    pairCount++
                }
                i += 2
            }
            val oddTail = lines.size % 2 == 1
            persist()
            renderList()
            dialog.dismiss()
            buildString {
                append("已新增 $pairCount 张卡片")
                if (dupCount > 0) append("，跳过 $dupCount 条重复英文")
                if (oddTail) append("；最后一行英文缺少对应中文，已忽略")
            }.let { toast(it) }
        }

        dialog.setOnShowListener {
            dialogBinding.etInput.requestFocus()
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        }
        dialog.show()
    }

    // ---------------- 编辑（单击） ----------------

    private fun showEditDialog(index: Int) {
        if (index !in cards.indices) return
        val current = cards[index]
        val dialogBinding = DialogMultilineBinding.inflate(layoutInflater)
        dialogBinding.tvHint.text = getString(R.string.hint_edit)
        dialogBinding.etInput.setText("${current.english}\n${current.chinese}")
        dialogBinding.etInput.setSelection(dialogBinding.etInput.text?.length ?: 0)
        dialogBinding.etInput.minLines = 2

        val dialog = AlertDialog.Builder(this)
            .setTitle("编辑卡片")
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnDialogSave.setOnClickListener {
            val lines = dialogBinding.etInput.text?.toString().orEmpty()
                .split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.size < 2) {
                toast("请按第一行英文、第二行中文翻译填写")
                return@setOnClickListener
            }
            val en = lines[0]
            val zh = lines.drop(1).joinToString(" ")
            if (CardStore.isEnglishDuplicate(cards, en, index)) {
                toast("英文「$en」已存在，请修改后再保存")
                return@setOnClickListener
            }
            cards[index] = CardItem(en, zh)
            persist()
            renderList()
            dialog.dismiss()
            toast("已保存修改")
        }

        dialog.setOnShowListener { dialogBinding.etInput.requestFocus() }
        dialog.show()
    }

    // ---------------- 删除（长按） ----------------

    private fun confirmDelete(index: Int) {
        if (index !in cards.indices) return
        val item = cards[index]
        AlertDialog.Builder(this)
            .setTitle("删除卡片")
            .setMessage("是否删除「${item.english} / ${item.chinese}」？左右两栏该行会一起删除。")
            .setNegativeButton("取消", null)
            .setPositiveButton("确定") { _, _ ->
                cards.removeAt(index)
                persist()
                renderList()
                toast("已删除")
            }
            .show()
    }

    private fun confirmClearAll() {
        if (cards.isEmpty()) {
            toast("当前没有卡片")
            return
        }
        AlertDialog.Builder(this)
            .setTitle("清空全部")
            .setMessage("将删除全部 ${cards.size} 张卡片，删除后无法恢复，是否继续？")
            .setNegativeButton("取消", null)
            .setPositiveButton("清空") { _, _ ->
                cards.clear()
                persist()
                renderList()
                toast("已清空")
            }
            .show()
    }

    // ---------------- 保存 + 生成 PDF ----------------

    private fun doSaveAndGeneratePdf() {
        if (cards.isEmpty()) {
            toast(getString(R.string.empty_tip))
            return
        }
        // 校验：只校验英文重复，中文不校验
        val dup = CardStore.findDuplicateEnglish(cards)
        if (dup != null) {
            toast("存在重复英文「$dup」，请修改后再生成 PDF")
            return
        }

        persist()
        val mode = AppSettings.pdfLayoutMode(this)
        binding.btnSavePdf.isEnabled = false
        val progress = android.app.ProgressDialog(this).apply {
            setMessage("正在生成 PDF…")
            setCancelable(false)
            show()
        }

        Thread {
            var file: File? = null
            var error: String? = null
            try {
                val out = File(File(cacheDir, "share").apply { mkdirs() }, CardPdfGenerator.pdfFileName())
                file = CardPdfGenerator.generate(this, cards.toList(), out, mode)
            } catch (t: Throwable) {
                error = t.message ?: t.javaClass.simpleName
                CrashLogger.logException(this, "generatePdf failed", t)
            }
            runOnUiThread {
                progress.dismiss()
                binding.btnSavePdf.isEnabled = true
                if (file != null) {
                    lastPdf = file
                    showResultDialog(file, mode)
                } else {
                    AlertDialog.Builder(this)
                        .setTitle("生成失败")
                        .setMessage("PDF 生成失败：$error")
                        .setPositiveButton("确定", null)
                        .show()
                }
            }
        }.start()
    }

    private fun showResultDialog(pdf: File, mode: CardPdfGenerator.PdfLayoutMode) {
        val dialogBinding = DialogResultBinding.inflate(layoutInflater)
        dialogBinding.tvTitle.text = getString(R.string.set_title)
        dialogBinding.tvSub.visibility = View.VISIBLE
        val pages = CardPdfGenerator.pageCount(cards.size, mode)
        val modeName = if (mode == CardPdfGenerator.PdfLayoutMode.SPLIT) "英文/中文交替占格" else "每格英文+中文"
        dialogBinding.tvSub.text = "共 ${cards.size} 张卡片 · $pages 页 · A4 4×4 · $modeName"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        // 左：确定（直接关闭弹窗）
        dialogBinding.btnOk.setOnClickListener { dialog.dismiss() }

        // 右：分享（先存到 Download/waxx，再调系统分享）
        dialogBinding.btnShare.setOnClickListener {
            binding.btnSavePdf.isEnabled = false
            Thread {
                val path = StorageHelper.copyToDownload(
                    this, pdf,
                    pdf.name,
                    "application/pdf",
                    relativeDir = "waxx"
                )
                runOnUiThread {
                    binding.btnSavePdf.isEnabled = true
                    if (path != null) {
                        toast("PDF 已保存到 Download/waxx/${pdf.name}")
                    } else {
                        toast("保存到 Download/waxx 失败，仍可直接分享")
                    }
                    try {
                        ShareHelper.shareFile(this, pdf, "application/pdf", getString(R.string.app_share_title))
                    } catch (t: Throwable) {
                        CrashLogger.logException(this, "share failed", t)
                        toast("调起系统分享失败：${t.message}")
                    }
                }
            }.start()
            dialog.dismiss()
        }

        dialog.show()
    }

    // ---------------- 数据持久化 ----------------

    private fun persist() {
        CardStore.saveAll(this, cards)
    }

    // ---------------- 权限 / 崩溃提示 ----------------

    private fun requestLegacyStoragePermissionIfNeeded() {
        if (Build.VERSION.SDK_INT <= 28 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1001
            )
        }
    }

    /** 上次崩溃过：提示用户，可查看日志并转发给开发者。 */
    private fun checkLastCrash() {
        val log = CrashLogger.readLastCrash(this) ?: return
        AlertDialog.Builder(this)
            .setTitle("检测到上次异常退出")
            .setMessage("已自动生成崩溃日志并保存到手机 Download 目录（waxx_crash_*.log），可以把该文件发给开发者定位问题。")
            .setNegativeButton("关闭") { _, _ -> CrashLogger.clearLastCrash(this) }
            .setPositiveButton("查看日志") { _, _ ->
                startActivity(
                    Intent(this, CrashDetailActivity::class.java)
                        .putExtra(CrashDetailActivity.EXTRA_CONTENT, log)
                )
            }
            .show()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
