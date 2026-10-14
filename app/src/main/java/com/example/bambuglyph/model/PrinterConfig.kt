package com.example.bambuglyph.model

data class PrinterConfig(
    val ipAddress: String = "192.168.1.100",
    val port: Int = 8883,
    val serialNumber: String = "01P00A000000000",
    val accessCode: String = "12345678",
    val deviceType: GlyphDeviceType = GlyphDeviceType.AUTO_DETECT,
    val mockMode: Boolean = true
) {
    val isValid: Boolean
        get() = mockMode || (ipAddress.isNotBlank() && serialNumber.isNotBlank() && accessCode.isNotBlank())
}

data class PrinterStatus(
    val isConnected: Boolean = false,
    val isMockMode: Boolean = false,
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
