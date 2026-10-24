package com.lumina.ir.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumina.ir.core.ir.IrTransmitterManager
import com.lumina.ir.ui.theme.AccentDanger
import com.lumina.ir.ui.theme.AccentEmerald
import com.lumina.ir.ui.theme.AccentPrimary
import com.lumina.ir.ui.theme.BorderSubtle
import com.lumina.ir.ui.theme.SurfaceDarkElevated
import com.lumina.ir.ui.theme.TextMuted
import com.lumina.ir.ui.theme.TextPrimary
import com.lumina.ir.ui.theme.TextSecondary

@Composable
fun IrStatusHeader(
    deviceModel: String,
    irStatus: IrTransmitterManager.IrStatus,
    isLampOn: Boolean,
    onTogglePower: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isTransmitting = irStatus is IrTransmitterManager.IrStatus.Transmitting

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isTransmitting) 1.4f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val statusDotColor by animateColorAsState(
        targetValue = when (irStatus) {
            is IrTransmitterManager.IrStatus.Transmitting -> Color(0xFF00E5FF)
            is IrTransmitterManager.IrStatus.Ready -> AccentEmerald
            is IrTransmitterManager.IrStatus.MissingHardware -> AccentDanger
            is IrTransmitterManager.IrStatus.Error -> AccentDanger
        },
        label = "statusColor"
    )

    val statusText = when (irStatus) {
        is IrTransmitterManager.IrStatus.Transmitting -> "Transmitiendo NEC..."
        is IrTransmitterManager.IrStatus.Ready -> "Honor 200 Pro IR Activo"
        is IrTransmitterManager.IrStatus.MissingHardware -> "Modo Simulación (Sin IR)"
        is IrTransmitterManager.IrStatus.Error -> "Error de Puerto IR"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Indicador de Hardware IR
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceDarkElevated)
                .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(statusDotColor)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = null,
                        tint = statusDotColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "38 kHz NEC",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted
                    )
                }
                Text(
                    text = statusText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            }
        }

        // Botón Maestro de Encendido / Apagado Rápido con Retroalimentación
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(if (isLampOn) AccentPrimary else SurfaceDarkElevated)
                .border(
                    1.dp,
                    if (isLampOn) AccentPrimary.copy(alpha = 0.8f) else BorderSubtle,
                    CircleShape
                )
                .clickable { onTogglePower(!isLampOn) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PowerSettingsNew,
                contentDescription = "Interruptor de Encendido",
                tint = if (isLampOn) Color.White else TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
