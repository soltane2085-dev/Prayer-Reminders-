package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Repository mediating between the Gemini API ([SpiritualQuoteService]) and the Room database
 * ([DailyQuoteDao], [SavedQuoteDao]) for caching and serving an uplifting daily quote on the main screen.
 */
class QuoteRepository(
    private val dailyQuoteDao: DailyQuoteDao,
    private val savedQuoteDao: SavedQuoteDao,
    private val quoteFetcher: suspend (theme: String, language: String) -> SpiritualQuote = { theme, language ->
        SpiritualQuoteService.fetchSpiritualQuote(theme = theme, language = language)
    }
) {
    companion object {
        private val dailyThemes = listOf(
            "Peace & Tranquility",
            "Gratitude & Contentment",
            "Patience & Hope",
            "Trust in Allah (Tawakkul)",
            "Divine Mercy & Forgiveness",
            "Inner Peace & Serenity",
            "Mindfulness & Connection"
        )

        fun todayDateKey(): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        }
    }

    /**
     * Observes the cached daily quote for [date] from Room.
     */
    fun observeDailyQuote(date: String = todayDateKey()): Flow<DailyQuoteEntity?> {
        return dailyQuoteDao.observeQuoteForDate(date)
    }

    /**
     * Observes the most recently stored daily quote in Room.
     */
    fun observeLatestDailyQuote(): Flow<DailyQuoteEntity?> {
        return dailyQuoteDao.observeLatestQuote()
    }

    /**
     * Observes whether a quote's text is bookmarked in the user's saved quotes table.
     */
    fun isQuoteSaved(text: String): Flow<Boolean> {
        return savedQuoteDao.checkQuoteExists(text)
    }

    /**
     * Returns the Room-cached quote for [date] if available; otherwise fetches a fresh
     * uplifting quote from the Gemini API, stores it in Room, and returns it.
     */
    suspend fun getOrFetchDailyQuote(
        date: String = todayDateKey(),
        language: String = "AR",
        forceRefresh: Boolean = false
    ): DailyQuoteEntity {
        val existing = dailyQuoteDao.getQuoteForDate(date)
        if (!forceRefresh && existing != null && existing.arabicText.isNotBlank()) {
            return existing
        }

        val theme = pickTheme(forceRefresh)
        var fetched = quoteFetcher(theme, language)

        // When manually refreshing offline or without an API key, rotate away from the current quote
        if (forceRefresh && existing != null && fetched.arabicText == existing.arabicText) {
            val alternatives = SpiritualQuoteService.curatedSpiritualQuotes
                .filter { it.arabicText != existing.arabicText }
            if (alternatives.isNotEmpty()) {
                fetched = alternatives.random()
            }
        }

        val entity = DailyQuoteEntity(
            date = date,
            arabicText = fetched.arabicText,
            translation = fetched.translation,
            source = fetched.source,
            theme = fetched.theme,
            reflection = fetched.reflection,
            isAiGenerated = fetched.isAiGenerated,
            fetchedAt = System.currentTimeMillis()
        )
        dailyQuoteDao.upsertDailyQuote(entity)
        return entity
    }

    /**
     * Forces a fresh fetch from Gemini API and updates today's Room entry.
     */
    suspend fun refreshDailyQuote(
        date: String = todayDateKey(),
        language: String = "AR"
    ): DailyQuoteEntity {
        return getOrFetchDailyQuote(date = date, language = language, forceRefresh = true)
    }

    /**
     * Toggles a [DailyQuoteEntity] in the user's saved favorites table.
     */
    suspend fun toggleFavorite(quote: DailyQuoteEntity): Boolean {
        val currentSaved = savedQuoteDao.getAllSavedQuotes().firstOrNull().orEmpty()
        val alreadySaved = currentSaved.any { it.text == quote.arabicText }
        if (alreadySaved) {
            savedQuoteDao.deleteQuoteByText(quote.arabicText)
            return false
        } else {
            savedQuoteDao.insertQuote(
                SavedQuote(
                    text = quote.arabicText,
                    author = quote.source
                )
            )
            return true
        }
    }

    private fun pickTheme(forceRefresh: Boolean): String {
        return if (forceRefresh) {
            dailyThemes.random()
        } else {
            val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            dailyThemes[dayOfYear % dailyThemes.size]
        }
    }
}
