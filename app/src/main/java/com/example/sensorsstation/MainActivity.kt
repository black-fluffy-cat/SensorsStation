package com.example.sensorsstation

import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private val bluetoothManager = BluetoothManager()
    private var bluetoothSocket: BluetoothSocket? = null
    private var messageProcessor: MessageProcessor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!checkBluetoothPermissions(this)) {
            requestBluetoothPermissions(this)
        } else {
            val bluetoothManager = BluetoothManager()
            bluetoothManager.getPairedDevices()
        }

        connectToHC06Button.setOnClickListener {
            bluetoothSocket = connectToUltraHC06()
            messageProcessor = MessageProcessor(bluetoothSocket, ::onDataReceived)
        }

        closeSocketButton.setOnClickListener { closeBluetoothSocket() }

        startReceivingDataButton.setOnClickListener {
            messageProcessor?.startReceivingData()
        }

        stopReceivingDataButton.setOnClickListener {
            messageProcessor?.stopReceivingData()
        }

        startBeepButton.setOnClickListener {
            messageProcessor?.startTone()
        }
        stopBeepButton.setOnClickListener { messageProcessor?.stopTone() }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        if (requestCode == REQUEST_BT_PERMISSIONS_CODE && grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            bluetoothManager.getPairedDevices()
        }
    }

    private fun connectToUltraHC06(): BluetoothSocket? {
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
            distanceLabel.text = "${receivedUnits.distanceCm} cm\n ${receivedUnits.temperatureC} °C"
        }
    }

    private fun closeBluetoothSocket() {
        bluetoothSocket?.close()
        bluetoothSocket = null
        messageProcessor?.destroy()
        messageProcessor = null
    }
}