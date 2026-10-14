package com.example.bambuglyph.model

enum class GlyphDeviceType(val displayName: String, val matrixSize: Int, val deviceIdentifier: String) {
    PHONE_3("Nothing Phone (3)", 25, "DEVICE_23112"),
    PHONE_4A_PRO("Nothing Phone (4a) Pro", 13, "DEVICE_25111p"),
    AUTO_DETECT("Auto Detect", 25, "")
}

enum class PrintState(val displayLabel: String) {
    IDLE("Idle"),
    PREHEAT("Preheat (PH)"),
    PRINTING("Printing"),
    PAUSED("Paused"),
    COMPLETED("Completed"),
    FAILED("Failed")
}
