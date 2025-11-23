package com.example.receiptify.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.receiptify.databinding.ItemCategoryBinding
import com.example.receiptify.model.CategorySummary
import java.text.NumberFormat
import java.util.Locale

class CategoryAdapter(
    private val onCategoryClick: (CategorySummary) -> Unit
) : ListAdapter<CategorySummary, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding, onCategoryClick)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CategoryViewHolder(
        private val binding: ItemCategoryBinding,
        private val onCategoryClick: (CategorySummary) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)

        fun bind(category: CategorySummary) {
            binding.apply {
                // 카테고리 이름
                tvCategoryName.text = category.name

                // 금액
                tvCategoryAmount.text = "₩ ${numberFormat.format(category.amount)}"

                // 건수
                tvCategoryCount.text = "${category.count}건"

                // 퍼센트
                tvCategoryPercentage.text = "전체의 ${String.format("%.1f", category.percentage)}%"

                // 프로그레스 바
                progressBar.progress = category.percentage.toInt()

                // 아이콘 배경색
                categoryIconContainer.setBackgroundColor(
                    ContextCompat.getColor(root.context, category.color)
                )

                // 아이콘
                ivCategoryIcon.setImageResource(category.icon)

                // 클릭 이벤트
                root.setOnClickListener {
                    onCategoryClick(category)
                }
            }
        }
    }

    class CategoryDiffCallback : DiffUtil.ItemCallback<CategorySummary>() {
        override fun areItemsTheSame(oldItem: CategorySummary, newItem: CategorySummary): Boolean {
            return oldItem.code == newItem.code
        }

        override fun areContentsTheSame(oldItem: CategorySummary, newItem: CategorySummary): Boolean {
            return oldItem == newItem
        }
    }
}