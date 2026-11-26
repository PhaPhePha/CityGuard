package com.hoangpha.cityguard.presentation.map.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun MapStatusOverlay(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    isLoading: Boolean = false,
    onRetry: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .padding(24.dp)
            .widthIn(max = 320.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("map_loading")
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )

            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            if (onRetry != null && !isLoading) {
                Button(
                    onClick = onRetry,
                    modifier = Modifier.testTag("map_retry")
                ) {
                    Text("Thử lại")
                }
            }
        }
    }
}

@Composable
fun MapStatusBanner(
    message: String,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onRefresh: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.widthIn(max = 240.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )

                Spacer(Modifier.height(6.dp))
            }

            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )

            if (onRefresh != null && !isLoading) {
                TextButton(
                    onClick = onRefresh,
                    modifier = Modifier.testTag("map_refresh")
                ) {
                    Text("Tải lại")
                }
            }
        }
    }
}