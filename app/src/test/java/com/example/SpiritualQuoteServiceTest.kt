package com.example

import com.example.data.SpiritualQuoteService
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SpiritualQuoteServiceTest {

    private fun buildInterceptingClient(handler: (url: String, attemptIndex: Int) -> Pair<Int, String>): OkHttpClient {
        var counter = 0
        val interceptor = Interceptor { chain ->
            counter++
            val req = chain.request()
            val (code, body) = handler(req.url.toString(), counter)
            Response.Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message(if (code == 200) "OK" else "Service Unavailable")
                .body(body.toResponseBody("application/json; charset=utf-8".toMediaType()))
                .build()
        }
        return OkHttpClient.Builder().addInterceptor(interceptor).build()
    }

    private fun geminiJsonResponse(arabicText: String, translation: String, source: String): String {
        val innerJson = """
            {"arabic_text":"$arabicText","translation":"$translation","source":"$source","theme":"Patience","reflection":"Stay steadfast."}
        """.trimIndent().replace("\"", "\\\"")
        return """{"candidates":[{"content":{"parts":[{"text":"$innerJson"}]}}]}"""
    }

    @Test
    fun `retries primary model on 503 and returns AI quote on recovery`() = runBlocking {
        val client = buildInterceptingClient { _, attempt ->
            if (attempt == 1) {
                503 to """{"error":{"code":503,"message":"The model is overloaded."}}"""
            } else {
                200 to geminiJsonResponse(
                    arabicText = "وَاصْبِرْ وَمَا صَبْرُكَ إِلَّا بِاللَّهِ",
                    translation = "And be patient, and your patience is not but through Allah.",
                    source = "سورة النحل: 127"
                )
            }
        }

        val quote = SpiritualQuoteService.fetchSpiritualQuote(
            theme = "Patience",
            language = "EN",
            apiKey = "AIzaFakeTestKey12345",
            client = client,
            models = listOf("gemini-2.5-flash", "gemini-3-flash-preview"),
            retryDelayMs = 0L
        )

        assertTrue(quote.isAiGenerated)
        assertEquals("وَاصْبِرْ وَمَا صَبْرُكَ إِلَّا بِاللَّهِ", quote.arabicText)
        assertEquals("سورة النحل: 127", quote.source)
    }

    @Test
    fun `falls back to secondary model when primary persistently returns 503`() = runBlocking {
        val calledUrls = mutableListOf<String>()
        val client = buildInterceptingClient { url, _ ->
            calledUrls.add(url)
            if (url.contains("gemini-3.5-flash")) {
                503 to """{"error":{"code":503,"message":"Overloaded"}}"""
            } else {
                200 to geminiJsonResponse(
                    arabicText = "إِنَّ اللَّهَ مَعَ الصَّابِرِينَ",
                    translation = "Indeed, Allah is with the patient.",
                    source = "سورة البقرة: 153"
                )
            }
        }

        val quote = SpiritualQuoteService.fetchSpiritualQuote(
            theme = "Patience",
            language = "EN",
            apiKey = "AIzaFakeTestKey12345",
            client = client,
            models = listOf("gemini-3.5-flash", "gemini-2.5-flash"),
            retryDelayMs = 0L
        )

        assertTrue(quote.isAiGenerated)
        assertEquals("إِنَّ اللَّهَ مَعَ الصَّابِرِينَ", quote.arabicText)
        assertTrue(calledUrls.any { it.contains("gemini-3.5-flash") })
        assertTrue(calledUrls.any { it.contains("gemini-2.5-flash") })
    }

    @Test
    fun `returns curated spiritual quote gracefully when all models return 503`() = runBlocking {
        val client = buildInterceptingClient { _, _ ->
            503 to """{"error":{"code":503,"message":"Service Unavailable"}}"""
        }

        val quote = SpiritualQuoteService.fetchSpiritualQuote(
            theme = "Peace & Tranquility",
            language = "AR",
            apiKey = "AIzaFakeTestKey12345",
            client = client,
            models = listOf("gemini-2.5-flash", "gemini-3-flash-preview"),
            retryDelayMs = 0L
        )

        assertFalse(quote.isAiGenerated)
        assertTrue(quote.arabicText.isNotBlank())
        assertEquals("Peace & Tranquility", quote.theme)
    }
}
