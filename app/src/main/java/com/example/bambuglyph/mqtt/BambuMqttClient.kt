package com.example.bambuglyph.mqtt

import android.util.Log
import com.example.bambuglyph.data.PrinterRepository
import com.example.bambuglyph.model.PrintState
import com.example.bambuglyph.model.PrinterConfig
import com.example.bambuglyph.model.PrinterStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.json.JSONObject
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class BambuMqttClient(
    private val config: PrinterConfig,
    private val scope: CoroutineScope
) {
    private val tag = "BambuMqttClient"
    private var mqttClient: MqttClient? = null
    private var pushJob: Job? = null
    private var isConnecting = false

    fun connect() {
        if (isConnecting || mqttClient?.isConnected == true) return
        isConnecting = true

        scope.launch(Dispatchers.IO) {
            try {
                val brokerUrl = "ssl://${config.ipAddress}:${config.port}"
                val clientId = "bambu_glyph_${config.serialNumber.takeLast(4)}_${System.currentTimeMillis() % 10000}"
                Log.i(tag, "Connecting to Bambu printer at $brokerUrl...")

                PrinterRepository.updateStatus(
                    PrinterRepository.status.value.copy(
                        isConnected = false,
                        statusMessage = "Connecting to ${config.ipAddress}..."
                    )
                )

                val client = MqttClient(brokerUrl, clientId, MemoryPersistence())
                mqttClient = client

                val options = MqttConnectOptions().apply {
                    isCleanSession = true
                    connectionTimeout = 10
                    keepAliveInterval = 30
                    userName = "bblp"
                    password = config.accessCode.toCharArray()
                    socketFactory = createTrustAllSocketFactory()
                    setSSLHostnameVerifier { _, _ -> true }
                    isAutomaticReconnect = true
                }

                client.setCallback(object : MqttCallbackExtended {
                    override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                        Log.i(tag, "Connected to Bambu MQTT (reconnect=$reconnect)")
                        PrinterRepository.updateStatus(
                            PrinterRepository.status.value.copy(
                                isConnected = true,
                                statusMessage = "Connected to ${config.serialNumber}"
                            )
                        )
                        subscribeAndRequestStatus()
                    }

                    override fun connectionLost(cause: Throwable?) {
                        Log.w(tag, "Connection lost: ${cause?.message}")
                        PrinterRepository.updateStatus(
                            PrinterRepository.status.value.copy(
                                isConnected = false,
                                statusMessage = "Connection lost: ${cause?.localizedMessage ?: "Reconnecting..."}"
                            )
                        )
                    }

                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        if (message != null) {
                            val payload = String(message.payload)
                            parseReportMessage(payload)
                        }
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {}
                })

                client.connect(options)
                isConnecting = false

            } catch (e: Exception) {
                Log.e(tag, "Failed to connect to Bambu printer: ${e.message}", e)
                isConnecting = false
                PrinterRepository.updateStatus(
                    PrinterRepository.status.value.copy(
                        isConnected = false,
                        statusMessage = "Connection failed: ${e.localizedMessage}"
                    )
                )
            }
        }
    }

    private fun subscribeAndRequestStatus() {
        val client = mqttClient ?: return
        val reportTopic = "device/${config.serialNumber}/report"
        val requestTopic = "device/${config.serialNumber}/request"

        try {
            client.subscribe(reportTopic, 0)
            Log.i(tag, "Subscribed to $reportTopic")

            // Send pushall request immediately
            sendPushall(requestTopic)

            // Schedule periodic pushall request every 25 seconds
            pushJob?.cancel()
            pushJob = scope.launch(Dispatchers.IO) {
                while (isActive && client.isConnected) {
                    delay(25_000)
                    sendPushall(requestTopic)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error during subscription: ${e.message}", e)
        }
    }

    private fun sendPushall(requestTopic: String) {
        try {
            val pushPayload = JSONObject().apply {
                put("pushing", JSONObject().apply {
                    put("sequence_id", "0")
                    put("command", "pushall")
                })
            }.toString()

            mqttClient?.publish(requestTopic, MqttMessage(pushPayload.toByteArray()).apply { qos = 0 })
            Log.d(tag, "Sent pushall command to $requestTopic")
        } catch (e: Exception) {
            Log.w(tag, "Failed to send pushall: ${e.message}")
        }
    }

    private fun parseReportMessage(jsonString: String) {
        try {
            val root = JSONObject(jsonString)
            if (!root.has("print")) return

            val printObj = root.getJSONObject("print")
            val current = PrinterRepository.status.value

            val gcodeState = if (printObj.has("gcode_state")) {
                printObj.optString("gcode_state", current.gcodeState)
            } else current.gcodeState

            val printState = mapGcodeStateToPrintState(gcodeState)

            val remainingMinutes = if (printObj.has("mc_remaining_time")) {
                printObj.optInt("mc_remaining_time", current.remainingMinutes)
            } else current.remainingMinutes

            val percent = if (printObj.has("mc_percent")) {
                printObj.optInt("mc_percent", current.percent)
            } else current.percent

            val nozzleTemp = if (printObj.has("nozzle_temper")) {
                printObj.optDouble("nozzle_temper", current.nozzleTemp.toDouble()).toFloat()
            } else current.nozzleTemp

            val nozzleTarget = if (printObj.has("nozzle_target_temper")) {
                printObj.optDouble("nozzle_target_temper", current.nozzleTargetTemp.toDouble()).toFloat()
            } else current.nozzleTargetTemp

            val bedTemp = if (printObj.has("bed_temper")) {
                printObj.optDouble("bed_temper", current.bedTemp.toDouble()).toFloat()
            } else current.bedTemp

            val bedTarget = if (printObj.has("bed_target_temper")) {
                printObj.optDouble("bed_target_temper", current.bedTargetTemp.toDouble()).toFloat()
            } else current.bedTargetTemp

            val subtask = if (printObj.has("subtask_name")) {
                printObj.optString("subtask_name", current.subtaskName)
            } else current.subtaskName

            val updated = current.copy(
                isConnected = true,
                isMockMode = false,
                gcodeState = gcodeState,
                printState = printState,
                remainingMinutes = remainingMinutes,
                percent = percent,
                nozzleTemp = nozzleTemp,
                nozzleTargetTemp = nozzleTarget,
                bedTemp = bedTemp,
                bedTargetTemp = bedTarget,
                subtaskName = subtask,
                lastUpdated = System.currentTimeMillis(),
                statusMessage = "$gcodeState • ${remainingMinutes}m left ($percent%)"
            )

            PrinterRepository.updateStatus(updated)

        } catch (e: Exception) {
            Log.e(tag, "Error parsing MQTT report JSON: ${e.message}", e)
        }
    }

    private fun mapGcodeStateToPrintState(gcodeState: String): PrintState {
        return when (gcodeState.uppercase()) {
            "PREPARE" -> PrintState.PREHEAT
            "RUNNING" -> PrintState.PRINTING
            "PAUSE" -> PrintState.PAUSED
            "FINISH" -> PrintState.COMPLETED
            "FAILED" -> PrintState.FAILED
            "IDLE" -> PrintState.IDLE
            else -> PrintState.IDLE
        }
    }

    fun disconnect() {
        pushJob?.cancel()
        pushJob = null
        scope.launch(Dispatchers.IO) {
            try {
                if (mqttClient?.isConnected == true) {
                    mqttClient?.disconnect()
                }
                mqttClient?.close()
            } catch (e: Exception) {
                Log.w(tag, "Error disconnecting MQTT: ${e.message}")
            }
            mqttClient = null
            isConnecting = false
            PrinterRepository.updateStatus(
                PrinterRepository.status.value.copy(
                    isConnected = false,
                    statusMessage = "Disconnected"
                )
            )
        }
    }

    /**
     * Bambu 3D printers use self-signed TLS certificates for local LAN mode.
     * We create an SSLSocketFactory that trusts the printer certificate.
     */
    private fun createTrustAllSocketFactory(): javax.net.ssl.SSLSocketFactory {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("TLSv1.2")
        sslContext.init(null, trustAllCerts, SecureRandom())
        return sslContext.socketFactory
    }
}
