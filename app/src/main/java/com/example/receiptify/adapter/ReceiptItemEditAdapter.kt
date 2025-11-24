package com.example.receiptify.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.receiptify.databinding.ItemReceiptEditBinding

class ReceiptItemEditAdapter(
    private val items: MutableList<ReceiptItemEdit>,
    private val onItemChanged: () -> Unit,
    private val onDeleteItem: (Int) -> Unit
) : RecyclerView.Adapter<ReceiptItemEditAdapter.ViewHolder>() {

    data class ReceiptItemEdit(
        var name: String,
        var quantity: Int,
        var unitPrice: Int,
        var amount: Int
    )

    inner class ViewHolder(private val binding: ItemReceiptEditBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var isUpdating = false

        fun bind(item: ReceiptItemEdit, position: Int) {
            isUpdating = true

            binding.etItemName.setText(item.name)
            binding.etQuantity.setText(item.quantity.toString())
            binding.etUnitPrice.setText(item.unitPrice.toString())
            binding.etAmount.setText(item.amount.toString())

            isUpdating = false

            // 품목명 변경
            binding.etItemName.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (!isUpdating) {
                        item.name = s.toString()
                    }
                }
            })

            // 수량 변경
            binding.etQuantity.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (!isUpdating) {
                        item.quantity = s.toString().toIntOrNull() ?: 0
                        updateAmount(item)
                    }
                }
            })

            // 단가 변경
            binding.etUnitPrice.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (!isUpdating) {
                        item.unitPrice = s.toString().toIntOrNull() ?: 0
                        updateAmount(item)
                    }
                }
            })

            // 금액 변경 (직접 입력)
            binding.etAmount.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (!isUpdating) {
                        item.amount = s.toString().toIntOrNull() ?: 0
                        onItemChanged()
                    }
                }
            })

            // 삭제 버튼
            binding.btnDelete.setOnClickListener {
                onDeleteItem(position)
            }
        }

        private fun updateAmount(item: ReceiptItemEdit) {
            isUpdating = true
            item.amount = item.quantity * item.unitPrice
            binding.etAmount.setText(item.amount.toString())
            isUpdating = false
            onItemChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReceiptEditBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size
}