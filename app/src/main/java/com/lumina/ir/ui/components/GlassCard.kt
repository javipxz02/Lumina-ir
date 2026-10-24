package com.lumina.ir.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lumina.ir.ui.theme.BorderSubtle
import com.lumina.ir.ui.theme.SurfaceDarkFrosted

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    glowColor: Color = Color.Transparent,
    content: @Composable BoxScope.() -> Unit
) {
    val animatedGlow by animateColorAsState(
        targetValue = glowColor.copy(alpha = 0.25f),
        animationSpec = tween(400),
        label = "glowAnim"
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = if (glowColor != Color.Transparent) 16.dp else 4.dp,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = animatedGlow,
                spotColor = animatedGlow
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(SurfaceDarkFrosted)
            .border(
                BorderStroke(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            if (glowColor != Color.Transparent) glowColor.copy(alpha = 0.4f) else BorderSubtle.copy(alpha = 0.6f),
                            BorderSubtle.copy(alpha = 0.15f)
                        )
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .padding(16.dp),
        content = content
    )
}
