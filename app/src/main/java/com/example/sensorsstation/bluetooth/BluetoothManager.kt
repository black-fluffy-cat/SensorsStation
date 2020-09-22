package com.example.sensorsstation.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.example.sensorsstation.tag
import java.io.IOException
import java.util.UUID

const val ultraHC06Name = "ultraHC06"

class BluetoothManager {

    private val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    fun connectToDevice(device: BluetoothDevice): BluetoothSocket? {
        val bluetoothSocket = createBluetoothSocket(device)
        mBluetoothAdapter.cancelDiscovery()
        return try {
            bluetoothSocket?.connect()
            Log.d(tag, "connectToDevice: after connect")
            bluetoothSocket
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun createBluetoothSocket(device: BluetoothDevice): BluetoothSocket? {
        return try {
            device.createRfcommSocketToServiceRecord(UUID.fromString(device.uuids[0].toString()))
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun getPairedDevices(): Set<BluetoothDevice> = mBluetoothAdapter.bondedDevices

    fun getUltraHC06Device() = getPairedDevices().find { it.name == ultraHC06Name }
}