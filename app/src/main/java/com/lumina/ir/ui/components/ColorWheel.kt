package com.lumina.ir.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumina.ir.core.protocol.NecIrProtocol
import com.lumina.ir.ui.theme.BorderSubtle
import com.lumina.ir.ui.theme.SurfaceDarkElevated
import com.lumina.ir.ui.theme.TextMuted
import com.lumina.ir.ui.theme.TextPrimary
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ColorWheel(
    selectedColor: NecIrProtocol.LampColor,
    onColorSelected: (NecIrProtocol.LampColor) -> Unit,
    modifier: Modifier = Modifier
) {
    val chromaticColors = remember { NecIrProtocol.CHROMATIC_LAMP_COLORS.filter { !it.isWhite } }
    val whiteColor = remember { NecIrProtocol.CHROMATIC_LAMP_COLORS.first { it.isWhite } }

    val animatedSelectedColor by animateColorAsState(
        targetValue = selectedColor.previewColor,
        animationSpec = tween(250),
        label = "wheelColorAnim"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val dx = offset.x - center.x
                        val dy = offset.y - center.y
                        val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                        val radius = size.width / 2f

                        // Si el toque es en el centro, activar Blanco Neutro
                        if (distance < radius * 0.45f) {
                            onColorSelected(whiteColor)
                            return@detectTapGestures
                        }

                        // Calcular ángulo y hacer snap magnético al color físico más próximo
                        val angleRad = atan2(dy.toDouble(), dx.toDouble())
                        var degrees = Math.toDegrees(angleRad).toFloat()
                        if (degrees < 0) degrees += 360f

                        val snapped = NecIrProtocol.findClosestColorByAngle(degrees)
                        onColorSelected(snapped)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val dx = change.position.x - center.x
                        val dy = change.position.y - center.y
                        val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                        val radius = size.width / 2f

                        if (distance >= radius * 0.45f) {
                            val angleRad = atan2(dy.toDouble(), dx.toDouble())
                            var degrees = Math.toDegrees(angleRad).toFloat()
                            if (degrees < 0) degrees += 360f

                            val snapped = NecIrProtocol.findClosestColorByAngle(degrees)
                            if (snapped.id != selectedColor.id) {
                                onColorSelected(snapped)
                            }
                        }
                    }
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.width / 2f - 24.dp.toPx()
            val strokeWidth = 32.dp.toPx()

            // 1. Anillo de fondo con gradiente espectral continuo
            val sweepGradient = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFFFF1122), // 0°
                    Color(0xFFFF7B00), // 40°
                    Color(0xFFFFEA00), // 68°
                    Color(0xFF00E63D), // 120°
                    Color(0xFF00F0FF), // 195°
                    Color(0xFF1A38FF), // 240°
                    Color(0xFF8800FF), // 285°
                    Color(0xFFFF00A0), // 315°
                    Color(0xFFFF1122)  // 360°
                ),
                center = center
            )

            // Anillo base de color
            drawCircle(
                brush = sweepGradient,
                radius = outerRadius,
                center = center,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Sombra interna sutil
            drawCircle(
                color = Color.Black.copy(alpha = 0.35f),
                radius = outerRadius - (strokeWidth / 2f),
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // 2. Dibujar nodos magnéticos para cada uno de los 15 colores cromáticos reales
            for (color in chromaticColors) {
                val rad = Math.toRadians(color.chromaticAngle.toDouble())
                val nodeX = center.x + (outerRadius * cos(rad)).toFloat()
                val nodeY = center.y + (outerRadius * sin(rad)).toFloat()
                val isSelected = selectedColor.id == color.id

                // Halo / Glow para el nodo seleccionado
                if (isSelected) {
                    drawCircle(
                        color = Color.White,
                        radius = 16.dp.toPx(),
                        center = Offset(nodeX, nodeY),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }

                // Punto del nodo
                drawCircle(
                    color = color.previewColor,
                    radius = if (isSelected) 10.dp.toPx() else 6.dp.toPx(),
                    center = Offset(nodeX, nodeY)
                )
            }
        }

        // 3. Disco Central: Previsualizador real del color de hardware y Blanco Neutro
        Box(
            modifier = Modifier
                .size(150.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = CircleShape,
                    ambientColor = animatedSelectedColor.copy(alpha = 0.7f),
                    spotColor = animatedSelectedColor.copy(alpha = 0.9f)
                )
                .clip(CircleShape)
                .background(SurfaceDarkElevated)
                .border(2.dp, animatedSelectedColor.copy(alpha = 0.6f), CircleShape)
                .clickable { onColorSelected(whiteColor) },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(12.dp)
            ) {
                // Mini muestra circular
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(animatedSelectedColor)
                        .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = selectedColor.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1
                )

                Text(
                    text = "CMD 0x${selectedColor.command.toString(16).uppercase().padStart(2, '0')}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )

                if (selectedColor.isWhite) {
                    Text(
                        text = "● MODO BLANCO",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
