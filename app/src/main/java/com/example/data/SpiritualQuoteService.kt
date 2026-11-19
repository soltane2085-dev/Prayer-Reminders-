package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Data model for a spiritually uplifting quote fetched via Gemini API or curated fallback.
 */
data class SpiritualQuote(
    val arabicText: String,
    val translation: String,
    val source: String,
    val theme: String = "Peace & Tranquility",
    val reflection: String = "",
    val isAiGenerated: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Service responsible for communicating with the Gemini API (model: gemini-3.5-flash)
 * to generate and retrieve spiritually uplifting, inspirational Islamic reflections,
 * Hadiths, and Quranic wisdom.
 */
object SpiritualQuoteService {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // Predefined curated spiritual quotes for offline resilience and immediate delight
    val curatedSpiritualQuotes = listOf(
        SpiritualQuote(
            arabicText = "أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ",
            translation = "Verily, in the remembrance of Allah do hearts find rest.",
            source = "القرآن الكريم، سورة الرعد: 28",
            theme = "Peace & Tranquility",
            reflection = "When life feels overwhelming, centering your focus upon Allah brings deep, unshakable calmness to the soul.",
            isAiGenerated = false
        ),
        SpiritualQuote(
            arabicText = "إِنَّ مَعَ الْعُسْرِ يُسْرًا",
            translation = "Indeed, with hardship comes ease.",
            source = "القرآن الكريم، سورة الشرح: 6",
            theme = "Patience & Hope",
            reflection = "Every difficult moment is intimately accompanied by Divine relief and spiritual growth. Trust the unfolding journey.",
            isAiGenerated = false
        ),
        SpiritualQuote(
            arabicText = "عَجَبًا لِأَمْرِ الْمُؤْمِنِ إِنَّ أَمْرَهُ كُلَّهُ خَيْرٌ، إِنْ أَصَابَتْهُ سَرَّاءُ شَكَرَ فَكَانَ خَيْرًا لَهُ، وَإِنْ أَصَابَتْهُ ضَرَّاءُ صَبَرَ فَكَانَ خَيْرًا لَهُ",
            translation = "How wonderful is the affair of the believer! Everything in his affair is good for him: if good comes to him he is grateful, and if adversity touches him he is patient.",
            source = "صحيح مسلم عن النبي ﷺ",
            theme = "Gratitude & Contentment",
            reflection = "A state of heartfelt shukr in ease and steadfast sabr in trial ensures perpetual peace and Divine reward.",
            isAiGenerated = false
        ),
        SpiritualQuote(
            arabicText = "وَمَن يَتَوَكَّلْ عَلَى اللَّهِ فَهُوَ حَسْبُهُ ۚ إِنَّ اللَّهَ بَالِغُ أَمْرِهِ",
            translation = "And whoever relies upon Allah - then He is sufficient for him. Indeed, Allah will accomplish His purpose.",
            source = "القرآن الكريم، سورة الطلاق: 3",
            theme = "Trust in Allah (Tawakkul)",
            reflection = "Handing your worries over to the Almighty lifts the heavy burden off your shoulders. He knows the destination.",
            isAiGenerated = false
        ),
        SpiritualQuote(
            arabicText = "إِنَّ رَحْمَتِي تَغْلِبُ غَضَبِي",
            translation = "Verily, My Mercy overcomes My Wrath.",
            source = "حديث قدسي (صحيح البخاري ومسلم)",
            theme = "Divine Mercy & Forgiveness",
            reflection = "No matter how distant you may feel, the boundless compassion of Allah is always nearer than your next breath.",
            isAiGenerated = false
        ),
        SpiritualQuote(
            arabicText = "اللَّهُمَّ إِنِّي أَسْأَلُكَ نَفْسًا بِكَ مُطْمَئِنَّةً، تُؤْمِنُ بِلِقَائِكَ، وَتَرْضَى بِقَضَائِكَ، وَتَقْنَعُ بِعَطَائِكَ",
            translation = "O Allah, I ask You for a soul that is at peace with You, believes in meeting You, is pleased with Your decree, and content with Your gifts.",
            source = "دعاء مأثور عن السلف الصالح",
            theme = "Inner Peace & Serenity",
            reflection = "True wealth is not abundance of possessions, but tranquil contentment within the heart.",
            isAiGenerated = false
        ),
        SpiritualQuote(
            arabicText = "احْفَظِ اللَّهَ يَحْفَظْكَ، احْفَظِ اللَّهَ تَجِدْهُ تُجَاهَكَ، إِذَا سَأَلْتَ فَاسْأَلِ اللَّهَ، وَإِذَا اسْتَعَنْتَ فَاسْتَعِنْ بِاللَّهِ",
            translation = "Be mindful of Allah and He will protect you. Be mindful of Allah and you will find Him before you. If you ask, ask Allah; and if you seek help, seek help from Allah.",
            source = "سنن الترمذي عن النبي ﷺ",
            theme = "Mindfulness & Connection",
            reflection = "Keeping Allah in our everyday intentions and prayers fills our life with divine protection and clarity.",
            isAiGenerated = false
        )
    )

    fun getRandomCuratedQuote(): SpiritualQuote {
        return curatedSpiritualQuotes.random()
    }

    /**
     * Fetches a spiritually uplifting quote using Gemini API with automatic retry and model fallback.
     * @param theme Topic or spiritual focus (e.g. "Patience", "Gratitude", "Peace & Serenity", "Trust in Allah", "Mercy")
     * @param language App target language ("AR", "EN", "FR")
     */
    suspend fun fetchSpiritualQuote(
        theme: String = "Peace & Tranquility",
        language: String = "AR",
        apiKey: String = BuildConfig.GEMINI_API_KEY,
        client: OkHttpClient = httpClient,
        models: List<String> = GeminiApiHelper.defaultModels,
        retryDelayMs: Long = 500L
    ): SpiritualQuote = withContext(Dispatchers.IO) {
        if (!GeminiApiHelper.hasValidApiKey(apiKey)) {
            Log.w("SpiritualQuoteService", "Gemini API key not configured, returning curated spiritual quote.")
            return@withContext getRandomCuratedQuote().copy(theme = theme)
        }

        val prompt = """
            You are a wise spiritual Islamic scholar and writer.
            Generate a single deeply moving, spiritually-uplifting, and peaceful Islamic reflection, Quranic ayah, or authentic Hadith on the spiritual theme: "$theme".
            
            Respond STRICTLY in a valid JSON format with the following keys:
            - "arabic_text": (string) Beautiful and accurate Arabic text of the verse, hadith, or spiritual wisdom with proper diacritics.
            - "translation": (string) A clear and poetic translation in ${if (language == "FR") "French" else "English"}.
            - "source": (string) The source/author (e.g. "القرآن الكريم، سورة البقرة: 186" or "صحيح البخاري عن النبي ﷺ" or "علي بن أبي طالب").
            - "theme": (string) Short spiritual topic label (e.g. "$theme").
            - "reflection": (string) A concise 1-2 sentence uplifting insight or practical life reflection in ${if (language == "AR") "Arabic" else if (language == "FR") "French" else "English"}.

            Do NOT wrap the response in markdown code blocks like ```json ... ```. Output raw JSON text only.
        """.trimIndent()

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
                put("temperature", 0.7)
            })
        }

        try {
            val text = GeminiApiHelper.generateContentText(
                payload = jsonRequest,
                apiKey = apiKey,
                client = client,
                models = models,
                retryDelayMs = retryDelayMs,
                callerTag = "SpiritualQuoteService"
            ) ?: return@withContext getRandomCuratedQuote().copy(theme = theme)

            val quoteJson = JSONObject(text)
            val arabicText = quoteJson.optString("arabic_text", "").ifEmpty {
                quoteJson.optString("text", "")
            }
            val translation = quoteJson.optString("translation", "")
            val source = quoteJson.optString("source", "").ifEmpty {
                quoteJson.optString("author", "حكمة إسلامية")
            }
            val themeTag = quoteJson.optString("theme", theme)
            val reflection = quoteJson.optString("reflection", "")

            if (arabicText.isNotEmpty()) {
                SpiritualQuote(
                    arabicText = arabicText,
                    translation = translation,
                    source = source,
                    theme = themeTag,
                    reflection = reflection,
                    isAiGenerated = true
                )
            } else {
                getRandomCuratedQuote().copy(theme = theme)
            }
        } catch (e: Exception) {
            Log.w("SpiritualQuoteService", "Failed parsing quote, using curated fallback: ${e.message}")
            getRandomCuratedQuote().copy(theme = theme)
        }
    }
}
