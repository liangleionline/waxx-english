package com.waxx.englishcard

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.waxx.englishcard.databinding.ActivityCrashBinding
import java.io.File

/** 展示上一次崩溃日志，支持直接分享给开发者。 */
class CrashDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCrashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val content = intent.getStringExtra(EXTRA_CONTENT)
            ?: CrashLogger.readLastCrash(this)
            ?: "没有读取到崩溃日志"
        binding.tvLog.text = content
        binding.tvPath.text = "同时已保存到：Download/waxx_crash_*.log"

        binding.btnCloseCrash.setOnClickListener { finish() }
        binding.btnShareLog.setOnClickListener {
            try {
                val f = File(cacheDir, "waxx_crash_share_${StorageHelper.timestamp()}.log")
                f.writeText(content)
                ShareHelper.shareFile(this, f, "text/plain", "分享崩溃日志")
            } catch (t: Throwable) {
                android.widget.Toast.makeText(this, "分享失败：${t.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        const val EXTRA_CONTENT = "extra_content"
    }
}
