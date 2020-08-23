package com.example.sensorsstation

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean


data class ReceivedUnits(val distanceCm: Int, val dhtTemperatureC: Int, val d18b20TemperatureC: Int,
                         val solarPanelVoltage: Float)

class MessageProcessor {

    private val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 200)
    private val speakerThreadShouldRun = AtomicBoolean(true)
    private var partialMessage = ""
    private var fullMessageCentimeters = Int.MAX_VALUE

    fun startTone() {
        toneGenerator.startTone(ToneGenerator.TONE_SUP_DIAL)
    }

    fun stopTone() {
        toneGenerator.stopTone()
    }

    fun processCleanMessage(cleanMessage: String, delimiter: String = "/"): ReceivedUnits {
        val splitMessage = cleanMessage.split(delimiter)
        for (msg in splitMessage) {
            Log.d(tag, "..$msg..")
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
    fun processReceivedMessage(receivedMessage: String): String? {
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

    fun startSpeakerThread() {
        speakerThreadShouldRun.set(true)
        Thread {
            while (speakerThreadShouldRun.get()) {
                processMessageToSpeaker()
            }
            toneGenerator.stopTone()
            Log.d(tag, "Thread is exiting")
        }.start()
    }

    private fun processMessageToSpeaker() {
        when {
            fullMessageCentimeters <= 5 -> toneGenerator.startTone(ToneGenerator.TONE_SUP_DIAL)
            fullMessageCentimeters > 70 -> toneGenerator.stopTone()
            else -> {
                var calculatedDelay = -1.0f
                if ((fullMessageCentimeters > 5) && (fullMessageCentimeters <= 15)) {
                    calculatedDelay = 1.0f / 9f
                } else if ((fullMessageCentimeters > 15) && (fullMessageCentimeters <= 30)) {
                    calculatedDelay = 1.0f / 6f
                } else if ((fullMessageCentimeters > 30) && (fullMessageCentimeters <= 40)) {
                    calculatedDelay = 1.0f / 4f
                } else if ((fullMessageCentimeters > 40) && (fullMessageCentimeters <= 70)) {
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
        speakerThreadShouldRun.set(false)
    }

    fun destroy() {
        stopReceivingData()
    }

    fun setNewDistance(distance: Int) {
        fullMessageCentimeters = distance
    }
}