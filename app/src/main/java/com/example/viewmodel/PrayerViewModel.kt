package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calculator.AsrJuristicMethod
import com.example.calculator.CalculationMethod
import com.example.calculator.CityPreset
import com.example.calculator.HijriCalendarHelper
import com.example.calculator.HijriDate
import com.example.calculator.PrayerTimeCalculator
import com.example.data.AppDatabase
import com.example.data.DailyQuoteEntity
import com.example.data.GeminiQuoteService
import com.example.data.PrayerRecord
import com.example.data.QuoteProvider
import com.example.data.QuoteRepository
import com.example.data.SavedQuote
import com.example.data.StaticQuote
import com.example.data.GeoLocationResult
import com.example.data.LocationFetchResult
import com.example.data.LocationRepository
import com.example.data.MosqueLocatorRepository
import com.example.data.MosqueSearchOutcome
import com.example.data.NearbyMosque
import com.example.receiver.PrayerAlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PrayerViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs: SharedPreferences = application.getSharedPreferences("prayer_settings", Context.MODE_PRIVATE)
    private val database = AppDatabase.getDatabase(application)
    private val repository = com.example.data.PrayerRepository(
        database.prayerRecordDao(),
        database.savedQuoteDao()
    )
    private val quoteRepository = QuoteRepository(
        database.dailyQuoteDao(),
        database.savedQuoteDao()
    )
    private val locationRepository = LocationRepository(application)
    private val mosqueLocatorRepository = MosqueLocatorRepository(application)
    private val calculator = PrayerTimeCalculator()

    private val defaultLangCode: String = run {
        val sysLang = Locale.getDefault().language
        when {
            sysLang.startsWith("ar") -> "AR"
            sysLang.startsWith("fr") -> "FR"
            sysLang.startsWith("en") -> "EN"
            else -> "AR"
        }
    }

    private val _appLang = MutableStateFlow(prefs.getString("app_language", defaultLangCode) ?: defaultLangCode)
    val appLang = _appLang.asStateFlow()

    fun updateAppLanguage(lang: String) {
        _appLang.value = lang
        prefs.edit().putString("app_language", lang).apply()
    }

    private val _themeMode = MutableStateFlow(prefs.getString("app_theme", "system") ?: "system")
    val themeMode = _themeMode.asStateFlow()

    fun updateThemeMode(theme: String) {
        _themeMode.value = theme
        prefs.edit().putString("app_theme", theme).apply()
    }

    private val _showAboutDialog = MutableStateFlow(false)
    val showAboutDialog = _showAboutDialog.asStateFlow()

    fun setShowAboutDialog(show: Boolean) {
        _showAboutDialog.value = show
    }

    // --- State Observables ---
    private val _cityPresetIdx = MutableStateFlow(prefs.getInt("city_preset_idx", 0))
    val cityPresetIdx = _cityPresetIdx.asStateFlow()

    private val _customLocEnabled = MutableStateFlow(prefs.getBoolean("custom_loc_enabled", false))
    val customLocEnabled = _customLocEnabled.asStateFlow()

    private val _customLat = MutableStateFlow(prefs.getFloat("custom_lat", 21.3891f))
    val customLat = _customLat.asStateFlow()

    private val _customLon = MutableStateFlow(prefs.getFloat("custom_lon", 39.8579f))
    val customLon = _customLon.asStateFlow()

    private val _customTz = MutableStateFlow(prefs.getFloat("custom_tz", 3.0f))
    val customTz = _customTz.asStateFlow()

    private val _customCityName = MutableStateFlow(prefs.getString("custom_city_name", "Custom Location") ?: "Custom Location")
    val customCityName = _customCityName.asStateFlow()

    private val _calcMethod = MutableStateFlow(
        try { CalculationMethod.valueOf(prefs.getString("calc_method", CalculationMethod.MWL.name) ?: CalculationMethod.MWL.name) }
        catch (e: Exception) { CalculationMethod.MWL }
    )
    val calcMethod = _calcMethod.asStateFlow()

    private val _asrMethod = MutableStateFlow(
        try { AsrJuristicMethod.valueOf(prefs.getString("asr_method", AsrJuristicMethod.STANDARD.name) ?: AsrJuristicMethod.STANDARD.name) }
        catch (e: Exception) { AsrJuristicMethod.STANDARD }
    )
    val asrMethod = _asrMethod.asStateFlow()

    // Notification Toggles
    private val _notifyFajr = MutableStateFlow(prefs.getBoolean("notify_Fajr", true))
    val notifyFajr = _notifyFajr.asStateFlow()

    private val _notifyDhuhr = MutableStateFlow(prefs.getBoolean("notify_Dhuhr", true))
    val notifyDhuhr = _notifyDhuhr.asStateFlow()

    private val _notifyAsr = MutableStateFlow(prefs.getBoolean("notify_Asr", true))
    val notifyAsr = _notifyAsr.asStateFlow()

    private val _notifyMaghrib = MutableStateFlow(prefs.getBoolean("notify_Maghrib", true))
    val notifyMaghrib = _notifyMaghrib.asStateFlow()

    private val _notifyIsha = MutableStateFlow(prefs.getBoolean("notify_Isha", true))
    val notifyIsha = _notifyIsha.asStateFlow()

    // Alert Sound Type Preference
    private val _alertSoundType = MutableStateFlow(prefs.getString("alert_sound_type", "ADHAN_SHORT") ?: "ADHAN_SHORT")
    val alertSoundType = _alertSoundType.asStateFlow()

    // Offline pre-download states for Adhan sounds
    private val _isFullAdhanDownloaded = MutableStateFlow(false)
    val isFullAdhanDownloaded = _isFullAdhanDownloaded.asStateFlow()

    private val _isShortAdhanDownloaded = MutableStateFlow(false)
    val isShortAdhanDownloaded = _isShortAdhanDownloaded.asStateFlow()

    private val _isDownloadingFullAdhan = MutableStateFlow(false)
    val isDownloadingFullAdhan = _isDownloadingFullAdhan.asStateFlow()

    private val _isDownloadingShortAdhan = MutableStateFlow(false)
    val isDownloadingShortAdhan = _isDownloadingShortAdhan.asStateFlow()

    private val _customAdhanFileName = MutableStateFlow(prefs.getString("custom_adhan_file_name", null))
    val customAdhanFileName = _customAdhanFileName.asStateFlow()

    // Dynamic Calculations
    private val _prayerTimes = MutableStateFlow<Map<String, String>>(emptyMap())
    val prayerTimes = _prayerTimes.asStateFlow()

    private val _nextPrayerName = MutableStateFlow("")
    val nextPrayerName = _nextPrayerName.asStateFlow()

    private val _nextPrayerCountdown = MutableStateFlow("00:00:00")
    val nextPrayerCountdown = _nextPrayerCountdown.asStateFlow()

    // Quotes State
    private val _todayQuote = MutableStateFlow(QuoteProvider.getRandomQuote())
    val todayQuote = _todayQuote.asStateFlow()

    // Spiritually-uplifting quote powered by Gemini API
    private val _spiritualQuote = MutableStateFlow(com.example.data.SpiritualQuoteService.getRandomCuratedQuote())
    val spiritualQuote = _spiritualQuote.asStateFlow()

    private val _isSpiritualQuoteLoading = MutableStateFlow(false)
    val isSpiritualQuoteLoading = _isSpiritualQuoteLoading.asStateFlow()

    private val _spiritualQuoteError = MutableStateFlow("")
    val spiritualQuoteError = _spiritualQuoteError.asStateFlow()

    private val _selectedSpiritualTheme = MutableStateFlow("السكينة والطمأنينة")
    val selectedSpiritualTheme = _selectedSpiritualTheme.asStateFlow()

    // Habit Records & Daily Date Key
    val todayDateStr: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    // Daily Quote backed by Room + Gemini API via QuoteRepository
    val dailyQuote: StateFlow<DailyQuoteEntity?> = quoteRepository.observeDailyQuote(todayDateStr)
        .onEach { entity ->
            if (entity != null) {
                _todayQuote.value = StaticQuote(text = entity.arabicText, author = entity.source)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isDailyQuoteLoading = MutableStateFlow(false)
    val isDailyQuoteLoading = _isDailyQuoteLoading.asStateFlow()

    val isDailyQuoteSaved: StateFlow<Boolean> = dailyQuote.flatMapLatest { entity ->
        val text = entity?.arabicText ?: _todayQuote.value.text
        quoteRepository.isQuoteSaved(text)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val savedQuotes: StateFlow<List<SavedQuote>> = repository.getAllSavedQuotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isTodayQuoteSaved: StateFlow<Boolean> = _todayQuote.flatMapLatest { quote ->
        repository.isQuoteSaved(quote.text)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isSpiritualQuoteSaved: StateFlow<Boolean> = _spiritualQuote.flatMapLatest { quote ->
        repository.isQuoteSaved(quote.arabicText)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _selectedTrackingDate = MutableStateFlow(todayDateStr)
    val selectedTrackingDate = _selectedTrackingDate.asStateFlow()

    fun setSelectedTrackingDate(dateStr: String) {
        _selectedTrackingDate.value = dateStr
    }

    val todayRecord: StateFlow<PrayerRecord> = repository.getRecordForDate(todayDateStr)
        .map { it ?: PrayerRecord(todayDateStr) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PrayerRecord(todayDateStr))

    val selectedDateRecord: StateFlow<PrayerRecord> = _selectedTrackingDate.flatMapLatest { date ->
        repository.getRecordForDate(date).map { it ?: PrayerRecord(date) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PrayerRecord(todayDateStr))

    val recentRecords: StateFlow<List<PrayerRecord>> = repository.getRecentRecords(30)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val perfectDaysCount: StateFlow<Int> = repository.getPerfectDaysCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // AI custom fields
    private val _aiTopicInput = MutableStateFlow("")
    val aiTopicInput = _aiTopicInput.asStateFlow()

    private val _aiIsGenerating = MutableStateFlow(false)
    val aiIsGenerating = _aiIsGenerating.asStateFlow()

    private val _aiErrorMsg = MutableStateFlow("")
    val aiErrorMsg = _aiErrorMsg.asStateFlow()

    private val _generatedQuote = MutableStateFlow<StaticQuote?>(null)
    val generatedQuote = _generatedQuote.asStateFlow()

    val isGeneratedQuoteSaved: StateFlow<Boolean> = _generatedQuote.flatMapLatest { quote ->
        if (quote == null) flowOf(false)
        else repository.isQuoteSaved(quote.text)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Location Search custom fields
    private val _locationSearchQuery = MutableStateFlow("")
    val locationSearchQuery = _locationSearchQuery.asStateFlow()

    private val _locationSearchResult = MutableStateFlow<GeoLocationResult?>(null)
    val locationSearchResult = _locationSearchResult.asStateFlow()

    private val _locationIsSearching = MutableStateFlow(false)
    val locationIsSearching = _locationIsSearching.asStateFlow()

    private val _locationSearchErrorMsg = MutableStateFlow("")
    val locationSearchErrorMsg = _locationSearchErrorMsg.asStateFlow()

    // Mosque Search state fields
    private val _mosqueSearchQuery = MutableStateFlow("")
    val mosqueSearchQuery = _mosqueSearchQuery.asStateFlow()

    private val _mosqueSearchResult = MutableStateFlow<com.example.data.MosqueSearchResult?>(null)
    val mosqueSearchResult = _mosqueSearchResult.asStateFlow()

    private val _mosqueIsSearching = MutableStateFlow(false)
    val mosqueIsSearching = _mosqueIsSearching.asStateFlow()

    private val _mosqueSearchErrorMsg = MutableStateFlow("")
    val mosqueSearchErrorMsg = _mosqueSearchErrorMsg.asStateFlow()

    // Linked Mosque persistent states
    private val _linkedMosqueEnabled = MutableStateFlow(prefs.getBoolean("linked_mosque_enabled", false))
    val linkedMosqueEnabled = _linkedMosqueEnabled.asStateFlow()

    private val _linkedMosqueName = MutableStateFlow(prefs.getString("linked_mosque_name", "") ?: "")
    val linkedMosqueName = _linkedMosqueName.asStateFlow()

    private val _linkedMosqueSlug = MutableStateFlow(prefs.getString("linked_mosque_slug", "") ?: "")
    val linkedMosqueSlug = _linkedMosqueSlug.asStateFlow()

    private val _linkedMosqueAddress = MutableStateFlow(prefs.getString("linked_mosque_address", "") ?: "")
    val linkedMosqueAddress = _linkedMosqueAddress.asStateFlow()

    private val _linkedMosqueSource = MutableStateFlow(prefs.getString("linked_mosque_source", "MAWAQIT") ?: "MAWAQIT")
    val linkedMosqueSource = _linkedMosqueSource.asStateFlow()

    private val _iqamahOffsetFajr = MutableStateFlow(prefs.getInt("iqamah_offset_Fajr", 20))
    val iqamahOffsetFajr = _iqamahOffsetFajr.asStateFlow()

    private val _iqamahOffsetDhuhr = MutableStateFlow(prefs.getInt("iqamah_offset_Dhuhr", 15))
    val iqamahOffsetDhuhr = _iqamahOffsetDhuhr.asStateFlow()

    private val _iqamahOffsetAsr = MutableStateFlow(prefs.getInt("iqamah_offset_Asr", 15))
    val iqamahOffsetAsr = _iqamahOffsetAsr.asStateFlow()

    private val _iqamahOffsetMaghrib = MutableStateFlow(prefs.getInt("iqamah_offset_Maghrib", 10))
    val iqamahOffsetMaghrib = _iqamahOffsetMaghrib.asStateFlow()

    private val _iqamahOffsetIsha = MutableStateFlow(prefs.getInt("iqamah_offset_Isha", 15))
    val iqamahOffsetIsha = _iqamahOffsetIsha.asStateFlow()

    private val _hijriOffset = MutableStateFlow(prefs.getInt("hijri_offset", 0))
    val hijriOffset = _hijriOffset.asStateFlow()

    val currentHijriDate: StateFlow<HijriDate> = _hijriOffset.map { offset ->
        val cal = Calendar.getInstance()
        HijriCalendarHelper.convertGregorianToHijri(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
            offset
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        HijriCalendarHelper.convertGregorianToHijri(
            Calendar.getInstance().get(Calendar.YEAR),
            Calendar.getInstance().get(Calendar.MONTH) + 1,
            Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
            prefs.getInt("hijri_offset", 0)
        )
    )

    private val _showDateConverterDialog = MutableStateFlow(false)
    val showDateConverterDialog = _showDateConverterDialog.asStateFlow()

    fun setShowDateConverterDialog(show: Boolean) {
        _showDateConverterDialog.value = show
    }

    val iqamahTimes: StateFlow<Map<String, String>> = combine(
        _prayerTimes,
        _linkedMosqueEnabled,
        _iqamahOffsetFajr,
        _iqamahOffsetDhuhr,
        _iqamahOffsetAsr,
        _iqamahOffsetMaghrib,
        _iqamahOffsetIsha
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val times = args[0] as Map<String, String>
        val enabled = args[1] as Boolean
        if (!enabled || times.isEmpty()) return@combine emptyMap<String, String>()
        val iqamahs = mutableMapOf<String, String>()
        val offsets = mapOf(
            "Fajr" to (args[2] as Int),
            "Dhuhr" to (args[3] as Int),
            "Asr" to (args[4] as Int),
            "Maghrib" to (args[5] as Int),
            "Isha" to (args[6] as Int)
        )
        times.forEach { (prayerName, adhanStr) ->
            val offset = offsets[prayerName]
            if (offset != null) {
                val parts = adhanStr.split(":")
                if (parts.size == 2) {
                    try {
                        val h = parts[0].toInt()
                        val mn = parts[1].toInt()
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, h)
                            set(Calendar.MINUTE, mn)
                            add(Calendar.MINUTE, offset)
                        }
                        val formatted = String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
                        iqamahs[prayerName] = formatted
                    } catch (e: Exception) {
                        // ignore parsing/format errors
                    }
                }
            }
        }
        iqamahs
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        recalculateTimes()
        // Fetch or load today's uplifting quote from Room + Gemini API
        loadDailyQuote(forceRefresh = false)
        // Proactively pre-download and monitor the alert sounds for reliable offline use
        checkAndDownloadOfflineSounds()
        // Start live countdown timer updates
        viewModelScope.launch {
            while (true) {
                updateCountdown()
                delay(1000)
            }
        }
    }

    // --- Recalculation logic ---
    fun recalculateTimes() {
        val now = Calendar.getInstance()
        val lat: Double
        val lon: Double
        val tz: Double

        if (_customLocEnabled.value) {
            lat = _customLat.value.toDouble()
            lon = _customLon.value.toDouble()
            tz = _customTz.value.toDouble()
        } else {
            val preset = CityPreset.presets.getOrElse(_cityPresetIdx.value) { CityPreset.presets[0] }
            lat = preset.latitude
            lon = preset.longitude
            tz = preset.timezone
        }

        val times = calculator.calculateTimes(
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH) + 1,
            now.get(Calendar.DAY_OF_MONTH),
            lat, lon, tz,
            _calcMethod.value,
            _asrMethod.value
        )
        _prayerTimes.value = times

        // Make sure to reschedule alarms based on any settings changed!
        viewModelScope.launch(Dispatchers.IO) {
            PrayerAlarmScheduler.scheduleNextAlarm(getApplication())
        }
    }

    private fun updateCountdown() {
        val times = _prayerTimes.value
        if (times.isEmpty()) return

        val lat: Double
        val lon: Double
        val tz: Double

        if (_customLocEnabled.value) {
            lat = _customLat.value.toDouble()
            lon = _customLon.value.toDouble()
            tz = _customTz.value.toDouble()
        } else {
            val preset = CityPreset.presets.getOrElse(_cityPresetIdx.value) { CityPreset.presets[0] }
            lat = preset.latitude
            lon = preset.longitude
            tz = preset.timezone
        }

        val now = Calendar.getInstance()
        val todayCalendar = Calendar.getInstance()

        // Assemble candidates for upcoming prayers
        val candidates = mutableListOf<Pair<String, Calendar>>()
        val keys = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")

        // Today's candidates
        keys.forEach { name ->
            times[name]?.let { timeStr ->
                val parts = timeStr.split(":")
                val h = parts[0].toInt()
                val m = parts[1].toInt()
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, h)
                    set(Calendar.MINUTE, m)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (cal.after(now)) {
                    candidates.add(Pair(name, cal))
                }
            }
        }

        // If today's prayers are empty (all passed), look at tomorrow's Fajr
        if (candidates.isEmpty()) {
            val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
            val timesTomorrow = calculator.calculateTimes(
                tomorrow.get(Calendar.YEAR),
                tomorrow.get(Calendar.MONTH) + 1,
                tomorrow.get(Calendar.DAY_OF_MONTH),
                lat, lon, tz,
                _calcMethod.value,
                _asrMethod.value
            )
            timesTomorrow["Fajr"]?.let { timeStr ->
                val parts = timeStr.split(":")
                val h = parts[0].toInt()
                val m = parts[1].toInt()
                val cal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, h)
                    set(Calendar.MINUTE, m)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                candidates.add(Pair("Fajr (Tomorrow)", cal))
            }
        }

        candidates.sortBy { it.second.timeInMillis }
        val next = candidates.firstOrNull()

        if (next != null) {
            _nextPrayerName.value = next.first
            val diffMs = next.second.timeInMillis - now.timeInMillis
            val diffSec = maxOf(0, diffMs / 1000)
            val hours = diffSec / 3600
            val minutes = (diffSec % 3600) / 60
            val seconds = diffSec % 60
            _nextPrayerCountdown.value = String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            _nextPrayerName.value = "N/A"
            _nextPrayerCountdown.value = "00:00:00"
        }
    }

    // --- Action Handlers ---

    fun selectPresetCity(idx: Int) {
        _cityPresetIdx.value = idx
        prefs.edit().putInt("city_preset_idx", idx).apply()
        _customLocEnabled.value = false
        prefs.edit().putBoolean("custom_loc_enabled", false).apply()
        recalculateTimes()
    }

    fun enableCustomLocation(lat: Double, lon: Double, tz: Double, name: String) {
        _customLat.value = lat.toFloat()
        _customLon.value = lon.toFloat()
        _customTz.value = tz.toFloat()
        _customCityName.value = name

        prefs.edit().apply {
            putBoolean("custom_loc_enabled", true)
            putFloat("custom_lat", lat.toFloat())
            putFloat("custom_lon", lon.toFloat())
            putFloat("custom_tz", tz.toFloat())
            putString("custom_city_name", name)
        }.apply()

        _customLocEnabled.value = true
        recalculateTimes()
    }

    fun updateCalculationMethod(method: CalculationMethod) {
        _calcMethod.value = method
        prefs.edit().putString("calc_method", method.name).apply()
        recalculateTimes()
    }

    fun updateAsrMethod(method: AsrJuristicMethod) {
        _asrMethod.value = method
        prefs.edit().putString("asr_method", method.name).apply()
        recalculateTimes()
    }

    fun toggleNotification(prayerName: String) {
        val key = "notify_$prayerName"
        val curr = prefs.getBoolean(key, true)
        val newValue = !curr
        prefs.edit().putBoolean(key, newValue).apply()

        when (prayerName) {
            "Fajr" -> _notifyFajr.value = newValue
            "Dhuhr" -> _notifyDhuhr.value = newValue
            "Asr" -> _notifyAsr.value = newValue
            "Maghrib" -> _notifyMaghrib.value = newValue
            "Isha" -> _notifyIsha.value = newValue
        }

        // Schedule next alarm immediately reflecting changed alerts
        viewModelScope.launch(Dispatchers.IO) {
            PrayerAlarmScheduler.scheduleNextAlarm(getApplication())
        }
    }

    // --- Checking Prayers (Habits) ---
    fun togglePrayerChecked(prayerName: String, targetDate: String = todayDateStr) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.togglePrayer(targetDate, prayerName)
        }
    }

    fun markAllPrayers(targetDate: String = todayDateStr, completed: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.markAllPrayersForDate(targetDate, completed)
        }
    }

    // --- Star/Favorite Quotes ---
    fun toggleFavoriteQuote(quote: StaticQuote) {
        viewModelScope.launch(Dispatchers.IO) {
            val savedQuotesList = savedQuotes.value
            val matches = savedQuotesList.filter { it.text == quote.text }
            if (matches.isNotEmpty()) {
                // Remove favorite
                repository.deleteQuoteByText(quote.text)
            } else {
                // Add favorite
                repository.saveQuote(text = quote.text, author = quote.author)
            }
        }
    }

    fun removeStarredQuoteById(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteQuoteById(id)
        }
    }

    // --- Daily Quotes Operations (Room + Gemini API via QuoteRepository) ---
    fun loadDailyQuote(forceRefresh: Boolean = false) {
        if (_isDailyQuoteLoading.value) return
        _isDailyQuoteLoading.value = true
        viewModelScope.launch {
            try {
                val entity = quoteRepository.getOrFetchDailyQuote(
                    date = todayDateStr,
                    language = _appLang.value,
                    forceRefresh = forceRefresh
                )
                _todayQuote.value = StaticQuote(text = entity.arabicText, author = entity.source)
            } catch (e: Exception) {
                Log.w("PrayerViewModel", "Failed loading daily quote: ${e.message}")
            } finally {
                _isDailyQuoteLoading.value = false
            }
        }
    }

    fun rollNewTodayQuote() {
        loadDailyQuote(forceRefresh = true)
    }

    fun toggleFavoriteDailyQuote() {
        val currentDaily = dailyQuote.value
        if (currentDaily != null) {
            viewModelScope.launch(Dispatchers.IO) {
                quoteRepository.toggleFavorite(currentDaily)
            }
        } else {
            toggleFavoriteQuote(_todayQuote.value)
        }
    }

    // --- Gemini Spiritually-Uplifting Quotes Operations ---
    fun selectSpiritualTheme(theme: String) {
        _selectedSpiritualTheme.value = theme
        fetchSpiritualQuote(theme)
    }

    fun fetchSpiritualQuote(theme: String? = null) {
        val activeTheme = theme ?: _selectedSpiritualTheme.value
        val lang = _appLang.value
        _isSpiritualQuoteLoading.value = true
        _spiritualQuoteError.value = ""

        viewModelScope.launch {
            try {
                val quote = com.example.data.SpiritualQuoteService.fetchSpiritualQuote(
                    theme = activeTheme,
                    language = lang
                )
                _spiritualQuote.value = quote
            } catch (e: Exception) {
                _spiritualQuoteError.value = "Unable to fetch new quote right now. Showing daily wisdom."
            } finally {
                _isSpiritualQuoteLoading.value = false
            }
        }
    }

    fun toggleFavoriteSpiritualQuote() {
        viewModelScope.launch(Dispatchers.IO) {
            val quote = _spiritualQuote.value
            val savedQuotesList = repository.getAllSavedQuotes().firstOrNull() ?: emptyList()
            val matches = savedQuotesList.filter { it.text == quote.arabicText }
            if (matches.isNotEmpty()) {
                repository.deleteQuoteByText(quote.arabicText)
            } else {
                repository.saveQuote(text = quote.arabicText, author = quote.source)
            }
        }
    }

    fun updateTopicInput(input: String) {
        _aiTopicInput.value = input
    }

    // Gemini API Query
    fun generateAIQuote() {
        val topic = _aiTopicInput.value.trim()
        if (topic.isEmpty()) {
            _aiErrorMsg.value = "Please enter a topic to inspire from."
            return
        }

        _aiIsGenerating.value = true
        _aiErrorMsg.value = ""

        viewModelScope.launch {
            val result = GeminiQuoteService.generateSpiritualQuote(topic)
            _aiIsGenerating.value = false
            if (result != null) {
                _generatedQuote.value = StaticQuote(text = result.first, author = result.second)
            } else {
                _aiErrorMsg.value = "Failed to run Gemini quote generator. Check your internet connection or API Key. In the meantime, enjoy local quotes!"
            }
        }
    }

    // Trigger test notification
    fun triggerTestNotification() {
        PrayerAlarmScheduler.scheduleTestNotification(getApplication())
    }

    fun updateLocationSearchQuery(query: String) {
        _locationSearchQuery.value = query
    }

    fun searchLocation() {
        val query = _locationSearchQuery.value.trim()
        if (query.isEmpty()) {
            _locationSearchErrorMsg.value = "Please enter a place or city name."
            return
        }

        _locationIsSearching.value = true
        _locationSearchErrorMsg.value = ""
        _locationSearchResult.value = null

        viewModelScope.launch {
            val result = GeminiQuoteService.searchLocation(query)
            _locationIsSearching.value = false
            if (result != null) {
                if (result.success) {
                    _locationSearchResult.value = result
                } else {
                    _locationSearchErrorMsg.value = result.errorMessage.ifEmpty { "Location not found." }
                }
            } else {
                _locationSearchErrorMsg.value = "Failed to call Gemini API. Verify your Internet connection or API Key."
            }
        }
    }

    fun clearLocationSearch() {
        _locationSearchQuery.value = ""
        _locationSearchResult.value = null
        _locationSearchErrorMsg.value = ""
    }

    fun updateMosqueSearchQuery(query: String) {
        _mosqueSearchQuery.value = query
    }

    fun searchMosque() {
        val query = _mosqueSearchQuery.value.trim()
        if (query.isEmpty()) {
            _mosqueSearchErrorMsg.value = "يرجى إدخال اسم المسجد أو المدينة."
            return
        }

        _mosqueIsSearching.value = true
        _mosqueSearchErrorMsg.value = ""
        _mosqueSearchResult.value = null

        viewModelScope.launch {
            val result = GeminiQuoteService.searchMosque(query)
            _mosqueIsSearching.value = false
            if (result != null) {
                if (result.success) {
                    _mosqueSearchResult.value = result
                } else {
                    _mosqueSearchErrorMsg.value = result.errorMessage.ifEmpty { "لم يتم العثور على المسجد." }
                }
            } else {
                _mosqueSearchErrorMsg.value = "فشل الاتصال بـ Gemini API. تحقق من اتصالك بالإنترنت."
            }
        }
    }

    fun clearMosqueSearch() {
        _mosqueSearchQuery.value = ""
        _mosqueSearchResult.value = null
        _mosqueSearchErrorMsg.value = ""
    }

    fun linkMosque(mosque: com.example.data.MosqueSearchResult) {
        _linkedMosqueEnabled.value = true
        _linkedMosqueName.value = mosque.name
        _linkedMosqueSlug.value = mosque.mosqueSlug
        _linkedMosqueAddress.value = mosque.address
        _linkedMosqueSource.value = mosque.sourcePlatform

        _iqamahOffsetFajr.value = mosque.fajrOffset
        _iqamahOffsetDhuhr.value = mosque.dhuhrOffset
        _iqamahOffsetAsr.value = mosque.asrOffset
        _iqamahOffsetMaghrib.value = mosque.maghribOffset
        _iqamahOffsetIsha.value = mosque.ishaOffset

        prefs.edit().apply {
            putBoolean("linked_mosque_enabled", true)
            putString("linked_mosque_name", mosque.name)
            putString("linked_mosque_slug", mosque.mosqueSlug)
            putString("linked_mosque_address", mosque.address)
            putString("linked_mosque_source", mosque.sourcePlatform)
            putInt("iqamah_offset_Fajr", mosque.fajrOffset)
            putInt("iqamah_offset_Dhuhr", mosque.dhuhrOffset)
            putInt("iqamah_offset_Asr", mosque.asrOffset)
            putInt("iqamah_offset_Maghrib", mosque.maghribOffset)
            putInt("iqamah_offset_Isha", mosque.ishaOffset)
            apply()
        }
        _prayerTimes.value = _prayerTimes.value.toMap()
    }

    fun unlinkMosque() {
        _linkedMosqueEnabled.value = false
        _linkedMosqueName.value = ""
        _linkedMosqueSlug.value = ""
        _linkedMosqueAddress.value = ""

        prefs.edit().apply {
            putBoolean("linked_mosque_enabled", false)
            putString("linked_mosque_name", "")
            putString("linked_mosque_slug", "")
            putString("linked_mosque_address", "")
            apply()
        }
        _prayerTimes.value = _prayerTimes.value.toMap()
    }

    fun updateIqamahOffset(prayerName: String, minutes: Int) {
        val bounded = minutes.coerceIn(0, 60)
        when (prayerName) {
            "Fajr" -> {
                _iqamahOffsetFajr.value = bounded
                prefs.edit().putInt("iqamah_offset_Fajr", bounded).apply()
            }
            "Dhuhr" -> {
                _iqamahOffsetDhuhr.value = bounded
                prefs.edit().putInt("iqamah_offset_Dhuhr", bounded).apply()
            }
            "Asr" -> {
                _iqamahOffsetAsr.value = bounded
                prefs.edit().putInt("iqamah_offset_Asr", bounded).apply()
            }
            "Maghrib" -> {
                _iqamahOffsetMaghrib.value = bounded
                prefs.edit().putInt("iqamah_offset_Maghrib", bounded).apply()
            }
            "Isha" -> {
                _iqamahOffsetIsha.value = bounded
                prefs.edit().putInt("iqamah_offset_Isha", bounded).apply()
            }
        }
        _prayerTimes.value = _prayerTimes.value.toMap()
    }

    fun updateHijriOffset(offset: Int) {
        val bounded = offset.coerceIn(-5, 5)
        _hijriOffset.value = bounded
        prefs.edit().putInt("hijri_offset", bounded).apply()
    }

    private val _gpsLoading = MutableStateFlow(false)
    val gpsLoading = _gpsLoading.asStateFlow()

    private val _gpsError = MutableStateFlow<String?>(null)
    val gpsError = _gpsError.asStateFlow()

    private val _gpsAccuracyMeters = MutableStateFlow<Float?>(null)
    val gpsAccuracyMeters = _gpsAccuracyMeters.asStateFlow()

    fun hasLocationPermission(): Boolean = locationRepository.hasLocationPermission()

    fun detectGPSLocation(@Suppress("UNUSED_PARAMETER") context: Context = getApplication()) {
        if (_gpsLoading.value) return
        _gpsError.value = null
        _gpsLoading.value = true

        viewModelScope.launch {
            val lang = _appLang.value
            when (val result = locationRepository.getCurrentCoordinates(forceFresh = true)) {
                is LocationFetchResult.Success -> {
                    val coords = result.coordinates
                    val fallbackName = when (lang) {
                        "AR" -> "موقعي الحالي (GPS)"
                        "FR" -> "Ma position actuelle (GPS)"
                        else -> "Current Location (GPS)"
                    }
                    _gpsAccuracyMeters.value = coords.accuracyMeters
                    enableCustomLocation(
                        lat = coords.latitude,
                        lon = coords.longitude,
                        tz = coords.timezoneOffsetHours,
                        name = coords.cityName ?: fallbackName
                    )
                    _gpsError.value = null
                    _gpsLoading.value = false
                }
                LocationFetchResult.PermissionDenied -> {
                    _gpsError.value = when (lang) {
                        "AR" -> "يرجى السماح بصلاحية الوصول للموقع الجغرافي لتحديد مكانكم بدقة."
                        "FR" -> "Veuillez autoriser l'accès à la localisation pour calculer les horaires avec précision."
                        else -> "Please grant location permission to detect your coordinates accurately."
                    }
                    _gpsLoading.value = false
                }
                LocationFetchResult.LocationUnavailable -> {
                    _gpsError.value = when (lang) {
                        "AR" -> "لم نتمكن من جلب إحداثيات GPS بدقة. يرجى التأكد من تشغيل خدمة الموقع والمحاولة مرة أخرى."
                        "FR" -> "Impossible de récupérer les coordonnées GPS. Vérifiez que la localisation est activée et réessayez."
                        else -> "Unable to retrieve GPS coordinates. Please ensure Location Services are enabled and try again."
                    }
                    _gpsLoading.value = false
                }
                is LocationFetchResult.Error -> {
                    _gpsError.value = when (lang) {
                        "AR" -> "فشل في جلب الموقع الجغرافي: ${result.message}"
                        "FR" -> "Échec de la détection de la position : ${result.message}"
                        else -> "Failed to detect location: ${result.message}"
                    }
                    _gpsLoading.value = false
                }
            }
        }
    }

    // --- Mosque Locator (Google Maps + Places API + GPS) ---
    private val _showMosqueLocator = MutableStateFlow(false)
    val showMosqueLocator = _showMosqueLocator.asStateFlow()

    private val _nearbyMosques = MutableStateFlow<List<NearbyMosque>>(emptyList())
    val nearbyMosques = _nearbyMosques.asStateFlow()

    private val _selectedMosque = MutableStateFlow<NearbyMosque?>(null)
    val selectedMosque = _selectedMosque.asStateFlow()

    private val _isLoadingMosques = MutableStateFlow(false)
    val isLoadingMosques = _isLoadingMosques.asStateFlow()

    private val _mosqueLocatorError = MutableStateFlow<String?>(null)
    val mosqueLocatorError = _mosqueLocatorError.asStateFlow()

    private val _mosqueMissingApiKey = MutableStateFlow(!mosqueLocatorRepository.hasValidApiKey())
    val mosqueMissingApiKey = _mosqueMissingApiKey.asStateFlow()

    private val _mosqueSearchRadiusMeters = MutableStateFlow(5000.0)
    val mosqueSearchRadiusMeters = _mosqueSearchRadiusMeters.asStateFlow()

    private val _mosqueCenterCoordinates = MutableStateFlow(getActiveCoordinates())
    val mosqueCenterCoordinates = _mosqueCenterCoordinates.asStateFlow()

    private val _isMosqueUsingGps = MutableStateFlow(false)
    val isMosqueUsingGps = _isMosqueUsingGps.asStateFlow()

    fun getActiveCoordinates(): Pair<Double, Double> {
        return if (_customLocEnabled.value) {
            _customLat.value.toDouble() to _customLon.value.toDouble()
        } else {
            val preset = CityPreset.presets.getOrElse(_cityPresetIdx.value) { CityPreset.presets[0] }
            preset.latitude to preset.longitude
        }
    }

    fun setShowMosqueLocator(show: Boolean) {
        _showMosqueLocator.value = show
        if (show) {
            _mosqueCenterCoordinates.value = getActiveCoordinates()
            loadNearbyMosques(useCurrentGps = locationRepository.hasLocationPermission())
        }
    }

    fun selectMosque(mosque: NearbyMosque?) {
        _selectedMosque.value = mosque
    }

    fun updateMosqueRadius(radiusMeters: Double) {
        if (_mosqueSearchRadiusMeters.value == radiusMeters) return
        _mosqueSearchRadiusMeters.value = radiusMeters
        loadNearbyMosques(useCurrentGps = _isMosqueUsingGps.value && locationRepository.hasLocationPermission())
    }

    fun loadNearbyMosques(useCurrentGps: Boolean = true) {
        if (_isLoadingMosques.value) return
        _isLoadingMosques.value = true
        _mosqueLocatorError.value = null

        viewModelScope.launch {
            val lang = _appLang.value
            var center = getActiveCoordinates()
            var gpsActive = false

            if (useCurrentGps && locationRepository.hasLocationPermission()) {
                when (val locResult = locationRepository.getCurrentCoordinates(forceFresh = true)) {
                    is LocationFetchResult.Success -> {
                        val coords = locResult.coordinates
                        center = coords.latitude to coords.longitude
                        gpsActive = true
                        _gpsAccuracyMeters.value = coords.accuracyMeters
                        val fallbackName = when (lang) {
                            "AR" -> "موقعي الحالي (GPS)"
                            "FR" -> "Ma position actuelle (GPS)"
                            else -> "Current Location (GPS)"
                        }
                        enableCustomLocation(
                            lat = coords.latitude,
                            lon = coords.longitude,
                            tz = coords.timezoneOffsetHours,
                            name = coords.cityName ?: fallbackName
                        )
                    }
                    else -> {
                        gpsActive = false
                    }
                }
            }

            _isMosqueUsingGps.value = gpsActive
            _mosqueCenterCoordinates.value = center

            when (val outcome = mosqueLocatorRepository.findNearbyMosques(
                latitude = center.first,
                longitude = center.second,
                radiusMeters = _mosqueSearchRadiusMeters.value
            )) {
                is MosqueSearchOutcome.Success -> {
                    _mosqueMissingApiKey.value = false
                    _nearbyMosques.value = outcome.mosques
                    val currentSelectedId = _selectedMosque.value?.id
                    _selectedMosque.value = outcome.mosques.firstOrNull { it.id == currentSelectedId }
                        ?: outcome.mosques.firstOrNull()
                    _mosqueLocatorError.value = null
                }
                MosqueSearchOutcome.MissingApiKey -> {
                    _mosqueMissingApiKey.value = true
                    _nearbyMosques.value = emptyList()
                    _selectedMosque.value = null
                    _mosqueLocatorError.value = when (lang) {
                        "AR" -> "مفتاح خرائط جوجل (MAPS_API_KEY) غير مُعد أو غير صالح. أضف مفتاح Google Maps API صحيح (يبدأ بـ AIza...) في لوحة Secrets في AI Studio."
                        "FR" -> "La clé Google Maps (MAPS_API_KEY) est manquante ou invalide. Ajoutez une clé API valide dans le panneau Secrets d'AI Studio."
                        else -> "Google Maps API key (MAPS_API_KEY) is missing or invalid. Add a valid Google Maps API key in the AI Studio Secrets panel."
                    }
                }
                is MosqueSearchOutcome.Error -> {
                    _mosqueMissingApiKey.value = false
                    _mosqueLocatorError.value = outcome.message
                }
            }
            _isLoadingMosques.value = false
        }
    }

    fun importCustomAdhan(uri: android.net.Uri, originalName: String): Boolean {
        val context = getApplication<Application>()
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val targetFile = java.io.File(context.filesDir, "custom_adhan.mp3")
                targetFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                
                _customAdhanFileName.value = originalName
                prefs.edit().putString("custom_adhan_file_name", originalName).apply()
                
                // Automatically switch alertSoundType to CUSTOM
                updateAlertSoundType("CUSTOM")
                true
            } ?: false
        } catch (e: Exception) {
            Log.e("PrayerViewModel", "Error copying custom adhan file", e)
            false
        }
    }

    fun updateAlertSoundType(type: String) {
        _alertSoundType.value = type
        prefs.edit().putString("alert_sound_type", type).apply()
        com.example.receiver.AdhanAudioPlayer.triggerPreDownload(getApplication(), type)
        viewModelScope.launch(Dispatchers.IO) {
            PrayerAlarmScheduler.scheduleNextAlarm(getApplication())
        }
    }

    fun checkAdhanOfflineStatus() {
        val context = getApplication<android.app.Application>()
        _isFullAdhanDownloaded.value = com.example.receiver.AdhanAudioPlayer.isOfflineReady(context, "ADHAN_FULL")
        _isShortAdhanDownloaded.value = com.example.receiver.AdhanAudioPlayer.isOfflineReady(context, "ADHAN_SHORT")
    }

    fun checkAndDownloadOfflineSounds() {
        val context = getApplication<android.app.Application>()
        checkAdhanOfflineStatus()

        viewModelScope.launch {
            if (!com.example.receiver.AdhanAudioPlayer.isOfflineReady(context, "ADHAN_FULL")) {
                _isDownloadingFullAdhan.value = true
                val ready = com.example.receiver.AdhanAudioPlayer.downloadIfNeeded(context, "ADHAN_FULL")
                _isFullAdhanDownloaded.value = ready || com.example.receiver.AdhanAudioPlayer.isOfflineReady(context, "ADHAN_FULL")
                _isDownloadingFullAdhan.value = false
            } else {
                _isFullAdhanDownloaded.value = true
            }

            if (!com.example.receiver.AdhanAudioPlayer.isOfflineReady(context, "ADHAN_SHORT")) {
                _isDownloadingShortAdhan.value = true
                val ready = com.example.receiver.AdhanAudioPlayer.downloadIfNeeded(context, "ADHAN_SHORT")
                _isShortAdhanDownloaded.value = ready || com.example.receiver.AdhanAudioPlayer.isOfflineReady(context, "ADHAN_SHORT")
                _isDownloadingShortAdhan.value = false
            } else {
                _isShortAdhanDownloaded.value = true
            }
        }
    }

    fun triggerAdhanDownload(type: String) {
        val context = getApplication<android.app.Application>()
        viewModelScope.launch {
            if (type == "ADHAN_FULL") {
                _isDownloadingFullAdhan.value = true
                val ready = com.example.receiver.AdhanAudioPlayer.downloadIfNeeded(context, "ADHAN_FULL")
                _isFullAdhanDownloaded.value = ready || com.example.receiver.AdhanAudioPlayer.isOfflineReady(context, "ADHAN_FULL")
                _isDownloadingFullAdhan.value = false
            } else if (type == "ADHAN_SHORT") {
                _isDownloadingShortAdhan.value = true
                val ready = com.example.receiver.AdhanAudioPlayer.downloadIfNeeded(context, "ADHAN_SHORT")
                _isShortAdhanDownloaded.value = ready || com.example.receiver.AdhanAudioPlayer.isOfflineReady(context, "ADHAN_SHORT")
                _isDownloadingShortAdhan.value = false
            }
        }
    }

    fun playTestSound() {
        com.example.receiver.AdhanAudioPlayer.play(getApplication(), _alertSoundType.value)
    }

    fun stopTestSound() {
        com.example.receiver.AdhanAudioPlayer.stop()
    }

    override fun onCleared() {
        super.onCleared()
        com.example.receiver.AdhanAudioPlayer.stop()
    }
}
