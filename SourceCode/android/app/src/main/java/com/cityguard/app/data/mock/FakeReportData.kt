package com.cityguard.app.data.mock

import com.cityguard.app.domain.model.IncidentCategory
import com.cityguard.app.domain.model.ReportSummary
object FakeReportData {

    val reports = listOf(
        ReportSummary(
            "R001",
            "Ùn tắc giờ cao điểm",
            "Các phương tiện đang di chuyển chậm.",
            IncidentCategory.TRAFFIC_JAM,
            "Khu vực A",
            10.775, 106.700
        ),
        ReportSummary(
            "R002",
            "Ổ gà trên mặt đường",
            "Mặt đường xuống cấp.",
            IncidentCategory.ROAD_DAMAGE,
            "Khu vực B",
            10.785, 106.710
        ),
        ReportSummary(
            "R003",
            "Ngập nước sau mưa",
            "Nước ngập gây khó khăn di chuyển.",
            IncidentCategory.FLOODING,
            "Khu vực C",
            10.800, 106.725
        ),
        ReportSummary(
            "R004",
            "Giao thông đông đúc",
            "Lưu lượng xe tăng cao.",
            IncidentCategory.TRAFFIC_JAM,
            "Khu vực D",
            10.805, 106.730
        ),
        ReportSummary(
            "R005",
            "Đường đang sửa chữa",
            "Công trình sửa chữa mặt đường.",
            IncidentCategory.ROAD_DAMAGE,
            "Khu vực E",
            10.810, 106.735
        ),
        ReportSummary(
            id = "R006",
            title = "Cây đổ chắn đường",
            description = "Cây lớn đổ gây cản trở giao thông.",
            category = IncidentCategory.ROAD_OBSTRUCTION,
            address = "Khu vực H",
            latitude = 10.795,
            longitude = 106.720
        )
    )
}