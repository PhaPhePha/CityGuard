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
            "Đường tạm thời bị chặn",
            "Có rào chắn trên đường.",
            IncidentCategory.ROAD_BLOCK,
            "Khu vực B",
            10.780, 106.705
        ),
        ReportSummary(
            "R003",
            "Ổ gà trên mặt đường",
            "Mặt đường xuống cấp.",
            IncidentCategory.ROAD_DAMAGE,
            "Khu vực C",
            10.785, 106.710
        ),
        ReportSummary(
            "R004",
            "Đèn tín hiệu không hoạt động",
            "Đèn giao thông đang gặp sự cố.",
            IncidentCategory.BROKEN_TRAFFIC_LIGHT,
            "Khu vực D",
            10.790, 106.715
        ),
        ReportSummary(
            "R005",
            "Cây đổ chắn đường",
            "Cành cây lớn chắn một phần đường.",
            IncidentCategory.FALLEN_TREE,
            "Khu vực E",
            10.795, 106.720
        ),
        ReportSummary(
            "R006",
            "Ngập nước sau mưa",
            "Nước ngập gây khó khăn di chuyển.",
            IncidentCategory.FLOODING,
            "Khu vực F",
            10.800, 106.725
        ),
        ReportSummary(
            "R007",
            "Giao thông đông đúc",
            "Lưu lượng xe tăng cao.",
            IncidentCategory.TRAFFIC_JAM,
            "Khu vực G",
            10.805, 106.730
        ),
        ReportSummary(
            "R008",
            "Đường đang sửa chữa",
            "Công trình sửa chữa mặt đường.",
            IncidentCategory.ROAD_DAMAGE,
            "Khu vực H",
            10.810, 106.735
        )
    )
}