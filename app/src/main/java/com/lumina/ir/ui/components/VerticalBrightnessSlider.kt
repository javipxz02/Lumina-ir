package com.lumina.ir.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumina.ir.ui.theme.AccentAmber
import com.lumina.ir.ui.theme.BorderSubtle
import com.lumina.ir.ui.theme.SurfaceDarkElevated
import com.lumina.ir.ui.theme.TextMuted
import com.lumina.ir.ui.theme.TextPrimary

@Composable
fun VerticalBrightnessSlider(
    currentLevel: Int, // 1 a 8
    maxLevel: Int = 8,
    activeColor: Color = AccentAmber,
    onLevelChangeRequested: (targetLevel: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val fraction = (currentLevel.toFloat() / maxLevel.toFloat()).coerceIn(0.12f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(150),
        label = "brightnessAnim"
    )

    Column(
        modifier = modifier
            .width(68.dp)
            .height(240.dp)
            .clip(RoundedCornerShape(34.dp))
            .background(SurfaceDarkElevated)
            .border(1.dp, BorderSubtle, RoundedCornerShape(34.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val clickedFraction = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                    val newLevel = (clickedFraction * maxLevel).toInt().coerceIn(1, maxLevel)
                    if (newLevel != currentLevel) {
                        onLevelChangeRequested(newLevel)
                    }
                }
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, _ ->
                    change.consume()
                    val dragFraction = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                    val newLevel = (dragFraction * maxLevel).toInt().coerceIn(1, maxLevel)
                    if (newLevel != currentLevel) {
                        onLevelChangeRequested(newLevel)
                    }
                }
            },
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Contenedor visual del nivel de brillo lleno (sube desde abajo)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(animatedFraction)
                .clip(RoundedCornerShape(34.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            activeColor.copy(alpha = 0.9f),
                            activeColor.copy(alpha = 0.45f)
                        )
                    )
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Icon(
                    imageVector = if (currentLevel > 4) Icons.Default.BrightnessHigh else Icons.Default.BrightnessLow,
                    contentDescription = "Brillo",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )

                Text(
                    text = "${((currentLevel.toFloat() / maxLevel) * 100).toInt()}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
