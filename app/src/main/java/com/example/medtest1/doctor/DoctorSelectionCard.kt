package com.example.medtest1.doctor

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.medtest1.network.DoctorProfile
import com.example.medtest1.ui.theme.LocalMedAppColors

@Composable
fun DoctorSelectionCard(
    doctor: DoctorProfile,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val app = LocalMedAppColors.current
    val scheme = MaterialTheme.colorScheme
    val specialtyLabel = formatDoctorSpecialtyLabel(doctor.specialty).ifBlank { "Врач" }
    val visual = specialtyVisual(doctor.specialty, scheme)
    val pulse = rememberInfiniteTransition(label = "doctor-card-pulse")
    val ringScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = if (doctor.onDuty) 1.06f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "doctor-ring-scale"
    )
    val ringAlpha by pulse.animateFloat(
        initialValue = 0.45f,
        targetValue = if (doctor.onDuty) 0.95f else 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "doctor-ring-alpha"
    )

    Card(
        modifier = modifier
            .width(168.dp)
            .clickable(onClick = onClick)
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    listOf(
                        visual.accent.copy(alpha = ringAlpha),
                        app.accentRing.copy(alpha = ringAlpha * 0.7f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = app.cardOnHero.copy(alpha = 0.92f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = visual.container.copy(alpha = 0.75f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = visual.icon,
                        contentDescription = null,
                        tint = visual.accent,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        specialtyLabel.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = visual.accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .scale(ringScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    visual.accent.copy(alpha = 0.35f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                DoctorAvatar(doctor = doctor, size = 68.dp)
            }
            Text(
                doctor.fullName.ifBlank { doctor.username },
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                color = app.onHero,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (doctor.onDuty) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(app.accentRing)
                    )
                    Text(
                        "На смене",
                        style = MaterialTheme.typography.labelSmall,
                        color = app.accentRing,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            if (doctor.experienceYears > 0) {
                Text(
                    "Стаж ${doctor.experienceYears} лет",
                    style = MaterialTheme.typography.labelSmall,
                    color = app.onHeroMuted
                )
            }
        }
    }
}
