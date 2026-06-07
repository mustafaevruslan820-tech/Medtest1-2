package com.example.medtest1.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.medtest1.R

@Composable
fun HomeExpandableFolder(
    title: String,
    subtitle: String,
    icon: ImageVector = Icons.Filled.Folder,
    accent: Color,
    onAccent: Color,
    surfaceColor: Color,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "folder-chevron"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(tween(320, easing = FastOutSlowInEasing)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = accent.copy(alpha = 0.22f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(26.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = onAccent
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = onAccent.copy(alpha = 0.78f)
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Свернуть" else "Развернуть",
                    tint = accent,
                    modifier = Modifier.rotate(chevronRotation)
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(220)) + expandVertically(tween(280)),
                exit = fadeOut(tween(180)) + shrinkVertically(tween(220))
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = onAccent.copy(alpha = 0.12f))
                    content()
                }
            }
        }
    }
}

@Composable
fun MedScanIconButton(
    onClick: () -> Unit,
    enabled: Boolean,
    isBusy: Boolean,
    modifier: Modifier = Modifier
) {
    val pulse = rememberInfiniteTransition(label = "scan-pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = if (isBusy) 1f else 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan-scale"
    )

    Box(
        modifier = modifier
            .size(80.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(enabled = enabled && !isBusy, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_scanner_camera),
            contentDescription = if (isBusy) "Сканирование" else "Сканировать штрихкод",
            tint = Color.Unspecified,
            modifier = Modifier.fillMaxSize()
        )
        if (isBusy) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.5.dp,
                color = Color(0xFF14B8A6)
            )
        }
    }
}

@Composable
fun HomeBottomNavBar(
    selectedTab: String,
    onTreatment: () -> Unit,
    onDiary: () -> Unit,
    onNext: () -> Unit,
    planTabModifier: Modifier = Modifier,
    diaryTabModifier: Modifier = Modifier,
    nextTabModifier: Modifier = Modifier,
    modifier: Modifier = Modifier
) {
    val barBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F4C47), Color(0xFF0C3D4A))
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = Color.Transparent,
            shadowElevation = 12.dp,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(barBrush)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HomeBottomNavItem(
                    label = "Лечение",
                    icon = Icons.Filled.MedicalServices,
                    selected = selectedTab == "plan",
                    onClick = onTreatment,
                    modifier = Modifier
                        .weight(1f)
                        .then(planTabModifier)
                )
                HomeBottomNavItem(
                    label = "Дневник",
                    icon = Icons.Filled.EditNote,
                    selected = selectedTab == "diary",
                    onClick = onDiary,
                    modifier = Modifier
                        .weight(1f)
                        .then(diaryTabModifier)
                )
                HomeBottomNavItem(
                    label = "Далее",
                    icon = Icons.Filled.EventAvailable,
                    selected = selectedTab == "next",
                    onClick = onNext,
                    modifier = Modifier
                        .weight(1f)
                        .then(nextTabModifier)
                )
            }
        }
    }
}

@Composable
private fun HomeBottomNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) Color(0xFF14B8A6).copy(alpha = 0.28f) else Color.Transparent,
        animationSpec = tween(260),
        label = "nav-bg"
    )
    val contentColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) Color(0xFFCCFBF1) else Color(0xFF94A3B8),
        animationSpec = tween(260),
        label = "nav-fg"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 380f),
        label = "nav-scale"
    )

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) Color(0xFF5EEAD4) else contentColor,
            modifier = Modifier.size(26.dp)
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor,
            maxLines = 1
        )
        if (selected) {
            Box(
                Modifier
                    .size(width = 28.dp, height = 3.dp)
                    .background(Color(0xFF5EEAD4), RoundedCornerShape(2.dp))
            )
        }
    }
}
