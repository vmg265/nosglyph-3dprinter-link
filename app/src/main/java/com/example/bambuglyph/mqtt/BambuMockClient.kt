package com.example.bambuglyph.mqtt

import com.example.bambuglyph.data.PrinterRepository
import com.example.bambuglyph.model.PrintState
import com.example.bambuglyph.model.PrinterStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class BambuMockClient(private val scope: CoroutineScope) {
    private var simulationJob: Job? = null

    fun start() {
        setDemoState(PrintState.PREHEAT)
    }

    fun stop() {
        simulationJob?.cancel()
        simulationJob = null
        PrinterRepository.updateStatus(
            PrinterRepository.status.value.copy(
                isConnected = false,
                isMockMode = false,
                statusMessage = "Demo stopped"
            )
        )
    }

    fun setDemoState(state: PrintState) {
        val current = PrinterRepository.status.value
        val updated = when (state) {
            PrintState.PREHEAT -> {
                current.copy(
                    isConnected = true,
                    isMockMode = true,
                    gcodeState = "PREPARE",
                    printState = PrintState.PREHEAT,
                    remainingMinutes = 52,
                    percent = 5,
                    nozzleTemp = 210f,
                    nozzleTargetTemp = 220f,
                    bedTemp = 58f,
                    bedTargetTemp = 60f,
                    subtaskName = "benchy_pla.gcode",
                    lastUpdated = System.currentTimeMillis(),
                    statusMessage = "Demo: Preheating (PH)"
                )
            }
            PrintState.PRINTING -> {
                current.copy(
                    isConnected = true,
                    isMockMode = true,
                    gcodeState = "RUNNING",
                    printState = PrintState.PRINTING,
                    remainingMinutes = 45,
                    percent = 68,
                    nozzleTemp = 220f,
                    nozzleTargetTemp = 220f,
                    bedTemp = 60f,
                    bedTargetTemp = 60f,
                    subtaskName = "benchy_pla.gcode",
                    lastUpdated = System.currentTimeMillis(),
                    statusMessage = "Demo: Printing • 45m left (68%)"
                )
            }
            PrintState.PAUSED -> {
                current.copy(
                    isConnected = true,
                    isMockMode = true,
                    gcodeState = "PAUSE",
                    printState = PrintState.PAUSED,
                    remainingMinutes = 45,
                    percent = 68,
                    nozzleTemp = 200f,
                    nozzleTargetTemp = 220f,
                    bedTemp = 60f,
                    bedTargetTemp = 60f,
                    subtaskName = "benchy_pla.gcode",
                    lastUpdated = System.currentTimeMillis(),
                    statusMessage = "Demo: Paused (||)"
                )
            }
            PrintState.COMPLETED -> {
                current.copy(
                    isConnected = true,
                    isMockMode = true,
                    gcodeState = "FINISH",
                    printState = PrintState.COMPLETED,
                    remainingMinutes = 0,
                    percent = 100,
                    nozzleTemp = 42f,
                    nozzleTargetTemp = 0f,
                    bedTemp = 36f,
                    bedTargetTemp = 0f,
                    subtaskName = "benchy_pla.gcode",
                    lastUpdated = System.currentTimeMillis(),
                    statusMessage = "Demo: 100% Complete (Breathe Checkmark)"
                )
            }
            PrintState.IDLE -> {
                current.copy(
                    isConnected = true,
                    isMockMode = true,
                    gcodeState = "IDLE",
                    printState = PrintState.IDLE,
                    remainingMinutes = 0,
                    percent = 0,
                    nozzleTemp = 25f,
                    nozzleTargetTemp = 0f,
                    bedTemp = 24f,
                    bedTargetTemp = 0f,
                    subtaskName = "",
                    lastUpdated = System.currentTimeMillis(),
                    statusMessage = "Demo: Standby"
                )
            }
            PrintState.FAILED -> {
                current.copy(
                    isConnected = true,
                    isMockMode = true,
                    gcodeState = "FAILED",
                    printState = PrintState.FAILED,
                    remainingMinutes = 0,
                    percent = 32,
                    nozzleTemp = 180f,
                    nozzleTargetTemp = 0f,
                    bedTemp = 45f,
                    bedTargetTemp = 0f,
                    subtaskName = "benchy_pla.gcode",
                    lastUpdated = System.currentTimeMillis(),
                    statusMessage = "Demo: Print Failed"
                )
            }
        }
        PrinterRepository.updateStatus(updated)
    }

    fun startAutoDemoCycle() {
        simulationJob?.cancel()
        simulationJob = scope.launch(Dispatchers.Default) {
            val sequence = listOf(
                PrintState.PREHEAT,
                PrintState.PRINTING,
                PrintState.PAUSED,
                PrintState.COMPLETED,
                PrintState.IDLE
            )
            var index = 0
            while (isActive) {
                setDemoState(sequence[index % sequence.size])
                index++
                delay(8_000)
            }
        }
    }
}
