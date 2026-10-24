package com.lumina.ir.core.protocol

import androidx.compose.ui.graphics.Color

/**
 * Motor de Codificación y Diccionario del Protocolo Infrarrojo NEC (38 kHz).
 *
 * Estándar NEC:
 * - Frecuencia portadora: 38.000 Hz (período ~26.3 µs).
 * - Header / AGC burst: 9.000 µs ON (marca), 4.500 µs OFF (espacio).
 * - Bit Lógico '1': 560 µs ON, 1.690 µs OFF (período total: 2.250 µs).
 * - Bit Lógico '0': 560 µs ON, 560 µs OFF (período total: 1.120 µs).
 * - Stop bit: 560 µs ON.
 *
 * Formato de trama de 32 bits (LSB first por cada byte):
 * [Byte 0: Dirección (8 bits)]
 * [Byte 1: Dirección Invertida o Extendida (8 bits)]
 * [Byte 2: Comando (8 bits)]
 * [Byte 3: Comando Invertido (8 bits, verificación de paridad ~Comando)]
 */
object NecIrProtocol {

    const val CARRIER_FREQUENCY_HZ = 38000

    // Constantes de temporización en microsegundos (µs)
    private const val HEADER_MARK = 9000
    private const val HEADER_SPACE = 4500
    private const val BIT_MARK = 560
    private const val BIT_ONE_SPACE = 1690
    private const val BIT_ZERO_SPACE = 560
    private const val STOP_BIT_MARK = 560

    // Dirección típica del chipset de lámparas RGB económicas (APA106 / WS2811 / 24-key controllers)
    const val DEFAULT_ADDRESS_BYTE = 0x00
    const val DEFAULT_INVERTED_ADDRESS_BYTE = 0xFF
    const val DEFAULT_ADDRESS_16 = 0x00FF

    /**
     * Convierte una dirección de 16 bits y un comando de 8 bits en el patrón de microsegundos exacto
     * para [android.hardware.ConsumerIrManager.transmit].
     *
     * @param address Dirección de 16 bits (ej. 0x00FF)
     * @param command Comando de 8 bits (ej. 0x02)
     * @return Array de 67 enteros alternando pulsos ON y OFF en microsegundos.
     */
    fun encodeNec(address: Int = DEFAULT_ADDRESS_16, command: Int): IntArray {
        // En estándar NEC: Byte 0 = Dirección alta (0x00), Byte 1 = Dirección baja / Invertida (0xFF)
        val addrHigh = (address shr 8) and 0xFF
        val addrLow = address and 0xFF
        val cmd = command and 0xFF
        val cmdInv = cmd.inv() and 0xFF

        return buildPattern(byteArrayOf(addrHigh.toByte(), addrLow.toByte(), cmd.toByte(), cmdInv.toByte()))
    }

    /**
     * Convierte un código hexadecimal completo de 32 bits (ej. 0x00FF906F)
     * en el patrón de microsegundos de la portadora NEC.
     */
    fun encode32Bit(hexCode: Long): IntArray {
        val b0 = ((hexCode shr 24) and 0xFF).toByte()
        val b1 = ((hexCode shr 16) and 0xFF).toByte()
        val b2 = ((hexCode shr 8) and 0xFF).toByte()
        val b3 = (hexCode and 0xFF).toByte()
        return buildPattern(byteArrayOf(b0, b1, b2, b3))
    }

    /**
     * Genera el array de microsegundos alternando ON y OFF:
     * - Posiciones pares (0, 2, 4...): Pulso infrarrojo activo (marca ON).
     * - Posiciones impares (1, 3, 5...): Pausa infrarroja inactiva (espacio OFF).
     */
    private fun buildPattern(bytes: ByteArray): IntArray {
        // 1 header mark + 1 header space + (32 bits * 2) + 1 stop bit + 1 trailing pause = 68 valores pares (exigido por HAL de Android)
        val pattern = IntArray(68)
        var idx = 0

        // 1. Header (9000 µs ON, 4500 µs OFF)
        pattern[idx++] = HEADER_MARK
        pattern[idx++] = HEADER_SPACE

        // 2. 32 bits de carga útil (LSB first por cada byte según especificación NEC)
        for (byteVal in bytes) {
            val unsignedByte = byteVal.toInt() and 0xFF
            for (bitIdx in 0..7) {
                val bit = (unsignedByte shr bitIdx) and 0x01
                pattern[idx++] = BIT_MARK
                pattern[idx++] = if (bit == 1) BIT_ONE_SPACE else BIT_ZERO_SPACE
            }
        }

        // 3. Stop bit final (560 µs ON)
        pattern[idx++] = STOP_BIT_MARK

        // 4. Pausa de apagado final (10.000 µs OFF) para cerrar el ciclo de portadora y garantizar longitud par
        pattern[idx] = 10000

        return pattern
    }

