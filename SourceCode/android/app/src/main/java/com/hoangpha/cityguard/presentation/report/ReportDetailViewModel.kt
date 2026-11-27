package com.hoangpha.cityguard.presentation.report

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoangpha.cityguard.data.RepositoryProvider
import com.hoangpha.cityguard.domain.model.VoteType
import com.hoangpha.cityguard.domain.repository.ReportRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

class ReportDetailViewModel @JvmOverloads constructor(
    savedStateHandle: SavedStateHandle,
    // Hôm nay là dữ liệu giả; khi có API chỉ đổi đối tượng này
    private val repository: ReportRepository = RepositoryProvider.reportRepository
) : ViewModel() {

    // ID do navigation truyền sang (route "report_detail/{reportId}")
    private val reportId: String = savedStateHandle["reportId"] ?: ""

    private val _uiState = MutableStateFlow(ReportDetailUiState())
    val uiState: StateFlow<ReportDetailUiState> = _uiState.asStateFlow()

    // TV2 - Thịnh, tích hợp TV4: My Reports mở detail khi mất mạng phải có error/retry thay vì crash.
    // Giữ nguyên nghiệp vụ vote/comment của TV4; chỉ quản lý lượt tải chi tiết ban đầu và thử lại.
    private var loadJob: Job? = null
    init { load() }

    fun load() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, notFound = false, errorMessage = null) }
        loadJob = viewModelScope.launch {
            try {
                repository.observeReport(reportId).collect { report ->
                    _uiState.update {
                        it.copy(isLoading = false, report = report, notFound = report == null, errorMessage = null)
                    }
                }
            } catch (e: CancellationException) {
                throw e // TV2: đóng màn hình/đổi request là hủy hợp lệ, không phải lỗi mạng.
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, report = null, notFound = false,
                        errorMessage = "Không tải được báo cáo. Kiểm tra kết nối rồi thử lại.")
                }
            }
        }
    }

    fun vote(type: VoteType) {
        if (_uiState.value.isVoting) return               // chặn bấm liên tục
        _uiState.update { it.copy(isVoting = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                repository.vote(reportId, type)
                repository.observeReport(reportId).first()?.let { updated ->
                    _uiState.update { it.copy(report = updated) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Không gửi được lựa chọn. Vui lòng thử lại.") }
            } finally {
                _uiState.update { it.copy(isVoting = false) }
            }
        }
    }

    fun onCommentChanged(text: String) {
        if (text.length <= ReportDetailUiState.MAX_COMMENT_LENGTH) {
            _uiState.update { it.copy(commentText = text, commentError = null) }
        }
    }

    fun sendComment() {
        val state = _uiState.value
        if (state.isSendingComment) return                // chặn gửi trùng
        val content = state.commentText.trim()
        if (content.isEmpty()) {
            _uiState.update { it.copy(commentError = "Bình luận không được để trống") }
            return
        }
        _uiState.update { it.copy(isSendingComment = true, commentError = null, errorMessage = null) }
        viewModelScope.launch {
            try {
                repository.addComment(reportId, content)
                repository.observeReport(reportId).first()?.let { updated ->
                    _uiState.update { it.copy(report = updated) }
                }
                _uiState.update { it.copy(commentText = "") }    // gửi xong mới xóa ô nhập
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Gửi bình luận thất bại. Vui lòng thử lại.") }
            } finally {
                _uiState.update { it.copy(isSendingComment = false) }
            }
        }
    }
}
