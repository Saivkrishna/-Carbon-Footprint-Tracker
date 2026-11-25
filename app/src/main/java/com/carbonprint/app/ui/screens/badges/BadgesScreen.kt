package com.carbonprint.app.ui.screens.badges

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carbonprint.app.data.local.entity.BadgeEntity
import com.carbonprint.app.ui.theme.AccentAmber
import com.carbonprint.app.ui.theme.EmeraldDark
import com.carbonprint.app.ui.theme.EmeraldPrimary
import com.carbonprint.app.ui.theme.MintLight
import com.carbonprint.app.ui.theme.TextMuted
import com.carbonprint.app.ui.theme.TextPrimary
import com.carbonprint.app.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgesScreen(
    viewModel: BadgesViewModel,
    onBack: () -> Unit
) {
    val badges by viewModel.badges.collectAsState()
    val unlockedCount = badges.count { it.isUnlocked }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Eco Achievements", fontWeight = FontWeight.Bold, color = EmeraldDark) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Summary Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MintLight)
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(AccentAmber.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = AccentAmber,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "$unlockedCount of ${badges.size} Unlocked",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldDark
                            )
                            Text(
                                text = "Earn badges by walking, cycling and saving CO2",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            items(badges, key = { it.id }) { badge ->
                BadgeItemCard(badge = badge)
            }
        }
    }
}

@Composable
fun BadgeItemCard(badge: BadgeEntity) {
    val icon: ImageVector = when (badge.iconName) {
        "directions_walk" -> Icons.Default.DirectionsWalk
        "directions_bike" -> Icons.Default.DirectionsBike
        "eco" -> Icons.Default.Eco
        "forest" -> Icons.Default.Forest
        else -> Icons.Default.EmojiEvents
    }

    val progress = (badge.currentProgress / badge.targetProgress).toFloat().coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (badge.isUnlocked) MaterialTheme.colorScheme.surface else Color(0xFFF9FAF9)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (badge.isUnlocked) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        if (badge.isUnlocked) MintLight else Color(0xFFEEEEEE),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (badge.isUnlocked) icon else Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (badge.isUnlocked) EmeraldPrimary else TextMuted,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = badge.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (badge.isUnlocked) TextPrimary else TextMuted
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (badge.isUnlocked) {
                        Surface(
                            color = MintLight,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "UNLOCKED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = badge.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = EmeraldPrimary,
                    trackColor = MintLight
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row {
                    val progressLabel = if (badge.targetProgress >= 1000.0) {
                        "${String.format("%.1f", badge.currentProgress / 1000.0)} / ${String.format("%.0f", badge.targetProgress / 1000.0)} km"
                    } else {
                        "${String.format("%.0f", badge.currentProgress)} / ${String.format("%.0f", badge.targetProgress)}"
                    }
                    Text(
                        text = progressLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (badge.unlockedAt != null && badge.isUnlocked) {
                        val date = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(badge.unlockedAt))
                        Text(
                            text = "Achieved $date",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPrimary
                        )
                    }
                }
            }
        }
    }
}
