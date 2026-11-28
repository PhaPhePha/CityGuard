
package com.hoangpha.cityguard.presentation.report

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.hoangpha.cityguard.domain.model.Comment
import com.hoangpha.cityguard.domain.model.IncidentCategory
import com.hoangpha.cityguard.domain.model.Report
import com.hoangpha.cityguard.domain.model.VoteType
import com.hoangpha.cityguard.presentation.map.components.markerIconRes
import com.hoangpha.cityguard.ui.theme.CityGuardAppTheme
import com.hoangpha.cityguard.ui.theme.CityGuardDarkYellow
import com.hoangpha.cityguard.ui.theme.CityGuardGreen
import com.hoangpha.cityguard.ui.theme.CityGuardRed
import com.hoangpha.cityguard.ui.theme.CityGuardYellow

// Route: kết nối ViewModel với UI
@Composable
fun ReportDetailRoute(
    onBack: () -> Unit,
    viewModel: ReportDetailViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ReportDetailScreen(
        state = state,
        onBack = onBack,
        onVote = viewModel::vote,
        onCommentChanged = viewModel::onCommentChanged,
        onSendComment = viewModel::sendComment
    )
}

// Screen: hiển thị giao diện theo state
@Composable
fun ReportDetailScreen(
    state: ReportDetailUiState,
    onBack: () -> Unit,
    onVote: (VoteType) -> Unit,
    onCommentChanged: (String) -> Unit,
    onSendComment: () -> Unit,
    modifier: Modifier = Modifier
) {
    val report = state.report

    when {
        state.isLoading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        state.notFound || report == null -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Không tìm thấy báo cáo",
                    style = MaterialTheme.typography.titleMedium
                )

                Button(onClick = onBack) {
                    Text("Quay lại")
                }
            }
        }

        else -> {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                // Ảnh đầu trang
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .background(Color(0xFFE8E2D0))
                    ) {
                        val imageUri = report.imageUri

                        if (imageUri != null) {
                            AsyncImage(
                                model = imageUri,
                                contentDescription = "Ảnh sự cố",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = "Chưa có ảnh",
                                modifier = Modifier.align(Alignment.Center),
                                color = Color.DarkGray
                            )
                        }

                        // Nút quay lại
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .statusBarsPadding()
                                .padding(12.dp)
                                .size(40.dp)
                                .background(Color.White, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Quay lại",
                                tint = Color.Black
                            )
                        }
                    }
                }

                // Nội dung trắng bo góc
                item {
                    Column(
                        modifier = Modifier
                            .offset(y = (-20).dp)
                            .fillMaxWidth()
                            .background(
                                Color.White,
                                RoundedCornerShape(
                                    topStart = 20.dp,
                                    topEnd = 20.dp
                                )
                            )
                            .padding(16.dp)
                    ) {
                        // Thông tin báo cáo
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(
                                    id = report.category.markerIconRes()
                                ),
                                contentDescription = report.category.displayName,
                                modifier = Modifier.size(44.dp)
                            )

                            Spacer(Modifier.width(10.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = report.category.displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )

                                Text(
                                    text = "📍 ${report.address}",
                                    fontSize = 12.sp
                                )

                                Text(
                                    text = "🕒 ${report.createdAtLabel}",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Badge trạng thái và độ tin cậy
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val statusColors = detailStatusColors(report.status)

                            Text(
                                text = detailStatusLabel(report.status),
                                modifier = Modifier
                                    .background(
                                        statusColors.first,
                                        RoundedCornerShape(5.dp)
                                    )
                                    .padding(
                                        horizontal = 7.dp,
                                        vertical = 5.dp
                                    ),
                                color = statusColors.second,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            val trustColors = detailTrustColors(report.trustLabel)

                            Text(
                                text = detailTrustLabel(report.trustLabel),
                                modifier = Modifier
                                    .background(
                                        trustColors.first,
                                        RoundedCornerShape(5.dp)
                                    )
                                    .padding(
                                        horizontal = 7.dp,
                                        vertical = 5.dp
                                    ),
                                color = trustColors.second,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp)
                        )

                        // Mô tả sự cố
                        Text(
                            text = report.description,
                            fontSize = 14.sp
                        )

                        Spacer(Modifier.height(12.dp))

                        // Thống kê Vote
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            VoteCount(
                                text = "👍 ${report.confirmCount} xác nhận",
                                color = CityGuardGreen,
                                modifier = Modifier.weight(1f)
                            )

                            VoteCount(
                                text = "👎 ${report.noLongerCount} phản đối",
                                color = CityGuardRed,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        // Nút xác nhận
                        VoteButton(
                            label = "Tôi cũng gặp tình trạng này",
                            selected = report.myVote == VoteType.CONFIRMED,
                            enabled = !state.isVoting,
                            primary = true,
                            onClick = {
                                onVote(VoteType.CONFIRMED)
                            }
                        )

                        Spacer(Modifier.height(8.dp))

                        // Nút phản đối
                        VoteButton(
                            label = "Không còn nữa",
                            selected = report.myVote == VoteType.NO_LONGER,
                            enabled = !state.isVoting,
                            primary = false,
                            onClick = {
                                onVote(VoteType.NO_LONGER)
                            }
                        )

                        // Hiển thị lỗi
                        state.errorMessage?.let { error ->
                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = "Bình luận cộng đồng",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Danh sách comment
                if (report.comments.isEmpty()) {
                    item {
                        Text(
                            text = "Chưa có bình luận",
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    items(
                        items = report.comments,
                        key = { comment -> comment.id }
                    ) { comment ->
                        CommentCard(comment)
                    }
                }

                // Ô nhập comment
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Avatar("Bạn")

                        OutlinedTextField(
                            value = state.commentText,
                            onValueChange = onCommentChanged,
                            placeholder = {
                                Text("Thêm bình luận...")
                            },
                            isError = state.commentError != null,
                            supportingText = {
                                state.commentError?.let { error ->
                                    Text(error)
                                }
                            },
                            enabled = !state.isSendingComment,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.weight(1f),
                            trailingIcon = {
                                IconButton(
                                    onClick = onSendComment,
                                    enabled = !state.isSendingComment
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Gửi"
                                    )
                                }
                            }
                        )
                    }

                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }
}

// Ô hiển thị số Vote
@Composable
private fun VoteCount(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .border(
                1.dp,
                color,
                RoundedCornerShape(10.dp)
            )
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp
        )
    }
}

// Nút Vote
@Composable
private fun VoteButton(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    primary: Boolean,
    onClick: () -> Unit
) {
    val buttonText = if (selected) "✓ $label" else label
    val buttonModifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
    val shape = RoundedCornerShape(12.dp)

    if (primary) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = buttonModifier,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selected) {
                    Color(0xFFFFE082)
                } else {
                    CityGuardYellow
                },
                contentColor = Color.Black
            )
        ) {
            Text(
                text = buttonText,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = buttonModifier,
            shape = shape,
            border = BorderStroke(1.dp, CityGuardYellow),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (selected) {
                    Color(0xFFFFF3C4)
                } else {
                    Color.White
                },
                contentColor = Color.Black
            )
        ) {
            Text(
                text = buttonText,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

// Card comment
@Composable
private fun CommentCard(comment: Comment) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = BorderStroke(1.dp, Color(0xFFCCCCCC))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Avatar(comment.authorName)

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = comment.authorName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    Spacer(Modifier.width(6.dp))

                    Text(
                        text = comment.createdAtLabel,
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }

                Text(
                    text = comment.content,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// Avatar
@Composable
private fun Avatar(name: String) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(CityGuardYellow, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.firstOrNull()?.uppercase() ?: "?",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

// Mapping Trust Score từ Backend
private fun detailTrustLabel(label: String): String = when (label.uppercase()) {
    "HIGH", "CAO" -> "Độ tin cậy: Cao"
    "LOW", "THẤP" -> "Độ tin cậy: Thấp"
    "MEDIUM", "TRUNG BÌNH" -> "Độ tin cậy: Trung bình"
    "PENDING_VERIFY", "PENDING", "CHỜ XÁC MINH" -> "Độ tin cậy: Chờ xác minh"
    else -> "Độ tin cậy: $label"
}

// Màu Trust Score
private fun detailTrustColors(label: String): Pair<Color, Color> = when (label.uppercase()) {
    "HIGH", "CAO" ->
        Pair(Color(0xFFD6F3D8), CityGuardGreen)

    "LOW", "THẤP" ->
        Pair(Color(0xFFFFD9D9), CityGuardRed)

    "MEDIUM", "TRUNG BÌNH" ->
        Pair(Color(0xFFFFF3C4), CityGuardDarkYellow)

    else ->
        Pair(Color(0xFFFFF3C4), CityGuardDarkYellow)
}

// Mapping trạng thái
private fun detailStatusLabel(status: String): String = when (status.uppercase()) {
    "PENDING_VERIFY" -> "Đang chờ xác minh"
    "ACTIVE" -> "Đang hoạt động"
    "EXPIRED" -> "Đã hết hạn"
    "RESOLVED" -> "Đã xử lý"
    "HIDDEN" -> "Đã ẩn"
    else -> status
}

// Màu trạng thái
private fun detailStatusColors(status: String): Pair<Color, Color> = when (status.uppercase()) {
    "ACTIVE", "RESOLVED" ->
        Pair(Color(0xFFD6F3D8), CityGuardGreen)

    "EXPIRED", "HIDDEN" ->
        Pair(Color(0xFFFFD9D9), CityGuardRed)

    else ->
        Pair(Color(0xFFFFF3C4), CityGuardDarkYellow)
}

// ----- Preview -----
private val sampleReport = Report(
    id = "R001",
    category = IncidentCategory.TRAFFIC_JAM,
    description = "Kẹt xe kéo dài từ ngã tư đến cầu. Các phương tiện lưu thông chậm.",
    address = "Xô Viết Nghệ Tĩnh, Bình Thạnh",
    createdAtLabel = "5 phút trước",
    trustLabel = "HIGH",
    confirmCount = 9,
    noLongerCount = 1,
    myVote = VoteType.CONFIRMED,
    comments = listOf(
        Comment(
            "C001",
            "Nguyễn Văn A",
            "Đang kẹt xe tại đây, vẫn còn đông.",
            "1 phút trước"
        )
    )
)

@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 780,
    name = "Có dữ liệu"
)
@Composable
private fun ReportDetailContentPreview() {
    CityGuardAppTheme {
        ReportDetailScreen(
            state = ReportDetailUiState(
                isLoading = false,
                report = sampleReport
            ),
            onBack = {},
            onVote = {},
            onCommentChanged = {},
            onSendComment = {}
        )
    }
}

@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 780,
    name = "Đang tải"
)
@Composable
private fun ReportDetailLoadingPreview() {
    CityGuardAppTheme {
        ReportDetailScreen(
            state = ReportDetailUiState(isLoading = true),
            onBack = {},
            onVote = {},
            onCommentChanged = {},
            onSendComment = {}
        )
    }
}

@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 780,
    name = "Không tìm thấy"
)
@Composable
private fun ReportDetailNotFoundPreview() {
    CityGuardAppTheme {
        ReportDetailScreen(
            state = ReportDetailUiState(
                isLoading = false,
                notFound = true
            ),
            onBack = {},
            onVote = {},
            onCommentChanged = {},
            onSendComment = {}
        )
    }
}
