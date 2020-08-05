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

data class ReceivedUnits(val distanceCm: Int, val dhtTemperatureC: Int, val d18b20TemperatureC: Int)

class MessageProcessor(private val bluetoothSocket: BluetoothSocket?,
                       private val onDataReceived: (ReceivedUnits) -> Unit) {

    private var fullMessageCentimeters = Int.MAX_VALUE
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 200)
    private var shouldReceiveData = AtomicBoolean(true)
    private var threadShouldRun = AtomicBoolean(true)

    private var partialMessage = ""

    fun startTone() {
        Log.d("ABAB", "bt socket is: $bluetoothSocket")
        toneGenerator.startTone(ToneGenerator.TONE_SUP_DIAL)
    }

    fun stopTone() {
        toneGenerator.stopTone()
    }

    fun startReceivingData() {
        threadShouldRun.set(true)
        startSpeakerThread()
        shouldReceiveData.set(true)
        val receiveBuffer = ByteArray(1024)
        bluetoothSocket?.let { socket ->
            CoroutineScope(Dispatchers.IO).launch {
                withContext(Dispatchers.IO) {
                    while (shouldReceiveData.get()) {
                        Log.d("ABAB", "Trying to receive data...")
                        val receivedMessage = try {
                            val amountOfReceivedBytes = socket.inputStream.read(receiveBuffer)
                            String(receiveBuffer, 0, amountOfReceivedBytes)
                        } catch (e: IOException) {
                            Log.e("ABAB", "error", e)
                            null
                        }

                        receivedMessage?.let { rcvMsg ->
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
        return ReceivedUnits(splitMessage[0].toInt(), splitMessage[1].toInt(), splitMessage[2].toInt())
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
            while (threadShouldRun.get()) {
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
        threadShouldRun.set(false)
        shouldReceiveData.set(false)
    }

    fun destroy() {
        shouldReceiveData.set(false)
        threadShouldRun.set(false)
    }
}