package com.lumina.ir.core.ir

import android.content.Context
import android.hardware.ConsumerIrManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.lumina.ir.core.protocol.NecIrProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Gestor de transmisión Infrarroja para Android (`ConsumerIrManager`).
 * Diseñado y probado para el puerto IR interno de dispositivos de alta gama como el Honor 200 Pro.
 *
 * Incluye:
 * - Detección de hardware y rangos de frecuencia (38 kHz).
 * - Cola asíncrona segura en Coroutines (Dispatchers.IO) para evitar congelar la interfaz.
 * - Cooldown estricto anti-saturación del fotodiodo receptor (por defecto 110 ms).
 * - Retroalimentación háptica sutil en cada pulso emitido.
 */
class IrTransmitterManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "IrTransmitterManager"
        const val DEFAULT_COOLDOWN_MS = 110L

        @Volatile
        private var INSTANCE: IrTransmitterManager? = null

        fun getInstance(context: Context): IrTransmitterManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: IrTransmitterManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    sealed interface IrStatus {
        data object Ready : IrStatus
        data class Transmitting(val code: Long) : IrStatus
        data class Error(val message: String) : IrStatus
        data object MissingHardware : IrStatus
    }

    data class IrPacket(
        val pattern: IntArray,
        val hexCode32: Long,
        val customCooldownMs: Long = DEFAULT_COOLDOWN_MS
    )

    private val irManager: ConsumerIrManager? =
        context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val packetQueue = Channel<IrPacket>(capacity = Channel.UNLIMITED)

    private val _status = MutableStateFlow<IrStatus>(IrStatus.Ready)
    val status: StateFlow<IrStatus> = _status.asStateFlow()

    private val _lastTransmittedCode = MutableStateFlow<Long?>(null)
    val lastTransmittedCode: StateFlow<Long?> = _lastTransmittedCode.asStateFlow()

    private val _transmissionCount = MutableStateFlow(0L)
    val transmissionCount: StateFlow<Long> = _transmissionCount.asStateFlow()

    private val _deviceModel = MutableStateFlow("${Build.MANUFACTURER} ${Build.MODEL}")
    val deviceModel: StateFlow<String> = _deviceModel.asStateFlow()

    var isHapticEnabled: Boolean = true
    var globalCooldownMs: Long = DEFAULT_COOLDOWN_MS

    init {
        checkHardware()
        startQueueWorker()
    }

    /**
     * Comprueba si el dispositivo dispone de emisor IR y si soporta la frecuencia de 38 kHz.
     */
    fun hasIrEmitter(): Boolean {
        return irManager?.hasIrEmitter() == true
    }

    fun is38kHzSupported(): Boolean {
        val ranges = irManager?.carrierFrequencies ?: return false
        for (range in ranges) {
            if (NecIrProtocol.CARRIER_FREQUENCY_HZ in range.minFrequency..range.maxFrequency) {
                return true
            }
        }
        // Muchos fabricantes no exponen los rangos pero sí transmiten a 38kHz
        return ranges.isNotEmpty() || hasIrEmitter()
    }

    private fun checkHardware() {
        if (!hasIrEmitter()) {
            _status.value = IrStatus.MissingHardware
            Log.w(TAG, "Dispositivo ${Build.MANUFACTURER} ${Build.MODEL} sin emisor IR detectado.")
        } else {
            _status.value = IrStatus.Ready
            Log.i(TAG, "Emisor IR inicializado con éxito. Soporta 38kHz: ${is38kHzSupported()}")
        }
    }

    /**
     * Bucle consumidor de la cola de paquetes en hilo IO con temporización regulada.
     */
    private fun startQueueWorker() {
        scope.launch {
            for (packet in packetQueue) {
                executeTransmission(packet)
                val cooldown = if (packet.customCooldownMs > 0) packet.customCooldownMs else globalCooldownMs
                delay(cooldown)
                if (_status.value is IrStatus.Transmitting) {
                    _status.value = IrStatus.Ready
                }
            }
        }
    }

    /**
     * Dispara físicamente el array de pulsos infrarrojos en el hardware.
     */
    private fun executeTransmission(packet: IrPacket) {
        _status.value = IrStatus.Transmitting(packet.hexCode32)
        _lastTransmittedCode.value = packet.hexCode32
        _transmissionCount.value += 1

        triggerHapticTick()

        if (hasIrEmitter()) {
            try {
                irManager?.transmit(NecIrProtocol.CARRIER_FREQUENCY_HZ, packet.pattern)
                Log.d(TAG, "IR transmitido: 0x${packet.hexCode32.toString(16).uppercase().padStart(8, '0')}")
            } catch (e: Exception) {
                Log.e(TAG, "Fallo al transmitir comando IR", e)
                _status.value = IrStatus.Error(e.localizedMessage ?: "Error de transmisión IR")
            }
        } else {
            // Modo Simulación/Debug para pruebas en dispositivos sin blaster IR
            Log.d(TAG, "[SIMULACIÓN IR] Transmitido: 0x${packet.hexCode32.toString(16).uppercase().padStart(8, '0')} (${packet.pattern.size} pulsos)")
        }
    }

    /**
     * Genera una vibración táctil muy suave e instantánea (12 ms).
     */
    private fun triggerHapticTick() {
        if (!isHapticEnabled || vibrator?.hasVibrator() != true) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(VibrationEffect.createOneShot(14, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (_: Exception) {
            // Silencioso en caso de restricción en modo batería
        }
    }

    /**
     * Encola un comando NEC estándar de 8 bits con dirección opcional (por defecto 0x00FF).
     * Incluye soporte para repetición rápida (útil para receptores que necesitan ráfaga de activación).
     */
    fun sendCommand(
        command: Int,
        address: Int = NecIrProtocol.DEFAULT_ADDRESS_16,
        repeatCount: Int = 1,
        cooldownMs: Long = globalCooldownMs
    ) {
        val pattern = NecIrProtocol.encodeNec(address, command)
        val fullCode = NecIrProtocol.buildFullCode(command, address)
        repeat(repeatCount.coerceAtLeast(1)) {
            packetQueue.trySend(IrPacket(pattern, fullCode, cooldownMs))
        }
    }

    /**
     * Encola un código completo de 32 bits en hexadecimal (ej. 0x00FF04FB).
     */
    fun sendHex32(hexCode: Long, cooldownMs: Long = globalCooldownMs) {
        val pattern = NecIrProtocol.encode32Bit(hexCode)
        packetQueue.trySend(IrPacket(pattern, hexCode, cooldownMs))
    }

    /**
     * Envía un comando repetido N veces con cadencia segura (útil para subir/bajar brillo varios pasos).
     */
    fun sendCommandRepeated(command: Int, count: Int, intervalMs: Long = 110L) {
        scope.launch {
            repeat(count) {
                sendCommand(command, cooldownMs = intervalMs)
            }
        }
    }
}
