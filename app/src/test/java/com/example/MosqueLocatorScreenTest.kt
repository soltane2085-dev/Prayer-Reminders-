package com.example

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import com.example.ui.screens.MosqueLocatorScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.PrayerViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MosqueLocatorScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `MosqueLocatorScreen opens and renders cleanly without crashing`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = PrayerViewModel(app)
        viewModel.setShowMosqueLocator(true)

        composeTestRule.setContent {
            MyApplicationTheme(themeMode = "dark") {
                MosqueLocatorScreen(
                    viewModel = viewModel,
                    onClose = { viewModel.setShowMosqueLocator(false) }
                )
            }
        }

        composeTestRule.onNodeWithTag("mosque_locator_back_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("mosque_locator_gps_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("mosque_locator_radius_5000").assertIsDisplayed()
    }
}
