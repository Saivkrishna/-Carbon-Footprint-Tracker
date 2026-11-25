package com.carbonprint.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carbonprint.app.domain.model.TransportMode
import com.carbonprint.app.ui.theme.EmeraldDark
import com.carbonprint.app.ui.theme.EmeraldPrimary
import com.carbonprint.app.ui.theme.MintLight

@Composable
fun ModeChip(
    mode: TransportMode,
    modifier: Modifier = Modifier,
    subMode: String? = null,
    backgroundColor: Color = MintLight,
    contentColor: Color = EmeraldDark
) {
    val isMotorcycle = subMode?.contains("MOTORCYCLE", ignoreCase = true) == true
    val isCar = mode == TransportMode.IN_VEHICLE && !isMotorcycle

    val icon: ImageVector = when {
        isMotorcycle -> Icons.Default.TwoWheeler
        mode == TransportMode.WALKING -> Icons.Default.DirectionsWalk
        mode == TransportMode.RUNNING -> Icons.Default.DirectionsRun
        mode == TransportMode.ON_BICYCLE -> Icons.Default.DirectionsBike
        isCar -> Icons.Default.DirectionsCar
        mode == TransportMode.TRANSIT -> Icons.Default.DirectionsBus
        mode == TransportMode.STILL -> Icons.Default.LocationOn
        else -> Icons.Default.HelpOutline
    }

    val labelText = when {
        isMotorcycle -> "Motorcycle"
        isCar -> "Car"
        mode == TransportMode.TRANSIT -> "Bus / Transit"
        else -> mode.displayName
    }

    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = labelText,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = labelText,
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
