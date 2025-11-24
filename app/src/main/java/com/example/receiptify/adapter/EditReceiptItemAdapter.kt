package com.example.receiptify.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.receiptify.databinding.ItemEditReceiptItemBinding
import com.example.receiptify.model.EditableReceiptItem

class EditReceiptItemAdapter(
    private val onItemChanged: (Int, EditableReceiptItem) -> Unit,
    private val onItemDeleted: (Int) -> Unit
) : ListAdapter<EditableReceiptItem, EditReceiptItemAdapter.ItemViewHolder>(ItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemEditReceiptItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ItemViewHolder(binding, onItemChanged, onItemDeleted)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    class ItemViewHolder(
        private val binding: ItemEditReceiptItemBinding,
        private val onItemChanged: (Int, EditableReceiptItem) -> Unit,
        private val onItemDeleted: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentPosition: Int = -1

        fun bind(item: EditableReceiptItem, position: Int) {
            currentPosition = position

            // 기존 리스너 제거
            binding.etItemName.removeTextChangedListener(nameWatcher)
            binding.etItemQuantity.removeTextChangedListener(quantityWatcher)
            binding.etItemAmount.removeTextChangedListener(amountWatcher)

            // 데이터 설정
            binding.etItemName.setText(item.name)
            binding.etItemQuantity.setText(item.quantity.toString())
            binding.etItemAmount.setText(item.amount.toString())

            // 리스너 재등록
            binding.etItemName.addTextChangedListener(nameWatcher)
            binding.etItemQuantity.addTextChangedListener(quantityWatcher)
            binding.etItemAmount.addTextChangedListener(amountWatcher)

            // 삭제 버튼
            binding.btnDeleteItem.setOnClickListener {
                onItemDeleted(currentPosition)
            }
        }

        private val nameWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                notifyChange()
            }
        }

        private val quantityWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                notifyChange()
            }
        }

        private val amountWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                notifyChange()
            }
        }

        private fun notifyChange() {
            val name = binding.etItemName.text.toString()
            val quantity = binding.etItemQuantity.text.toString().toIntOrNull() ?: 1
            val amount = binding.etItemAmount.text.toString().toIntOrNull() ?: 0

            onItemChanged(
                currentPosition,
                EditableReceiptItem(name, quantity, amount)
            )
        }
    }

    class ItemDiffCallback : DiffUtil.ItemCallback<EditableReceiptItem>() {
        override fun areItemsTheSame(oldItem: EditableReceiptItem, newItem: EditableReceiptItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: EditableReceiptItem, newItem: EditableReceiptItem): Boolean {
            return oldItem == newItem
        }
    }
}