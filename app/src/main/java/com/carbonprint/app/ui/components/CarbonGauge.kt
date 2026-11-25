package com.carbonprint.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carbonprint.app.ui.theme.AccentAmber
import com.carbonprint.app.ui.theme.AccentRed
import com.carbonprint.app.ui.theme.EmeraldPrimary
import com.carbonprint.app.ui.theme.MintLight
import com.carbonprint.app.ui.theme.TextMuted
import com.carbonprint.app.ui.theme.TextPrimary
import com.carbonprint.app.ui.theme.TextSecondary

@Composable
fun CarbonGauge(
    currentKg: Double,
    targetKg: Double,
    modifier: Modifier = Modifier
) {
    val progress = if (targetKg > 0.0) (currentKg / targetKg).toFloat().coerceIn(0f, 1f) else 0f
    val ratio = if (targetKg > 0.0) currentKg / targetKg else 0.0
    val progressColor = when {
        ratio >= 1.0 -> AccentRed
        ratio >= 0.8 -> AccentAmber
        else -> EmeraldPrimary
    }
    val remainingKg = (targetKg - currentKg).coerceAtLeast(0.0)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Weekly Carbon Budget",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${String.format("%.1f", currentKg)} / ${String.format("%.1f", targetKg)} kg",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = progressColor
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = progressColor,
                trackColor = MintLight,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (ratio >= 1.0) "Budget limit exceeded" else "${String.format("%.1f", remainingKg)} kg remaining",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (ratio >= 1.0) AccentRed else TextSecondary
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${String.format("%.0f", ratio * 100)}% used",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = progressColor
                )
            }
        }
    }
}
