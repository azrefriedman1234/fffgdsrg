package com.pasiflonet.mobile.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pasiflonet.mobile.databinding.RowSourceBinding
import com.pasiflonet.mobile.td.SourceRow

class SourcesAdapter(
    private val onClick: (Long) -> Unit
) : RecyclerView.Adapter<SourcesAdapter.VH>() {

    private var items: List<SourceRow> = emptyList()

    fun submit(list: List<SourceRow>) {
        items = list
        notifyDataSetChanged()
    }

    class VH(val b: RowSourceBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = RowSourceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val it = items[position]
        holder.b.tvTitle.text = it.title
        holder.b.tvSub.text = it.lastMessageSummary
        holder.b.root.setOnClickListener { _ -> onClick(it.chatId) }
    }
}