    /**
     * Construye un código 32-bit NEC a partir de un comando de 8 bits usando la dirección 0x00FF.
     * Ejemplo: comando 0x02 -> 0x00FF02FD
     */
    fun buildFullCode(command: Int, address: Int = DEFAULT_ADDRESS_16): Long {
        val cmd = command and 0xFF
        val cmdInv = cmd.inv() and 0xFF
        return ((address.toLong() and 0xFFFF) shl 16) or ((cmd.toLong() shl 8)) or cmdInv.toLong()
    }

    // =========================================================================
    // DICCIONARIO DE COMANDOS NEC PARA CONTROLADOR RGB DE 24 TECLAS (0x00FF)
    // =========================================================================

    // Controles Básicos
    const val CMD_BRIGHTNESS_UP = 0x00   // 0x00FF00FF
    const val CMD_BRIGHTNESS_DOWN = 0x01 // 0x00FF01FE
    const val CMD_POWER_OFF = 0x02       // 0x00FF02FD
    const val CMD_POWER_ON = 0x03        // 0x00FF03FC

    // Modos de Fábrica
    const val CMD_MODE_FLASH = 0x0B      // 0x00FF0BF4
    const val CMD_MODE_STROBE = 0x0F     // 0x00FF0FF0
    const val CMD_MODE_FADE = 0x13       // 0x00FF13EC
    const val CMD_MODE_SMOOTH = 0x17     // 0x00FF17E8

    /**
     * Modelo de Color Físico de la Lámpara con metadatos de hardware y posición cromática.
     */
    data class LampColor(
        val id: Int,
        val name: String,
        val command: Int,
        val hexCode32: Long,
        val previewColor: Color,
        val chromaticAngle: Float, // Ángulo en grados [0..360] en el círculo cromático HSV
        val isWhite: Boolean = false
    )

