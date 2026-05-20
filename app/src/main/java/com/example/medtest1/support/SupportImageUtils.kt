package com.example.medtest1.support

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlin.math.max

object SupportImageUtils {
    private const val MAX_EDGE_PX = 1280
    private const val MAX_BYTES = 900_000

    fun encodeImageForUpload(context: Context, uri: Uri): Pair<String, String>? {
        val bytes = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull() ?: return null
        if (bytes.isEmpty()) return null

        var bitmap = decodeBitmap(bytes, context, uri) ?: return null
        bitmap = scaleDown(bitmap, MAX_EDGE_PX)
        val encoded = compressToJpegBase64(bitmap)
        if (!bitmap.isRecycled) bitmap.recycle()
        return encoded
    }

    private fun decodeBitmap(bytes: ByteArray, context: Context, uri: Uri): Bitmap? {
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)?.let { return it }
            }
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { return it }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                return ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = false
                }
            }
            runCatching {
                val source = ImageDecoder.createSource(ByteBuffer.wrap(bytes))
                return ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = false
                }
            }
        }
        return null
    }

    private fun scaleDown(source: Bitmap, maxEdge: Int): Bitmap {
        val w = source.width
        val h = source.height
        val longest = max(w, h)
        if (longest <= maxEdge) return source
        val scale = maxEdge.toFloat() / longest.toFloat()
        val nw = max(1, (w * scale).toInt())
        val nh = max(1, (h * scale).toInt())
        val scaled = Bitmap.createScaledBitmap(source, nw, nh, true)
        if (scaled !== source) source.recycle()
        return scaled
    }

    private fun compressToJpegBase64(bitmap: Bitmap): Pair<String, String>? {
        var quality = 82
        var bestBytes: ByteArray? = null
        while (quality >= 30) {
            val stream = ByteArrayOutputStream()
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)) {
                quality -= 10
                continue
            }
            val bytes = stream.toByteArray()
            if (bytes.isEmpty()) {
                quality -= 10
                continue
            }
            bestBytes = bytes
            if (bytes.size <= MAX_BYTES) {
                return Base64.encodeToString(bytes, Base64.NO_WRAP) to "image/jpeg"
            }
            quality -= 10
        }
        val fallback = bestBytes ?: return null
        if (fallback.size > 1_050_000) return null
        return Base64.encodeToString(fallback, Base64.NO_WRAP) to "image/jpeg"
    }
}
