package com.example.receiptify.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.receiptify.R
import com.example.receiptify.databinding.ItemTransactionBinding
import com.example.receiptify.model.Transaction
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionAdapter : ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TransactionViewHolder(
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)
        private val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA)

        fun bind(transaction: Transaction) {
            binding.apply {
                tvStoreName.text = transaction.storeName
                tvCategory.text = getCategoryName(transaction.category)
                tvDate.text = dateFormat.format(Date(transaction.date))
                tvAmount.text = "₩ ${numberFormat.format(transaction.amount)}"

                // Category color
                val categoryColor = getCategoryColor(transaction.category)
                viewCategoryIndicator.setBackgroundColor(
                    ContextCompat.getColor(root.context, categoryColor)
                )
            }
        }

        private fun getCategoryName(category: String): String {
            val context = binding.root.context
            return when (category.lowercase()) {
                "food" -> context.getString(R.string.category_food)
                "transport" -> context.getString(R.string.category_transport)
                "shopping" -> context.getString(R.string.category_shopping)
                else -> context.getString(R.string.category_others)
            }
        }

        private fun getCategoryColor(category: String): Int {
            return when (category.lowercase()) {
                "food" -> R.color.category_food
                "transport" -> R.color.category_transport
                "shopping" -> R.color.category_shopping
                else -> R.color.category_others
            }
        }
    }

    class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
}