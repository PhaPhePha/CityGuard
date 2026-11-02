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
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog
@RunWith(AndroidJUnit4::class)
class MapScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun clickingMarkerThenCardOpensCorrectReport() {
        var openedReportId: String? = null
        var selectedByClick: String? = null

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
                        selectedByClick = reportId

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

        composeTestRule.waitForIdle()

// 1. Kiểm tra callback đã nhận đúng R001.
        composeTestRule.runOnIdle {
            assertEquals("R001", selectedByClick)
        }

// 2. In cây giao diện TRƯỚC khi kiểm tra.
        composeTestRule
            .onRoot(useUnmergedTree = true)
            .printToLog("CITYGUARD_TEST")

// 3. Kiểm tra Report Card trong unmerged tree.
        composeTestRule
            .onNodeWithTag(
                "report_card_R001",
                useUnmergedTree = true
            )
            .assertExists()

// 4. Kiểm tra Card đang hiển thị.
        composeTestRule
            .onNodeWithTag(
                "report_card_R001",
                useUnmergedTree = true
            )
            .assertIsDisplayed()

// 5. Kiểm tra nội dung báo cáo.
        composeTestRule
            .onNodeWithText(
                "Ùn tắc giờ cao điểm",
                useUnmergedTree = true
            )
            .assertIsDisplayed()


        composeTestRule.onRoot()
            .printToLog("CITYGUARD_TEST")

        composeTestRule
            .onNodeWithTag("report_card_R001")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Ùn tắc giờ cao điểm")
            .assertIsDisplayed()

        // Chọn marker khác để kiểm tra state cập nhật.
        composeTestRule
            .onNodeWithTag("marker_R003")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag("report_card_R003")
            .assertIsDisplayed()
            .performClick()

        // Đảm bảo mở đúng Report Details.
        composeTestRule.runOnIdle {
            assertEquals("R003", openedReportId)
        }
    }
}