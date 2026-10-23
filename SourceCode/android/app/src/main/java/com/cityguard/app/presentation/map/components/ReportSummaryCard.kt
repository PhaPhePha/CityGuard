package com.cityguard.app.presentation.map.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cityguard.app.domain.model.ReportSummary

/** Card mô phỏng Figma. Timestamp, độ tin cậy và ảnh hiện là placeholder cho M1. */
@Composable
internal fun ReportSummaryCard(
    report: ReportSummary,
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("report_card_${report.id}")
            .clickable(onClick = onOpenDetails),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 158.dp)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(report.category.markerSymbol(), fontSize = 21.sp)
                    Spacer(Modifier.width(5.dp))
                    Text(
                        report.category.displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(5.dp))
                Text(report.title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 2)
                Text("📍 ${report.address}", fontSize = 11.sp, maxLines = 2)
                Text("◷ 5 phút trước (demo)", fontSize = 11.sp, color = Color.Gray)
                Spacer(Modifier.height(5.dp))
                Text(
                    "Độ tin cậy: Cao (demo)",
                    modifier = Modifier
                        .background(Color(0xFFD6F3D8), RoundedCornerShape(5.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    color = MapColors.Green,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Box(
                modifier = Modifier
                    .size(width = 92.dp, height = 104.dp)
                    .background(Color(0xFFE8E2D7), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("📷\nẢnh demo", color = Color.DarkGray, fontSize = 12.sp)
            }
        }
    }
}
