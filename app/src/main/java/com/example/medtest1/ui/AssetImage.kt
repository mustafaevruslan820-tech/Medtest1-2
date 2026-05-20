package com.example.medtest1.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

fun assetImageModel(fileName: String): String =
    "file:///android_asset/images/$fileName"

@Composable
fun AssetImage(
    fileName: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    AsyncImage(
        model = assetImageModel(fileName),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}
