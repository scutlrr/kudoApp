package com.kudo.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.kudo.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var storage: LinkStorage
    private lateinit var adapter: LinkAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        storage = LinkStorage(this)

        adapter = LinkAdapter(
            onCopy = { item -> copyToClipboard(item.url) },
            onOpen = { item -> openUrl(item.url) },
            onDelete = { item ->
                storage.deleteLink(item.id)
                refreshList()
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_clear_all -> {
                    showClearConfirmDialog()
                    true
                }
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun refreshList() {
        val links = storage.getLinks()
        adapter.submitList(links)

        if (links.isEmpty()) {
            binding.recyclerView.visibility = View.GONE
            binding.emptyView.visibility = View.VISIBLE
            binding.tvLinkCount.visibility = View.GONE
        } else {
            binding.recyclerView.visibility = View.VISIBLE
            binding.emptyView.visibility = View.GONE
            binding.tvLinkCount.visibility = View.VISIBLE
            binding.tvLinkCount.text = getString(R.string.total_links, links.size)
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("link", text))
        com.google.android.material.snackbar.Snackbar
            .make(binding.root, R.string.copied, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
            .show()
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            // URL格式无效
        }
    }

    private fun showClearConfirmDialog() {
        AlertDialog.Builder(this)
            .setMessage(R.string.clear_confirm)
            .setPositiveButton(R.string.confirm) { _, _ ->
                storage.clearAll()
                refreshList()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
