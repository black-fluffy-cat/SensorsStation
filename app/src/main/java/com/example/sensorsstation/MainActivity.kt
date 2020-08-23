package com.example.sensorsstation

import android.bluetooth.BluetoothSocket
import android.graphics.Color
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.sensorsstation.bluetooth.BluetoothManager
import com.example.sensorsstation.bluetooth.ConnectionManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {

    private var bluetoothSocket: BluetoothSocket? = null
    private var messageProcessor: MessageProcessor? = null
    private val isConnectingToBluetooth = AtomicBoolean(false)
    private val connectionManager =
        ConnectionManager(::onDataReceived,
            ::onConnectionLost)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!checkBluetoothPermissions(this)) {
            requestBluetoothPermissions(this)
        } else {
            val bluetoothManager =
                BluetoothManager()
            bluetoothManager.getPairedDevices()
        }

        connectToHC06Button.setOnClickListener { tryToConnectToBluetooth() }

        closeSocketButton.setOnClickListener { closeBluetoothSocket() }

        startReceivingDataButton.setOnClickListener { connectionManager.startReceivingData() }

        stopReceivingDataButton.setOnClickListener { connectionManager.stopReceivingData() }

        startBeepButton.setOnClickListener { messageProcessor?.startTone() }

        stopBeepButton.setOnClickListener { messageProcessor?.stopTone() }

        ledBrightnessControlSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, p2: Boolean) {
                connectionManager.sendIntegerThroughBluetooth(progress)
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
                // empty
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                // empty
            }
        })
    }

    private fun tryToConnectToBluetooth() {
        if (isConnectingToBluetooth.compareAndSet(false, true)) {
            connectingToBtInfoGroup.isVisible = true
            CoroutineScope(Dispatchers.IO).launch {
                connectionManager.tryToConnect(::afterConnectionAttempt)
            }
        } else {
            Toast.makeText(this, "Already connecting...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun afterConnectionAttempt(btSocket: BluetoothSocket?) {
        bluetoothSocket = btSocket
        runOnUiThread {
            isConnectingToBluetooth.set(false)
            connectingToBtProgressBar.isVisible = false
            btSocket?.apply { onConnectionSuccessful() } ?: onConnectingFailed()
        }
    }

    private fun onConnectionSuccessful() {
        messageProcessor = MessageProcessor()
        connectingToBtStatusLabel.apply {
            setTextColor(Color.GREEN)
            text = "Connected"
        }
        connectingToBtProgressBar.isVisible = false
        Toast.makeText(this@MainActivity, "Connected", Toast.LENGTH_SHORT).show()
    }

    private fun onConnectingFailed() {
        connectingToBtStatusLabel.apply {
            setTextColor(Color.RED)
            text = "Connecting failed"
        }
        connectingToBtProgressBar.isVisible = false
        Toast.makeText(this@MainActivity, "Connecting failed", Toast.LENGTH_SHORT)
            .show()
    }

    private fun onConnectionLost() {
        connectingToBtStatusLabel.apply {
            setTextColor(Color.RED)
            text = "Connection lost"
        }
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