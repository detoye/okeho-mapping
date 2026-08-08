package com.okeho.mapping.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.okeho.mapping.domain.model.SyncStatus
import com.okeho.mapping.ui.theme.StatusFailed
import com.okeho.mapping.ui.theme.StatusGood
import com.okeho.mapping.ui.theme.StatusPending
import com.okeho.mapping.ui.theme.StatusSynced
import com.okeho.mapping.ui.theme.White

@Composable
fun SyncStatusBadge(status: SyncStatus, modifier: Modifier = Modifier) {
    val (backgroundColor, textColor) = when (status) {
        SyncStatus.PENDING -> StatusPending to Color.Black
        SyncStatus.SYNCED -> StatusSynced to White
        SyncStatus.FAILED -> StatusFailed to White
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
    }
}

@Composable
fun GpsAccuracyBadge(accuracy: Float, modifier: Modifier = Modifier) {
    val isGood = accuracy <= 15f
    val backgroundColor = if (isGood) StatusGood else StatusFailed
    val text = if (isGood) "Good (${accuracy.toInt()}m)" else "Poor (${accuracy.toInt()}m)"

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = White
        )
    }
}
