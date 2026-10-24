package com.lumina.ir.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.lumina.ir.R
import com.lumina.ir.core.ir.IrTransmitterManager
import com.lumina.ir.core.protocol.NecIrProtocol
import com.lumina.ir.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * Servicio en Primer Plano (Foreground Service) que ejecuta el Motor de Escenas Continuas.
 * Permite que los degradados, efecto respiración/vela y temporizadores de atardecer
 * sigan enviando ráfagas IR incluso cuando la pantalla del móvil se apaga o la app se minimiza.
 */
class SceneEngineService : Service() {

    companion object {
        const val CHANNEL_ID = "lumina_scene_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_GRADIENT = "com.lumina.ir.START_GRADIENT"
        const val ACTION_START_BREATHING = "com.lumina.ir.START_BREATHING"
        const val ACTION_START_SUNSET = "com.lumina.ir.START_SUNSET"
        const val ACTION_STOP_SCENE = "com.lumina.ir.STOP_SCENE"

        const val EXTRA_SUNSET_MINUTES = "extra_sunset_minutes"
        const val EXTRA_STEP_INTERVAL_SEC = "extra_step_interval_sec"
        const val EXTRA_COLOR_IDS = "extra_color_ids"

        // Estado global observable por la interfaz Jetpack Compose
        private val _engineState = MutableStateFlow<EngineState>(EngineState.Idle)
        val engineState: StateFlow<EngineState> = _engineState.asStateFlow()

        fun stop(context: Context) {
            val intent = Intent(context, SceneEngineService::class.java).apply {
                action = ACTION_STOP_SCENE
            }
            context.startService(intent)
        }
    }

