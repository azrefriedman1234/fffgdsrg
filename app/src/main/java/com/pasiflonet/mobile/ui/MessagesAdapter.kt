package com.pasiflonet.mobile.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.pasiflonet.mobile.R
import com.pasiflonet.mobile.databinding.ItemMessageBinding
import com.pasiflonet.mobile.model.MessageUi

class MessagesAdapter(
    private val onDetails: (MessageUi) -> Unit,
    private val requestThumb: (fileId: Int) -> Unit,
    private val resolveLocalThumb: (fileId: Int, cb: (String?) -> Unit) -> Unit
) : RecyclerView.Adapter<MessagesAdapter.VH>() {

    private val items = ArrayList<MessageUi>()

    fun submit(list: List<MessageUi>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    inner class VH(private val b: ItemMessageBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: MessageUi) {
            b.tvText.text = item.text
            b.tvMeta.text = "id=${item.messageId}  סוג=${item.mediaType}"

            b.btnDetails.isEnabled = item.fileId != null
            b.btnDetails.setOnClickListener { onDetails(item) }

            val thumbId = item.thumbnailFileId
            if (thumbId != null) {
                requestThumb(thumbId)
                resolveLocalThumb(thumbId) { path ->
                    if (path != null) {
                        b.ivThumb.load(java.io.File(path)) {
                            placeholder(R.drawable.ic_launcher_foreground)
                            error(R.drawable.ic_launcher_foreground)
                        }
                    } else {
                        b.ivThumb.setImageResource(R.drawable.ic_launcher_foreground)
                    }
                }
            } else {
                b.ivThumb.setImageResource(R.drawable.ic_launcher_foreground)
            }
        }
    }
}
