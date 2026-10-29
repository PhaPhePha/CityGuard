
package com.cityguard.app.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.cityguard.app.domain.location.DeviceLocation
import com.cityguard.app.domain.location.LocationProvider
import com.cityguard.app.domain.location.LocationResult
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class AndroidLocationProvider(
    private val context: Context
) : LocationProvider {
// Tạo đối tượng truy cập dịch vụ vị trí
    private val locationClient =
        LocationServices.getFusedLocationProviderClient(context)
// Yêu cầu một lần lấy vị trí
    override suspend fun getCurrentLocation(): LocationResult {
        val fineGranted = hasPermission(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarseGranted = hasPermission(
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (!fineGranted && !coarseGranted) {
            return LocationResult.PermissionDenied
        }

        val locationManager =
            context.getSystemService(LocationManager::class.java)
// Kiểm tra dịch vụ vị trí toàn thiết bị đang bật
        if (!locationManager.isLocationEnabled) {
            return LocationResult.ServicesDisabled
        }

        val cancellationSource = CancellationTokenSource()

        return try {
            val request = CurrentLocationRequest.Builder()
                .setPriority(
                    if (fineGranted) {
                        Priority.PRIORITY_HIGH_ACCURACY
                    } else {
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY
                    }
                )
                .setGranularity(
                    Granularity.GRANULARITY_PERMISSION_LEVEL
                )
                .setMaxUpdateAgeMillis(15_000L)
                .setDurationMillis(15_000L)
                .build()

            val currentLocation = locationClient
                .getCurrentLocation(
                    request,
                    cancellationSource.token
                ).await()
// Lấy vị trí gần nhất được lưu trong cache, nếu co
            // await(): Chờ kết quả bất đồng bộ trong coroutine
            val location = currentLocation
                ?: locationClient.lastLocation.await()

            if (location == null) {
                LocationResult.Unavailable
            } else {
                // Kiểm tra tọa độ có quá cũ không
                classifyLocation(
                    location = location,
                    approximate = !fineGranted,
                    fromCache = currentLocation == null
                )
            }
        } catch (exception: SecurityException) {
            LocationResult.PermissionDenied
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            LocationResult.Failure(
                exception.message ?: "Không thể lấy vị trí"
            )
        } finally {
            cancellationSource.cancel()
        }
    }

    private fun classifyLocation(
        location: Location,
        approximate: Boolean,
        fromCache: Boolean
    ): LocationResult {
        val ageMillis =
            //Đọc thời gian tương đối của lần lấy tọa độ
            (SystemClock.elapsedRealtimeNanos() -
                    location.elapsedRealtimeNanos) / 1_000_000L

        // Chỉ chấp nhận vị trí không quá 2 phút.
        if (ageMillis < 0 || ageMillis > 120_000L) {
            return LocationResult.Stale
        }

        return LocationResult.Success(
            point = DeviceLocation(
                latitude = location.latitude,
                longitude = location.longitude
            ),
            approximate = approximate,
            fromCache = fromCache
        )
    }

    private fun hasPermission(permission: String): Boolean {
        // Kiểm tra quyền đã được cấp chưa
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
}

//