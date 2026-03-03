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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kudo.app.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var linkAdapter: LinkAdapter
    private lateinit var docAdapter: DocAdapter
    private lateinit var chatAdapter: ChatAdapter

    private var currentPage = "links" // links, docs, chat
    private var currentTab = 0 // 0=links, 1=docs

    // 对话中选中的资料
    private val selectedLinkIds = mutableListOf<String>()
    private val selectedDocIds = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAdapters()
        setupNavigation()
        setupTabs()
        setupChat()
    }

    override fun onResume() {
        super.onResume()
        refreshCurrentList()
    }

    private fun setupAdapters() {
        linkAdapter = LinkAdapter(
            onCopy = { copyToClipboard(it.url) },
            onOpen = { openUrl(it.url) },
            onDelete = { item ->
                lifecycleScope.launch {
                    try {
                        ApiClient.deleteLink(item.id)
                        refreshLinks()
                    } catch (e: Exception) {
                        showError(e)
                    }
                }
            }
        )

        docAdapter = DocAdapter(
            onDelete = { item ->
                lifecycleScope.launch {
                    try {
                        ApiClient.deleteDocument(item.id)
                        refreshDocs()
                    } catch (e: Exception) {
                        showError(e)
                    }
                }
            }
        )

        chatAdapter = ChatAdapter()

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = linkAdapter
    }

    private fun setupNavigation() {
        // Toolbar 汉堡菜单
        binding.toolbar.setNavigationOnClickListener { binding.drawerLayout.open() }
        binding.chatToolbar.setNavigationOnClickListener { binding.drawerLayout.open() }

        // 主菜单
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_clear_all -> {
                    showClearConfirmDialog()
                    true
                }
                else -> false
            }
        }

        // 侧边栏导航
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            binding.drawerLayout.close()
            when (menuItem.itemId) {
                R.id.nav_links -> {
                    showLinksPage()
                    binding.tabLayout.getTabAt(0)?.select()
                }
                R.id.nav_docs -> {
                    showLinksPage()
                    binding.tabLayout.getTabAt(1)?.select()
                }
                R.id.nav_chat -> showChatPage()
            }
            true
        }
        binding.navigationView.setCheckedItem(R.id.nav_links)
    }

    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("链接"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("文档"))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                when (currentTab) {
                    0 -> {
                        binding.recyclerView.adapter = linkAdapter
                        binding.navigationView.setCheckedItem(R.id.nav_links)
                        refreshLinks()
                    }
                    1 -> {
                        binding.recyclerView.adapter = docAdapter
                        binding.navigationView.setCheckedItem(R.id.nav_docs)
                        refreshDocs()
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupChat() {
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.chatRecyclerView.adapter = chatAdapter

        binding.btnSend.setOnClickListener {
            val message = binding.etMessage.text?.toString()?.trim() ?: return@setOnClickListener
            if (message.isEmpty()) return@setOnClickListener

            binding.etMessage.text?.clear()
            chatAdapter.addMessage(ChatMessage("user", message))
            binding.chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)

            lifecycleScope.launch {
                try {
                    val reply = ApiClient.chat(
                        message = message,
                        linkIds = selectedLinkIds,
                        docIds = selectedDocIds,
                        history = chatAdapter.getMessages().dropLast(1) // 不含刚发的
                    )
                    chatAdapter.addMessage(ChatMessage("assistant", reply))
                    binding.chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
                } catch (e: Exception) {
                    chatAdapter.addMessage(ChatMessage("assistant", "网络错误: ${e.message}"))
                    binding.chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
                }
            }
        }
    }

    private fun showLinksPage() {
        currentPage = if (currentTab == 0) "links" else "docs"
        binding.linksPage.visibility = View.VISIBLE
        binding.chatPage.visibility = View.GONE
        refreshCurrentList()
    }

    private fun showChatPage() {
        currentPage = "chat"
        binding.linksPage.visibility = View.GONE
        binding.chatPage.visibility = View.VISIBLE
        updateSelectedInfo()
    }

    private fun updateSelectedInfo() {
        val count = selectedLinkIds.size + selectedDocIds.size
        binding.tvSelectedInfo.text = if (count > 0) {
            "已选择 ${selectedLinkIds.size} 条链接, ${selectedDocIds.size} 个文档作为上下文"
        } else {
            "点击右上角选择链接或文档作为对话上下文"
        }
    }

    private fun refreshCurrentList() {
        when {
            currentPage == "chat" -> return
            currentTab == 0 -> refreshLinks()
            currentTab == 1 -> refreshDocs()
        }
    }

    private fun refreshLinks() {
        lifecycleScope.launch {
            try {
                val links = ApiClient.getLinks()
                linkAdapter.submitList(links)
                updateListUI(links.isEmpty(), links.size, isLinks = true)
            } catch (e: Exception) {
                showError(e)
            }
        }
    }

    private fun refreshDocs() {
        lifecycleScope.launch {
            try {
                val docs = ApiClient.getDocuments()
                docAdapter.submitList(docs)
                updateListUI(docs.isEmpty(), docs.size, isLinks = false)
            } catch (e: Exception) {
                showError(e)
            }
        }
    }

    private fun updateListUI(isEmpty: Boolean, count: Int, isLinks: Boolean) {
        if (isEmpty) {
            binding.recyclerView.visibility = View.GONE
            binding.emptyView.visibility = View.VISIBLE
            binding.tvItemCount.visibility = View.GONE
            binding.tvEmptyIcon.text = if (isLinks) "📎" else "📂"
            binding.tvEmptyTitle.text = if (isLinks) getString(R.string.no_links_yet) else getString(R.string.no_docs_yet)
        } else {
            binding.recyclerView.visibility = View.VISIBLE
            binding.emptyView.visibility = View.GONE
            binding.tvItemCount.visibility = View.VISIBLE
            binding.tvItemCount.text = if (isLinks) {
                getString(R.string.total_links, count)
            } else {
                getString(R.string.total_docs, count)
            }
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("link", text))
        Snackbar.make(binding.root, R.string.copied, Snackbar.LENGTH_SHORT).show()
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {}
    }

    private fun showClearConfirmDialog() {
        AlertDialog.Builder(this)
            .setMessage(R.string.clear_confirm)
            .setPositiveButton(R.string.confirm) { _, _ ->
                lifecycleScope.launch {
                    try {
                        if (currentTab == 0) {
                            ApiClient.clearLinks()
                            refreshLinks()
                        }
                    } catch (e: Exception) {
                        showError(e)
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showError(e: Exception) {
        Snackbar.make(binding.root, "错误: ${e.message}", Snackbar.LENGTH_LONG).show()
    }
}
