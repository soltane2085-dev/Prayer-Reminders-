package com.example.data

import android.util.Log
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

data class GeoLocationResult(
    val success: Boolean,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val timezoneOffset: Double,
    val errorMessage: String = ""
)

data class MosqueSearchResult(
    val success: Boolean,
    val name: String,
    val address: String,
    val fajrOffset: Int,
    val dhuhrOffset: Int,
    val asrOffset: Int,
    val maghribOffset: Int,
    val ishaOffset: Int,
    val sourcePlatform: String = "MAWAQIT",
    val mosqueSlug: String = "",
    val errorMessage: String = ""
)

object GeminiQuoteService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun generateSpiritualQuote(topic: String): Pair<String, String>? = withContext(Dispatchers.IO) {
        val prompt = "Generate a single spiritually-uplifting, peaceful, and inspirational quote or reflection IN ARABIC related to the topic: '$topic'. " +
                "Respond strictly in plain text using the separator symbol '~' to isolate the text and the source. " +
                "The response MUST follow this exact format: 'QuoteText ~ Author/Source'. " +
                "Example format: إنما الأعمال بالنيات ~ الرسول محمد ﷺ " +
                "Do not include any extra lines, bullets, or markdown bold asterisks. Everything must be in standard elegant Arabic."

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
        }

        try {
            val text = GeminiApiHelper.generateContentText(
                payload = jsonRequest,
                client = client,
                callerTag = "GeminiService"
            ) ?: return@withContext null

            Log.d("GeminiService", "Response received: $text")
            if (text.contains("~")) {
                val split = text.split("~")
                val quoteText = split[0].trim().removePrefix("\"").removeSuffix("\"").trim()
                val authorText = split[1].trim().removePrefix("\"").removeSuffix("\"").trim()
                Pair(quoteText, authorText)
            } else {
                Pair(text, "Sajda Wisdom")
            }
        } catch (e: Exception) {
            Log.w("GeminiService", "Exception generating quote: ${e.message}")
            null
        }
    }

    suspend fun searchLocation(query: String): GeoLocationResult? = withContext(Dispatchers.IO) {
        val prompt = "Find the geographic coordinates and timezone UTC offset for the following city or location query: \"$query\". " +
                "Respond STRICTLY in a valid JSON format with the following keys:\n" +
                "- \"success\": boolean (true if found, false otherwise)\n" +
                "- \"name\": string (the beautifully formatted name of the city, state/province, and country, e.g. \"Paris, France\" or \"Makkah, Saudi Arabia\")\n" +
                "- \"latitude\": double (the decimal latitude, e.g. 48.8566)\n" +
                "- \"longitude\": double (the decimal longitude, e.g. 2.3522)\n" +
                "- \"timezone_offset\": double (the standard timezone UTC offset in hours, e.g. 3.0, -5.0, 5.5, 1.0)\n" +
                "- \"error_message\": string (if not success, explain why, else empty)\n\n" +
                "Do not wrap the response in markdown code blocks like ```json ... ```. Respond in plain raw JSON text only."

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
            })
        }

        try {
            val text = GeminiApiHelper.generateContentText(
                payload = jsonRequest,
                client = client,
                callerTag = "GeminiService"
            ) ?: return@withContext null

            val resultObj = JSONObject(text)
            val success = resultObj.optBoolean("success", false)
            if (success) {
                GeoLocationResult(
                    success = true,
                    name = resultObj.optString("name", query),
                    latitude = resultObj.optDouble("latitude", 0.0),
                    longitude = resultObj.optDouble("longitude", 0.0),
                    timezoneOffset = resultObj.optDouble("timezone_offset", 0.0),
                    errorMessage = ""
                )
            } else {
                GeoLocationResult(
                    success = false,
                    name = "",
                    latitude = 0.0,
                    longitude = 0.0,
                    timezoneOffset = 0.0,
                    errorMessage = resultObj.optString("error_message", "Location not found.")
                )
            }
        } catch (e: Exception) {
            Log.w("GeminiService", "Exception in searchLocation: ${e.message}")
            null
        }
    }

    suspend fun searchMosque(query: String): MosqueSearchResult? = withContext(Dispatchers.IO) {
        val prompt = "Find official mosque prayer and Iqamah (الإقامة) details for the given mosque name/location query: \"$query\". " +
                "Check databases like MAWAQIT or official sources to extract correct parameters. " +
                "Respond STRICTLY in a valid JSON format with the following keys:\n" +
                "- \"success\": boolean (true if found and parameters are verified, false otherwise)\n" +
                "- \"name\": string (the beautifully formatted name of the mosque, e.g. \"La Grande Mosquée de Paris\" or \"Mawaqit Masjid Al-Nur\")\n" +
                "- \"address\": string (the address, city, and country description)\n" +
                "- \"fajr_offset\": integer (default interval/offset in minutes between Fajr Adhan and Fajr Iqamah, e.g. 20)\n" +
                "- \"dhuhr_offset\": integer (default interval/offset in minutes between Dhuhr Adhan and Dhuhr Iqamah, e.g. 15)\n" +
                "- \"asr_offset\": integer (default interval/offset in minutes between Asr Adhan and Asr Iqamah, e.g. 15)\n" +
                "- \"maghrib_offset\": integer (default interval/offset in minutes between Maghrib Adhan and Maghrib Iqamah, e.g. 7 or 10)\n" +
                "- \"isha_offset\": integer (default interval/offset in minutes between Isha Adhan and Isha Iqamah, e.g. 15)\n" +
                "- \"source_platform\": string (underlying platform database, usually \"MAWAQIT\", \"Official\" or \"Community\")\n" +
                "- \"mosque_slug\": string (the representative URL slug/id for this mosque, e.g. \"mosquee-de-paris\" or \"masjid-al-noor-nyc\")\n" +
                "- \"error_message\": string (if not success, explain why, else empty)\n\n" +
                "Do not wrap the response in markdown code blocks like ```json ... ```. Respond in plain raw JSON text only."

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
            })
        }

        try {
            val text = GeminiApiHelper.generateContentText(
                payload = jsonRequest,
                client = client,
                callerTag = "GeminiService"
            ) ?: return@withContext null

            val resultObj = JSONObject(text)
            val success = resultObj.optBoolean("success", false)
            if (success) {
                MosqueSearchResult(
                    success = true,
                    name = resultObj.optString("name", query),
                    address = resultObj.optString("address", ""),
                    fajrOffset = resultObj.optInt("fajr_offset", 20),
                    dhuhrOffset = resultObj.optInt("dhuhr_offset", 15),
                    asrOffset = resultObj.optInt("asr_offset", 15),
                    maghribOffset = resultObj.optInt("maghrib_offset", 10),
                    ishaOffset = resultObj.optInt("isha_offset", 15),
                    sourcePlatform = resultObj.optString("source_platform", "MAWAQIT"),
                    mosqueSlug = resultObj.optString("mosque_slug", "mosque"),
                    errorMessage = ""
                )
            } else {
                MosqueSearchResult(
                    success = false,
                    name = "",
                    address = "",
                    fajrOffset = 20,
                    dhuhrOffset = 15,
                    asrOffset = 15,
                    maghribOffset = 10,
                    ishaOffset = 15,
                    sourcePlatform = "MAWAQIT",
                    mosqueSlug = "",
                    errorMessage = resultObj.optString("error_message", "Mosque not found.")
                )
            }
        } catch (e: Exception) {
            Log.w("GeminiService", "Exception in searchMosque: ${e.message}")
            null
        }
    }
}
