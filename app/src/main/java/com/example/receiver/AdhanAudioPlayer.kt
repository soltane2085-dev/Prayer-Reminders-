package com.example.receiver

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object AdhanAudioPlayer {
    private const val TAG = "AdhanPlayer"
    private const val MIN_VALID_AUDIO_BYTES = 10 * 1024L

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val fullDownloadMutex = Mutex()
    private val shortDownloadMutex = Mutex()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .connectionPool(ConnectionPool(0, 1, TimeUnit.SECONDS))
            .build()
    }

    private val fullAdhanUrls = listOf(
        "https://www.islamcan.com/audio/adhan/azan1.mp3",
        "https://archive.org/download/MakkahAdhan/MakkahAdhan.mp3",
        "https://www.islamcan.com/audio/adhan/azan2.mp3"
    )

    private val shortAdhanUrls = listOf(
        "https://www.islamcan.com/audio/adhan/azan15.mp3",
        "https://www.islamcan.com/audio/adhan/azan16.mp3"
    )

    private fun adjustVolumeToHeardLevel(context: Context) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null) {
                val maxAlarm = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                val currentAlarm = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
                val targetAlarm = (maxAlarm * 0.75f).toInt().coerceAtLeast(1)
                if (currentAlarm < targetAlarm) {
                    audioManager.setStreamVolume(AudioManager.STREAM_ALARM, targetAlarm, 0)
                    Log.d(TAG, "Adjusted STREAM_ALARM volume to $targetAlarm / $maxAlarm")
                }

                val maxMusic = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val currentMusic = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                val targetMusic = (maxMusic * 0.75f).toInt().coerceAtLeast(1)
                if (currentMusic < targetMusic) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetMusic, 0)
                    Log.d(TAG, "Adjusted STREAM_MUSIC volume to $targetMusic / $maxMusic")
                }

                val maxNotif = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
                val currentNotif = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
                val targetNotif = (maxNotif * 0.75f).toInt().coerceAtLeast(1)
                if (currentNotif < targetNotif) {
                    audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, targetNotif, 0)
                    Log.d(TAG, "Adjusted STREAM_NOTIFICATION volume to $targetNotif / $maxNotif")
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Could not adjust system stream volumes programmatically: ${e.message}")
        }
    }

    fun getLocalFile(context: Context, soundType: String): File? {
        return when (soundType) {
            "ADHAN_FULL" -> File(context.filesDir, "adhan_full.mp3")
            "ADHAN_SHORT" -> File(context.filesDir, "adhan_short.mp3")
            "CUSTOM" -> File(context.filesDir, "custom_adhan.mp3")
            else -> null
        }
    }

    fun isOfflineReady(context: Context, soundType: String): Boolean {
        val file = getLocalFile(context, soundType) ?: return false
        return file.exists() && file.length() > MIN_VALID_AUDIO_BYTES
    }

    /**
     * Suspends until the requested Adhan sound is verified locally or all HTTPS mirrors fail.
     * Serialized per [soundType] to prevent concurrent writes to the same file.
     */
    suspend fun downloadIfNeeded(context: Context, soundType: String): Boolean {
        val mutex = when (soundType) {
            "ADHAN_FULL" -> fullDownloadMutex
            "ADHAN_SHORT" -> shortDownloadMutex
            else -> return isOfflineReady(context, soundType)
        }
        return mutex.withLock {
            withContext(Dispatchers.IO) {
                downloadSync(context.applicationContext, soundType)
            }
        }
    }

    private fun downloadSync(context: Context, soundType: String): Boolean {
        val file = getLocalFile(context, soundType) ?: return false

        if (file.exists() && file.length() > MIN_VALID_AUDIO_BYTES) {
            return true
        }

        val urls = when (soundType) {
            "ADHAN_FULL" -> fullAdhanUrls
            "ADHAN_SHORT" -> shortAdhanUrls
            else -> return false
        }

        for (urlString in urls) {
            val tempFile = File(context.filesDir, "${file.name}.tmp")
            try {
                Log.d(TAG, "Starting download from: $urlString")
                val request = Request.Builder()
                    .url(urlString)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                    .header("Accept", "audio/mpeg, audio/*;q=0.9, */*;q=0.8")
                    .header("Connection", "close")
                    .get()
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.w(TAG, "Mirror returned HTTP ${response.code} for $urlString")
                        return@use
                    }

                    val contentType = response.header("Content-Type")?.lowercase().orEmpty()
                    if (contentType.contains("text/html") || contentType.contains("application/json") || contentType.contains("text/xml")) {
                        Log.w(TAG, "Mirror returned non-audio content type ($contentType) for $urlString")
                        return@use
                    }

                    val body = response.body ?: return@use
                    body.byteStream().use { input ->
                        FileOutputStream(tempFile).use { output ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                            }
                            output.flush()
                        }
                    }

                    if (isValidAudioFile(tempFile)) {
                        if (file.exists()) file.delete()
                        if (tempFile.renameTo(file)) {
                            Log.d(TAG, "Successfully downloaded ${file.name} (${file.length()} bytes)")
                            return true
                        } else {
                            tempFile.copyTo(file, overwrite = true)
                            tempFile.delete()
                            Log.d(TAG, "Successfully copied ${file.name} (${file.length()} bytes)")
                            return true
                        }
                    } else {
                        Log.w(TAG, "Downloaded payload from $urlString was invalid or too small")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Mirror unavailable ($urlString): ${e.message}")
            } catch (t: Throwable) {
                Log.w(TAG, "Unexpected error downloading from $urlString: ${t.message}")
            } finally {
                if (tempFile.exists()) {
                    tempFile.delete()
                }
            }
        }

        Log.w(TAG, "Unable to download $soundType from remote mirrors; system default alert remains active")
        return false
    }

    private fun isValidAudioFile(file: File): Boolean {
        if (!file.exists() || file.length() <= MIN_VALID_AUDIO_BYTES) return false
        return try {
            val header = ByteArray(16)
            val readCount = file.inputStream().use { it.read(header) }
            if (readCount <= 0) return false
            val prefix = String(header, 0, readCount, Charsets.UTF_8).trimStart()
            !prefix.startsWith("<") && !prefix.startsWith("{")
        } catch (_: Exception) {
            false
        }
    }

    fun triggerPreDownload(context: Context, soundType: String) {
        if (soundType == "ADHAN_FULL" || soundType == "ADHAN_SHORT") {
            val appContext = context.applicationContext
            scope.launch {
                try {
                    downloadIfNeeded(appContext, soundType)
                } catch (t: Throwable) {
                    Log.w(TAG, "Pre-download skipped: ${t.message}")
                }
            }
        }
    }

    private fun releasePlayerOnMain() {
        try {
            mediaPlayer?.let { player ->
                try {
                    if (player.isPlaying) {
                        player.stop()
                    }
                } catch (e: Throwable) {
                    Log.w(TAG, "Error stopping player before release: ${e.message}")
                }
                try {
                    player.release()
                } catch (e: Throwable) {
                    Log.w(TAG, "Error calling release on player: ${e.message}")
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Global error releasing player: ${e.message}")
        } finally {
            mediaPlayer = null
        }
    }

    fun play(context: Context, soundType: String) {
        if (soundType == "SILENT") {
            Log.d(TAG, "Silent mode request, stopping and exiting")
            stop()
            return
        }

        val appContext = context.applicationContext
        adjustVolumeToHeardLevel(appContext)

        scope.launch {
            try {
                val file = getLocalFile(appContext, soundType)
                var preparedToPlayLocal = false

                if (soundType == "ADHAN_FULL" || soundType == "ADHAN_SHORT") {
                    val exists = file?.exists() == true && file.length() > MIN_VALID_AUDIO_BYTES
                    if (!exists) {
                        withContext(Dispatchers.Main) {
                            try {
                                Toast.makeText(
                                    appContext,
                                    "جاري تحميل صوت الأذان لأول مرة، يرجى الانتظار ثوانٍ...\nDownloading Adhan audio...",
                                    Toast.LENGTH_LONG
                                ).show()
                            } catch (e: Throwable) {
                                Log.w(TAG, "Toast error: ${e.message}")
                            }
                        }
                        val downloaded = downloadIfNeeded(appContext, soundType)
                        if (downloaded) {
                            preparedToPlayLocal = true
                            withContext(Dispatchers.Main) {
                                try {
                                    Toast.makeText(
                                        appContext,
                                        "تم التحميل بنجاح ويبدأ التشغيل الآن.\nDownloaded successfully. Playing now...",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } catch (e: Throwable) {
                                    Log.w(TAG, "Toast error: ${e.message}")
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                try {
                                    Toast.makeText(
                                        appContext,
                                        "تعذر تنزيل الأذان. سيتم تشغيل النغمة الافتراضية.\nDownload unavailable. Playing system default...",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } catch (e: Throwable) {
                                    Log.w(TAG, "Toast error: ${e.message}")
                                }
                            }
                        }
                    } else {
                        preparedToPlayLocal = true
                    }
                } else if (soundType == "CUSTOM") {
                    if (file?.exists() == true) {
                        preparedToPlayLocal = true
                    }
                }

                withContext(Dispatchers.Main) {
                    try {
                        releasePlayerOnMain()

                        if (preparedToPlayLocal && file != null && file.exists()) {
                            try {
                                val player = MediaPlayer()
                                mediaPlayer = player
                                player.apply {
                                    setAudioAttributes(
                                        AudioAttributes.Builder()
                                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                            .setUsage(AudioAttributes.USAGE_ALARM)
                                            .build()
                                    )
                                    setVolume(1.0f, 1.0f)
                                    setDataSource(file.absolutePath)
                                    prepareAsync()
                                    setOnPreparedListener {
                                        try {
                                            it.start()
                                            Log.d(TAG, "Local file playback started: ${file.name}")
                                        } catch (t: Throwable) {
                                            Log.w(TAG, "Error starting local file player: ${t.message}")
                                            playSystemDefaultOnMain(appContext)
                                        }
                                    }
                                    setOnErrorListener { _, what, extra ->
                                        Log.w(TAG, "Local playback error ($what, $extra), falling back to native ringtone")
                                        playSystemDefaultOnMain(appContext)
                                        true
                                    }
                                }
                            } catch (e: Throwable) {
                                Log.w(TAG, "Error preparing local file player, fallback: ${e.message}")
                                playSystemDefaultOnMain(appContext)
                            }
                        } else {
                            playSystemDefaultOnMain(appContext)
                        }
                    } catch (e: Throwable) {
                        Log.w(TAG, "Error inside playing on Main block: ${e.message}")
                        playSystemDefaultOnMain(appContext)
                    }
                }
            } catch (globalException: Throwable) {
                Log.w(TAG, "Global error inside play launch: ${globalException.message}")
                withContext(Dispatchers.Main) {
                    playSystemDefaultOnMain(appContext)
                }
            }
        }
    }

    private fun playSystemDefaultOnMain(context: Context) {
        try {
            releasePlayerOnMain()
            val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            if (defaultUri == null) {
                Log.w(TAG, "No system default alert URI available")
                return
            }

            adjustVolumeToHeardLevel(context.applicationContext)

            val player = MediaPlayer()
            mediaPlayer = player
            player.apply {
                setDataSource(context, defaultUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                )
                setVolume(1.0f, 1.0f)
                prepareAsync()
                setOnPreparedListener {
                    try {
                        it.start()
                        Log.d(TAG, "System default notification ringtone started")
                    } catch (t: Throwable) {
                        Log.w(TAG, "Error starting default ringtone player: ${t.message}")
                    }
                }
                setOnErrorListener { _, what, extra ->
                    Log.w(TAG, "Default ringtone player error ($what, $extra)")
                    true
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error fallback system sound: ${e.message}")
        }
    }

    fun stop() {
        scope.launch(Dispatchers.Main) {
            releasePlayerOnMain()
        }
    }
}
