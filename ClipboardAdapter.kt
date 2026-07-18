package com.smart.clipboard.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smart.clipboard.R
import com.smart.clipboard.data.ClipboardItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClipboardAdapter(
    private val onCopyClick: (String) -> Unit,
    private val onPinClick: (ClipboardItem) -> Unit,
    private val onDeleteClick: (ClipboardItem) -> Unit
) : ListAdapter<ClipboardItem, ClipboardAdapter.ClipboardViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClipboardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_clipboard, parent, false)
        return ClipboardViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClipboardViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ClipboardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvText: TextView = itemView.findViewById(R.id.tvClipboardText)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        private val btnCopy: ImageButton = itemView.findViewById(R.id.btnCopy)
        private val btnPin: ImageButton = itemView.findViewById(R.id.btnPin)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        fun bind(item: ClipboardItem) {
            tvText.text = item.text
            
            val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
            tvTimestamp.text = sdf.format(Date(item.timestamp))

            // Pin icon tint or state
            if (item.isPinned) {
                btnPin.setImageResource(android.R.drawable.star_on)
            } else {
                btnPin.setImageResource(android.R.drawable.star_off)
            }

            itemView.setOnClickListener { onCopyClick(item.text) }
            btnCopy.setOnClickListener { onCopyClick(item.text) }
            btnPin.setOnClickListener { onPinClick(item) }
            btnDelete.setOnClickListener { onDeleteClick(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ClipboardItem>() {
        override fun areItemsTheSame(oldItem: ClipboardItem, newItem: ClipboardItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ClipboardItem, newItem: ClipboardItem): Boolean {
            return oldItem == newItem
        }
    }
}
