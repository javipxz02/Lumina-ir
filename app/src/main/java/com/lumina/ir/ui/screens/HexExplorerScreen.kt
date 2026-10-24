package com.lumina.ir.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
fun HexExplorerScreen(
    viewModel: LuminaViewModel,
    modifier: Modifier = Modifier
) {
    val address by viewModel.currentAddress.collectAsState()
    val command by viewModel.explorerCommand.collectAsState()
    val isAutoScanning by viewModel.isAutoScanning.collectAsState()
    val discoveredCodes by viewModel.discoveredCodes.collectAsState()

    var showSaveDialog by remember { mutableStateOf(false) }
    var customLabelText by remember { mutableStateOf("") }

    val fullCode32 = NecIrProtocol.buildFullCode(command, address)
    val binaryString = Integer.toBinaryString(command).padStart(8, '0')
    val invertedCmd = command.inv() and 0xFF

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Laboratorio de Escaneo NEC",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Fuerza bruta de 256 comandos (0x00 a 0xFF) para descubrir funciones ocultas del controlador LED.",
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 1. Pantalla de Lectura Hexadecimal y Binaria estilo Instrumento de Hardware
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            glowColor = if (isAutoScanning) AccentNeonPink else AccentPrimary
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DIRECCIÓN: 0x${address.toString(16).uppercase().padStart(4, '0')}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                    Text(
                        text = "PARIDAD: ~0x${invertedCmd.toString(16).uppercase().padStart(2, '0')}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Display Gigante del Comando Hexadecimal Actual
                Text(
                    text = "0x${command.toString(16).uppercase().padStart(2, '0')}",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isAutoScanning) AccentNeonPink else AccentPrimary,
                    letterSpacing = 2.sp
                )

                Text(
                    text = "Decimal: $command / 255 • Trama Completa: 0x${fullCode32.toString(16).uppercase().padStart(8, '0')}",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Representación Binaria Bit a Bit
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    binaryString.forEach { bit ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (bit == '1') AccentPrimary.copy(alpha = 0.8f) else SurfaceDarkElevated)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(4.dp))
                                .padding(horizontal = 7.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = bit.toString(),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (bit == '1') Color.White else TextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Slider selector de comando (0 a 255)
                Slider(
                    value = command.toFloat(),
                    onValueChange = { viewModel.setExplorerCommand(it.toInt()) },
                    valueRange = 0f..255f,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentPrimary,
                        activeTrackColor = AccentPrimary,
                        inactiveTrackColor = BorderSubtle
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Controles de Disparo, Paso y Auto-Escaneo
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.previousExplorerCode() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceDarkElevated),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.FastRewind, contentDescription = "Anterior", tint = TextPrimary)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "-1", color = TextPrimary)
            }

            Button(
                onClick = { viewModel.testCurrentExplorerCode() },
                modifier = Modifier.weight(2f),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.RadioButtonChecked, contentDescription = "Probar", tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Disparar Pulso", fontWeight = FontWeight.Bold, color = Color.White)
            }

            Button(
                onClick = { viewModel.nextExplorerCode() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceDarkElevated),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "+1", color = TextPrimary)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(imageVector = Icons.Default.FastForward, contentDescription = "Siguiente", tint = TextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Botón de Auto-Escáner Continuo (700ms)
            Button(
                onClick = { viewModel.toggleAutoScan() },
                modifier = Modifier.weight(1.2f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAutoScanning) AccentDanger else AccentNeonPink
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = if (isAutoScanning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isAutoScanning) "Pausar Escáner" else "Auto-Escáner (700ms)",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Botón Guardar Hallazgo
            Button(
                onClick = {
                    customLabelText = "Función Oculta 0x${command.toString(16).uppercase().padStart(2, '0')}"
                    showSaveDialog = true
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.BookmarkAdd, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Guardar", fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Lista de Códigos Descubiertos y Guardados
        Text(
            text = "CÓDIGOS GUARDADOS (${discoveredCodes.size})",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            letterSpacing = 1.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(discoveredCodes, key = { it.id }) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceDarkElevated)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = item.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text(
                            text = "CMD: 0x${item.command.toString(16).uppercase().padStart(2, '0')} • NEC: 0x${item.hexCode32.toString(16).uppercase().padStart(8, '0')}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted
                        )
                    }

                    Row {
                        Button(
                            onClick = { viewModel.triggerDiscoveredCode(item) },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(text = "Probar", fontSize = 11.sp)
                        }

                        IconButton(
                            onClick = { viewModel.removeDiscoveredCode(item.id) },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar", tint = TextMuted)
                        }
                    }
                }
            }
        }
    }

    // Cuadro de Diálogo para Asignar Nombre al Código Descubierto
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = {
                Text(text = "Guardar Código Oculto", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Asigna una etiqueta para identificar este comando en tu panel principal (Comando 0x${command.toString(16).uppercase().padStart(2, '0')}).",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = customLabelText,
                        onValueChange = { customLabelText = it },
                        label = { Text("Nombre del efecto / función") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.saveDiscoveredCode(customLabelText)
                        showSaveDialog = false
                    }
                ) {
                    Text("Guardar en Panel", color = AccentEmerald, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancelar", color = TextMuted)
                }
            },
            containerColor = SurfaceDarkElevated
        )
    }
}
