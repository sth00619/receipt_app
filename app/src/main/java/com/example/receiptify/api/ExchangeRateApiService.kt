package com.example.receiptify.api

import com.example.receiptify.model.ExchangeRateResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ExchangeRateApiService {
    @GET("latest")
    suspend fun getLatestRates(
        @Query("from") base: String = "EUR",
        @Query("to") symbols: String
    ): Response<ExchangeRateResponse>

    @GET
    suspend fun getHistoricalRates(
        @retrofit2.http.Url url: String
    ): Response<ExchangeRateResponse>

    // Helper to get historical by date path since base url is fixed
    @GET("{date}")
    suspend fun getRatesByDate(
        @retrofit2.http.Path("date") date: String,
        @Query("from") base: String = "EUR",
        @Query("to") symbols: String
    ): Response<ExchangeRateResponse>
}
