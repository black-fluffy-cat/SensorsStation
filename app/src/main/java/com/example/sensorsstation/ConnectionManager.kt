package com.example.sensorsstation

import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ConnectionManager {

    private val bluetoothManager = BluetoothManager()
    private var bluetoothSocket: BluetoothSocket? = null

    fun tryToConnect(afterConnectionAttempt: (BluetoothSocket?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            connectToUltraHC06().let { socket ->
                bluetoothSocket = socket
                afterConnectionAttempt(socket)
            }
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
}