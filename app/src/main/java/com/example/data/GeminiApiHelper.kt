package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

internal object GeminiApiHelper {
    private const val TAG = "GeminiApiHelper"
    private const val DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    val defaultModels: List<String> = listOf(
        "gemini-2.5-flash",
        "gemini-3-flash-preview",
        "gemini-3.5-flash",
        "gemini-2.5-flash-lite"
    )

    val defaultClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    fun hasValidApiKey(apiKey: String = BuildConfig.GEMINI_API_KEY): Boolean {
        val trimmed = apiKey.trim()
        return trimmed.isNotEmpty() &&
            trimmed != "MY_GEMINI_API_KEY" &&
            trimmed != "GEMINI_API_KEY"
    }

    /**
     * Executes a Gemini `generateContent` POST call with automatic transient retry (503/429/5xx)
     * and model fallback. Returns extracted candidate text, or null on non-fatal unavailability.
     */
    suspend fun generateContentText(
        payload: JSONObject,
        apiKey: String = BuildConfig.GEMINI_API_KEY,
        client: OkHttpClient = defaultClient,
        models: List<String> = defaultModels,
        baseUrl: String = DEFAULT_BASE_URL,
        retryDelayMs: Long = 500L,
        callerTag: String = TAG
    ): String? {
        if (!hasValidApiKey(apiKey)) {
            Log.w(callerTag, "Gemini API key not configured; skipping remote call.")
            return null
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val payloadStr = payload.toString()

        for ((modelIndex, modelName) in models.withIndex()) {
            val url = "$baseUrl/$modelName:generateContent?key=$apiKey"
            val maxAttemptsForModel = if (modelIndex == 0) 2 else 1

            for (attempt in 1..maxAttemptsForModel) {
                val request = Request.Builder()
                    .url(url)
                    .post(payloadStr.toRequestBody(mediaType))
                    .build()

                try {
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val bodyStr = response.body?.string() ?: return null
                            return extractCandidateText(bodyStr)
                        }

                        val code = response.code
                        val msg = response.message
                        val isTransient = code == 429 || code == 500 || code == 502 || code == 503 || code == 504
                        val isModelNotFound = code == 404

                        if (code == 401 || code == 403) {
                            Log.w(callerTag, "Gemini API authorization failed ($code $msg).")
                            return null
                        }

                        if (!isTransient && !isModelNotFound) {
                            Log.w(callerTag, "Gemini API returned non-retryable status $code $msg for $modelName.")
                            return null
                        }

                        Log.w(callerTag, "Transient/model issue on $modelName (attempt $attempt): HTTP $code $msg")

                        if (isTransient && attempt < maxAttemptsForModel && retryDelayMs > 0) {
                            delay(retryDelayMs * attempt)
                        } else {
                            // Move to next fallback model
                            break
                        }
                    }
                } catch (e: IOException) {
                    Log.w(callerTag, "Network error calling $modelName (attempt $attempt): ${e.message}")
                    if (attempt < maxAttemptsForModel && retryDelayMs > 0) {
                        delay(retryDelayMs * attempt)
                    } else {
                        break
                    }
                } catch (e: Exception) {
                    Log.w(callerTag, "Unexpected failure calling $modelName: ${e.message}")
                    return null
                }
            }
        }

        Log.w(callerTag, "All Gemini models unavailable right now; using local fallback.")
        return null
    }

    internal fun extractCandidateText(responseBody: String): String? {
        return try {
            val rootJson = JSONObject(responseBody)
            val candidates = rootJson.optJSONArray("candidates") ?: return null
            val firstCandidate = candidates.optJSONObject(0) ?: return null
            val content = firstCandidate.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            var text = parts.optJSONObject(0)?.optString("text")?.trim() ?: return null
            if (text.startsWith("```")) {
                text = text.removePrefix("```json").removePrefix("```").trim()
            }
            if (text.endsWith("```")) {
                text = text.removeSuffix("```").trim()
            }
            text.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            Log.w(TAG, "Failed parsing Gemini candidate response: ${e.message}")
            null
        }
    }
}
