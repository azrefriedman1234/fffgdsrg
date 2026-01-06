package com.pasiflonet.mobile.ui

import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SourcesAdapter(
    private val onClick: (SourceRow) -> Unit
) : RecyclerView.Adapter<SourcesAdapter.VH>() {

    private val items = mutableListOf<SourceRow>()

    fun submit(list: List<SourceRow>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val tv = TextView(parent.context).apply {
            textSize = 16f
            setPadding(24, 20, 24, 20)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        return VH(tv, onClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
    override fun getItemCount(): Int = items.size

    class VH(
        private val tv: TextView,
        private val onClick: (SourceRow) -> Unit
    ) : RecyclerView.ViewHolder(tv) {
        fun bind(item: SourceRow) {
            tv.text = item.title
            tv.setOnClickListener { onClick(item) }
        }
    }
}
