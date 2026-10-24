package com.lumina.ir.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumina.ir.core.protocol.NecIrProtocol
import com.lumina.ir.ui.components.ColorWheel
import com.lumina.ir.ui.components.GlassCard
import com.lumina.ir.ui.components.IrStatusHeader
import com.lumina.ir.ui.components.VerticalBrightnessSlider
import com.lumina.ir.ui.theme.AccentPrimary
import com.lumina.ir.ui.theme.BorderSubtle
import com.lumina.ir.ui.theme.SurfaceDarkElevated
import com.lumina.ir.ui.theme.TextMuted
import com.lumina.ir.ui.theme.TextPrimary
import com.lumina.ir.ui.theme.TextSecondary
import com.lumina.ir.ui.viewmodel.LuminaViewModel

@Composable
fun DashboardScreen(
    viewModel: LuminaViewModel,
    modifier: Modifier = Modifier
) {
    val irStatus by viewModel.irStatus.collectAsState()
    val isLampOn by viewModel.isLampOn.collectAsState()
    val selectedColor by viewModel.selectedColor.collectAsState()
    val brightnessLevel by viewModel.brightnessLevel.collectAsState()
    val deviceModel by viewModel.deviceModel.collectAsState()
    val discoveredCodes by viewModel.discoveredCodes.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Cabecera de Estado IR del Honor 200 Pro y Switch Maestro
        IrStatusHeader(
            deviceModel = deviceModel,
            irStatus = irStatus,
            isLampOn = isLampOn,
            onTogglePower = { viewModel.togglePower(it) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Panel Central: Color Wheel Magnético + Slider de Brillo por Pulsos
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            glowColor = selectedColor.previewColor
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Selector Circular Cromático con Snap Magnético a los 16 Colores Reales
                Box(modifier = Modifier.weight(1f)) {
                    ColorWheel(
                        selectedColor = selectedColor,
                        onColorSelected = { viewModel.selectColor(it) }
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Control Deslizante Vertical de Brillo
                VerticalBrightnessSlider(
                    currentLevel = brightnessLevel,
                    maxLevel = 8,
                    activeColor = selectedColor.previewColor,
                    onLevelChangeRequested = { viewModel.setBrightnessLevel(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Paleta Rápida de Colores Frecuentes
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    text = "ACCESOS CROMÁTICOS DIRECTOS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(NecIrProtocol.CHROMATIC_LAMP_COLORS) { color ->
                        val isSelected = selectedColor.id == color.id
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color.previewColor)
                                .border(
                                    if (isSelected) 3.dp else 1.dp,
                                    if (isSelected) Color.White else BorderSubtle,
                                    CircleShape
                                )
                                .clickable { viewModel.selectColor(color) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Modos Nativos de Fábrica de la Lámpara (ROM)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    text = "MODOS INTERNOS DE FÁBRICA (CHIP ROM)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FactoryModeButton(
                        title = "Flash",
                        icon = Icons.Default.FlashOn,
                        onClick = { viewModel.sendFactoryMode(NecIrProtocol.CMD_MODE_FLASH) },
                        modifier = Modifier.weight(1f)
                    )
                    FactoryModeButton(
                        title = "Strobe",
                        icon = Icons.Default.Waves,
                        onClick = { viewModel.sendFactoryMode(NecIrProtocol.CMD_MODE_STROBE) },
                        modifier = Modifier.weight(1f)
                    )
                    FactoryModeButton(
                        title = "Fade",
                        icon = Icons.Default.AutoAwesome,
                        onClick = { viewModel.sendFactoryMode(NecIrProtocol.CMD_MODE_FADE) },
                        modifier = Modifier.weight(1f)
                    )
                    FactoryModeButton(
                        title = "Smooth",
                        icon = Icons.Default.Waves,
                        onClick = { viewModel.sendFactoryMode(NecIrProtocol.CMD_MODE_SMOOTH) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 5. Botones Personalizados Descubiertos en el Hex Explorer (si existen)
        if (discoveredCodes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = "COMANDOS DESCUBIERTOS VINCULADOS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(discoveredCodes) { code ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceDarkElevated)
                                    .border(1.dp, AccentPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .clickable { viewModel.triggerDiscoveredCode(code) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Column {
                                    Text(text = code.name, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = "0x${code.command.toString(16).uppercase().padStart(2, '0')}",
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun FactoryModeButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDarkElevated)
            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        }
    }
}
