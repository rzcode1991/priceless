package com.example.priceless

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class GetTime {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val apiEndpoint =
        "http://api.timezonedb.com/v2.1/get-time-zone?key=3DP6C4MVV1ZV&format=json&by=zone&zone=Asia/Tehran"


    suspend fun getCurrentTimeAndDate(): Result<Pair<String, String>> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(apiEndpoint)
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (!responseBody.isNullOrEmpty()) {
                        val jsonObject = JSONObject(responseBody)
                        val dateTime = jsonObject.optString("formatted")
                        val dateTimeUnix = jsonObject.optLong("timestamp", 0L)
                        val gmtOffset = jsonObject.optLong("gmtOffset", 0L)

                        if (!dateTime.isNullOrEmpty() && dateTimeUnix != 0L && gmtOffset != 0L) {
                            val millisInString = (dateTimeUnix - gmtOffset).toString()
                            Log.d("Extracted date_time", "dateTime: $dateTime millis: $millisInString")

                            val dateAndTimePair = Pair(dateTime, millisInString)
                            Result.success(dateAndTimePair)
                        } else {
                            Result.failure(Exception("Error parsing the response"))
                        }
                    } else {
                        Result.failure(Exception("${response.code} ${response.message}"))
                    }
                } else {
                    Result.failure(Exception("${response.code} ${response.message}"))
                }
            } catch (e: SocketTimeoutException) {
                Result.failure(e)
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }


}
