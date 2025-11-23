package com.example.receiptify.model

data class CategorySummary(
    val code: String?,
    val name: String,
    val icon: Int,
    val color: Int,
    val amount: Long,
    val count: Int,
    val percentage: Float
)