package com.hoangpha.cityguard.presentation.report

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File

// Tạo một file ảnh trống trong thư mục riêng của app và trả về địa chỉ (Uri)
// để app camera ghi ảnh vào. Thư mục này khai báo trong res/xml/file_paths.xml.
fun createCameraImageUri(context: Context): Uri {
    val directory = File(
        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
        "CityGuard"
    )
    if (!directory.exists()) {
        directory.mkdirs()
    }
    val file = File.createTempFile("REPORT_", ".jpg", directory)

    // Authority phải khớp AndroidManifest.xml: ${applicationId}.fileprovider
    // context.packageName trả về applicationId, nên không bị lệch khi namespace khác
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}