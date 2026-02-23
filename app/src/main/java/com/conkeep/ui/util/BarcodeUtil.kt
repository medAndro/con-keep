package com.conkeep.ui.util

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import com.google.zxing.common.BitMatrix

/**
 * ZXing의 BitMatrix를 Compose에서 사용 가능한 ImageBitmap으로 변환합니다.
 */
fun BitMatrix.toImageBitmap(): ImageBitmap {
    val width = this.width
    val height = this.height
    val pixels = IntArray(width * height)

    for (y in 0 until height) {
        val offset = y * width
        for (x in 0 until width) {
            // true면 검정색(0xFF000000), false면 흰색(0xFFFFFFFF)
            pixels[offset + x] = if (this[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
    }

    val bitmap = createBitmap(width, height)
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return bitmap.asImageBitmap()
}
