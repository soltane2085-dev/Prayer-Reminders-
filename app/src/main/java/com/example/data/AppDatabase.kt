package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "saved_quotes")
data class SavedQuote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val author: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "prayer_records")
data class PrayerRecord(
    @PrimaryKey val date: String, // format "YYYY-MM-DD"
    val fajrPrayed: Boolean = false,
    val dhuhrPrayed: Boolean = false,
    val asrPrayed: Boolean = false,
    val maghribPrayed: Boolean = false,
    val ishaPrayed: Boolean = false,
    val duhaPrayed: Boolean = false,
    val witrPrayed: Boolean = false,
    val qiyamPrayed: Boolean = false,
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
) {
    val completedCount: Int
        get() = (if (fajrPrayed) 1 else 0) +
                (if (dhuhrPrayed) 1 else 0) +
                (if (asrPrayed) 1 else 0) +
                (if (maghribPrayed) 1 else 0) +
                (if (ishaPrayed) 1 else 0)

    val isAllCompleted: Boolean
        get() = completedCount == 5

    val completionRatio: Float
        get() = completedCount / 5f
}

@Dao
interface SavedQuoteDao {
    @Query("SELECT * FROM saved_quotes ORDER BY timestamp DESC")
    fun getAllSavedQuotes(): Flow<List<SavedQuote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuote(quote: SavedQuote)

    @Query("DELETE FROM saved_quotes WHERE id = :id")
    suspend fun deleteQuoteById(id: Int)

    @Query("DELETE FROM saved_quotes WHERE text = :text")
    suspend fun deleteQuoteByText(text: String)

    @Query("SELECT EXISTS(SELECT 1 FROM saved_quotes WHERE text = :text LIMIT 1)")
    fun checkQuoteExists(text: String): Flow<Boolean>
}

@Dao
interface PrayerRecordDao {
    @Query("SELECT * FROM prayer_records ORDER BY date DESC LIMIT :limit")
    fun getRecentRecords(limit: Int = 30): Flow<List<PrayerRecord>>

    @Query("SELECT * FROM prayer_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<PrayerRecord>>

    @Query("SELECT * FROM prayer_records WHERE date = :date LIMIT 1")
    fun getRecordByDate(date: String): Flow<PrayerRecord?>

    @Query("SELECT * FROM prayer_records WHERE date = :date LIMIT 1")
    suspend fun getRecordByDateSuspend(date: String): PrayerRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: PrayerRecord)

    @Query("DELETE FROM prayer_records WHERE date = :date")
    suspend fun deleteRecordByDate(date: String)

    @Query("SELECT COUNT(*) FROM prayer_records WHERE fajrPrayed = 1 AND dhuhrPrayed = 1 AND asrPrayed = 1 AND maghribPrayed = 1 AND ishaPrayed = 1")
    fun getPerfectDaysCount(): Flow<Int>
}

@Entity(tableName = "daily_quotes")
data class DailyQuoteEntity(
    @PrimaryKey val date: String, // format "yyyy-MM-dd"
    val arabicText: String,
    val translation: String = "",
    val source: String,
    val theme: String = "Peace & Tranquility",
    val reflection: String = "",
    val isAiGenerated: Boolean = true,
    val fetchedAt: Long = System.currentTimeMillis()
)

@Dao
interface DailyQuoteDao {
    @Query("SELECT * FROM daily_quotes WHERE date = :date LIMIT 1")
    fun observeQuoteForDate(date: String): Flow<DailyQuoteEntity?>

    @Query("SELECT * FROM daily_quotes WHERE date = :date LIMIT 1")
    suspend fun getQuoteForDate(date: String): DailyQuoteEntity?

    @Query("SELECT * FROM daily_quotes ORDER BY date DESC LIMIT 1")
    fun observeLatestQuote(): Flow<DailyQuoteEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyQuote(quote: DailyQuoteEntity)

    @Query("DELETE FROM daily_quotes WHERE date = :date")
    suspend fun deleteQuoteForDate(date: String)
}

@Database(
    entities = [SavedQuote::class, PrayerRecord::class, DailyQuoteEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun savedQuoteDao(): SavedQuoteDao
    abstract fun prayerRecordDao(): PrayerRecordDao
    abstract fun dailyQuoteDao(): DailyQuoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "prayer_reminders_db"
                )
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
