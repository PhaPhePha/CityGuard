package com.hoangpha.cityguard.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoangpha.cityguard.data.RepositoryProvider
import com.hoangpha.cityguard.domain.model.Report
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MyReportsViewModel : ViewModel() {
    private val _reports = MutableStateFlow<List<Report>>(emptyList())
    val reports = _reports.asStateFlow()
    val error = MutableStateFlow<String?>(null)
    init { viewModelScope.launch { runCatching { RepositoryProvider.reportRepository.myReports() }.onSuccess { _reports.value = it }.onFailure { error.value = "Không tải được báo cáo của bạn" } } }
}
