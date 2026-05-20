package com.example.medtest1.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.CircleShape

/**
 * Logo on the welcome screen only — [app_brand_logo.png] (photo 2: medicine illustration).
 * Never uses the install icon.
 */
@Composable
fun WelcomeLogo(
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hasAsset = remember {
        runCatching {
            context.assets.open("images/app_brand_logo.png").close()
            true
        }.getOrDefault(false)
    }
    if (hasAsset) {
        AssetImage(
            fileName = "app_brand_logo.png",
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier.clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Добавьте\napp_brand_logo.png",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
