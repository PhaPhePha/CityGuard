package com.hoangpha.cityguard.presentation.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.hoangpha.cityguard.domain.model.Comment
import com.hoangpha.cityguard.domain.model.IncidentCategory
import com.hoangpha.cityguard.domain.model.Report
import com.hoangpha.cityguard.domain.model.VoteType
import com.hoangpha.cityguard.ui.theme.CityGuardAppTheme

// "Route": nối ViewModel với màn hình
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

// "Screen": chỉ vẽ theo state
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
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        state.notFound || report == null -> {
            Column(
                modifier = modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Không tìm thấy báo cáo", style = MaterialTheme.typography.titleMedium)
                Button(onClick = onBack) { Text("Quay lại") }
            }
        }

        else -> {
            LazyColumn(
                modifier = modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    TextButton(onClick = onBack) { Text("← Quay lại") }
                }
                report.imageUri?.let { uri ->
                    item {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Ảnh sự cố",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                item {
                    Text(report.category.displayName, style = MaterialTheme.typography.headlineSmall)
                    Text(report.address)
                    Text(report.createdAtLabel, style = MaterialTheme.typography.bodySmall)
                    Text("Độ tin cậy: ${report.trustLabel}")
                    Spacer(Modifier.height(8.dp))
                    Text(report.description)
                    Spacer(Modifier.height(8.dp))
                    Text("👍 ${report.confirmCount} xác nhận    👎 ${report.noLongerCount} phản đối")
                }
                item {
                    VoteButton(
                        label = "Tôi cũng gặp tình trạng này",
                        selected = report.myVote == VoteType.CONFIRMED,
                        enabled = !state.isVoting,
                        onClick = { onVote(VoteType.CONFIRMED) }
                    )
                    Spacer(Modifier.height(4.dp))
                    VoteButton(
                        label = "Không còn nữa",
                        selected = report.myVote == VoteType.NO_LONGER,
                        enabled = !state.isVoting,
                        onClick = { onVote(VoteType.NO_LONGER) }
                    )
                }
                state.errorMessage?.let {
                    item { Text(it, color = MaterialTheme.colorScheme.error) }
                }
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("Bình luận cộng đồng", style = MaterialTheme.typography.titleMedium)
                }
                if (report.comments.isEmpty()) {
                    item { Text("Chưa có bình luận") }
                } else {
                    items(report.comments, key = { it.id }) { comment ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(8.dp)) {
                                Text(
                                    "${comment.authorName} · ${comment.createdAtLabel}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(comment.content)
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = state.commentText,
                        onValueChange = onCommentChanged,
                        label = { Text("Thêm bình luận...") },
                        isError = state.commentError != null,
                        supportingText = { state.commentError?.let { Text(it) } },
                        enabled = !state.isSendingComment,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = onSendComment, enabled = !state.isSendingComment) {
                            Text(if (state.isSendingComment) "Đang gửi..." else "Gửi")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoteButton(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text("✓ $label")
        }
    } else {
        OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text(label)
        }
    }
}

// ----- Xem thử trong Android Studio -----
private val sampleReport = Report(
    id = "R001",
    category = IncidentCategory.TRAFFIC_JAM,
    description = "Kẹt xe kéo dài từ ngã tư đến cầu. Các phương tiện lưu thông chậm.",
    address = "Xô Viết Nghệ Tĩnh, Bình Thạnh",
    createdAtLabel = "5 phút trước",
    trustLabel = "Cao",
    confirmCount = 9,
    noLongerCount = 1,
    myVote = VoteType.CONFIRMED,
    comments = listOf(Comment("C001", "Nguyễn Văn A", "Đang kẹt xe tại đây, vẫn còn đông.", "1 phút trước"))
)

@Preview(showBackground = true, widthDp = 360, heightDp = 780, name = "Có dữ liệu")
@Composable
private fun ReportDetailContentPreview() {
    CityGuardAppTheme {
        ReportDetailScreen(
            state = ReportDetailUiState(isLoading = false, report = sampleReport),
            onBack = {}, onVote = {}, onCommentChanged = {}, onSendComment = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780, name = "Đang tải")
@Composable
private fun ReportDetailLoadingPreview() {
    CityGuardAppTheme {
        ReportDetailScreen(
            state = ReportDetailUiState(isLoading = true),
            onBack = {}, onVote = {}, onCommentChanged = {}, onSendComment = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780, name = "Không tìm thấy")
@Composable
private fun ReportDetailNotFoundPreview() {
    CityGuardAppTheme {
        ReportDetailScreen(
            state = ReportDetailUiState(isLoading = false, notFound = true),
            onBack = {}, onVote = {}, onCommentChanged = {}, onSendComment = {}
        )
    }
}