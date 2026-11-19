package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.MosqueLocatorRepository
import com.example.data.MosqueSearchOutcome
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.SearchNearbyResponse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Proxy

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MosqueLocatorRepositoryTest {

    private fun fakePlacesClient(places: List<Place>?, failure: Exception? = null): PlacesClient {
        return Proxy.newProxyInstance(
            PlacesClient::class.java.classLoader,
            arrayOf(PlacesClient::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "searchNearby" -> {
                    if (failure != null) {
                        Tasks.forException<SearchNearbyResponse>(failure)
                    } else {
                        Tasks.forResult(SearchNearbyResponse.newInstance(places ?: emptyList()))
                    }
                }
                else -> null
            }
        } as PlacesClient
    }

    @Test
    fun `returns MissingApiKey when MAPS_API_KEY is blank or placeholder`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()

        val blankRepo = MosqueLocatorRepository(app, apiKeyProvider = { "" })
        assertFalse(blankRepo.hasValidApiKey())
        assertTrue(blankRepo.findNearbyMosques(21.4225, 39.8262) is MosqueSearchOutcome.MissingApiKey)

        val placeholderRepo = MosqueLocatorRepository(app, apiKeyProvider = { "MY_MAPS_API_KEY" })
        assertFalse(placeholderRepo.hasValidApiKey())
        assertTrue(placeholderRepo.findNearbyMosques(21.4225, 39.8262) is MosqueSearchOutcome.MissingApiKey)

        val numericInvalidRepo = MosqueLocatorRepository(app, apiKeyProvider = { "5486" })
        assertFalse(numericInvalidRepo.hasValidApiKey())
        assertTrue(numericInvalidRepo.findNearbyMosques(21.4225, 39.8262) is MosqueSearchOutcome.MissingApiKey)
    }

    @Test
    fun `sorts nearby mosques by ascending distance from user GPS coordinates`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()

        val farMosque = Place.builder()
            .setId("mosque_far")
            .setDisplayName("Al-Noor Mosque")
            .setFormattedAddress("200 Main St")
            .setLocation(LatLng(21.4400, 39.8262))
            .setRating(4.6)
            .setUserRatingCount(140)
            .build()

        val nearMosque = Place.builder()
            .setId("mosque_near")
            .setDisplayName("Masjid Al-Haram Gate")
            .setFormattedAddress("100 Central Plaza")
            .setLocation(LatLng(21.4250, 39.8262))
            .setRating(4.9)
            .setUserRatingCount(999)
            .build()

        val repo = MosqueLocatorRepository(
            context = app,
            apiKeyProvider = { "AIzaFakeValidTestKey123" },
            placesClientFactory = { _, _ -> fakePlacesClient(listOf(farMosque, nearMosque)) }
        )

        val outcome = repo.findNearbyMosques(latitude = 21.4225, longitude = 39.8262, radiusMeters = 5000.0)
        assertTrue(outcome is MosqueSearchOutcome.Success)
        val mosques = (outcome as MosqueSearchOutcome.Success).mosques
        assertEquals(2, mosques.size)
        assertEquals("mosque_near", mosques[0].id)
        assertEquals("Masjid Al-Haram Gate", mosques[0].name)
        assertEquals("mosque_far", mosques[1].id)
        assertTrue(mosques[0].distanceMeters < mosques[1].distanceMeters)
    }

    @Test
    fun `returns Error outcome when Places searchNearby fails`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()

        val repo = MosqueLocatorRepository(
            context = app,
            apiKeyProvider = { "AIzaFakeValidTestKey123" },
            placesClientFactory = { _, _ ->
                fakePlacesClient(places = null, failure = IllegalStateException("Places quota exceeded"))
            }
        )

        val outcome = repo.findNearbyMosques(21.4225, 39.8262)
        assertTrue(outcome is MosqueSearchOutcome.Error)
        assertEquals("Places quota exceeded", (outcome as MosqueSearchOutcome.Error).message)
    }

    @Test
    fun `calculateDistanceMeters computes accurate geodesic separation`() {
        val dist = MosqueLocatorRepository.calculateDistanceMeters(
            lat1 = 21.4225,
            lon1 = 39.8262,
            lat2 = 21.4315,
            lon2 = 39.8262
        )
        // ~1 degree latitude ~= 111.1 km -> 0.009 deg ~= 1000 m
        assertEquals(1000.0, dist, 20.0)
    }
}
