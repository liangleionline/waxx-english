package com.waxx.englishcard

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.waxx.englishcard.databinding.ActivitySettingsBinding

/** 设置页：选择 PDF 卡片排版方式（英文/中文交替占格 或 每格英文+中文）。 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

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
                Toast.makeText(
                    this,
                    if (mode == AppSettings.MODE_SPLIT) "已切换：英文 / 中文交替占格" else "已切换：每格英文 + 中文",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.btnSettingsBack.setOnClickListener { finish() }
    }
}
