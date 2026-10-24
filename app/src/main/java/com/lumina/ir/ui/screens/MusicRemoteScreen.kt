package com.lumina.ir.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumina.ir.core.protocol.NecIrProtocol
import com.lumina.ir.ui.components.GlassCard
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
fun MusicRemoteScreen(
    viewModel: LuminaViewModel,
    modifier: Modifier = Modifier
) {
    val selectedProfile by viewModel.selectedMusicProfile.collectAsState()
    val autoDetectStatus by viewModel.autoDetectStatus.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Selector de Perfil de Chipset para el mando
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PERFIL DE TU LÁMPARA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "ADDR: 0x${selectedProfile.address.toString(16).uppercase().padStart(4, '0')}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = AccentPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(viewModel.musicProfiles) { profile ->
                        val isSelected = selectedProfile.id == profile.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) AccentPrimary else SurfaceDarkElevated)
                                .border(1.dp, if (isSelected) AccentPrimary else BorderSubtle, RoundedCornerShape(10.dp))
                                .clickable { viewModel.selectMusicProfile(profile) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = profile.name,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Botón Auto-Detectar Encendido
                Button(
                    onClick = {
                        if (autoDetectStatus == null) viewModel.startAutoDetect() else viewModel.stopAutoDetect()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (autoDetectStatus == null) AccentEmerald else AccentDanger
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (autoDetectStatus == null) Icons.Default.Search else Icons.Default.Stop,
                        contentDescription = null,
                        tint = if (autoDetectStatus == null) Color.Black else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = autoDetectStatus ?: "Auto-Detectar Encendido (Prueba Rápida)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (autoDetectStatus == null) Color.Black else Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // RÉPLICA VISUAL 1:1 DEL MANDO FÍSICO DE LA FOTO
        Box(
            modifier = Modifier
                .width(280.dp)
                .shadow(elevation = 16.dp, shape = RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFE8ECEF)) // Carcasa blanca/gris del mando real
                .border(2.dp, Color(0xFF1E293B), RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // FILA 1: [B+] [B-] [OFF (Rojo)] [ON (Negro)]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    RemoteRoundButton(
                        bgColor = Color.White,
                        borderColor = Color.Black,
                        onClick = { viewModel.sendMusicButton(selectedProfile.cmdBrightUp) }
                    ) {
                        Icon(imageVector = Icons.Default.BrightnessHigh, contentDescription = "Brillo+", tint = Color.Black, modifier = Modifier.size(20.dp))
                    }
                    RemoteRoundButton(
                        bgColor = Color.White,
                        borderColor = Color.Black,
                        onClick = { viewModel.sendMusicButton(selectedProfile.cmdBrightDown) }
                    ) {
                        Icon(imageVector = Icons.Default.BrightnessLow, contentDescription = "Brillo-", tint = Color.Black, modifier = Modifier.size(20.dp))
                    }
                    RemoteRoundButton(
                        bgColor = Color(0xFFDC2626), // Rojo
                        borderColor = Color(0xFF991B1B),
                        onClick = { viewModel.sendMusicButton(selectedProfile.cmdPowerOff) }
                    ) {
                        Icon(imageVector = Icons.Default.PowerSettingsNew, contentDescription = "OFF", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    RemoteRoundButton(
                        bgColor = Color(0xFF0F172A), // Negro
                        borderColor = Color.Black,
                        onClick = { viewModel.sendMusicButton(selectedProfile.cmdPowerOn) }
                    ) {
                        Icon(imageVector = Icons.Default.PowerSettingsNew, contentDescription = "ON", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }

                // FILA 2: [R] [G] [B] [W]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    RemoteTextButton(text = "R", bgColor = Color(0xFFEF4444), textColor = Color.White) {
                        viewModel.sendMusicButton(selectedProfile.cmdRed)
                    }
                    RemoteTextButton(text = "G", bgColor = Color(0xFF22C55E), textColor = Color.White) {
                        viewModel.sendMusicButton(selectedProfile.cmdGreen)
                    }
                    RemoteTextButton(text = "B", bgColor = Color(0xFF2563EB), textColor = Color.White) {
                        viewModel.sendMusicButton(selectedProfile.cmdBlue)
                    }
                    RemoteTextButton(text = "W", bgColor = Color.White, textColor = Color.Black, borderColor = Color.Black) {
                        viewModel.sendMusicButton(selectedProfile.cmdWhite)
                    }
                }

                // FILA 3: [Naranja] [Lima] [Cian] [1H]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    RemoteColorButton(bgColor = Color(0xFFF97316)) {
                        viewModel.sendMusicButton(selectedProfile.cmdOrange)
                    }
                    RemoteColorButton(bgColor = Color(0xFF84CC16)) {
                        viewModel.sendMusicButton(selectedProfile.cmdLime)
                    }
                    RemoteColorButton(bgColor = Color(0xFF06B6D4)) {
                        viewModel.sendMusicButton(selectedProfile.cmdCyan)
                    }
                    RemoteTextButton(text = "1H", bgColor = Color(0xFF0F172A), textColor = Color.White, hasDottedBorder = true) {
                        viewModel.sendMusicButton(selectedProfile.cmdTimer1h)
                    }
                }

                // FILA 4: [Amarillo] [Verde Oscuro] [Púrpura] [2H]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    RemoteColorButton(bgColor = Color(0xFFEAB308)) {
                        viewModel.sendMusicButton(selectedProfile.cmdYellow)
                    }
                    RemoteColorButton(bgColor = Color(0xFF15803D)) {
                        viewModel.sendMusicButton(selectedProfile.cmdEmerald)
                    }
                    RemoteColorButton(bgColor = Color(0xFF7E22CE)) {
                        viewModel.sendMusicButton(selectedProfile.cmdPurple)
                    }
                    RemoteTextButton(text = "2H", bgColor = Color(0xFF0F172A), textColor = Color.White, hasDottedBorder = true) {
                        viewModel.sendMusicButton(selectedProfile.cmdTimer2h)
                    }
                }

                // FILA 5: [Speed+] [Mode] [Speed-] [Night]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    RemoteLabelButton(text = "Speed+") {
                        viewModel.sendMusicButton(selectedProfile.cmdSpeedUp)
                    }
                    RemoteLabelButton(text = "Mode") {
                        viewModel.sendMusicButton(selectedProfile.cmdMode)
                    }
                    RemoteLabelButton(text = "Speed-") {
                        viewModel.sendMusicButton(selectedProfile.cmdSpeedDown)
                    }
                    RemoteRoundButton(
                        bgColor = Color.White,
                        borderColor = Color.Black,
                        onClick = { viewModel.sendMusicButton(selectedProfile.cmdNight) }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.Bedtime, contentDescription = "Night", tint = Color.Black, modifier = Modifier.size(14.dp))
                            Text(text = "Night", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }

                // FILA 6: [Music 1] [Music 2] [Music 3] [Music 4]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    RemoteMusicButton(number = "1", tint = Color(0xFFEF4444)) {
                        viewModel.sendMusicButton(selectedProfile.cmdMusic1)
                    }
                    RemoteMusicButton(number = "2", tint = Color(0xFF22C55E)) {
                        viewModel.sendMusicButton(selectedProfile.cmdMusic2)
                    }
                    RemoteMusicButton(number = "3", tint = Color(0xFF3B82F6)) {
                        viewModel.sendMusicButton(selectedProfile.cmdMusic3)
                    }
                    RemoteMusicButton(number = "4", tint = Color(0xFFEC4899)) {
                        viewModel.sendMusicButton(selectedProfile.cmdMusic4)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun RemoteRoundButton(
    bgColor: Color,
    borderColor: Color = Color.Transparent,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .shadow(2.dp, CircleShape)
            .clip(CircleShape)
            .background(bgColor)
            .border(1.5.dp, borderColor, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun RemoteTextButton(
    text: String,
    bgColor: Color,
    textColor: Color,
    borderColor: Color = Color.Transparent,
    hasDottedBorder: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .shadow(2.dp, CircleShape)
            .clip(CircleShape)
            .background(bgColor)
            .border(1.5.dp, if (hasDottedBorder) Color.LightGray else borderColor, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = textColor
        )
    }
}

@Composable
fun RemoteColorButton(
    bgColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .shadow(2.dp, CircleShape)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(onClick = onClick)
    )
}

@Composable
fun RemoteLabelButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .shadow(2.dp, CircleShape)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.5.dp, Color.Black, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}

@Composable
fun RemoteMusicButton(
    number: String,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .shadow(2.dp, CircleShape)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.5.dp, Color.Black, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = "Música $number",
                tint = tint,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = number,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = tint
            )
        }
    }
}
