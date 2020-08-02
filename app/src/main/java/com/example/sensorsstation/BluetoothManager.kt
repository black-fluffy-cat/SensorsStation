package com.example.sensorsstation

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException
import java.util.*


class BluetoothManager {

    private val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    fun connectToDevice(device: BluetoothDevice): BluetoothSocket? {
        val bluetoothSocket = createBluetoothSocket(device)
        mBluetoothAdapter.cancelDiscovery()
        return try {
            bluetoothSocket?.connect()
            Log.d("ABAB", "connectToDevice: after connect")
            bluetoothSocket
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun createBluetoothSocket(device: BluetoothDevice): BluetoothSocket? {
        var bluetoothSocket: BluetoothSocket? = null
        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID.fromString(device.uuids[0].toString()))
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return bluetoothSocket
    }

    fun getPairedDevices(): Set<BluetoothDevice> {
        val pairedDevices: Set<BluetoothDevice> = mBluetoothAdapter.bondedDevices
        if (pairedDevices.isNotEmpty()) {
            for (device in pairedDevices) {
                Log.d("ABAB", "Paired device: ${device.name}, ${device.address}")
            }
        }
        return pairedDevices
    }

    fun getUltraHC06Device() = getPairedDevices().find { it.name == "ultraHC06" }
}