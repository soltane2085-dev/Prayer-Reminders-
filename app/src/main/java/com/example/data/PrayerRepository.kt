package com.example.data
 
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository class implementing the repository pattern to mediate
 * between Room DAO and ViewModel/UI for Prayer tracking and Saved Quotes.
 */
class PrayerRepository(
    private val prayerRecordDao: PrayerRecordDao,
    private val savedQuoteDao: SavedQuoteDao
) {
    // --- Prayer Records ---

    fun getRecordForDate(date: String): Flow<PrayerRecord?> {
        return prayerRecordDao.getRecordByDate(date)
    }

    fun getRecentRecords(limit: Int = 30): Flow<List<PrayerRecord>> {
        return prayerRecordDao.getRecentRecords(limit)
    }

    fun getAllRecords(): Flow<List<PrayerRecord>> {
        return prayerRecordDao.getAllRecords()
    }

    fun getPerfectDaysCount(): Flow<Int> {
        return prayerRecordDao.getPerfectDaysCount()
    }

    suspend fun getRecordForDateSuspend(date: String): PrayerRecord? {
        return prayerRecordDao.getRecordByDateSuspend(date)
    }

    suspend fun insertOrUpdateRecord(record: PrayerRecord) {
        prayerRecordDao.insertRecord(record.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun togglePrayer(date: String, prayerName: String): PrayerRecord {
        val existing = prayerRecordDao.getRecordByDateSuspend(date) ?: PrayerRecord(date = date)
        val updated = when (prayerName.lowercase()) {
            "fajr" -> existing.copy(fajrPrayed = !existing.fajrPrayed)
            "dhuhr" -> existing.copy(dhuhrPrayed = !existing.dhuhrPrayed)
            "asr" -> existing.copy(asrPrayed = !existing.asrPrayed)
            "maghrib" -> existing.copy(maghribPrayed = !existing.maghribPrayed)
            "isha" -> existing.copy(ishaPrayed = !existing.ishaPrayed)
            "duha" -> existing.copy(duhaPrayed = !existing.duhaPrayed)
            "witr" -> existing.copy(witrPrayed = !existing.witrPrayed)
            "qiyam" -> existing.copy(qiyamPrayed = !existing.qiyamPrayed)
            else -> existing
        }
        insertOrUpdateRecord(updated)
        return updated
    }

    suspend fun markAllPrayersForDate(date: String, markCompleted: Boolean): PrayerRecord {
        val existing = prayerRecordDao.getRecordByDateSuspend(date) ?: PrayerRecord(date = date)
        val updated = existing.copy(
            fajrPrayed = markCompleted,
            dhuhrPrayed = markCompleted,
            asrPrayed = markCompleted,
            maghribPrayed = markCompleted,
            ishaPrayed = markCompleted
        )
        insertOrUpdateRecord(updated)
        return updated
    }

    suspend fun deleteRecord(date: String) {
        prayerRecordDao.deleteRecordByDate(date)
    }

    // --- Saved Quotes ---

    fun getAllSavedQuotes(): Flow<List<SavedQuote>> {
        return savedQuoteDao.getAllSavedQuotes()
    }

    fun isQuoteSaved(text: String): Flow<Boolean> {
        return savedQuoteDao.checkQuoteExists(text)
    }

    suspend fun saveQuote(text: String, author: String) {
        savedQuoteDao.insertQuote(SavedQuote(text = text, author = author))
    }

    suspend fun deleteQuoteById(id: Int) {
        savedQuoteDao.deleteQuoteById(id)
    }

    suspend fun deleteQuoteByText(text: String) {
        savedQuoteDao.deleteQuoteByText(text)
    }
}
