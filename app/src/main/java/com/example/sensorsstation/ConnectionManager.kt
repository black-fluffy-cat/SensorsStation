package com.example.sensorsstation

import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

const val maxNumberOfContinuousIOErrors = 3

class ConnectionManager(private val onDataReceived: (ReceivedUnits) -> Unit,
                        private val onConnectionLost: () -> Unit) {

    private val bluetoothManager = BluetoothManager()
    private var bluetoothSocket: BluetoothSocket? = null
    private val isReceivingData = AtomicBoolean(false)
    private val shouldReceiveData = AtomicBoolean(true)
    private var numberOfContinuousIOErrors = 0
    private val messageProcessor = MessageProcessor()

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


    fun startReceivingData() {
        bluetoothSocket?.let { socket ->
            if (isReceivingData.compareAndSet(false, true)) {
                messageProcessor.startSpeakerThread()
                shouldReceiveData.set(true)
                startReceivingCoroutine(socket)
            }
        }
    }

    private fun startReceivingCoroutine(btSocket: BluetoothSocket) {
        val receiveBuffer = ByteArray(1024)
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.IO) {
                while (shouldReceiveData.get()) {
                    Log.d("ABAB", "Trying to receive data...")
                    val receivedMessage = receiveMessage(btSocket, receiveBuffer)
                    if (numberOfContinuousIOErrors == maxNumberOfContinuousIOErrors) {
                        break
                    }

                    receivedMessage?.let { rcvMsg ->
                        numberOfContinuousIOErrors = 0
                        Log.d("ABAB", "message: $rcvMsg, length: " + "${rcvMsg.length}")
                        val cleanFullMessage = messageProcessor.processReceivedMessage(rcvMsg)
                        cleanFullMessage?.let { message ->
                            val receivedUnits = messageProcessor.processCleanMessage(message)
                            messageProcessor.setNewDistance(receivedUnits.distanceCm)
                            onDataReceived(receivedUnits)
                        }
                    }
                }
            }
        }
    }

    fun stopReceivingData() {
        shouldReceiveData.set(false)
        isReceivingData.set(false)
        messageProcessor.stopReceivingData()
    }

    private fun receiveMessage(btSocket: BluetoothSocket, receiveBuffer: ByteArray) = try {
        val amountOfReceivedBytes = btSocket.inputStream.read(receiveBuffer)
        String(receiveBuffer, 0, amountOfReceivedBytes)
    } catch (e: IOException) {
        Log.e("ABAB", "error", e)
        if (++numberOfContinuousIOErrors == maxNumberOfContinuousIOErrors) {
            onConnectionLost()
        }
        null
    }

    fun sendInteger(number: Int) {
        bluetoothSocket?.outputStream?.write(number)
    }
}