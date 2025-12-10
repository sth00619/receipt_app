package com.example.receiptify.model

data class ExchangeRateResponse(
    val amount: Double,
    val base: String,
    val date: String,
    val rates: Map<String, Double>
)
