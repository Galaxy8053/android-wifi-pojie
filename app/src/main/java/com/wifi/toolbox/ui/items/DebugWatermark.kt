package com.wifi.toolbox.ui.items

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.core.graphics.toColorInt
import com.wifi.toolbox.BuildConfig

@Composable
fun DebugWatermark() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.save()
            canvas.nativeCanvas.translate(drawContext.size.width, 0f)
            canvas.nativeCanvas.rotate(45f)
            val paint = android.graphics.Paint().apply { color = "#77FF0000".toColorInt() }
            canvas.nativeCanvas.drawRect(-200f, 90f, 200f, 170f, paint)
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 32f
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
            }
            canvas.nativeCanvas.drawText("DEBUG", 0f, 130f, textPaint)
            textPaint.textSize = 24f
            textPaint.isFakeBoldText = false
            canvas.nativeCanvas.drawText(
                "${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})_${BuildConfig.BUILD_COUNT}",
                0f,
                160f,
                textPaint
            )
            canvas.nativeCanvas.restore()
        }
    }
}