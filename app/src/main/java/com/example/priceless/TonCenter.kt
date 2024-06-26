package com.example.priceless

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class TonCenter {

    private val client = OkHttpClient()

    suspend fun makeHttpRequest(walletAddress: String): Result<TonApiResponse?> {
        return withContext(Dispatchers.IO) {
            val url = "https://tonapi.io/v2/blockchain/accounts/$walletAddress/transactions?limit=1&sort_order=desc"

            val request = Request.Builder()
                .url(url)
                .build()

            try {
                val response = client.newCall(request).execute()
                Log.e("my_tag", "response is: $response")
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    Log.e("my_tag", "responseBody is: $responseBody")
                    if (responseBody != null) {
                        val tonApiResponse = parseResponse(responseBody)
                        Log.e("my_tag", "tonApiResponse is: $tonApiResponse")
                        if (tonApiResponse != null) {
                            Result.success(tonApiResponse)
                        } else {
                            Result.failure(Exception("Failed to parse the response"))
                        }
                    } else {
                        Result.failure(Exception("Response body is null"))
                    }
                } else {
                    Result.failure(Exception("HTTP request failed with status: ${response.code}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun parseResponse(response: String): TonApiResponse? {
        return try {
            val jsonResponse = JSONObject(response)
            val transactionsArray = jsonResponse.getJSONArray("transactions")

            if (transactionsArray.length() > 0) {
                val firstTransaction = transactionsArray.getJSONObject(0)
                val outMsgsArray = firstTransaction.getJSONArray("out_msgs")
                if (outMsgsArray.length() > 0) {
                    val firstOutMsg = outMsgsArray.getJSONObject(0)
                    val source = firstOutMsg.getJSONObject("source").optString("address")
                    val destination = firstOutMsg.getJSONObject("destination").optString("address")
                    val value = firstOutMsg.optString("value")
                    val bodyHash = firstOutMsg.optString("hash")
                    if (!source.isNullOrEmpty() && !destination.isNullOrEmpty() && !value.isNullOrEmpty() && !bodyHash.isNullOrEmpty()) {
                        TonApiResponse(source, destination, value, bodyHash)
                    } else {
                        null
                    }
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


}

data class TonApiResponse(
    val source: String,
    val destination: String,
    val value: String,
    val bodyHash: String
)
