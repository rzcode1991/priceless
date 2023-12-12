package com.example.priceless

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException

class TonCenter {

    private val client = OkHttpClient()

    suspend fun makeHttpRequest(walletAddress: String): Result<YourData?> {
        return withContext(Dispatchers.IO) {
            val url = "https://toncenter.com/api/v2/getTransactions?address=$walletAddress&limit=1&to_lt=0&archival=true"

            val request = Request.Builder()
                .url(url)
                .addHeader("X-API-Key", "3848e2ab95bb53a005950d35df679387e0f22579644a762f160082e6c1ca58ee")
                .build()

            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (!responseBody.isNullOrEmpty()){
                        val result = parseResponse(responseBody)
                        Result.success(result)
                    }else{
                        Result.failure(Exception("${response.code} ${response.message}"))
                    }
                } else {
                    Result.failure(Exception("${response.code} ${response.message}"))
                }
            } catch (e: IOException) {
                Result.failure(Exception("IO exception: ${e.message}"))
            } catch (e: SocketTimeoutException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun parseResponse(response: String): YourData? {
        try {
            val jsonResponse = JSONObject(response)
            val resultArray = jsonResponse.getJSONArray("result")

            if (resultArray.length() > 0) {
                val firstTransaction = resultArray.getJSONObject(0)
                if (firstTransaction != null){
                    val outMsgArray = firstTransaction.getJSONArray("out_msgs")
                    return if (outMsgArray.length() > 0) {
                        val firstOutMsg = outMsgArray.getJSONObject(0)
                        if (firstOutMsg != null){
                            val source = firstOutMsg.optString("source")
                            val destination = firstOutMsg.optString("destination")
                            val value = firstOutMsg.optString("value")
                            val bodyHash = firstOutMsg.optString("body_hash")
                            if (!source.isNullOrEmpty() && !destination.isNullOrEmpty() &&
                                !value.isNullOrEmpty() && !bodyHash.isNullOrEmpty()){
                                YourData(source, destination, value, bodyHash)
                            }else{
                                null
                            }
                        }else{
                            null
                        }
                    } else {
                        null
                    }
                }else{
                    return null
                }
            } else {
                return null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

}

data class YourData(val source: String, val destination: String, val value: String, val bodyHash: String)