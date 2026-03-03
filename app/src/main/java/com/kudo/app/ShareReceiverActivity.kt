package com.kudo.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kudo.app.databinding.ActivityShareReceiverBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class ShareReceiverActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShareReceiverBinding

    private var sharedUrl: String? = null
    private var sharedTitle: String? = null
    private var sharedFileUri: Uri? = null
    private var sharedFileName: String? = null
    private var sharedMimeType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShareReceiverBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        handleIncomingIntent(intent)

        binding.btnSave.setOnClickListener {
            binding.btnSave.isEnabled = false
            binding.btnSave.text = "保存中..."

            lifecycleScope.launch {
                try {
                    if (sharedFileUri != null) {
                        // 上传文档
                        ApiClient.uploadDocument(
                            contentResolver,
                            sharedFileUri!!,
                            sharedFileName ?: "unknown",
                            sharedMimeType ?: "application/octet-stream"
                        )
                        Snackbar.make(binding.root, R.string.doc_saved, Snackbar.LENGTH_SHORT).show()
                    } else if (sharedUrl != null) {
                        // 保存链接
                        ApiClient.createLink(sharedUrl!!, sharedTitle)
                        Snackbar.make(binding.root, R.string.link_saved, Snackbar.LENGTH_SHORT).show()
                    }
                    // 跳转主界面
                    val mainIntent = Intent(this@ShareReceiverActivity, MainActivity::class.java)
                    mainIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(mainIntent)
                    finish()
                } catch (e: Exception) {
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = "保存"
                    Snackbar.make(binding.root, "保存失败: ${e.message}", Snackbar.LENGTH_LONG).show()
                }
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
                val mimeType = intent.type ?: ""
                if (mimeType.startsWith("text/")) {
                    // 文本/链接分享
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    sharedTitle = intent.getStringExtra(Intent.EXTRA_SUBJECT)
                    sharedUrl = extractUrl(sharedText)

                    if (sharedUrl != null) {
                        binding.tvSharedContent.text = sharedUrl
                        if (!sharedTitle.isNullOrEmpty()) {
                            binding.tvSharedTitle.text = sharedTitle
                            binding.tvSharedTitle.visibility = View.VISIBLE
                        }
                        binding.btnSave.text = "保存链接"
                    } else {
                        binding.tvSharedContent.text = "未能识别到链接内容"
                        binding.btnSave.isEnabled = false
                    }
                } else {
                    // 文档分享
                    val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    if (uri != null) {
                        sharedFileUri = uri
                        sharedMimeType = mimeType
                        sharedFileName = getFileName(uri)

                        binding.tvSharedContent.text = "📄 ${sharedFileName ?: "文件"}"
                        binding.tvSharedTitle.text = "类型: $mimeType"
                        binding.tvSharedTitle.visibility = View.VISIBLE
                        binding.btnSave.text = "上传文档"
                    } else {
                        binding.tvSharedContent.text = "未能获取到文件"
                        binding.btnSave.isEnabled = false
                    }
                }
            }
            Intent.ACTION_VIEW -> {
                sharedUrl = intent.data?.toString()
                if (sharedUrl != null) {
                    binding.tvSharedContent.text = sharedUrl
                    binding.btnSave.text = "保存链接"
                }
            }
        }
    }

    private fun extractUrl(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val urlPattern = Regex("https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+")
        val match = urlPattern.find(text)
        return match?.value ?: if (text.startsWith("http://") || text.startsWith("https://")) text else null
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) name = it.getString(idx)
                }
            }
        }
        if (name == null) {
            name = uri.lastPathSegment
        }
        return name
    }
}
