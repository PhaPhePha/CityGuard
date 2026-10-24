package com.cityguard.app.presentation.map

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cityguard.app.data.mock.FakeReportData
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.cityguard.app.presentation.map.components.FakeMap
@RunWith(AndroidJUnit4::class)
class MapScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun clickingMarkerThenCardOpensCorrectReport() {
        var openedReportId: String? = null

        composeTestRule.setContent {
            var state by remember {
                mutableStateOf(
                    MapUiState.Success(
                        reports = FakeReportData.reports
                    )
                )
            }

            MaterialTheme {
                MapScreen(
                    uiState = state,
                    onSelectReport = { reportId ->
                        state = state.copy(
                            selectedReportId = reportId
                        )
                    },
                    onOpenReportDetails = { reportId ->
                        openedReportId = reportId
                    },
                    mapContent = { reports, selectedId, onClick ->
                        FakeMap(
                            reports = reports,
                            selectedReportId = selectedId,
                            onMarkerClick = onClick,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                )
            }
        }

        composeTestRule
            .onNodeWithTag("marker_R001")
            .performClick()

        composeTestRule
            .onNodeWithTag("report_card_R001")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Ùn tắc giờ cao điểm")
            .assertIsDisplayed()

        // Đổi lựa chọn để phát hiện lỗi giữ lại report cũ.
        composeTestRule
            .onNodeWithTag("marker_R003")
            .performClick()

        composeTestRule
            .onNodeWithTag("report_card_R003")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals("R003", openedReportId)
        }
    }

}