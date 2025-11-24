package com.example.receiptify.model

data class EditableReceiptItem(
    var name: String,
    var quantity: Int,
    var amount: Int,
    val id: String = java.util.UUID.randomUUID().toString()
)