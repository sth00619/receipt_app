package com.example.receiptify.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.receiptify.api.models.ReceiptResponse
import com.example.receiptify.databinding.ItemReceiptBinding
import java.text.NumberFormat
import java.util.*

class ReceiptListAdapter(
    private val onClick: (ReceiptResponse) -> Unit
) : RecyclerView.Adapter<ReceiptListAdapter.ViewHolder>() {

    private val items = mutableListOf<ReceiptResponse>()
    private val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)

    fun submitList(list: List<ReceiptResponse>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class ViewHolder(
        private val binding: ItemReceiptBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ReceiptResponse) {
            binding.tvStoreName.text = item.storeName
            binding.tvAmount.text = "₩ ${numberFormat.format(item.totalAmount)}"
            binding.tvDate.text = item.transactionDate

            binding.root.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReceiptBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }
}
