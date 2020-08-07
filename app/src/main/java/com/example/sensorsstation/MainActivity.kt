package com.example.sensorsstation

import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean


class MainActivity : AppCompatActivity() {

    private val bluetoothManager = BluetoothManager()
    private var bluetoothSocket: BluetoothSocket? = null
    private var messageProcessor: MessageProcessor? = null
    private val isConnectingToBluetooth = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!checkBluetoothPermissions(this)) {
            requestBluetoothPermissions(this)
        } else {
            val bluetoothManager = BluetoothManager()
            bluetoothManager.getPairedDevices()
        }

        connectToHC06Button.setOnClickListener { tryToConnectToBluetooth() }

        closeSocketButton.setOnClickListener { closeBluetoothSocket() }

        startReceivingDataButton.setOnClickListener { messageProcessor?.startReceivingData() }

        stopReceivingDataButton.setOnClickListener { messageProcessor?.stopReceivingData() }

        startBeepButton.setOnClickListener { messageProcessor?.startTone() }

        stopBeepButton.setOnClickListener { messageProcessor?.stopTone() }
    }

    private fun tryToConnectToBluetooth() {
        if (isConnectingToBluetooth.compareAndSet(false, true)) {
            connectingToBtInfoGroup.isVisible = true
            CoroutineScope(Dispatchers.IO).launch {
                connectToUltraHC06()?.let { socket ->
                    bluetoothSocket = socket
                    messageProcessor =
                        MessageProcessor(socket, ::onDataReceived)
                }
                isConnectingToBluetooth.set(false)
                runOnUiThread {
                    connectingToBtProgressBar.isVisible = false
                    if (bluetoothSocket != null) {
                        onConnectionSuccessful()
                    } else {
                        onConnectingFailed()
                    }
                }
            }
        } else {
            Toast.makeText(this, "Already connecting...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onConnectionSuccessful() {
        connectingToBtStatusLabel.apply {
            setTextColor(Color.GREEN)
            text = "Connected"
        }
        Toast.makeText(this@MainActivity, "Connected", Toast.LENGTH_SHORT).show()
    }

    private fun onConnectingFailed() {
        connectingToBtStatusLabel.apply {
            setTextColor(Color.RED)
            text = "Connecting failed"
        }
        Toast.makeText(this@MainActivity, "Connecting failed", Toast.LENGTH_SHORT)
            .show()
    }

    fun onConnectionLost() {
        connectingToBtStatusLabel.apply {
            setTextColor(Color.RED)
            text = "Connection lost"
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        if (requestCode == REQUEST_BT_PERMISSIONS_CODE && grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            bluetoothManager.getPairedDevices()
        }
    }

    private suspend fun connectToUltraHC06(): BluetoothSocket? {
        bluetoothManager.apply {
            getUltraHC06Device()?.let { device ->
                val bluetoothSocket = connectToDevice(device)
                Log.d("ABAB", "bluetoothSocket: $bluetoothSocket")
                return bluetoothSocket
            }
        }
        Log.e("ABAB", "getUltraHC06Device returning null")
        return null
    }

    private fun onDataReceived(receivedUnits: ReceivedUnits) {
        runOnUiThread {
            distanceLabel.text =
                "hcsr04:\t ${receivedUnits.distanceCm} cm\n dht11:\t ${receivedUnits.dhtTemperatureC} °C\n d18b20:\t ${receivedUnits.d18b20TemperatureC} °C\n Solar:\t ${receivedUnits.solarPanelVoltage} V"
        }
    }

    private fun closeBluetoothSocket() {
        bluetoothSocket?.close()
        bluetoothSocket = null
        messageProcessor?.destroy()
        messageProcessor = null
    }
}