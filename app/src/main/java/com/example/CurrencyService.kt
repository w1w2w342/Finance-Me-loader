package com.example

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object CurrencyService {
    // Fetches how many UAH you get for 1 unit of `currencyCode`.
    // Actually, open.er-api.com/v6/latest/{base} returns the rates.
    // So for base = UAH, it returns rates for 1 UAH to XYZ.
    // If we want to know how much 1 XYZ is in UAH, we just divide 1.0 / rate.
    // E.g., if base is UAH, USD rate is 0.024. 1 USD in UAH = 1 / 0.024.
    suspend fun getRateToUAH(currencyCode: String): Double = withContext(Dispatchers.IO) {
        if (currencyCode.uppercase() == "UAH") return@withContext 1.0
        try {
            val url = URL("https://open.er-api.com/v6/latest/UAH")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObject = JSONObject(response)
                val rates = jsonObject.getJSONObject("rates")
                val foreignRate = rates.optDouble(currencyCode.uppercase(), -1.0)
                if (foreignRate > 0) {
                    return@withContext 1.0 / foreignRate
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext 1.0
    }
    
    val supportedCurrencies = listOf("UAH", "USD", "EUR", "PLN", "GBP", "CZK")
}
