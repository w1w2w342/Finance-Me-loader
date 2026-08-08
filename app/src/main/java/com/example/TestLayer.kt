package com.example
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent

@Composable
fun TestGraphicsLayer() {
    val layer = rememberGraphicsLayer()
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.drawWithContent {
            layer.record {
                this@drawWithContent.drawContent()
            }
            drawContent()
        }
    )
}
