package com.example.sensorsstation

import android.bluetooth.BluetoothSocket
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

const val maxNumberOfContinuousIOErrors = 3

data class ReceivedUnits(val distanceCm: Int, val dhtTemperatureC: Int, val d18b20TemperatureC: Int,
                         val solarPanelVoltage: Float)

class MessageProcessor(private val bluetoothSocket: BluetoothSocket,
                       private val onDataReceived: (ReceivedUnits) -> Unit,
                       private val onConnectionLost: () -> Unit) {

    private var fullMessageCentimeters = Int.MAX_VALUE
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 200)
    private val shouldReceiveData = AtomicBoolean(true)
    private val isReceivingData = AtomicBoolean(false)
    private var numberOfContinuousIOErrors = 0

    private var partialMessage = ""

    fun startTone() {
        Log.d("ABAB", "bt socket is: $bluetoothSocket")
        toneGenerator.startTone(ToneGenerator.TONE_SUP_DIAL)
    }

    fun stopTone() {
        toneGenerator.stopTone()
    }

    fun startReceivingData() {
        if (isReceivingData.compareAndSet(false, true)) {
            startSpeakerThread()
            shouldReceiveData.set(true)
            val receiveBuffer = ByteArray(1024)
            CoroutineScope(Dispatchers.IO).launch {
                withContext(Dispatchers.IO) {
                    while (shouldReceiveData.get()) {
                        Log.d("ABAB", "Trying to receive data...")
                        val receivedMessage = try {
                            val amountOfReceivedBytes =
                                bluetoothSocket.inputStream.read(receiveBuffer)
                            String(receiveBuffer, 0, amountOfReceivedBytes)
                        } catch (e: IOException) {
                            Log.e("ABAB", "error", e)
                            if (++numberOfContinuousIOErrors == maxNumberOfContinuousIOErrors) {
                                onConnectionLost()
                                break
                            }
                            null
                        }

                        receivedMessage?.let { rcvMsg ->
                            numberOfContinuousIOErrors = 0
                            Log.d("ABAB", "message: $rcvMsg, length: " + "${rcvMsg.length}")
                            val cleanFullMessage = processReceivedMessage(receivedMessage)
                            cleanFullMessage?.let { message ->
                                val receivedUnits = processCleanMessage(message)
                                fullMessageCentimeters = receivedUnits.distanceCm
                                onDataReceived(receivedUnits)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun processCleanMessage(cleanMessage: String, delimiter: String = "/"): ReceivedUnits {
        val splitMessage = cleanMessage.split(delimiter)
        for (msg in splitMessage) {
            Log.d("ABAB", "..$msg..")
        }
        // Hotfix
        val distanceCm = if (splitMessage[0].isNotEmpty()) {
            splitMessage[0].toInt()
        } else {
            0
        }
        return ReceivedUnits(distanceCm, splitMessage[1].toInt(),
            splitMessage[2].toInt(), splitMessage[3].toFloat())
    }

    /***
     * @return message if message is complete, null if received message is partial
     *
     */
    private fun processReceivedMessage(receivedMessage: String): String? {
        if (receivedMessage[receivedMessage.length - 1].toString() == "#") {
            var fullMessage = (partialMessage + receivedMessage)
            if (fullMessage.count { it.toString() == "#" } > 1) {
                val fullMessageLength = fullMessage.length
                var positionOfAlmostLastHash = 0
                for (i in fullMessageLength - 2 downTo 0) {
                    if (fullMessage[i].toString() == "#") {
                        positionOfAlmostLastHash = i
                        break
                    }
                }
                fullMessage = fullMessage.substring(positionOfAlmostLastHash,
                    fullMessageLength)
            }
            val cleanLastFullMessage = fullMessage.replace("#", "")
            partialMessage = ""
            return cleanLastFullMessage
        } else {
            partialMessage = receivedMessage
            return null
        }
    }

    private fun startSpeakerThread() {
        Thread {
            while (isReceivingData.get()) {
                processMessageToSpeaker()
//                Log.d("ABAB", "threadShouldRun is: ${threadShouldRun.get()}")
            }
            toneGenerator.stopTone()
            Log.d("ABAB", "Thread is exiting")
        }.start()
    }

    private fun processMessageToSpeaker() {
        //TODO try catch
        val centimeters = fullMessageCentimeters

        when {
            centimeters <= 5 -> toneGenerator.startTone(ToneGenerator.TONE_SUP_DIAL)
            centimeters > 70 -> toneGenerator.stopTone()
            else -> {
                var calculatedDelay = -1.0f
                if ((centimeters > 5) && (centimeters <= 15)) {
                    calculatedDelay = 1.0f / 9f
                } else if ((centimeters > 15) && (centimeters <= 30)) {
                    calculatedDelay = 1.0f / 6f
                } else if ((centimeters > 30) && (centimeters <= 40)) {
                    calculatedDelay = 1.0f / 4f
                } else if ((centimeters > 40) && (centimeters <= 70)) {
                    calculatedDelay = 1.0f / 2f
                }
                if (calculatedDelay != -1.0f) {
                    toneGenerator.startTone(ToneGenerator.TONE_SUP_DIAL)
                    Thread.sleep((calculatedDelay * 500).toLong())
                    toneGenerator.stopTone()
                    Thread.sleep((calculatedDelay * 500).toLong())
                }
            }
        }
    }

    fun stopReceivingData() {
        shouldReceiveData.set(false)
        isReceivingData.set(false)
    }

    fun destroy() {
        stopReceivingData()
    }
}