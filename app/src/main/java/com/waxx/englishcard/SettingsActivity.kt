package com.waxx.englishcard

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.waxx.englishcard.databinding.ActivitySettingsBinding

/** 设置页：PDF 排版方式 + 数据导入 / 导出。 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    /** 通过系统文件选择器挑选 JSON 备份文件。 */
    private val openBackupLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) handleImport(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val current = AppSettings.getPdfLayoutMode(this)
        binding.rgLayoutMode.check(
            if (current == AppSettings.MODE_SPLIT) binding.rbSplit.id else binding.rbCombined.id
        )

        binding.rgLayoutMode.setOnCheckedChangeListener { _, checkedId ->
            val mode = if (checkedId == binding.rbSplit.id) AppSettings.MODE_SPLIT else AppSettings.MODE_COMBINED
            if (mode != AppSettings.getPdfLayoutMode(this)) {
                AppSettings.setPdfLayoutMode(this, mode)
                toast(
                    if (mode == AppSettings.MODE_SPLIT) "已切换：英文 / 中文交替占格"
                    else "已切换：每格英文 + 中文"
                )
            }
        }

        binding.btnSettingsBack.setOnClickListener { finish() }
        binding.btnExportData.setOnClickListener { doExport() }
        binding.btnImportData.setOnClickListener { pickBackupFile() }
    }

    // ---------------- 导出 ----------------

    private fun doExport() {
        val cards = CardStore.load(this)
        if (cards.isEmpty()) {
            toast("当前没有卡片可导出")
            return
        }
        binding.btnExportData.isEnabled = false
        Thread {
            var result: DataBackup.ExportResult? = null
            var error: String? = null
            try {
                result = DataBackup.export(this, cards.toList())
            } catch (t: Throwable) {
                error = t.message ?: t.javaClass.simpleName
                CrashLogger.logException(this, "export backup failed", t)
            }
            runOnUiThread {
                binding.btnExportData.isEnabled = true
                val exported = result
                if (exported == null) {
                    AlertDialog.Builder(this)
                        .setTitle("导出失败")
                        .setMessage(error ?: "未知错误")
                        .setPositiveButton("确定", null)
                        .show()
                    return@runOnUiThread
                }
                val path = exported.downloadPath
                AlertDialog.Builder(this)
                    .setTitle("导出成功")
                    .setMessage(
                        if (path != null)
                            "共 ${cards.size} 张卡片\n已保存到：$path"
                        else
                            "共 ${cards.size} 张卡片\n已生成备份文件，可直接分享"
                    )
                    .setNegativeButton("确定", null)
                    .setPositiveButton("分享文件") { _, _ ->
                        try {
                            ShareHelper.shareFile(
                                this, exported.shareFile, "application/json", "分享卡片备份"
                            )
                        } catch (t: Throwable) {
                            CrashLogger.logException(this, "share backup failed", t)
                            toast("调起系统分享失败：${t.message}")
                        }
                    }
                    .show()
            }
        }.start()
    }

    // ---------------- 导入 ----------------

    private fun pickBackupFile() {
        try {
            openBackupLauncher.launch(arrayOf("*/*"))
        } catch (t: Throwable) {
            CrashLogger.logException(this, "open document failed", t)
            toast("无法打开文件选择器：${t.message}")
        }
    }

    private fun handleImport(uri: Uri) {
        Thread {
            var parsed: List<CardItem>? = null
            var error: String? = null
            try {
                parsed = DataBackup.readFromUri(this, uri)
            } catch (t: Throwable) {
                error = t.message ?: t.javaClass.simpleName
            }
            runOnUiThread {
                val incoming = parsed
                if (incoming == null) {
                    AlertDialog.Builder(this)
                        .setTitle("导入失败")
                        .setMessage(error ?: "文件解析失败，请确认是本 App 导出的 JSON 备份")
                        .setPositiveButton("确定", null)
                        .show()
                    return@runOnUiThread
                }
                askImportMode(incoming)
            }
        }.start()
    }

    private fun askImportMode(incoming: List<CardItem>) {
        val existing = CardStore.load(this)
        val options = arrayOf(
            "覆盖现有数据（清空后导入 ${incoming.size} 张）",
            "追加合并（保留现有 ${existing.size} 张，自动跳过重复英文）"
        )
        AlertDialog.Builder(this)
            .setTitle("导入方式")
            .setItems(options) { _, which ->
                if (which == 0) {
                    CardStore.saveAll(this, incoming)
                    toast("已导入 ${incoming.size} 张卡片（已覆盖原有数据）")
                } else {
                    val merged = existing.toMutableList()
                    val (addedCount, skippedCount) = DataBackup.mergeInto(merged, incoming)
                    CardStore.saveAll(this, merged)
                    toast("已合并导入 $addedCount 张卡片，跳过 $skippedCount 条重复英文")
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
