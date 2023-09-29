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


    suspend fun getCurrentTimeAndDate(): Pair<String?, String?>? {
        return withContext(Dispatchers.IO) {
            var dateAndTimePair: Pair<String?, String?>? = null

            try {
                val request = Request.Builder()
                    .url(apiEndpoint)
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (!responseBody.isNullOrEmpty()) {
                        Log.d("full response", responseBody)
                        val jsonObject = JSONObject(responseBody)

                        val dateTime = jsonObject.optString("formatted")
                        val dateTimeUnix = jsonObject.optLong("timestamp", 0L)
                        val gmtOffset = jsonObject.optLong("gmtOffset", 0L)

                        if (!dateTime.isNullOrEmpty() && dateTimeUnix != 0L && gmtOffset != 0L) {
                            val millisInString = (dateTimeUnix - gmtOffset).toString()
                            Log.d("Extracted date_time", "dateTime: $dateTime millis: $millisInString")

                            dateAndTimePair = Pair(dateTime, millisInString)
                        }
                    }
                } else {
                    Log.e("http request failed", "${response.code} ${response.message}")
                }
            } catch (e: SocketTimeoutException) {
                Log.e("http request failed", "Socket timeout error: ${e.message}")
            } catch (e: IOException) {
                Log.e("http request failed", "IO error: ${Log.getStackTraceString(e)}")
            } catch (e: Exception) {
                Log.e("http request failed", "Unknown error: ${Log.getStackTraceString(e)}")
            }
            dateAndTimePair
        }
    }


}
