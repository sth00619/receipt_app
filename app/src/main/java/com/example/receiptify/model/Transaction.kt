package com.example.receiptify.model

data class Transaction(
    val id: String = "",
    val storeName: String = "",
    val category: String = "",
    val amount: Long = 0L,
    val date: Long = System.currentTimeMillis(),
    val userId: String = ""
)