package com.example.sensorsstation

import android.bluetooth.BluetoothSocket
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
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
    private val connectionManager = ConnectionManager(::onDataReceived, ::onConnectionLost)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!checkBluetoothPermissions(this)) {
            requestBluetoothPermissions(this)
        } else {
            BluetoothManager().getPairedDevices().forEach {
                Log.d(tag, "Paired device: ${it.name}, ${it.address}")
            }
        }

        connectToHC06Button.setOnClickListener { tryToConnectToBluetooth() }

        closeSocketButton.setOnClickListener { closeBluetoothSocket() }

        startReceivingDataButton.setOnClickListener { connectionManager.startReceivingData() }

        stopReceivingDataButton.setOnClickListener { connectionManager.stopReceivingData() }

        startBeepButton.setOnClickListener { messageProcessor?.startTone() }

        stopBeepButton.setOnClickListener { messageProcessor?.stopTone() }

        ledBrightnessControlSeekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, p2: Boolean) {
                connectionManager.sendIntegerThroughBluetooth(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // empty
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
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
            showShortToast(this, getString(R.string.already_connecting))
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
        val messageText = getString(R.string.connected)
        connectingToBtStatusLabel.apply {
            setTextColor(Color.GREEN)
            text = messageText
        }
        connectingToBtProgressBar.isVisible = false
        showShortToast(this, messageText)
    }

    private fun onConnectingFailed() {
        val messageText = getString(R.string.connecting_failed)
        connectingToBtStatusLabel.apply {
            setTextColor(Color.RED)
            text = messageText
        }
        connectingToBtProgressBar.isVisible = false
        showShortToast(this, messageText)
    }

    private fun onConnectionLost() {
        val messageText = getString(R.string.connection_lost)
        connectingToBtStatusLabel.apply {
            setTextColor(Color.RED)
            text = messageText
        }
        showShortToast(this, messageText)
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