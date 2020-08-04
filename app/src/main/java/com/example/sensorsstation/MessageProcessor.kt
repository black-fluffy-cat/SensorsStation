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

class MessageProcessor(private val bluetoothSocket: BluetoothSocket?,
                       private val onDataReceived: (String) -> Unit) {

    private var fullMessageCentimeters = Int.MAX_VALUE.toString()
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 200)
    private var shouldReceiveData = AtomicBoolean(true)
    private var threadShouldRun = AtomicBoolean(true)

    private val receiveBuffer = ByteArray(1024)
    private var amountOfReceivedBytes: Int = 0
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
//        startSpeakerThread()
        shouldReceiveData.set(true)
        bluetoothSocket?.let { socket ->
            CoroutineScope(Dispatchers.IO).launch {
                withContext(Dispatchers.IO) {


                    while (shouldReceiveData.get()) {
                        Log.d("ABAB", "Trying to receive data...")
                        val message = try {
                            amountOfReceivedBytes = socket.inputStream.read(receiveBuffer)
                            String(receiveBuffer, 0, amountOfReceivedBytes)
                        } catch (e: IOException) {
                            e.printStackTrace()
                            Log.e("ABAB", "error", e)
                            shouldReceiveData.set(false)
                            "IO Exception"
                        }
                        Log.d("ABAB",
                            "message: $message, length: ${message.length}, comparison: ${message[message.length - 1]} == #")
                        if (message[message.length - 1].toString() == "#") {
                            var fullMessage = (partialMessage + message)
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
                            fullMessage = fullMessage.replace("#", "")
                            fullMessageCentimeters = fullMessage
                            partialMessage = ""
                            onDataReceived(fullMessage)
                        } else {
                            partialMessage = message
                        }
                    }
                }
            }
        }
    }

    fun stopReceivingData() {
        threadShouldRun.set(false)
        shouldReceiveData.set(false)
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
        val centimeters = fullMessageCentimeters.toInt()

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

    fun destroy() {
        shouldReceiveData.set(false)
        threadShouldRun.set(false)
    }
}