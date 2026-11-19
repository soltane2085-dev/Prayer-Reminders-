package com.example.data

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.SearchNearbyRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class NearbyMosque(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Double,
    val rating: Double? = null,
    val userRatingCount: Int? = null
)

sealed interface MosqueSearchOutcome {
    data class Success(val mosques: List<NearbyMosque>) : MosqueSearchOutcome
    data object MissingApiKey : MosqueSearchOutcome
    data class Error(val message: String) : MosqueSearchOutcome
}

class MosqueLocatorRepository(
    context: Context,
    private val apiKeyProvider: () -> String = { BuildConfig.MAPS_API_KEY },
    private val placesClientFactory: ((Context, String) -> PlacesClient)? = null
) {
    private val appContext = context.applicationContext
    private val directExecutor = Executor { runnable -> runnable.run() }

    @Volatile
    private var cachedPlacesClient: PlacesClient? = null

    fun hasValidApiKey(): Boolean {
        val key = apiKeyProvider().trim()
        return key.isNotEmpty() &&
            key != "MY_MAPS_API_KEY" &&
            key != "YOUR_API_KEY" &&
            !key.equals("null", ignoreCase = true) &&
            !key.equals("undefined", ignoreCase = true) &&
            !key.startsWith("PLACEHOLDER", ignoreCase = true) &&
            key.any { it.isLetter() } &&
            key.length >= 16
    }

    private fun getOrCreatePlacesClient(apiKey: String): PlacesClient {
        cachedPlacesClient?.let { return it }
        synchronized(this) {
            cachedPlacesClient?.let { return it }
            val customFactory = placesClientFactory
            if (customFactory != null) {
                return customFactory(appContext, apiKey).also { cachedPlacesClient = it }
            }
            if (!Places.isInitialized()) {
                Places.initializeWithNewPlacesApiEnabled(appContext, apiKey)
            }
            return Places.createClient(appContext).also { cachedPlacesClient = it }
        }
    }

    suspend fun findNearbyMosques(
        latitude: Double,
        longitude: Double,
        radiusMeters: Double = 5000.0,
        maxResultCount: Int = 20
    ): MosqueSearchOutcome {
        val apiKey = apiKeyProvider().trim()
        if (!hasValidApiKey()) {
            return MosqueSearchOutcome.MissingApiKey
        }

        val client = try {
            getOrCreatePlacesClient(apiKey)
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to initialize Places client: ${t.message}")
            return MosqueSearchOutcome.Error(t.message ?: "Failed to initialize Google Places SDK.")
        }

        val boundedRadius = radiusMeters.coerceIn(500.0, 50000.0)
        val boundedMax = maxResultCount.coerceIn(1, 20)

        val placeFields = listOf(
            Place.Field.ID,
            Place.Field.DISPLAY_NAME,
            Place.Field.FORMATTED_ADDRESS,
            Place.Field.LOCATION,
            Place.Field.RATING,
            Place.Field.USER_RATING_COUNT
        )

        val bounds = CircularBounds.newInstance(LatLng(latitude, longitude), boundedRadius)
        val request = SearchNearbyRequest.builder(bounds, placeFields)
            .setIncludedTypes(listOf("mosque"))
            .setMaxResultCount(boundedMax)
            .build()

        return suspendCancellableCoroutine { continuation ->
            try {
                client.searchNearby(request)
                    .addOnSuccessListener(directExecutor) { response ->
                        val mosques = response.places
                            .mapNotNull { place ->
                                mapPlaceToNearbyMosque(
                                    place = place,
                                    originLat = latitude,
                                    originLon = longitude
                                )
                            }
                            .sortedBy { it.distanceMeters }
                        if (continuation.isActive) {
                            continuation.resume(MosqueSearchOutcome.Success(mosques))
                        }
                    }
                    .addOnFailureListener(directExecutor) { exception ->
                        Log.w(TAG, "Nearby mosque search failed: ${exception.message}")
                        if (continuation.isActive) {
                            continuation.resume(
                                MosqueSearchOutcome.Error(
                                    exception.localizedMessage ?: "Failed to load nearby mosques."
                                )
                            )
                        }
                    }
                    .addOnCanceledListener(directExecutor) {
                        if (continuation.isActive) {
                            continuation.resume(MosqueSearchOutcome.Error("Nearby mosque search canceled."))
                        }
                    }
            } catch (t: Throwable) {
                Log.w(TAG, "Error invoking searchNearby: ${t.message}")
                if (continuation.isActive) {
                    continuation.resume(
                        MosqueSearchOutcome.Error(t.localizedMessage ?: "Places API unavailable.")
                    )
                }
            }
        }
    }

    internal fun mapPlaceToNearbyMosque(
        place: Place,
        originLat: Double,
        originLon: Double
    ): NearbyMosque? {
        val location = place.location ?: return null
        val id = place.id ?: "${location.latitude},${location.longitude}"
        val name = place.displayName?.takeIf { it.isNotBlank() } ?: return null
        val address = place.formattedAddress ?: ""
        val distanceMeters = calculateDistanceMeters(
            lat1 = originLat,
            lon1 = originLon,
            lat2 = location.latitude,
            lon2 = location.longitude
        )
        return NearbyMosque(
            id = id,
            name = name,
            address = address,
            latitude = location.latitude,
            longitude = location.longitude,
            distanceMeters = distanceMeters,
            rating = place.rating,
            userRatingCount = place.userRatingCount
        )
    }

    companion object {
        private const val TAG = "MosqueLocatorRepo"
        private const val EARTH_RADIUS_METERS = 6371000.0

        fun calculateDistanceMeters(
            lat1: Double,
            lon1: Double,
            lat2: Double,
            lon2: Double
        ): Double {
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2.0).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2.0).pow(2.0)
            val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
            return EARTH_RADIUS_METERS * c
        }
    }
}
