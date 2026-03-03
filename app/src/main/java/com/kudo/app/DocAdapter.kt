package com.kudo.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kudo.app.databinding.ItemDocBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DocAdapter(
    private val onDelete: (DocItem) -> Unit
) : ListAdapter<DocItem, DocAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(val binding: ItemDocBinding) : RecyclerView.ViewHolder(binding.root)

    private class DiffCallback : DiffUtil.ItemCallback<DocItem>() {
        override fun areItemsTheSame(oldItem: DocItem, newItem: DocItem) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: DocItem, newItem: DocItem) = oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDocBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        with(holder.binding) {
            tvDocName.text = item.original_name

            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val sizeStr = formatFileSize(item.size)
            tvDocInfo.text = "$sizeStr · ${sdf.format(Date((item.timestamp * 1000).toLong()))}"

            tvDocIcon.text = getDocIcon(item.mime_type)

            btnDeleteDoc.setOnClickListener { onDelete(item) }
        }
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "${size}B"
            size < 1024 * 1024 -> "${size / 1024}KB"
            else -> String.format("%.1fMB", size / (1024.0 * 1024.0))
        }
    }

    private fun getDocIcon(mimeType: String): String {
        return when {
            mimeType.contains("pdf") -> "📕"
            mimeType.contains("word") || mimeType.contains("document") -> "📘"
            mimeType.contains("sheet") || mimeType.contains("excel") -> "📗"
            mimeType.contains("presentation") || mimeType.contains("powerpoint") -> "📙"
            mimeType.contains("image") -> "🖼️"
            else -> "📄"
        }
    }
}
