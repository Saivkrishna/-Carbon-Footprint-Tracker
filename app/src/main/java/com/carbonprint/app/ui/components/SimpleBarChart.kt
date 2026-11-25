package com.carbonprint.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carbonprint.app.ui.theme.EmeraldPrimary
import com.carbonprint.app.ui.theme.MintLight
import com.carbonprint.app.ui.theme.TextMuted
import com.carbonprint.app.ui.theme.TextPrimary
import com.carbonprint.app.ui.theme.TextSecondary

data class BarData(
    val label: String, // e.g. "Mon", "Tue"
    val value: Float   // CO2 in grams or kg
)

@Composable
fun SimpleBarChart(
    title: String,
    subtitle: String,
    data: List<BarData>,
    unit: String = "g",
    barColor: Color = EmeraldPrimary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (data.isEmpty() || data.all { it.value == 0f }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No emission records for this period.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            } else {
                val maxValue = (data.maxOfOrNull { it.value } ?: 1f).coerceAtLeast(10f)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    val barCount = data.size
                    val spacing = 16.dp.toPx()
                    val totalWidth = size.width
                    val availableBarWidth = (totalWidth - (spacing * (barCount + 1))) / barCount
                    val barWidth = availableBarWidth.coerceIn(16.dp.toPx(), 40.dp.toPx())
                    val chartHeight = size.height - 30.dp.toPx()

                    data.forEachIndexed { index, item ->
                        val left = spacing + index * (barWidth + spacing)
                        val barHeight = (item.value / maxValue) * chartHeight
                        val top = chartHeight - barHeight

                        // Draw background track
                        drawRoundRect(
                            color = MintLight,
                            topLeft = Offset(left, 0f),
                            size = Size(barWidth, chartHeight),
                            cornerRadius = CornerRadius(8f, 8f)
                        )

                        // Draw actual data bar
                        if (barHeight > 0) {
                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(left, top),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(8f, 8f)
                            )
                        }

                        // Draw label text below bar
                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint().apply {
                                color = android.graphics.Color.GRAY
                                textSize = 26f
                                textAlign = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                            }
                            drawText(
                                item.label,
                                left + barWidth / 2,
                                size.height - 4f,
                                paint
                            )
                        }
                    }
                }
            }
        }
    }
}
