package com.example

import android.Manifest
import android.app.Application
import android.location.Location
import androidx.test.core.app.ApplicationProvider
import com.example.data.LocationFetchResult
import com.example.data.LocationRepository
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.lang.reflect.Proxy

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocationRepositoryTest {

    private fun fakeFusedClient(
        currentLocation: Location? = null,
        lastLocation: Location? = null
    ): FusedLocationProviderClient {
        return Proxy.newProxyInstance(
            FusedLocationProviderClient::class.java.classLoader,
            arrayOf(FusedLocationProviderClient::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getCurrentLocation" -> Tasks.forResult(currentLocation)
                "getLastLocation" -> Tasks.forResult(lastLocation)
                "removeLocationUpdates" -> Tasks.forResult<Void>(null)
                "requestLocationUpdates" -> Tasks.forResult<Void>(null)
                else -> null
            }
        } as FusedLocationProviderClient
    }

    @Test
    fun `returns PermissionDenied when location permissions are missing`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(app).denyPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val repo = LocationRepository(app, fakeFusedClient())
        assertFalse(repo.hasLocationPermission())

        val result = repo.getCurrentCoordinates()
        assertTrue(result is LocationFetchResult.PermissionDenied)
    }

    @Test
    fun `returns Success with coordinates from getCurrentLocation`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)

        val mockLoc = Location("gps").apply {
            latitude = 21.4225
            longitude = 39.8262
            accuracy = 12.5f
            time = System.currentTimeMillis()
        }

        val repo = LocationRepository(app, fakeFusedClient(currentLocation = mockLoc))
        assertTrue(repo.hasLocationPermission())

        val result = repo.getCurrentCoordinates(forceFresh = true)
        assertTrue(result is LocationFetchResult.Success)
        val coords = (result as LocationFetchResult.Success).coordinates
        assertEquals(21.4225, coords.latitude, 0.0001)
        assertEquals(39.8262, coords.longitude, 0.0001)
        assertEquals(12.5f, coords.accuracyMeters ?: 0f, 0.01f)
    }

    @Test
    fun `falls back to lastLocation when getCurrentLocation returns null`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)

        val cachedLoc = Location("fused").apply {
            latitude = 33.5731
            longitude = -7.5898
            accuracy = 45.0f
            time = System.currentTimeMillis()
        }

        val repo = LocationRepository(
            app,
            fakeFusedClient(currentLocation = null, lastLocation = cachedLoc)
        )

        val result = repo.getCurrentCoordinates(forceFresh = false)
        assertTrue(result is LocationFetchResult.Success)
        val coords = (result as LocationFetchResult.Success).coordinates
        assertEquals(33.5731, coords.latitude, 0.0001)
        assertEquals(-7.5898, coords.longitude, 0.0001)
    }
}
