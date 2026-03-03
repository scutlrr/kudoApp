package com.kudo.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.kudo.app.databinding.ActivityShareReceiverBinding

class ShareReceiverActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShareReceiverBinding
    private lateinit var storage: LinkStorage

    private var sharedUrl: String? = null
    private var sharedTitle: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShareReceiverBinding.inflate(layoutInflater)
        setContentView(binding.root)

        storage = LinkStorage(this)

        binding.toolbar.setNavigationOnClickListener { finish() }

        handleIncomingIntent(intent)

        binding.btnSave.setOnClickListener {
            sharedUrl?.let { url ->
                storage.saveLink(url, sharedTitle)
                com.google.android.material.snackbar.Snackbar
                    .make(binding.root, R.string.link_saved, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                    .show()
                // 保存后跳转到主界面
                val mainIntent = Intent(this, MainActivity::class.java)
                mainIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(mainIntent)
                finish()
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIncomingIntent(it) }
    }

    private fun handleIncomingIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SEND -> {
                if (intent.type?.startsWith("text/") == true) {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    sharedTitle = intent.getStringExtra(Intent.EXTRA_SUBJECT)
                    sharedUrl = extractUrl(sharedText)
                }
            }
            Intent.ACTION_VIEW -> {
                sharedUrl = intent.data?.toString()
            }
        }

        if (sharedUrl != null) {
            binding.tvSharedContent.text = sharedUrl
            if (!sharedTitle.isNullOrEmpty()) {
                binding.tvSharedTitle.text = sharedTitle
                binding.tvSharedTitle.visibility = View.VISIBLE
            }
        } else {
            binding.tvSharedContent.text = "未能识别到链接内容"
            binding.btnSave.isEnabled = false
        }
    }

    private fun extractUrl(text: String?): String? {
        if (text.isNullOrBlank()) return null
        // 尝试从文本中提取URL
        val urlPattern = Regex("https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+")
        val match = urlPattern.find(text)
        return match?.value ?: if (text.startsWith("http://") || text.startsWith("https://")) text else null
    }
}
