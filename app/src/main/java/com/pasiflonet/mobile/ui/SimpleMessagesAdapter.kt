package com.pasiflonet.mobile.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pasiflonet.mobile.databinding.RowMessageBinding
import org.drinkless.tdlib.TdApi

class SimpleMessagesAdapter : RecyclerView.Adapter<SimpleMessagesAdapter.VH>() {
    private var items: List<TdApi.Message> = emptyList()

    fun submit(list: List<TdApi.Message>) {
        items = list
        notifyDataSetChanged()
    }

    class VH(val b: RowMessageBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = RowMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = items[position]
        holder.b.tvLine1.text = "msgId=${m.id} chatId=${m.chatId}"
        holder.b.tvLine2.text = m.content.javaClass.simpleName
    }
}
