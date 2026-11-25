package com.carbonprint.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carbonprint.app.domain.model.TransportMode
import com.carbonprint.app.ui.theme.EmeraldPrimary
import com.carbonprint.app.ui.theme.MintSecondary
import com.carbonprint.app.ui.theme.TextMuted
import com.carbonprint.app.ui.theme.TextPrimary
import com.carbonprint.app.ui.theme.TextSecondary

private val ModeColors = mapOf(
    TransportMode.WALKING to Color(0xFF4CAF50),
    TransportMode.RUNNING to Color(0xFF2E7D32),
    TransportMode.ON_BICYCLE to Color(0xFF2EB886),
    TransportMode.TRANSIT to Color(0xFF0288D1),
    TransportMode.IN_VEHICLE to Color(0xFFE53935),
    TransportMode.STILL to Color(0xFF9E9E9E),
    TransportMode.UNKNOWN to Color(0xFFBDBDBD)
)

@Composable
fun ModeDonutChart(
    modeDistances: Map<TransportMode, Double>,
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
                text = "Transport Mode Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Distance split by travel activity",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))

            val totalDistance = modeDistances.values.sum()

            if (totalDistance <= 0.0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No distance recorded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Donut Chart
                    Box(
                        modifier = Modifier.size(130.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(110.dp)) {
                            var startAngle = -90f
                            val strokeWidth = 24.dp.toPx()

                            modeDistances.forEach { (mode, distance) ->
                                if (distance > 0) {
                                    val sweep = (distance / totalDistance).toFloat() * 360f
                                    val color = ModeColors[mode] ?: EmeraldPrimary
                                    drawArc(
                                        color = color,
                                        startAngle = startAngle,
                                        sweepAngle = sweep,
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                    )
                                    startAngle += sweep
                                }
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format("%.1f", totalDistance / 1000.0),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "km total",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Legend Column
                    Column(modifier = Modifier.weight(1f)) {
                        modeDistances.filter { it.value > 0 }.forEach { (mode, distance) ->
                            val color = ModeColors[mode] ?: EmeraldPrimary
                            val pct = (distance / totalDistance * 100).toInt()
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 3.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(8.dp),
                                    shape = CircleShape,
                                    color = color
                                ) {}
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = mode.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "$pct%",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
