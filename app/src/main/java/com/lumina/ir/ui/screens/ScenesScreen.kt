package com.lumina.ir.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gradient
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumina.ir.core.protocol.NecIrProtocol
import com.lumina.ir.service.SceneEngineService
import com.lumina.ir.ui.components.GlassCard
import com.lumina.ir.ui.theme.AccentAmber
import com.lumina.ir.ui.theme.AccentDanger
import com.lumina.ir.ui.theme.AccentEmerald
import com.lumina.ir.ui.theme.AccentNeonPink
import com.lumina.ir.ui.theme.AccentPrimary
import com.lumina.ir.ui.theme.BorderSubtle
import com.lumina.ir.ui.theme.SurfaceDarkElevated
import com.lumina.ir.ui.theme.TextMuted
import com.lumina.ir.ui.theme.TextPrimary
import com.lumina.ir.ui.theme.TextSecondary
import com.lumina.ir.ui.viewmodel.LuminaViewModel

@Composable
fun ScenesScreen(
    viewModel: LuminaViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sceneState by viewModel.sceneEngineState.collectAsState()
    val gradientColors by viewModel.gradientSelectedColors.collectAsState()
    val gradientStepSec by viewModel.gradientStepSeconds.collectAsState()
    val sunsetMinutes by viewModel.sunsetMinutes.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Motor de Escenas Inteligentes",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Transiciones suaves ejecutadas en segundo plano mediante ráfagas temporizadas de pulsos NEC.",
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Banner de Estado de Escena Activa (si está corriendo)
        AnimatedVisibility(visible = sceneState !is SceneEngineService.EngineState.Idle) {
            val activeColor = when (val s = sceneState) {
                is SceneEngineService.EngineState.RunningGradient -> s.activeColor.previewColor
                is SceneEngineService.EngineState.RunningBreathing -> s.activeColor.previewColor
                is SceneEngineService.EngineState.RunningSunset -> s.activeColor.previewColor
                else -> AccentPrimary
            }

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                glowColor = activeColor
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ESCENA EN PRIMER PLANO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentEmerald,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        when (val s = sceneState) {
                            is SceneEngineService.EngineState.RunningGradient -> {
                                Text(
                                    text = "Degradado: ${s.activeColor.name} (Paso ${s.stepIndex}/${s.totalSteps})",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            }
                            is SceneEngineService.EngineState.RunningBreathing -> {
                                Text(
                                    text = "Vela / Respiración: ${s.phaseDescription}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            }
                            is SceneEngineService.EngineState.RunningSunset -> {
                                Text(
                                    text = "Atardecer: ${s.currentPhaseName} (${s.remainingSeconds / 60}m restantes)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            }
                            else -> Unit
                        }
                    }

                    Button(
                        onClick = { viewModel.stopCurrentScene(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentDanger),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Detener", fontSize = 12.sp)
                    }
                }
            }
        }

        // 1. Tarjeta Atardecer Progresivo (Sleep Timer)
        GlassCard(modifier = Modifier.fillMaxWidth(), glowColor = AccentAmber) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Bedtime, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = "Atardecer Progresivo (Sleep Timer)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(text = "Atenúa gradualmente hacia tonos cálidos y apaga la lámpara", fontSize = 12.sp, color = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(text = "Duración antes de apagado: $sunsetMinutes min", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(5, 15, 30, 45, 60).forEach { mins ->
                        val isSelected = sunsetMinutes == mins
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) AccentAmber else SurfaceDarkElevated)
                                .clickable { viewModel.setSunsetMinutes(mins) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${mins}m",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { viewModel.startSunsetScene(context) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Iniciar Temporizador de Atardecer", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Tarjeta Efecto Vela / Respiración Suave
        GlassCard(modifier = Modifier.fillMaxWidth(), glowColor = Color(0xFFFF5722)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFFF5722), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = "Efecto Vela / Respiración Relajante", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(text = "Curvas sinusoidales de brillo alternando tonos ámbar y coral", fontSize = 12.sp, color = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { viewModel.startBreathingScene(context) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Iniciar Efecto Vela", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Creador de Degradado Continuo Personalizado
        GlassCard(modifier = Modifier.fillMaxWidth(), glowColor = AccentPrimary) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Gradient, contentDescription = null, tint = AccentPrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = "Creador de Degradado Cromático Suave", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(text = "Ruta interpolada por círculo de color (2 a 6 colores)", fontSize = 12.sp, color = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Colores clave seleccionados (${gradientColors.size}/6):",
                    fontSize = 12.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Selector de colores
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(NecIrProtocol.CHROMATIC_LAMP_COLORS.filter { !it.isWhite }) { color ->
                        val isSelected = gradientColors.any { it.id == color.id }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color.previewColor)
                                .border(
                                    if (isSelected) 3.dp else 1.dp,
                                    if (isSelected) Color.White else BorderSubtle,
                                    CircleShape
                                )
                                .clickable { viewModel.toggleGradientColor(color) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Tiempo por paso: ${String.format("%.1f", gradientStepSec)} segundos",
                    fontSize = 12.sp,
                    color = TextPrimary
                )
                Slider(
                    value = gradientStepSec,
                    onValueChange = { viewModel.setGradientStepSeconds(it) },
                    valueRange = 0.5f..20f,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentPrimary,
                        activeTrackColor = AccentPrimary,
                        inactiveTrackColor = BorderSubtle
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.startGradientScene(context) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                    shape = RoundedCornerShape(14.dp),
                    enabled = gradientColors.size >= 2
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Lanzar Degradado por Software", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