    /**
     * 16 COLORES FÍSICOS REALES DE LA LÁMPARA ORDENADOS CROMÁTICAMENTE (0° -> 360° + Blanco).
     * Esto sustituye el orden matricial arbitrario del mando de plástico para permitir
     * transiciones naturales, degradados continuos y snap magnético en la rueda de color.
     */
    val CHROMATIC_LAMP_COLORS: List<LampColor> = listOf(
        // 0° - Rojos
        LampColor(
            id = 1,
            name = "Rojo Rubí",
            command = 0x04,
            hexCode32 = 0x00FF04FBL,
            previewColor = Color(0xFFFF1122),
            chromaticAngle = 0f
        ),
        // 22° - Naranja Rojizo
        LampColor(
            id = 2,
            name = "Coral Intenso",
            command = 0x08,
            hexCode32 = 0x00FF08F7L,
            previewColor = Color(0xFFFF4800),
            chromaticAngle = 22f
        ),
        // 40° - Naranja
        LampColor(
            id = 3,
            name = "Naranja Vivo",
            command = 0x0C,
            hexCode32 = 0x00FF0CF3L,
            previewColor = Color(0xFFFF7B00),
            chromaticAngle = 40f
        ),
        // 55° - Ámbar / Dorado
        LampColor(
            id = 4,
            name = "Ámbar Cálido",
            command = 0x10,
            hexCode32 = 0x00FF10EFL,
            previewColor = Color(0xFFFFAE00),
            chromaticAngle = 55f
        ),
        // 68° - Amarillo
        LampColor(
            id = 5,
            name = "Amarillo Sol",
            command = 0x14,
            hexCode32 = 0x00FF14EBL,
            previewColor = Color(0xFFFFEA00),
            chromaticAngle = 68f
        ),
        // 95° - Lima
        LampColor(
            id = 6,
            name = "Verde Lima",
            command = 0x15,
            hexCode32 = 0x00FF15EAL,
            previewColor = Color(0xFFA6FF00),
            chromaticAngle = 95f
        ),
        // 120° - Verde Puro
        LampColor(
            id = 7,
            name = "Verde Esmeralda",
            command = 0x05,
            hexCode32 = 0x00FF05FAL,
            previewColor = Color(0xFF00E63D),
            chromaticAngle = 120f
        ),
        // 145° - Verde Menta
        LampColor(
            id = 8,
            name = "Verde Menta",
            command = 0x09,
            hexCode32 = 0x00FF09F6L,
            previewColor = Color(0xFF00FF9D),
            chromaticAngle = 145f
        ),
        // 175° - Turquesa / Aqua
        LampColor(
            id = 9,
            name = "Turquesa Glaciar",
            command = 0x0D,
            hexCode32 = 0x00FF0DF2L,
            previewColor = Color(0xFF00F5D4),
            chromaticAngle = 175f
        ),
        // 195° - Cian
        LampColor(
            id = 10,
            name = "Cian Neón",
            command = 0x11,
            hexCode32 = 0x00FF11EEL,
            previewColor = Color(0xFF00E5FF),
            chromaticAngle = 195f
        ),
        // 220° - Azul Cielo
        LampColor(
            id = 11,
            name = "Azul Cielo",
            command = 0x0A,
            hexCode32 = 0x00FF0AF5L,
            previewColor = Color(0xFF0091FF),
            chromaticAngle = 220f
        ),
        // 240° - Azul Cobalto
        LampColor(
            id = 12,
            name = "Azul Zafiro",
            command = 0x06,
            hexCode32 = 0x00FF06F9L,
            previewColor = Color(0xFF1A38FF),
            chromaticAngle = 240f
        ),
        // 265° - Azul Marino Oscuro
        LampColor(
            id = 13,
            name = "Azul Profundo",
            command = 0x0E,
            hexCode32 = 0x00FF0EF1L,
            previewColor = Color(0xFF4C00FF),
            chromaticAngle = 265f
        ),
        // 285° - Púrpura / Violeta
        LampColor(
            id = 14,
            name = "Violeta Eléctrico",
            command = 0x12,
            hexCode32 = 0x00FF12EDL,
            previewColor = Color(0xFF8800FF),
            chromaticAngle = 285f
        ),
        // 315° - Magenta / Fucsia
        LampColor(
            id = 15,
            name = "Magenta Neón",
            command = 0x16,
            hexCode32 = 0x00FF16E9L,
            previewColor = Color(0xFFFF00A0),
            chromaticAngle = 315f
        ),
        // Blanco puro (Centro del espectro de la lámpara)
        LampColor(
            id = 16,
            name = "Blanco Neutro",
            command = 0x07,
            hexCode32 = 0x00FF07F8L,
            previewColor = Color(0xFFF8FAFC),
            chromaticAngle = -1f,
            isWhite = true
        )
    )

    /**
     * Encuentra el color físico más cercano a un ángulo cromático arbitrario [0..360].
     * Se usa para el 'snap magnético' en la interfaz táctil.
     */
    fun findClosestColorByAngle(angle: Float): LampColor {
        val chromaticOnly = CHROMATIC_LAMP_COLORS.filter { !it.isWhite }
        val normalizedAngle = (angle % 360f + 360f) % 360f

        return chromaticOnly.minByOrNull { color ->
            val diff = kotlin.math.abs(color.chromaticAngle - normalizedAngle)
            kotlin.math.min(diff, 360f - diff)
        } ?: CHROMATIC_LAMP_COLORS.first()
    }

    /**
     * Tabla extendida de 44 teclas de referencia para controladores RGB avanzados
     * (permite ajustar R/G/B incrementalmente y modos DIY).
     */
    object Extended44Key {
        const val CMD_RED_UP = 0x14
        const val CMD_RED_DOWN = 0x10
        const val CMD_GREEN_UP = 0x15
        const val CMD_GREEN_DOWN = 0x11
        const val CMD_BLUE_UP = 0x16
        const val CMD_BLUE_DOWN = 0x12
        const val CMD_DIY1 = 0x0C
        const val CMD_DIY2 = 0x0D
        const val CMD_DIY3 = 0x0E
        const val CMD_DIY4 = 0x08
        const val CMD_DIY5 = 0x09
        const val CMD_DIY6 = 0x0A
        const val CMD_AUTO = 0x0F
        const val CMD_QUICK = 0x17
        const val CMD_SLOW = 0x13
    }
}
