package com.kudo.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kudo.app.databinding.ItemLinkBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LinkAdapter(
    private val onCopy: (LinkItem) -> Unit,
    private val onOpen: (LinkItem) -> Unit,
    private val onDelete: (LinkItem) -> Unit
) : ListAdapter<LinkItem, LinkAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(val binding: ItemLinkBinding) : RecyclerView.ViewHolder(binding.root)

    private class DiffCallback : DiffUtil.ItemCallback<LinkItem>() {
        override fun areItemsTheSame(oldItem: LinkItem, newItem: LinkItem) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: LinkItem, newItem: LinkItem) = oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLinkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        with(holder.binding) {
            tvUrl.text = item.url

            if (!item.title.isNullOrEmpty()) {
                tvTitle.text = item.title
                tvTitle.visibility = android.view.View.VISIBLE
            } else {
                tvTitle.visibility = android.view.View.GONE
            }

            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            tvTime.text = sdf.format(Date((item.timestamp * 1000).toLong()))

            btnCopy.setOnClickListener { onCopy(item) }
            btnOpen.setOnClickListener { onOpen(item) }
            btnDelete.setOnClickListener { onDelete(item) }
        }
    }
}
