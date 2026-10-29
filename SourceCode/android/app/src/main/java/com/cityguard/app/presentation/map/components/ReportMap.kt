package com.cityguard.app.presentation.map.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cityguard.app.domain.model.ReportSummary
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.*
import com.cityguard.app.domain.location.DeviceLocation
import com.google.android.gms.maps.CameraUpdateFactory
@Composable
internal fun ReportMap(
    reports: List<ReportSummary>,
    selectedReportId: String?,
    onMarkerClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    deviceLocation: DeviceLocation? = null,
    locationRequestId: Int = 0,
    hasLocationPermission: Boolean = false
) {
    // Khởi tạo góc nhìn một lần cho state camera này.
    // Khu vực nằm gần các tọa độ demo R001–R008.
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(10.7925, 106.7175),
            13f
        )
    }

    var mapLoaded by remember { mutableStateOf(false) }
// LaunchedEffect thực hiện tác vụ coroutine trong Compose khi key thay đổi.
    LaunchedEffect(mapLoaded, locationRequestId) {
        if (
            mapLoaded &&
            locationRequestId > 0 &&
            deviceLocation != null
        ) {
            val target = LatLng(
                deviceLocation.latitude,
                deviceLocation.longitude
            )

            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    target,
                    16f
                ),
                800
            )
        }
    }


    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,

        properties = MapProperties(
            isMyLocationEnabled = hasLocationPermission
        ),
        onMapLoaded = {
            mapLoaded = true
        },
        uiSettings = MapUiSettings(
            scrollGesturesEnabled = true,
            zoomGesturesEnabled = true,
            zoomControlsEnabled = true,
            myLocationButtonEnabled = false,
            mapToolbarEnabled = false
        ),
        // Chừa khoảng cho nút và card đang đè lên map.
        // Dùng giá trị cố định để việc chọn card không đổi padding.
        contentPadding = PaddingValues(
            top = 64.dp,
            bottom = 240.dp
        )
    ) {
        reports.forEach { report ->
            key(report.id) {
                val selected = report.id == selectedReportId

                val markerState = rememberUpdatedMarkerState(
                    position = LatLng(
                        report.latitude,
                        report.longitude
                    )
                )

                MarkerComposable(
                    report.category,
                    selected,
                    state = markerState,
                    title = report.title,
                    contentDescription =
                        "${report.category.displayName}: ${report.title}",
                    anchor = Offset(0.5f, 0.5f),
                    zIndex = if (selected) 1f else 0f,
                    onClick = {
                        onMarkerClick(report.id)

                        // Đã xử lý click.
                        // Không chạy hành vi click mặc định của SDK.
                        true
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                report.category.markerColor(),
                                CircleShape
                            )
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) {
                                    Color.Black
                                } else {
                                    Color.White
                                },
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(
                                id = report.category.markerIconRes()
                            ),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}