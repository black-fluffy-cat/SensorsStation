package com.example.sensorsstation

import android.util.Log
import com.example.sensorsstation.threads.SpeakerThread


data class ReceivedUnits(val distanceCm: Int, val dhtTemperatureC: Int, val d18b20TemperatureC: Int,
                         val solarPanelVoltage: Float)

class MessageProcessor {

    private var partialMessage = ""
    private val speakerThread = SpeakerThread()

    fun startTone() {
        speakerThread.startTone()
    }

    fun stopTone() {
        speakerThread.stopTone()
    }

    fun getUnitsFromCleanMessage(cleanMessage: String, delimiter: String = "/"): ReceivedUnits {
        val splitMessage = cleanMessage.split(delimiter)
        for (msg in splitMessage) {
            Log.d(tag, "..$msg..")
        }
        // Hotfix, first position may be empty sometimes, other positions also but less often
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
    // fixme Possible bug and NPE exception is described below and with more details on my paper
    // This works only if last char is #, we can always receive messages split in half
    // and # will never be last char
    // NPE occurs if there will come partial message with # on the end, i.e. "/1/1#"
    fun processReceivedMessage(receivedMessage: String): String? {
        return if (receivedMessage.last() == '#') {
            var fullMessage = (partialMessage + receivedMessage)
            if (fullMessage.count { it == '#' } > 1) {
                var positionOfAlmostLastHash = 0
                for (i in fullMessage.length - 2 downTo 0) {
                    if (fullMessage[i] == '#') {
                        positionOfAlmostLastHash = i
                        break
                    }
                }
                fullMessage = fullMessage.substring(positionOfAlmostLastHash, fullMessage.length)
            }
            val cleanLastFullMessage = fullMessage.replace("#", "")
            partialMessage = ""
            cleanLastFullMessage
        } else {
            partialMessage = receivedMessage
            null
        }
    }

    fun startSpeakerThread() {
        // fixme Can interrupted thread start again?
        speakerThread.start()
    }


    fun stopReceivingData() {
        speakerThread.interrupt()
    }

    fun destroy() {
        stopReceivingData()
    }

    fun setNewDistance(distance: Int) {
        speakerThread.setFullMessageCentimeters(distance)
    }
}