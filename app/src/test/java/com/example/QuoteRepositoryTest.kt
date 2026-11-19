package com.example

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.QuoteRepository
import com.example.data.SpiritualQuote
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class QuoteRepositoryTest {

    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        database = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `fetches from Gemini fetcher once and serves cached quote from Room on subsequent reads`() = runBlocking {
        var fetchCount = 0
        val repo = QuoteRepository(
            dailyQuoteDao = database.dailyQuoteDao(),
            savedQuoteDao = database.savedQuoteDao(),
            quoteFetcher = { theme, _ ->
                fetchCount++
                SpiritualQuote(
                    arabicText = "فَإِنَّ مَعَ الْعُسْرِ يُسْرًا",
                    translation = "For indeed, with hardship will be ease.",
                    source = "القرآن الكريم، سورة الشرح: 5",
                    theme = theme,
                    reflection = "Hold onto patience; relief is already on the way.",
                    isAiGenerated = true
                )
            }
        )

        val dateKey = "2026-11-14"
        val first = repo.getOrFetchDailyQuote(date = dateKey, language = "EN")
        assertEquals(1, fetchCount)
        assertEquals("فَإِنَّ مَعَ الْعُسْرِ يُسْرًا", first.arabicText)
        assertTrue(first.isAiGenerated)

        // Second read for the same date should come directly from Room without calling Gemini again
        val second = repo.getOrFetchDailyQuote(date = dateKey, language = "EN")
        assertEquals(1, fetchCount)
        assertEquals(first.arabicText, second.arabicText)

        // Observed Flow from Room emits the persisted quote
        val observed = repo.observeDailyQuote(dateKey).first()
        assertNotNull(observed)
        assertEquals("القرآن الكريم، سورة الشرح: 5", observed?.source)
    }

    @Test
    fun `refreshDailyQuote forces new fetch and updates Room`() = runBlocking {
        var callIndex = 0
        val repo = QuoteRepository(
            dailyQuoteDao = database.dailyQuoteDao(),
            savedQuoteDao = database.savedQuoteDao(),
            quoteFetcher = { theme, _ ->
                callIndex++
                if (callIndex == 1) {
                    SpiritualQuote(
                        arabicText = "Quote One",
                        translation = "First",
                        source = "Source 1",
                        theme = theme,
                        isAiGenerated = true
                    )
                } else {
                    SpiritualQuote(
                        arabicText = "Quote Two",
                        translation = "Second",
                        source = "Source 2",
                        theme = theme,
                        isAiGenerated = true
                    )
                }
            }
        )

        val dateKey = "2026-11-14"
        repo.getOrFetchDailyQuote(date = dateKey)
        val refreshed = repo.refreshDailyQuote(date = dateKey)
        assertEquals(2, callIndex)
        assertEquals("Quote Two", refreshed.arabicText)

        val stored = repo.observeDailyQuote(dateKey).first()
        assertEquals("Quote Two", stored?.arabicText)
    }

    @Test
    fun `toggleFavorite stores and removes daily quote in saved_quotes`() = runBlocking {
        val repo = QuoteRepository(
            dailyQuoteDao = database.dailyQuoteDao(),
            savedQuoteDao = database.savedQuoteDao(),
            quoteFetcher = { theme, _ ->
                SpiritualQuote(
                    arabicText = "أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ",
                    translation = "In the remembrance of Allah do hearts find rest.",
                    source = "سورة الرعد: 28",
                    theme = theme,
                    isAiGenerated = true
                )
            }
        )

        val daily = repo.getOrFetchDailyQuote(date = "2026-11-14")
        assertFalse(repo.isQuoteSaved(daily.arabicText).first())

        val added = repo.toggleFavorite(daily)
        assertTrue(added)
        assertTrue(repo.isQuoteSaved(daily.arabicText).first())

        val removed = repo.toggleFavorite(daily)
        assertFalse(removed)
        assertFalse(repo.isQuoteSaved(daily.arabicText).first())
    }
}
