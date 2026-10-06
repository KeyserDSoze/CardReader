package com.keyserdsoze.cardreader.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.keyserdsoze.cardreader.model.CodeFormat

@Composable
fun BarcodeImage(value: String, format: CodeFormat, modifier: Modifier = Modifier) {
    val bitmap = remember(value, format) { runCatching { renderBarcode(value, format) }.getOrNull() }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = format.label,
            contentScale = ContentScale.Fit,
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp, max = 360.dp)
                .background(Color.White)
                .padding(16.dp),
        )
    }
}

private fun renderBarcode(value: String, format: CodeFormat): Bitmap {
    val square = format in setOf(CodeFormat.QR_CODE, CodeFormat.DATA_MATRIX, CodeFormat.AZTEC)
    val width = if (square) 900 else 1400
    val height = if (square) 900 else if (format == CodeFormat.PDF_417) 600 else 360
    val matrix = MultiFormatWriter().encode(
        value,
        BarcodeFormats.toZxing(format),
        width,
        height,
        mapOf(EncodeHintType.MARGIN to 2),
    )
    val pixels = IntArray(width * height)
    for (y in 0 until height) {
        for (x in 0 until width) pixels[y * width + x] = if (matrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
    }
    return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
}
