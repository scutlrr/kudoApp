package com.kudo.app

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import com.kudo.app.databinding.ItemChatMessageBinding

class ChatAdapter : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    private val messages = mutableListOf<ChatMessage>()

    class ViewHolder(val binding: ItemChatMessageBinding) : RecyclerView.ViewHolder(binding.root)

    fun addMessage(msg: ChatMessage) {
        messages.add(msg)
        notifyItemInserted(messages.size - 1)
    }

    fun getMessages(): List<ChatMessage> = messages.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChatMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val msg = messages[position]
        holder.binding.tvMessage.text = msg.content

        val params = holder.binding.cardMessage.layoutParams as FrameLayout.LayoutParams
        if (msg.role == "user") {
            params.gravity = Gravity.END
            holder.binding.cardMessage.setCardBackgroundColor(0xFF6200EE.toInt())
            holder.binding.tvMessage.setTextColor(0xFFFFFFFF.toInt())
        } else {
            params.gravity = Gravity.START
            holder.binding.cardMessage.setCardBackgroundColor(0xFFFFFFFF.toInt())
            holder.binding.tvMessage.setTextColor(0xFF212121.toInt())
        }
        holder.binding.cardMessage.layoutParams = params
    }

    override fun getItemCount() = messages.size
}