    sealed interface EngineState {
        data object Idle : EngineState
        data class RunningGradient(val activeColor: NecIrProtocol.LampColor, val stepIndex: Int, val totalSteps: Int) : EngineState
        data class RunningBreathing(val activeColor: NecIrProtocol.LampColor, val phaseDescription: String) : EngineState
        data class RunningSunset(val remainingSeconds: Int, val currentPhaseName: String, val activeColor: NecIrProtocol.LampColor) : EngineState
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var sceneJob: Job? = null
    private lateinit var irManager: IrTransmitterManager
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        irManager = IrTransmitterManager.getInstance(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_GRADIENT -> {
                val colorIds = intent.getIntArrayExtra(EXTRA_COLOR_IDS) ?: intArrayOf(1, 7, 12)
                val stepIntervalSec = intent.getFloatExtra(EXTRA_STEP_INTERVAL_SEC, 3.0f)
                startForegroundWithNotification("Degradado Cromático Activo", "Transición suave entre colores...")
                runGradientScene(colorIds, stepIntervalSec)
            }
            ACTION_START_BREATHING -> {
                startForegroundWithNotification("Efecto Vela / Respiración", "Modulación suave de brillo y tonos cálidos...")
                runBreathingScene()
            }
            ACTION_START_SUNSET -> {
                val minutes = intent.getIntExtra(EXTRA_SUNSET_MINUTES, 15)
                startForegroundWithNotification("Atardecer / Sleep Timer", "Apagado progresivo en $minutes min...")
                runSunsetScene(minutes)
            }
            ACTION_STOP_SCENE -> {
                stopCurrentScene()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundWithNotification(title: String, description: String) {
        val notification = buildNotification(title, description)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(title: String, content: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, SceneEngineService::class.java).apply {
            action = ACTION_STOP_SCENE
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Lumina IR • $title")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Detener", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(title: String, content: String) {
        notificationManager.notify(NOTIFICATION_ID, buildNotification(title, content))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_desc)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun stopCurrentScene() {
        sceneJob?.cancel()
        sceneJob = null
        _engineState.value = EngineState.Idle
    }

    // =========================================================================
    // 1. DEGRADADO CROMÁTICO CONTINUO (CUSTOM GRADIENT)
    // =========================================================================
    /**
     * Envía secuencias de colores intermedias a lo largo del círculo cromático entre los
     * colores elegidos por el usuario, eliminando los saltos abruptos.
     */
    private fun runGradientScene(anchorColorIds: IntArray, stepIntervalSec: Float) {
        stopCurrentScene()
        sceneJob = serviceScope.launch {
            val allColors = NecIrProtocol.CHROMATIC_LAMP_COLORS.filter { !it.isWhite }
            val anchors = anchorColorIds.toList().mapNotNull { id -> allColors.find { it.id == id } }
            if (anchors.size < 2) return@launch

            // Generar ruta cromática continua pasando por colores adyacentes
            val fullRoute = mutableListOf<NecIrProtocol.LampColor>()
            for (i in anchors.indices) {
                val c1 = anchors[i]
                val c2 = anchors[(i + 1) % anchors.size]
                val path = interpolateChromaticPath(c1, c2, allColors)
                fullRoute.addAll(path)
            }

            val stepDelayMs = ((stepIntervalSec * 1000f) / fullRoute.size.coerceAtLeast(1)).toLong().coerceIn(300L, 10000L)

            while (isActive) {
                for ((index, color) in fullRoute.withIndex()) {
                    if (!isActive) break
                    _engineState.value = EngineState.RunningGradient(color, index + 1, fullRoute.size)
                    updateNotification("Degradado Activo", "Paso ${index + 1}/${fullRoute.size} • ${color.name}")
                    irManager.sendCommand(color.command)
                    delay(stepDelayMs)
                }
            }
        }
    }

    /**
     * Encuentra la ruta más corta en el círculo cromático entre dos colores.
     */
    private fun interpolateChromaticPath(
        c1: NecIrProtocol.LampColor,
        c2: NecIrProtocol.LampColor,
        palette: List<NecIrProtocol.LampColor>
    ): List<NecIrProtocol.LampColor> {
        val i1 = palette.indexOf(c1)
        val i2 = palette.indexOf(c2)
        if (i1 == -1 || i2 == -1) return listOf(c1)

        val size = palette.size
        val forwardDistance = (i2 - i1 + size) % size
        val backwardDistance = (i1 - i2 + size) % size

        val result = mutableListOf<NecIrProtocol.LampColor>()
        if (forwardDistance <= backwardDistance) {
            for (step in 0 until forwardDistance) {
                result.add(palette[(i1 + step) % size])
            }
        } else {
            for (step in 0 until backwardDistance) {
                result.add(palette[(i1 - step + size) % size])
            }
        }
        return result
    }

    // =========================================================================
    // 2. EFECTO RESPIRACIÓN / VELA (BREATHING DIMMER)
    // =========================================================================
    /**
     * Modulación sinusoidal de brillo y oscilación entre tonos cálidos (Ámbar, Coral, Oro).
     */
    private fun runBreathingScene() {
        stopCurrentScene()
        sceneJob = serviceScope.launch {
            val warmColors = listOf(
                NecIrProtocol.CHROMATIC_LAMP_COLORS.first { it.id == 4 }, // Ámbar Cálido
                NecIrProtocol.CHROMATIC_LAMP_COLORS.first { it.id == 3 }, // Naranja Vivo
                NecIrProtocol.CHROMATIC_LAMP_COLORS.first { it.id == 2 }, // Coral Intenso
                NecIrProtocol.CHROMATIC_LAMP_COLORS.first { it.id == 5 }  // Amarillo Sol
            )

            var colorIndex = 0

            while (isActive) {
                val currentColor = warmColors[colorIndex % warmColors.size]
                colorIndex++

                _engineState.value = EngineState.RunningBreathing(currentColor, "Oscilando tono cálido...")
                irManager.sendCommand(currentColor.command)
                delay(300)

                // Fase 1: Atenuación suave (Brightness Down x 3) con pausas crecientes (curva sinusoidal)
                _engineState.value = EngineState.RunningBreathing(currentColor, "Atenuando suavemente...")
                for (step in 1..3) {
                    if (!isActive) break
                    irManager.sendCommand(NecIrProtocol.CMD_BRIGHTNESS_DOWN)
                    val sinFactor = sin((step / 3.0) * (Math.PI / 2.0))
                    val pauseMs = (400L + (sinFactor * 500L)).toLong()
                    delay(pauseMs)
                }

                // Pausa tenue
                delay(600)

                // Fase 2: Iluminación suave (Brightness Up x 3) con pausas decrecientes
                _engineState.value = EngineState.RunningBreathing(currentColor, "Aumentando intensidad...")
                for (step in 1..3) {
                    if (!isActive) break
                    irManager.sendCommand(NecIrProtocol.CMD_BRIGHTNESS_UP)
                    val sinFactor = sin(((4 - step) / 3.0) * (Math.PI / 2.0))
                    val pauseMs = (400L + (sinFactor * 500L)).toLong()
                    delay(pauseMs)
                }

                // Pausa de cresta luminosa
                delay(1200)
            }
        }
    }

    // =========================================================================
    // 3. TEMPORIZADOR DE ATARDECER PROGRESIVO (SUNSET / SLEEP TIMER)
    // =========================================================================
    /**
     * Reduce progresivamente la intensidad de la lámpara en X minutos,
     * modulando hacia tonos cálidos (Amarillo -> Ámbar -> Naranja -> Rojo)
     * y disparando finalmente Power OFF.
     */
    private fun runSunsetScene(totalMinutes: Int) {
        stopCurrentScene()
        sceneJob = serviceScope.launch {
            val totalSeconds = totalMinutes * 60
            var remaining = totalSeconds

            val sunsetPhases = listOf(
                Pair("Luz Dorada Inicial", NecIrProtocol.CHROMATIC_LAMP_COLORS.first { it.id == 5 }), // Amarillo Sol
                Pair("Tarde Cálida", NecIrProtocol.CHROMATIC_LAMP_COLORS.first { it.id == 4 }),       // Ámbar
                Pair("Crepúsculo Naranja", NecIrProtocol.CHROMATIC_LAMP_COLORS.first { it.id == 3 }), // Naranja
                Pair("Horizonte Coral", NecIrProtocol.CHROMATIC_LAMP_COLORS.first { it.id == 2 }),    // Coral
                Pair("Penumbras Rojas", NecIrProtocol.CHROMATIC_LAMP_COLORS.first { it.id == 1 })      // Rojo Rubí
            )

            val phaseIntervalSeconds = totalSeconds / sunsetPhases.size
            var currentPhaseIndex = 0

            // Configurar primer tono
            irManager.sendCommand(sunsetPhases[0].second.command)

            while (remaining > 0 && isActive) {
                val newPhaseIndex = ((totalSeconds - remaining) / phaseIntervalSeconds).coerceAtMost(sunsetPhases.size - 1)
                val currentPhase = sunsetPhases[newPhaseIndex]

                if (newPhaseIndex != currentPhaseIndex) {
                    currentPhaseIndex = newPhaseIndex
                    // Cambiar a color más cálido y atenuar 2 pasos
                    irManager.sendCommand(currentPhase.second.command)
                    delay(200)
                    irManager.sendCommand(NecIrProtocol.CMD_BRIGHTNESS_DOWN)
                    delay(150)
                    irManager.sendCommand(NecIrProtocol.CMD_BRIGHTNESS_DOWN)
                }

                _engineState.value = EngineState.RunningSunset(remaining, currentPhase.first, currentPhase.second)
                val mins = remaining / 60
                val secs = remaining % 60
                updateNotification("Atardecer Progresivo", "Fase: ${currentPhase.first} • Restante: ${mins}m ${secs}s")

                delay(1000)
                remaining--
            }

            if (isActive) {
                // Apagado final al culminar el atardecer
                irManager.sendCommand(NecIrProtocol.CMD_POWER_OFF)
                updateNotification("Lumina IR", "Lámpara apagada por temporizador de atardecer.")
                delay(2000)
                stopCurrentScene()
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        stopCurrentScene()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
