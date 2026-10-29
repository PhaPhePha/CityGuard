package com.cityguard.app.presentation.map

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cityguard.app.data.location.AndroidLocationProvider
import com.cityguard.app.domain.location.LocationResult

@Composable
fun MapRoute(
    modifier: Modifier = Modifier,
    onOpenProfile: (() -> Unit)? = null,
    onOpenReportDetails: ((String) -> Unit)? = null,
    mapViewModel: MapViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val locationProvider = remember(context.applicationContext) {
        AndroidLocationProvider(context.applicationContext)
    }

    val locationViewModel: LocationViewModel = viewModel(
        factory = remember(locationProvider) {
            LocationViewModel.factory(locationProvider)
        }
    )

    var hasLocationPermission by remember {
        mutableStateOf(context.hasLocationPermission())
    }

    var showPermissionExplanation by remember {
        mutableStateOf(false)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        hasLocationPermission = granted

        if (granted) {
            locationViewModel.locate()
        } else {
            locationViewModel.onPermissionDenied()
        }
    }

    // Kiểm tra lại quyền sau khi người dùng quay về từ Settings.
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = context.hasLocationPermission()
                hasLocationPermission = granted

                val current = locationViewModel.uiState
                if (!granted &&
                    current is LocationUiState.Finished &&
                    current.result is LocationResult.Success
                ) {
                    locationViewModel.onPermissionDenied()
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    MapScreen(
        uiState = mapViewModel.uiState,
        modifier = modifier,
        onOpenProfile = onOpenProfile,
        onOpenReportDetails = onOpenReportDetails,
        onSelectReport = mapViewModel::selectReport,
        locationUiState = locationViewModel.uiState,
        hasLocationPermission = hasLocationPermission,
        onLocationClick = {
            if (context.hasLocationPermission()) {
                hasLocationPermission = true
                locationViewModel.locate()
            } else {
                showPermissionExplanation = true
            }
        },
        onOpenLocationSettings = {
            context.startActivity(
                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            )
        },
        onOpenAppSettings = {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}")
            )
            context.startActivity(intent)
        }
    )

    if (showPermissionExplanation) {
        AlertDialog(
            onDismissRequest = {
                showPermissionExplanation = false
            },
            title = {
                Text("Cho phép sử dụng vị trí?")
            },
            text = {
                Text(
                    "CityGuard sử dụng vị trí khi bạn yêu cầu " +
                            "để tìm bạn trên bản đồ. Bạn vẫn có thể " +
                            "xem và kéo bản đồ nếu không cấp quyền."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionExplanation = false
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                ) {
                    Text("Tiếp tục")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPermissionExplanation = false
                    }
                ) {
                    Text("Không cần")
                }
            }
        )
    }
}

private fun Context.hasLocationPermission(): Boolean {
    val fineGranted = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val coarseGranted = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    return fineGranted || coarseGranted
}
