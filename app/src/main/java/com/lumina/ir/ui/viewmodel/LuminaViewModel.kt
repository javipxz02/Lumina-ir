package com.lumina.ir.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lumina.ir.core.ir.IrTransmitterManager
import com.lumina.ir.core.protocol.NecIrProtocol
import com.lumina.ir.service.SceneEngineService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class DiscoveredCode(
    val id: String,
    val name: String,
    val command: Int,
    val hexCode32: Long
)

class LuminaViewModel(application: Application) : AndroidViewModel(application) {

    private val irManager = IrTransmitterManager.getInstance(application)

    // Estado del hardware IR
    val irStatus: StateFlow<IrTransmitterManager.IrStatus> = irManager.status
    val lastCode: StateFlow<Long?> = irManager.lastTransmittedCode
    val deviceModel: StateFlow<String> = irManager.deviceModel

    // Estado de la Lámpara
    private val _isLampOn = MutableStateFlow(true)
    val isLampOn: StateFlow<Boolean> = _isLampOn.asStateFlow()

    private val _selectedColor = MutableStateFlow(NecIrProtocol.CHROMATIC_LAMP_COLORS.first { it.isWhite })
    val selectedColor: StateFlow<NecIrProtocol.LampColor> = _selectedColor.asStateFlow()

    private val _brightnessLevel = MutableStateFlow(6) // 1 a 8
    val brightnessLevel: StateFlow<Int> = _brightnessLevel.asStateFlow()

    // Estado del Motor de Escenas en Segundo Plano
    val sceneEngineState: StateFlow<SceneEngineService.EngineState> = SceneEngineService.engineState

    // Creador de Escenas Personalizadas
    private val _gradientSelectedColors = MutableStateFlow<List<NecIrProtocol.LampColor>>(
        listOf(
            NecIrProtocol.CHROMATIC_LAMP_COLORS[0], // Rojo
            NecIrProtocol.CHROMATIC_LAMP_COLORS[4], // Amarillo
            NecIrProtocol.CHROMATIC_LAMP_COLORS[6], // Verde
            NecIrProtocol.CHROMATIC_LAMP_COLORS[11] // Azul
        )
    )
    val gradientSelectedColors: StateFlow<List<NecIrProtocol.LampColor>> = _gradientSelectedColors.asStateFlow()

    private val _gradientStepSeconds = MutableStateFlow(3.0f)
    val gradientStepSeconds: StateFlow<Float> = _gradientStepSeconds.asStateFlow()

    private val _sunsetMinutes = MutableStateFlow(15)
    val sunsetMinutes: StateFlow<Int> = _sunsetMinutes.asStateFlow()

    // Laboratorio de Escaneo Hexadecimal (Fuerza Bruta 0x00..0xFF)
    private val _currentAddress = MutableStateFlow(NecIrProtocol.DEFAULT_ADDRESS_16)
    val currentAddress: StateFlow<Int> = _currentAddress.asStateFlow()

    private val _explorerCommand = MutableStateFlow(0x00)
    val explorerCommand: StateFlow<Int> = _explorerCommand.asStateFlow()

    private val _isAutoScanning = MutableStateFlow(false)
    val isAutoScanning: StateFlow<Boolean> = _isAutoScanning.asStateFlow()

    private val _discoveredCodes = MutableStateFlow<List<DiscoveredCode>>(
        listOf(
            DiscoveredCode("1", "Encendido Rápido", 0x03, 0x00FF03FCL),
            DiscoveredCode("2", "Blanco Neutro", 0x07, 0x00FF07F8L),
            DiscoveredCode("3", "Modo Estrobo Secreto", 0x0F, 0x00FF0FF0L)
        )
    )
    val discoveredCodes: StateFlow<List<DiscoveredCode>> = _discoveredCodes.asStateFlow()

    private var autoScanJob: Job? = null

    // =========================================================================
    // ACCIONES DE CONTROL DIRECTO
    // =========================================================================

    fun togglePower(on: Boolean) {
        _isLampOn.value = on
        val cmd = if (on) NecIrProtocol.CMD_POWER_ON else NecIrProtocol.CMD_POWER_OFF
        irManager.sendCommand(cmd)
    }

    fun selectColor(color: NecIrProtocol.LampColor) {
        _selectedColor.value = color
        if (!_isLampOn.value) _isLampOn.value = true
        irManager.sendCommand(color.command)
    }

    fun setBrightnessLevel(target: Int) {
        val current = _brightnessLevel.value
        if (target == current) return

        val diff = target - current
        _brightnessLevel.value = target

        if (diff > 0) {
            irManager.sendCommandRepeated(NecIrProtocol.CMD_BRIGHTNESS_UP, diff, intervalMs = 90L)
        } else {
            irManager.sendCommandRepeated(NecIrProtocol.CMD_BRIGHTNESS_DOWN, -diff, intervalMs = 90L)
        }
    }

