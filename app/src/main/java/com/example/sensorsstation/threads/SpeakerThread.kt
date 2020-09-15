package com.example.sensorsstation.threads

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import com.example.sensorsstation.tag

class SpeakerThread : Thread() {

    private var fullMessageCentimeters = Int.MAX_VALUE
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 200)

    override fun run() {
        super.run()
        // fixme is `while` necessary?
        while (true) {
            processMessageToSpeaker()
        }
    }

    override fun destroy() {
        toneGenerator.stopTone()
        Log.d(tag, "Thread is exiting")
    }

    fun setFullMessageCentimeters(centimeters: Int) {
        synchronized(fullMessageCentimeters) {
            fullMessageCentimeters = centimeters
        }
    }

    fun startTone() {
        toneGenerator.startTone(ToneGenerator.TONE_SUP_DIAL)
    }

    fun stopTone() {
        toneGenerator.stopTone()
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
                    sleep((calculatedDelay * 500).toLong())
                    toneGenerator.stopTone()
                    sleep((calculatedDelay * 500).toLong())
                }
            }
        }
    }
}