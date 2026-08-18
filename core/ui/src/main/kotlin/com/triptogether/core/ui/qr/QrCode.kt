package com.triptogether.core.ui.qr

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

private const val DEFAULT_QR_SIZE_PX = 512

/** Renders [payload] as a QR bitmap. Shared by invite and PromptPay dialogs. */
fun qrBitmap(
    payload: String,
    sizePx: Int = DEFAULT_QR_SIZE_PX,
): Bitmap {
    val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, sizePx, sizePx)
    val pixels =
        IntArray(sizePx * sizePx) { index ->
            val x = index % sizePx
            val y = index / sizePx
            if (matrix.get(x, y)) Color.BLACK else Color.WHITE
        }
    return Bitmap.createBitmap(pixels, sizePx, sizePx, Bitmap.Config.RGB_565)
}

@Composable
fun rememberQrBitmap(payload: String): Bitmap = remember(payload) { qrBitmap(payload) }
