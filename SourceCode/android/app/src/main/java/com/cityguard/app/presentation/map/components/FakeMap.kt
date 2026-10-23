package com.cityguard.app.presentation.map.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cityguard.app.domain.model.ReportSummary

/** Chỉ là bản đồ nền minh hoạ của M1: vị trí chấm KHÔNG lấy từ GPS. */
@Composable
internal fun FakeMap(
    reports: List<ReportSummary>,
    selectedReportId: String?,
    onMarkerClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.background(MapColors.Sand)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Công viên / sông mô phỏng như Figma; không phải tile bản đồ thực.
            drawRect(MapColors.Park, topLeft = Offset(w * .77f, h * .42f), size = Size(w * .14f, h * .17f))
            drawRect(MapColors.Park, topLeft = Offset(w * .17f, h * .73f), size = Size(w * .16f, h * .10f))
            drawLine(MapColors.Water, Offset(w * .9f, 0f), Offset(w * .68f, h), 25.dp.toPx())

            for (i in 1..12) {
                val y = h * i / 13f
                drawLine(MapColors.Road, Offset(0f, y), Offset(w, y + h * .03f), 3.dp.toPx())
            }
            for (i in 1..9) {
                val x = w * i / 10f
                drawLine(MapColors.Road, Offset(x, 0f), Offset(x - w * .18f, h), 3.dp.toPx())
            }
            drawLine(MapColors.MainRoad, Offset(w * .15f, 0f), Offset(w * .20f, h), 12.dp.toPx())
            drawLine(MapColors.MainRoad, Offset(0f, h * .78f), Offset(w, h * .89f), 12.dp.toPx())
            drawLine(MapColors.MainRoad, Offset(w * .1f, h * .1f), Offset(w * .93f, h * .54f), 9.dp.toPx())
        }

        Text(
            text = "BẢN ĐỒ MÔ PHỎNG • M1",
            fontSize = 10.sp,
            color = Color(0xFF66747A),
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
        )

        // Vòng xanh này minh họa hình thức GPS trên Figma, KHÔNG phải vị trí thật.
        Box(
            Modifier.align(Alignment.Center)
                .size(34.dp)
                .background(MapColors.Blue.copy(alpha = .17f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.size(11.dp).background(MapColors.Blue, CircleShape))
        }

        val positions = listOf(
            .12f to .17f, .62f to .13f, .82f to .31f, .27f to .43f,
            .66f to .52f, .14f to .66f, .80f to .70f, .47f to .28f
        )
        // M1 giới hạn 8 vị trí minh họa; các marker không theo latitude/longitude.
        reports.take(8).forEachIndexed { index, report ->
            val p = positions[index]
            IncidentMarker(
                report = report,
                selected = report.id == selectedReportId,
                onClick = { onMarkerClick(report.id) },
                modifier = Modifier.offset(
                    x = (maxWidth - 48.dp) * p.first,
                    y = (maxHeight - 52.dp) * p.second
                )
            )
        }
    }
}

@Composable
private fun IncidentMarker(
    report: ReportSummary,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = report.category.markerColor()
    Box(
        modifier = modifier
            .size(width = 48.dp, height = 52.dp)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Xem sự cố ${report.title}" }
            .testTag("marker_${report.id}"),
        contentAlignment = Alignment.TopCenter
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height * .36f
            val radius = size.width * .32f
            val tail = Path().apply {
                moveTo(cx - radius * .65f, cy + radius * .5f)
                lineTo(cx, size.height * .94f)
                lineTo(cx + radius * .65f, cy + radius * .5f)
                close()
            }
            drawPath(tail, color)
            drawCircle(color, radius, Offset(cx, cy))
            if (selected) {
                drawCircle(MapColors.Dark, radius + 2.dp.toPx(), Offset(cx, cy), style = Stroke(2.dp.toPx()))
            }
        }
        Text(
            text = report.category.markerSymbol(),
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 7.dp),
            fontWeight = FontWeight.Bold
        )
    }
}