    fun sendFactoryMode(modeCommand: Int) {
        irManager.sendCommand(modeCommand)
    }

    // =========================================================================
    // GESTIÓN DEL MOTOR DE ESCENAS
    // =========================================================================

    fun toggleGradientColor(color: NecIrProtocol.LampColor) {
        val current = _gradientSelectedColors.value.toMutableList()
        if (current.any { it.id == color.id }) {
            if (current.size > 2) {
                current.removeAll { it.id == color.id }
                _gradientSelectedColors.value = current
            }
        } else {
            if (current.size < 6) {
                current.add(color)
                _gradientSelectedColors.value = current
            }
        }
    }

    fun setGradientStepSeconds(seconds: Float) {
        _gradientStepSeconds.value = seconds
    }

    fun setSunsetMinutes(minutes: Int) {
        _sunsetMinutes.value = minutes
    }

    fun startGradientScene(context: Context) {
        val ids = _gradientSelectedColors.value.map { it.id }.toIntArray()
        val intent = Intent(context, SceneEngineService::class.java).apply {
            action = SceneEngineService.ACTION_START_GRADIENT
            putExtra(SceneEngineService.EXTRA_COLOR_IDS, ids)
            putExtra(SceneEngineService.EXTRA_STEP_INTERVAL_SEC, _gradientStepSeconds.value)
        }
        context.startService(intent)
    }

    fun startBreathingScene(context: Context) {
        val intent = Intent(context, SceneEngineService::class.java).apply {
            action = SceneEngineService.ACTION_START_BREATHING
        }
        context.startService(intent)
    }

    fun startSunsetScene(context: Context) {
        val intent = Intent(context, SceneEngineService::class.java).apply {
            action = SceneEngineService.ACTION_START_SUNSET
            putExtra(SceneEngineService.EXTRA_SUNSET_MINUTES, _sunsetMinutes.value)
        }
        context.startService(intent)
    }

    fun stopCurrentScene(context: Context) {
        SceneEngineService.stop(context)
    }

    // =========================================================================
    // LABORATORIO DE ESCANEO DE CÓDIGOS OCULTOS (HEX EXPLORER)
    // =========================================================================

    fun setExplorerCommand(cmd: Int) {
        _explorerCommand.value = cmd.coerceIn(0, 255)
    }

    fun testCurrentExplorerCode() {
        irManager.sendCommand(_explorerCommand.value, _currentAddress.value)
    }

    fun nextExplorerCode() {
        val next = (_explorerCommand.value + 1) % 256
        _explorerCommand.value = next
        testCurrentExplorerCode()
    }

    fun previousExplorerCode() {
        val prev = if (_explorerCommand.value - 1 < 0) 255 else _explorerCommand.value - 1
        _explorerCommand.value = prev
        testCurrentExplorerCode()
    }

    fun toggleAutoScan() {
        if (_isAutoScanning.value) {
            stopAutoScan()
        } else {
            startAutoScan()
        }
    }

    private fun startAutoScan() {
        _isAutoScanning.value = true
        autoScanJob?.cancel()
        autoScanJob = viewModelScope.launch {
            while (isActive && _isAutoScanning.value) {
                testCurrentExplorerCode()
                delay(700L) // Pausa para que el usuario observe la reacción del hardware
                _explorerCommand.value = (_explorerCommand.value + 1) % 256
            }
        }
    }

    private fun stopAutoScan() {
        _isAutoScanning.value = false
        autoScanJob?.cancel()
        autoScanJob = null
    }

    fun saveDiscoveredCode(customName: String) {
        val cmd = _explorerCommand.value
        val fullCode = NecIrProtocol.buildFullCode(cmd, _currentAddress.value)
        val newEntry = DiscoveredCode(
            id = System.currentTimeMillis().toString(),
            name = customName.ifBlank { "Comando 0x${cmd.toString(16).uppercase().padStart(2, '0')}" },
            command = cmd,
            hexCode32 = fullCode
        )
        _discoveredCodes.value = _discoveredCodes.value + newEntry
    }

    fun removeDiscoveredCode(id: String) {
        _discoveredCodes.value = _discoveredCodes.value.filter { it.id != id }
    }

    fun triggerDiscoveredCode(entry: DiscoveredCode) {
        irManager.sendCommand(entry.command, _currentAddress.value)
    }

    override fun onCleared() {
        stopAutoScan()
        super.onCleared()
    }
}
