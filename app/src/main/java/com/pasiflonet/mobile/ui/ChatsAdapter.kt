package com.pasiflonet.mobile.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pasiflonet.mobile.databinding.ItemChatBinding
import com.pasiflonet.mobile.model.ChatUi

class ChatsAdapter(
    private val onClick: (chatId: Long, title: String) -> Unit
) : RecyclerView.Adapter<ChatsAdapter.VH>() {

    private val items = ArrayList<ChatUi>()

    fun submit(list: List<ChatUi>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemChatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    inner class VH(private val b: ItemChatBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: ChatUi) {
            b.tvTitle.text = item.title
            b.tvSubtitle.text = item.subtitle
            b.root.setOnClickListener { onClick(item.id, item.title) }
        }
    }
}
