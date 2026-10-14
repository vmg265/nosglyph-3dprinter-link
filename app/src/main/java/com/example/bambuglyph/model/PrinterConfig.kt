package com.example.bambuglyph.model

data class PrinterConfig(
    val ipAddress: String = "",
    val port: Int = 8883,
    val serialNumber: String = "",
    val accessCode: String = "",
    val deviceType: GlyphDeviceType = GlyphDeviceType.AUTO_DETECT
) {
    val isValid: Boolean
        get() = ipAddress.isNotBlank() && serialNumber.isNotBlank() && accessCode.isNotBlank()
}

data class PrinterStatus(
    val isConnected: Boolean = false,
    val gcodeState: String = "IDLE",
    val printState: PrintState = PrintState.IDLE,
    val remainingMinutes: Int = 0,
    val percent: Int = 0,
    val nozzleTemp: Float = 0f,
    val nozzleTargetTemp: Float = 0f,
    val bedTemp: Float = 0f,
    val bedTargetTemp: Float = 0f,
    val subtaskName: String = "",
    val lastUpdated: Long = 0L,
    val statusMessage: String = "Ready"
)
