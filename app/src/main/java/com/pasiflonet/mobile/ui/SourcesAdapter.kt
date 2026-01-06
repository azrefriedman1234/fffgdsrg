package com.pasiflonet.mobile.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pasiflonet.mobile.databinding.RowSourceBinding
import com.pasiflonet.mobile.td.SourceRow

class SourcesAdapter(
    private var items: List<SourceRow>,
    private val onClick: (SourceRow) -> Unit
) : RecyclerView.Adapter<SourcesAdapter.VH>() {

    class VH(val b: RowSourceBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = RowSourceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val row = items[position]
        holder.b.tvTitle.text = row.title
        holder.b.tvSubtitle.text = row.subtitle
        holder.b.root.setOnClickListener { onClick(row) }
    }

    fun submit(newItems: List<SourceRow>) {
        items = newItems
        notifyDataSetChanged()
    }
}
