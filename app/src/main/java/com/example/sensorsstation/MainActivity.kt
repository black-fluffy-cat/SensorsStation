package com.example.sensorsstation

import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean


class MainActivity : AppCompatActivity() {

    private val bluetoothManager = BluetoothManager()
    private var bluetoothSocket: BluetoothSocket? = null
    private var shouldReceiveData: Boolean = true
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 200)
    private var threadShouldRun = AtomicBoolean(true)
    private var fullMessageCentimeters = Int.MAX_VALUE.toString()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!checkBluetoothPermissions(this)) {
            requestBluetoothPermissions(this)
        } else {
            val bluetoothManager = BluetoothManager()
            bluetoothManager.getPairedDevices()
        }

        connectToHC06Button.setOnClickListener { connectToUltraHC06() }
        closeSocketButton.setOnClickListener { closeBluetoothSocket() }
        startReceivingDataButton.setOnClickListener {
            threadShouldRun.set(true)
            Thread {
                while (threadShouldRun.get()) {
                    processMessageToSpeaker()
                    Log.d("ABAB", "threadShouldRun is: ${threadShouldRun.get()}")
                }
                toneGenerator.stopTone()
                Log.d("ABAB", "Thread is exiting")
            }.start()
            shouldReceiveData = true
            bluetoothSocket?.let { socket ->
                CoroutineScope(Dispatchers.IO).launch {
                    withContext(Dispatchers.IO) {
                        val receiveBuffer = ByteArray(1024)
                        var amountOfReceivedBytes: Int
                        var partialMessage = ""

                        while (shouldReceiveData) {
                            Log.d("ABAB", "Trying to receive data...")
                            val message = try {
                                amountOfReceivedBytes = socket.inputStream.read(receiveBuffer)
                                String(receiveBuffer, 0, amountOfReceivedBytes)
                            } catch (e: IOException) {
                                e.printStackTrace()
                                Log.e("ABAB", "error", e)
                                shouldReceiveData = false
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
                                runOnUiThread { distanceLabel.text = "$fullMessage cm" }
                            } else {
                                partialMessage = message
                            }
                        }
                    }
                }
            }
        }

        stopReceivingDataButton.setOnClickListener {
            threadShouldRun.set(false)
            shouldReceiveData = false
        }

        startBeepButton.setOnClickListener {
            toneGenerator.startTone(ToneGenerator.TONE_SUP_DIAL)
        }
        stopBeepButton.setOnClickListener { toneGenerator.stopTone() }
    }

    private fun processMessageToSpeaker() {
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        if (requestCode == REQUEST_BT_PERMISSIONS_CODE && grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            bluetoothManager.getPairedDevices()
        }
    }

    private fun connectToUltraHC06() {
        bluetoothManager.apply {
            getUltraHC06Device()?.let { device ->
                bluetoothSocket = connectToDevice(device)
                Log.d("ABAB", "bluetoothSocket: $bluetoothSocket")
            } ?: Log.e("ABAB", "getUltraHC06Device returned null")
        }
    }

    private fun closeBluetoothSocket() {
        bluetoothSocket?.close()
        bluetoothSocket = null
    }
}