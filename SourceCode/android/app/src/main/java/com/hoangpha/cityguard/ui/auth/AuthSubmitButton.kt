package com.hoangpha.cityguard.ui.auth
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hoangpha.cityguard.ui.theme.CityGuardBlack
import com.hoangpha.cityguard.ui.theme.CityGuardWhite

@Composable
internal fun AuthSubmitButton(
    text: String, loadingText: String, loading: Boolean, tag: String, onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !loading, // chặn gửi trùng, nhưng giữ nguyên vị trí/kích thước của nút.
        modifier = Modifier.fillMaxWidth().height(52.dp).testTag(tag)
            .semantics { stateDescription = if (loading) loadingText else "Sẵn sàng" },
        colors = ButtonDefaults.buttonColors(
            containerColor = CityGuardBlack, contentColor = CityGuardWhite,
            disabledContainerColor = CityGuardBlack.copy(alpha = 0.8f), disabledContentColor = CityGuardWhite
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(18.dp), color = CityGuardWhite, strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
        }
        Text(if (loading) loadingText else text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
