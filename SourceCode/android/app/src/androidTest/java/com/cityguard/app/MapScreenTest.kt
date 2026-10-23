package com.cityguard.app.presentation.map

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.cityguard.app.data.mock.FakeReportData

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MapScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun clickingMarkerShowsCorrectReport() {

        // Arrange: Prepare the screen with mock data
        composeTestRule.setContent {
            MaterialTheme {
                MapScreen(
                    uiState = MapUiState.Success(
                        FakeReportData.reports
                    )
                )
            }
        }

        // Act: Click the marker of report R001
        composeTestRule
            .onNodeWithContentDescription(
                "Xem sự cố Ùn tắc giờ cao điểm"
            )
            .performClick()

        // Assert: Verify the correct report is displayed
        composeTestRule
            .onNodeWithText("Ùn tắc giờ cao điểm")
            .assertIsDisplayed()
    }
}
