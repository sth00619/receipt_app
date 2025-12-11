package com.example.receiptify.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.receiptify.api.models.ReceiptItem
import com.example.receiptify.databinding.ItemReceiptItemBinding
import java.text.NumberFormat
import java.util.Locale

class ReceiptItemAdapter : ListAdapter<ReceiptItem, ReceiptItemAdapter.ReceiptItemViewHolder>(ReceiptItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceiptItemViewHolder {
        val binding = ItemReceiptItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReceiptItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReceiptItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ReceiptItemViewHolder(
        private val binding: ItemReceiptItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)

        fun bind(item: ReceiptItem) {
            binding.apply {
                tvItemName.text = item.name
                tvQuantity.text = "x${item.quantity}"

                if (item.unitPrice != null && item.unitPrice > 0) {
                    tvUnitPrice.text = " ₩${numberFormat.format(item.unitPrice.toLong())}"
                } else {
                    tvUnitPrice.text = ""
                }

                tvAmount.text = "₩${numberFormat.format(item.amount.toLong())}"
            }
        }
    }

    class ReceiptItemDiffCallback : DiffUtil.ItemCallback<ReceiptItem>() {
        override fun areItemsTheSame(oldItem: ReceiptItem, newItem: ReceiptItem): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: ReceiptItem, newItem: ReceiptItem): Boolean {
            return oldItem == newItem
        }
    }
}
