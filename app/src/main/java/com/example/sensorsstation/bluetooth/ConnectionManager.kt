package com.example.sensorsstation.bluetooth

import android.bluetooth.BluetoothSocket
import android.util.Log
import com.example.sensorsstation.MessageProcessor
import com.example.sensorsstation.ReceivedUnits
import com.example.sensorsstation.rest.SensorsData
import com.example.sensorsstation.rest.SensorsDataCall
import com.example.sensorsstation.tag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
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
    private var clearFullMessageCounter = 0

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
                Log.d(tag, "bluetoothSocket: $bluetoothSocket")
                return bluetoothSocket
            }
        }
        Log.e(tag, "getUltraHC06Device returning null")
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
                    Log.d(tag, "Trying to receive data...")
                    val receivedMessage = receiveMessage(btSocket, receiveBuffer)
                    if (numberOfContinuousIOErrors == maxNumberOfContinuousIOErrors) {
                        break
                    }

                    receivedMessage?.let { rcvMsg ->
                        numberOfContinuousIOErrors = 0
                        Log.d(tag, "message: $rcvMsg, length: " + "${rcvMsg.length}")
                        val cleanFullMessage = messageProcessor.processReceivedMessage(rcvMsg)
                        cleanFullMessage?.let { message ->
                            val receivedUnits = messageProcessor.processCleanMessage(message)
                            messageProcessor.setNewDistance(receivedUnits.distanceCm)
                            onCleanFullMessageReceived(receivedUnits)
                        }
                    }
                }
            }
        }
    }

    private fun onCleanFullMessageReceived(receivedUnits: ReceivedUnits) {
        onDataReceived(receivedUnits)
        if (++clearFullMessageCounter % 5 == 0) {
            clearFullMessageCounter = 0
            CoroutineScope(Dispatchers.IO).launch {
                SensorsDataCall.postSensorsData(SensorsData(
                    "hcsr04: ${receivedUnits.distanceCm} cm, dht11: ${receivedUnits.dhtTemperatureC} °C, d18b20: ${receivedUnits.d18b20TemperatureC} °C, Solar: ${receivedUnits.solarPanelVoltage} V"),
                    object : Callback<ResponseBody> {
                        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                            Log.e(tag, "Sending units to server failed", t)
                        }

                        override fun onResponse(call: Call<ResponseBody>,
                                                response: Response<ResponseBody>) {
                            Log.d(tag, "Sending to server successful, response: $response")
                        }
                    })
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
        Log.e(tag, "error", e)
        if (++numberOfContinuousIOErrors == maxNumberOfContinuousIOErrors) {
            onConnectionLost()
        }
        null
    }

    fun sendIntegerThroughBluetooth(number: Int) {
        bluetoothSocket?.outputStream?.write(number)
    }
}