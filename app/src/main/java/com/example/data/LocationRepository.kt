package com.example.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Geographic coordinates and metadata resolved for prayer time and Qibla calculations.
 */
data class LocationCoordinates(
    val latitude: Double,
    val longitude: Double,
    val timezoneOffsetHours: Double,
    val cityName: String?,
    val accuracyMeters: Float? = null,
    val timestampMillis: Long = System.currentTimeMillis()
)

/**
 * Sealed outcome for one-shot coordinate retrieval via [LocationRepository].
 */
sealed class LocationFetchResult {
    data class Success(val coordinates: LocationCoordinates) : LocationFetchResult()
    data object PermissionDenied : LocationFetchResult()
    data object LocationUnavailable : LocationFetchResult()
    data class Error(val message: String, val cause: Throwable? = null) : LocationFetchResult()
}

/**
 * Repository responsible for fetching and observing the user's current coordinates
 * via Google Play Services [FusedLocationProviderClient] and resolving city/timezone metadata.
 */
class LocationRepository(
    context: Context,
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context.applicationContext)
) {
    private val appContext: Context = context.applicationContext

    companion object {
        private const val TAG = "LocationRepository"
        private const val CURRENT_LOCATION_TIMEOUT_MS = 10_000L
        private const val LAST_LOCATION_TIMEOUT_MS = 3_000L
        private const val SINGLE_UPDATE_TIMEOUT_MS = 8_000L
        private const val GEOCODE_TIMEOUT_MS = 4_000L
        private const val MAX_CACHED_AGE_MS = 5 * 60 * 1000L
    }

    /**
     * Returns true if either fine or coarse location permission has been granted.
     */
    fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }

    private fun hasFineLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Fetches the user's current coordinates using [FusedLocationProviderClient].
     *
     * Strategy:
     * 1. Verify runtime location permissions.
     * 2. Request a current fix via `getCurrentLocation` with appropriate priority.
     * 3. Fall back to `lastLocation` if `getCurrentLocation` returns null.
     * 4. Fall back to a short single-update `requestLocationUpdates` call if still null.
     * 5. Resolve local UTC timezone offset and reverse-geocode city name on IO dispatcher.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentCoordinates(forceFresh: Boolean = false): LocationFetchResult {
        if (!hasLocationPermission()) {
            return LocationFetchResult.PermissionDenied
        }

        val priority = if (hasFineLocationPermission()) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }

        return try {
            var location: Location? = null

            // 1. Try modern getCurrentLocation API
            val cts = CancellationTokenSource()
            val currentRequest = CurrentLocationRequest.Builder()
                .setPriority(priority)
                .setDurationMillis(8_000L)
                .setMaxUpdateAgeMillis(if (forceFresh) 0L else MAX_CACHED_AGE_MS)
                .build()

            location = withTimeoutOrNull(CURRENT_LOCATION_TIMEOUT_MS) {
                try {
                    fusedLocationClient.getCurrentLocation(currentRequest, cts.token).awaitResult(cts)
                } catch (e: SecurityException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "getCurrentLocation failed, falling back to lastLocation: ${e.message}")
                    null
                }
            }

            // 2. Fall back to last known location
            if (location == null) {
                location = withTimeoutOrNull(LAST_LOCATION_TIMEOUT_MS) {
                    try {
                        fusedLocationClient.lastLocation.awaitResult()
                    } catch (e: SecurityException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "lastLocation failed: ${e.message}")
                        null
                    }
                }
            }

            // 3. Fall back to a single active location update
            if (location == null) {
                location = requestSingleLocationUpdate(priority)
            }

            if (location == null) {
                LocationFetchResult.LocationUnavailable
            } else {
                val coordinates = buildCoordinates(location)
                LocationFetchResult.Success(coordinates)
            }
        } catch (se: SecurityException) {
            Log.e(TAG, "Location permission missing at runtime", se)
            LocationFetchResult.PermissionDenied
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve location coordinates", e)
            LocationFetchResult.Error(
                message = e.localizedMessage ?: "Failed to retrieve coordinates",
                cause = e
            )
        }
    }

    /**
     * Observes location updates as a cold [Flow] using [FusedLocationProviderClient].
     * Automatically cleans up location callbacks when collection is cancelled.
     */
    @SuppressLint("MissingPermission")
    fun observeLocationUpdates(
        intervalMillis: Long = 60_000L,
        minDistanceMeters: Float = 500f
    ): Flow<LocationCoordinates> = callbackFlow {
        if (!hasLocationPermission()) {
            close(SecurityException("Location permission not granted"))
            return@callbackFlow
        }

        val priority = if (hasFineLocationPermission()) {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        } else {
            Priority.PRIORITY_LOW_POWER
        }

        val request = LocationRequest.Builder(priority, intervalMillis)
            .setMinUpdateIntervalMillis(intervalMillis / 2)
            .setMinUpdateDistanceMeters(minDistanceMeters)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val tzOffset = currentTimezoneOffsetHours()
                trySend(
                    LocationCoordinates(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        timezoneOffsetHours = tzOffset,
                        cityName = null,
                        accuracyMeters = if (loc.hasAccuracy()) loc.accuracy else null,
                        timestampMillis = loc.time
                    )
                )
            }
        }

        fusedLocationClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
            .addOnFailureListener { error ->
                Log.e(TAG, "Failed to register location updates", error)
                close(error)
            }

        awaitClose {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestSingleLocationUpdate(priority: Int): Location? {
        return withTimeoutOrNull(SINGLE_UPDATE_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                val request = LocationRequest.Builder(priority, 1_000L)
                    .setMaxUpdates(1)
                    .build()

                val callback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        fusedLocationClient.removeLocationUpdates(this)
                        if (cont.isActive) {
                            cont.resume(result.lastLocation)
                        }
                    }
                }

                cont.invokeOnCancellation {
                    fusedLocationClient.removeLocationUpdates(callback)
                }

                try {
                    fusedLocationClient.requestLocationUpdates(
                        request,
                        callback,
                        Looper.getMainLooper()
                    ).addOnFailureListener(directExecutor) { e ->
                        fusedLocationClient.removeLocationUpdates(callback)
                        if (cont.isActive) {
                            Log.w(TAG, "Single update request failed: ${e.message}")
                            cont.resume(null)
                        }
                    }
                } catch (se: SecurityException) {
                    if (cont.isActive) cont.resumeWithException(se)
                } catch (e: Exception) {
                    if (cont.isActive) cont.resume(null)
                }
            }
        }
    }

    private suspend fun buildCoordinates(location: Location): LocationCoordinates {
        val tzOffset = currentTimezoneOffsetHours()
        val cityName = resolveCityName(location.latitude, location.longitude)
        return LocationCoordinates(
            latitude = location.latitude,
            longitude = location.longitude,
            timezoneOffsetHours = tzOffset,
            cityName = cityName,
            accuracyMeters = if (location.hasAccuracy()) location.accuracy else null,
            timestampMillis = location.time
        )
    }

    private fun currentTimezoneOffsetHours(): Double {
        return TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 3_600_000.0
    }

    /**
     * Reverse-geocodes coordinates to a city/locality name when Geocoder is available.
     */
    suspend fun resolveCityName(latitude: Double, longitude: Double): String? {
        if (!Geocoder.isPresent()) return null

        return withContext(Dispatchers.IO) {
            withTimeoutOrNull(GEOCODE_TIMEOUT_MS) {
                try {
                    val geocoder = Geocoder(appContext, Locale.getDefault())
                    val addresses: List<Address>? =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            suspendCancellableCoroutine { cont ->
                                geocoder.getFromLocation(
                                    latitude,
                                    longitude,
                                    1,
                                    object : Geocoder.GeocodeListener {
                                        override fun onGeocode(addresses: MutableList<Address>) {
                                            if (cont.isActive) cont.resume(addresses)
                                        }

                                        override fun onError(errorMessage: String?) {
                                            Log.w(TAG, "Geocoder error: $errorMessage")
                                            if (cont.isActive) cont.resume(emptyList())
                                        }
                                    }
                                )
                            }
                        } else {
                            @Suppress("DEPRECATION")
                            geocoder.getFromLocation(latitude, longitude, 1)
                        }

                    val addr = addresses?.firstOrNull()
                    addr?.locality ?: addr?.subAdminArea ?: addr?.adminArea ?: addr?.countryName
                } catch (e: Exception) {
                    Log.w(TAG, "Reverse geocoding failed: ${e.message}")
                    null
                }
            }
        }
    }

    private val directExecutor = Executor { runnable -> runnable.run() }

    private suspend fun <T> Task<T>.awaitResult(
        cancellationTokenSource: CancellationTokenSource? = null
    ): T? = suspendCancellableCoroutine { cont ->
        if (cancellationTokenSource != null) {
            cont.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }
        }
        addOnSuccessListener(directExecutor) { result ->
            if (cont.isActive) cont.resume(result)
        }
        addOnFailureListener(directExecutor) { exception ->
            if (cont.isActive) cont.resumeWithException(exception)
        }
        addOnCanceledListener(directExecutor) {
            if (cont.isActive) cont.cancel()
        }
    }
}
